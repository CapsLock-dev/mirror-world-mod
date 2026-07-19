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

import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class ChunkCopyManager {
    static class ChunkCopyJob {
        ChunkCopyJob(UUID player, Queue<ChunkPos> chunks) {
            this.player = player;
            this.chunks = chunks;
        }
        UUID player;
        Queue<ChunkPos> chunks;
        CompletableFuture<Boolean> workInFlight = CompletableFuture.completedFuture(null);
    }
    private static final Queue<ChunkCopyJob> pendingJobs = new ArrayDeque<>();
    private static ChunkCopyJob activeJob;

    public static void registerEventListeners() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ChunkCopyManager.processJob(server);
        });
    }

    public static void processJob(MinecraftServer server) {
        if (activeJob == null && pendingJobs.isEmpty()) return;
        if (activeJob == null) {
            activeJob = pendingJobs.poll();
        }
        if (!activeJob.workInFlight.isDone()) return;
        ChunkPos pos = activeJob.chunks.poll();
        if (pos == null) {
            ServerPlayer p = server.getPlayerList().getPlayer(activeJob.player);
            if (p != null) p.sendSystemMessage(Component.literal("Chunk copying job finished"));
            activeJob = null;
            return;
        }
        ChunkCopyJob job = activeJob;
        activeJob.workInFlight = activeJob.workInFlight
                .thenCompose(a -> mirrorChunk(server, pos))
                .exceptionally(ex -> {
                    MirrorWorld.LOGGER.error("Unexpected error copying chunk {}", pos, ex);
                    return false;
                }).thenApply(success -> {
                    if (!success) {
                        ServerPlayer p = server.getPlayerList().getPlayer(job.player);
                        if (p != null) p.sendSystemMessage(Component.literal("Failed to copy one of the chunks"));
                    }
                    return success;
                });
    }

    public static boolean addJob(UUID playerUUID, Queue<ChunkPos> chunks) {
        if ((activeJob != null && activeJob.player.equals(playerUUID))
                || pendingJobs.stream().anyMatch(job -> job.player.equals(playerUUID)) ) {
            return false;
        }
        return pendingJobs.add(new ChunkCopyJob(playerUUID, chunks));
    }

    private static CompletableFuture<Boolean> mirrorChunk(MinecraftServer server, ChunkPos pos) {
        ServerLevel overworld = server.overworld();
        ServerLevel mirror_world = server.getLevel(MirrorWorldManager.MIRROR_DIMENSION_KEY);
        if (mirror_world == null) {
            MirrorWorld.LOGGER.error("MirrorWorld dimension doesn't exists");
            return CompletableFuture.completedFuture(false);
        }
        CompletableFuture<ChunkResult<ChunkAccess>> sourceFuture = overworld.getChunkSource()
                .getChunkFuture(pos.x, pos.z, ChunkStatus.FULL, true);
        CompletableFuture<ChunkResult<ChunkAccess>> destinationFuture = mirror_world.getChunkSource()
                .getChunkFuture(pos.x, pos.z, ChunkStatus.FULL, true);

        return sourceFuture.thenCombine(destinationFuture, (source, destination) -> {
            ChunkAccess sourceChunk = source.orElse(null);
            ChunkAccess destinationChunk = destination.orElse(null);
            if (sourceChunk == null || destinationChunk == null) {
                MirrorWorld.LOGGER.error("Can't access one of the chunks");
                return null;
            }
            return new ChunkAccess[]{sourceChunk, destinationChunk};
        }).thenApplyAsync(chunkAccesses -> {
            if (chunkAccesses == null) return false;
            return copyChunk(chunkAccesses[0], chunkAccesses[1], pos, server);
        }, server);
    }

    private static boolean copyChunk(ChunkAccess sourceChunk, ChunkAccess destinationChunk, ChunkPos pos, MinecraftServer server) {
        LevelChunkSection[] sourceSections = sourceChunk.getSections();
        LevelChunkSection[] destinationSections = destinationChunk.getSections();
        if (sourceSections.length != destinationSections.length) {
            MirrorWorld.LOGGER.error("Mirror dimension height is different from overworld, can't copy chunk {}", pos);
            return false;
        }
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
        ServerLevel mirror_world = server.getLevel(MirrorWorldManager.MIRROR_DIMENSION_KEY);
        if (mirror_world == null) {
            MirrorWorld.LOGGER.error("MirrorWorld dimension doesn't exists");
            return false;
        }
        ChunkMap chunkMap = mirror_world.getChunkSource().chunkMap;
        ThreadedLevelLightEngine lightEngine = (ThreadedLevelLightEngine) mirror_world.getLightEngine();
        CompletableFuture<ChunkAccess> lightingFuture = lightEngine.lightChunk(destinationChunk, false);
        lightingFuture.thenAcceptAsync(litChunk ->  {
            List<ServerPlayer> viewers = chunkMap.getPlayers(pos, false);
            ClientboundLevelChunkWithLightPacket packet =
                    new ClientboundLevelChunkWithLightPacket((LevelChunk) destinationChunk, lightEngine, null, null);
            for (ServerPlayer player : viewers) {
                player.connection.send(packet);
            }
        }, server);
        return true;
    }
}