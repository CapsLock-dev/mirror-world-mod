package capslab.mirrorworld.utils.chunkcopy;

import capslab.mirrorworld.MirrorWorld;
import capslab.mirrorworld.utils.playerdata.MirrorWorldManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ChunkCopyManager {
    static class ChunkCopyJob {
        ChunkCopyJob(UUID player, Queue<ChunkPos> chunks) {
            this.player = player;
            this.chunks = chunks;
        }
        UUID player;
        Queue<ChunkPos> chunks;
    }
    private static final Queue<ChunkCopyJob> pendingJobs = new ArrayDeque<>();
    private static ChunkCopyJob activeJob;

    public static void registerEventListeners() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ChunkCopyManager.processJob(server, 1);
        });
    }

    public static void processJob(MinecraftServer server, int chunkCount) {
        if (activeJob == null && pendingJobs.isEmpty()) return;
        if (activeJob == null) {
            activeJob = pendingJobs.poll();
        }
        ServerPlayer p = server.getPlayerList().getPlayer(activeJob.player);
        for (int i=0; i<chunkCount; ++i) {
            ChunkPos pos = activeJob.chunks.poll();
            if (pos == null) {
                if (p != null) p.sendSystemMessage(Component.literal("Chunk copying job finished"));
                activeJob = null;
                break;
            }
            boolean success = mirrorChunk(server, pos);
            if (!success && p != null) p.sendSystemMessage(Component.literal("Failed to copy one of the chunks"));
        }
    }

    public static boolean addJob(UUID playerUUID, Queue<ChunkPos> chunks) {
        if ((activeJob != null && activeJob.player.equals(playerUUID))
                || pendingJobs.stream().anyMatch(job -> job.player.equals(playerUUID)) ) {
            return false;
        }
        return pendingJobs.add(new ChunkCopyJob(playerUUID, chunks));
    }

    private static boolean mirrorChunk(MinecraftServer server, ChunkPos pos) {
        ServerLevel overworld = server.overworld();
        ServerLevel mirror_world = server.getLevel(MirrorWorldManager.MIRROR_DIMENSION_KEY);
        if (mirror_world == null) {
            MirrorWorld.LOGGER.error("MirrorWorld dimension doesn't exists");
            return false;
        }
        ChunkAccess sourceChunk = overworld.getChunk(pos.x, pos.z, ChunkStatus.FULL, true);
        ChunkAccess destinationChunk = mirror_world.getChunk(pos.x, pos.z, ChunkStatus.FULL, true);
        if (sourceChunk == null || destinationChunk == null) {
            MirrorWorld.LOGGER.error("Can't access one of the chunks");
            return false;
        }
        LevelChunkSection[] sourceSections = sourceChunk.getSections();
        LevelChunkSection[] destinationSections = destinationChunk.getSections();
        for (int i=0; i<sourceSections.length; ++i) {
            var source = sourceSections[i];
            var destination = destinationSections[i];
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        destination.setBlockState(x,y,z, source.getBlockState(x,y,z), true);
                    }
                }
            }
            destination.recalcBlockCounts();
        }

        RegistryAccess registryAccess = server.registryAccess();
        for (BlockPos blockPos : sourceChunk.getBlockEntitiesPos()) {
            BlockEntity sourceBe = sourceChunk.getBlockEntity(blockPos);
            if (sourceBe == null) continue;

            BlockState state = destinationChunk.getBlockState(blockPos);
            if (!(state.getBlock() instanceof EntityBlock entityBlock)) {
                continue;
            }

            BlockEntity newBe = entityBlock.newBlockEntity(blockPos, state);
            if (newBe == null) continue;

            TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registryAccess);
            sourceBe.saveWithFullMetadata(output);
            CompoundTag tag = output.buildResult();

            ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, registryAccess, tag);
            newBe.loadWithComponents(input);

            destinationChunk.setBlockEntity(newBe);
        }

        AABB chunkBounds = new AABB(
                pos.getMinBlockX(), destinationChunk.getMinY(), pos.getMinBlockZ(),
                pos.getMaxBlockX() + 1, destinationChunk.getMaxY(), pos.getMaxBlockZ() + 1
        );
        for (Entity sourceEntity : overworld.getEntities((Entity) null, chunkBounds, e -> !(e instanceof ServerPlayer))) {
            TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registryAccess);
            sourceEntity.save(output);
            CompoundTag tag = output.buildResult();

            Entity copy = EntityType.loadEntityRecursive(tag, mirror_world, EntitySpawnReason.COMMAND, entity -> entity);
            if (copy != null) {
                mirror_world.addFreshEntity(copy);
            }
        }

        ChunkMap chunkMap = mirror_world.getChunkSource().chunkMap;
        ThreadedLevelLightEngine lightEngine = (ThreadedLevelLightEngine) mirror_world.getLightEngine();
        CompletableFuture<ChunkAccess> lightingFuture = lightEngine.lightChunk(destinationChunk, false);
        lightingFuture.thenAcceptAsync(litChunk -> {
            List<ServerPlayer> viewers = chunkMap.getPlayers(pos, false);
            ClientboundLevelChunkWithLightPacket packet =
                    new ClientboundLevelChunkWithLightPacket((LevelChunk) destinationChunk, lightEngine, null, null);
            for (ServerPlayer player : viewers) {
                player.connection.send(packet);
            }
        });
        return true;
    }
}
