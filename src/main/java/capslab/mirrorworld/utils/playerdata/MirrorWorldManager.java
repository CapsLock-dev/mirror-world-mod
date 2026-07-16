package capslab.mirrorworld.utils.playerdata;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

import static capslab.mirrorworld.utils.playerdata.PlayerStorageManager.mirrorStorage;
import static capslab.mirrorworld.utils.playerdata.PlayerStorageManager.normalStorage;

public class MirrorWorldManager {

    public static final ResourceKey<Level> MIRROR_DIMENSION_KEY = ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("mirrorworld", "mirror_world_dim"));

    public static void registerEventListeners() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (handler.player.level().dimension().equals(MirrorWorldManager.MIRROR_DIMENSION_KEY)) {
                MirrorWorldManager.exitMirror(server, handler.player);
            }
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            PlayerStorageManager.setupStorages(server);
        });
    }

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

        ServerLevel mirrorWorld = server.getLevel(MirrorWorldManager.MIRROR_DIMENSION_KEY);
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