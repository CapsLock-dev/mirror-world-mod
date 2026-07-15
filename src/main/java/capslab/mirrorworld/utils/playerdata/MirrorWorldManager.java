package capslab.mirrorworld.utils.playerdata;

import capslab.mirrorworld.MirrorWorld;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

import static capslab.mirrorworld.utils.playerdata.PlayerStorageManager.mirrorStorage;
import static capslab.mirrorworld.utils.playerdata.PlayerStorageManager.normalStorage;

public class MirrorWorldManager {

    public enum Result {
        MIRROR_WORLD_UNAVAILABLE,
        OVERWORLD_DATA_MISSING,
        SUCCESS
    }

    public static Result enterMirror(MinecraftServer server, ServerPlayer player) {
        normalStorage.save(player);
        Optional<CompoundTag> mirrorData = mirrorStorage.load(player);

        Vec3 destination = mirrorData.map(PlayerStorageManager::extractPos).orElse(player.position());
        if (mirrorData.isPresent()) {
            PlayerStorageManager.applyPlayerData(player, mirrorData.get(), GameType.CREATIVE);
        } else {
            PlayerStorageManager.resetToFreshMirrorState(player);
        }

        ServerLevel mirrorWorld = server.getLevel(MirrorWorld.MIRROR_DIMENSION_KEY);
        if (mirrorWorld == null) {
            return Result.MIRROR_WORLD_UNAVAILABLE;
        }
        TeleportTransition transition = new TeleportTransition(mirrorWorld, destination, Vec3.ZERO, 0.0F, 0.0F, TeleportTransition.DO_NOTHING);
        player.teleport(transition);
        server.execute(() -> {
            player.connection.send(new ClientboundGameEventPacket(
                    ClientboundGameEventPacket.CHANGE_GAME_MODE, GameType.CREATIVE.getId()
            ));
        });
        return Result.SUCCESS;
    }
    public static Result exitMirror(MinecraftServer server, ServerPlayer player) {
        mirrorStorage.save(player);
        Optional<CompoundTag> normalData = normalStorage.load(player);

        Vec3 destination = normalData.map(PlayerStorageManager::extractPos).orElse(player.position());
        if (normalData.isPresent()) {
            PlayerStorageManager.applyPlayerData(player, normalData.get(), GameType.SURVIVAL);
        } else {
            return Result.OVERWORLD_DATA_MISSING;
        }

        ServerLevel overworld = server.overworld();
        TeleportTransition transition = new TeleportTransition(overworld, destination, Vec3.ZERO, 0.0F, 0.0F, TeleportTransition.DO_NOTHING);
        player.teleport(transition);
        server.execute(() -> {
            player.connection.send(new ClientboundGameEventPacket(
                    ClientboundGameEventPacket.CHANGE_GAME_MODE, GameType.SURVIVAL.getId()
            ));
        });
        return Result.SUCCESS;
    }
}