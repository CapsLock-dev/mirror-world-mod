package capslab.mirrorworld.utils.chunkcopy;

import capslab.mirrorworld.MirrorWorld;
import capslab.mirrorworld.utils.playerdata.MirrorWorldManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.UUID;

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
                ChunkMarkingManager.unmarkAllChunks(activeJob.player);
                if (p != null) p.sendSystemMessage(Component.literal("Chunk copying job finished"));
                activeJob = null;
                break;
            }
            boolean success = mirrorChunk(server, pos);
            if (!success && p != null) p.sendSystemMessage(Component.literal("Failed to copy one of the chunks"));
        }
    }

    public static boolean addJob(UUID playerUUID, Queue<ChunkPos> chunks) {
        return pendingJobs.add(new ChunkCopyJob(playerUUID, chunks));
    }

    private static boolean mirrorChunk(MinecraftServer server, ChunkPos pos) {
        Level overworld = server.overworld();
        Level mirror_world = server.getLevel(MirrorWorldManager.MIRROR_DIMENSION_KEY);
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
        return true;
    }
}
