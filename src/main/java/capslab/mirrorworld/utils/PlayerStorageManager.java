package capslab.mirrorworld.utils;

import capslab.mirrorworld.MirrorWorld;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Path;
import java.util.Optional;

public class PlayerStorageManager {
    public static MirrorPlayerDataStorage mirrorStorage;
    public static MirrorPlayerDataStorage normalStorage;

    public static void setupStorages(MinecraftServer server) {
        Path realPlayerDataDir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
        Path mirrorPlayerDataDir = realPlayerDataDir.getParent().resolve("mirror_playerdata");

        normalStorage = new MirrorPlayerDataStorage(realPlayerDataDir, server.getFixerUpper());
        mirrorStorage = new MirrorPlayerDataStorage(mirrorPlayerDataDir, server.getFixerUpper());
    }

    public static void returnOnDisconnect(MinecraftServer server, ServerPlayer player) {
        if (player.level().dimension().equals(MirrorWorld.MIRROR_DIMENSION_KEY)) {
            mirrorStorage.save(player);
            Optional<CompoundTag> normalData = normalStorage.load(player);
            normalData.ifPresent(tag -> {
                Vec3 dest = extractPos(tag);

                applyPlayerData(player, tag, GameType.SURVIVAL);

                player.connection.send(new ClientboundGameEventPacket(
                        ClientboundGameEventPacket.CHANGE_GAME_MODE, GameType.CREATIVE.getId()
                ));

                ServerLevel overworld = server.overworld();
                TeleportTransition transition = new TeleportTransition(
                        overworld, dest, Vec3.ZERO, 0.0F, 0.0F, TeleportTransition.DO_NOTHING
                );
                player.teleport(transition);
            });
        }
    }

    public static void applyPlayerData(ServerPlayer player, CompoundTag tag, GameType gameMode) {
        MirrorWorld.LOGGER.info("Player data apply");
        CompoundTag safe = tag.copy();
        safe.remove("Pos");
        safe.remove("Dimension");
        safe.remove("Rotation");

        ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), safe);
        player.load(input);
        player.removeAllEffects();
        player.setGameMode(gameMode);
        player.onUpdateAbilities();

        player.connection.send(new ClientboundSetHealthPacket(
                player.getHealth(),
                player.getFoodData().getFoodLevel(),
                player.getFoodData().getSaturationLevel()
        ));
        player.inventoryMenu.broadcastFullState();
    }

    public static void resetToFreshMirrorState(ServerPlayer player) {
        player.setGameMode(GameType.CREATIVE);
        player.getInventory().clearContent();
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0f);
        player.removeAllEffects();
        player.setExperienceLevels(0);
        player.setExperiencePoints(0);

        player.onUpdateAbilities();
        player.connection.send(new ClientboundSetHealthPacket(
                player.getHealth(),
                player.getFoodData().getFoodLevel(),
                player.getFoodData().getSaturationLevel()
        ));
        player.inventoryMenu.broadcastFullState();
    }

    public static Vec3 extractPos(CompoundTag tag) {
        ListTag posList = tag.getList("Pos").get();
        double x = posList.getDouble(0).get();
        double y = posList.getDouble(1).get();
        double z = posList.getDouble(2).get();
        return new Vec3(x, y, z);
    }
}
