package capslab.mirrorworld.utils.playerdata;

import capslab.mirrorworld.MirrorWorld;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Path;

public class PlayerStorageManager {
    public static MirrorPlayerDataStorage mirrorStorage;
    public static MirrorPlayerDataStorage normalStorage;

    public static final AttachmentType<ServerPlayer.RespawnConfig> MIRROR_RESPAWN_ATTACHMENT = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(MirrorWorld.MOD_ID, "mirror_respawn_attachment"),
            builder -> builder.persistent(ServerPlayer.RespawnConfig.CODEC)
    );
    public static final AttachmentType<ServerPlayer.RespawnConfig> OVERWORLD_RESPAWN_ATTACHMENT = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(MirrorWorld.MOD_ID, "overworld_respawn_attachment"),
            builder -> builder.persistent(ServerPlayer.RespawnConfig.CODEC)
    );
    public static final AttachmentType<String> PLAYERDATA_ORIGIN_ATTACHMENT = AttachmentRegistry.createPersistent(
            Identifier.fromNamespaceAndPath(MirrorWorld.MOD_ID, "playerdata_origin_attachment"),
            Codec.STRING
    );

    public static void setupStorages(MinecraftServer server) {
        Path realPlayerDataDir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
        Path mirrorPlayerDataDir = realPlayerDataDir.getParent().resolve("mirror_playerdata");

        normalStorage = new MirrorPlayerDataStorage(realPlayerDataDir, server.getFixerUpper());
        mirrorStorage = new MirrorPlayerDataStorage(mirrorPlayerDataDir, server.getFixerUpper());
    }

    public static void applyPlayerData(ServerPlayer player, CompoundTag tag, GameType gameMode) {
        CompoundTag safe = tag.copy();
        safe.remove("Pos");
        safe.remove("Dimension");
        safe.remove("Rotation");

        ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), safe);
        player.load(input);
        player.removeAllEffects();
        player.setGameMode(gameMode);
        player.onUpdateAbilities();

        if (gameMode == GameType.CREATIVE) {
            player.setAttached(PLAYERDATA_ORIGIN_ATTACHMENT, "mirror");
        } else if (gameMode == GameType.SURVIVAL) {
            player.setAttached(PLAYERDATA_ORIGIN_ATTACHMENT, "normal");
        }

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
        player.setAttached(PLAYERDATA_ORIGIN_ATTACHMENT, "mirror");

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
