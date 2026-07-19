package capslab.mirrorworld.utils.chunkcopy;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ChunkMarkingManager {
    private static final HashMap<UUID, Set<ChunkPos>> markedChunks = new HashMap<>();
    private static int markParticleTiming = 0;

    public static void registerEventListeners() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (markParticleTiming >= 10) {
                highlightMarkedChunks(server);
                markParticleTiming = 0;
            }
            ++markParticleTiming;
        });
    }

    public static boolean markChunk(ServerPlayer p, ChunkPos chunkPos) {
        Set<ChunkPos> chunks = markedChunks.computeIfAbsent(p.getUUID(), uuid -> new HashSet<>());
        if(!chunks.add(chunkPos)) {
            return false;
        }
        return true;
    }

    public static boolean unmarkChunk(ServerPlayer p, ChunkPos chunkPos) {
        var chunks = markedChunks.get(p.getUUID());
        if (chunks == null || chunks.isEmpty()) {
            return false;
        }
        return chunks.removeIf(chunkPos1 -> chunkPos1.equals(chunkPos));
    }

    public static void unmarkAllChunks(UUID playerUUID) {
        markedChunks.remove(playerUUID);
    }

    public static Set<ChunkPos> getMarked(UUID playerUUID) {
        return markedChunks.get(playerUUID);
    }

    public static void highlightMarkedChunks(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Set<ChunkPos> selected = markedChunks.get(player.getUUID());
            if (selected == null) continue;
            for (ChunkPos pos : selected) {
                drawChunkOutline(player, pos);
            }
        }
    }

    private static void drawChunkOutline(ServerPlayer player, ChunkPos chunkPos) {
        ServerLevel world = player.level();
        int minX = chunkPos.getMinBlockX();
        int maxX = chunkPos.getMaxBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int maxZ = chunkPos.getMaxBlockZ();
        int y = world.getHeight(Heightmap.Types.WORLD_SURFACE, chunkPos.getMiddleBlockX(), chunkPos.getMiddleBlockZ());
        for (int x = minX; x <= maxX; x++) {
            world.sendParticles(player, ParticleTypes.END_ROD, true, true, x + 0.5, y, minZ + 0.5, 1, 0, 0, 0, 0);
            world.sendParticles(player, ParticleTypes.END_ROD, true, true, x + 0.5, y, maxZ + 0.5, 1, 0, 0, 0, 0);
        }
        for (int z = minZ; z <= maxZ; z++) {
            world.sendParticles(player, ParticleTypes.END_ROD, true, true, minX + 0.5, y, z + 0.5, 1, 0, 0, 0, 0);
            world.sendParticles(player, ParticleTypes.END_ROD, true, true, maxX + 0.5, y, z + 0.5, 1, 0, 0, 0, 0);
        }
    }
}
