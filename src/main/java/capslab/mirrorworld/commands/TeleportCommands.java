package capslab.mirrorworld.commands;

import capslab.mirrorworld.MirrorWorld;
import capslab.mirrorworld.utils.PlayerStorageManager;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

import static capslab.mirrorworld.utils.PlayerStorageManager.mirrorStorage;
import static capslab.mirrorworld.utils.PlayerStorageManager.normalStorage;

public class TeleportCommands {
    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("mirror_tp")
                    .executes(TeleportCommands::teleportToMirror));
            dispatcher.register(Commands.literal("mirror_exit")
                    .executes(TeleportCommands::returnFromMirror));
        });
    }

    public static int teleportToMirror(CommandContext<CommandSourceStack> context) {
        ServerPlayer p = context.getSource().getPlayer();
        MinecraftServer server = context.getSource().getServer();
        if (p == null) return 1;
        if (p.level().dimension().equals(MirrorWorld.MIRROR_DIMENSION_KEY)) {
            context.getSource().sendFailure(Component.literal("Already in mirror dimension"));
            return 1;
        }
        ServerLevel mirror_world = context.getSource().getServer().getLevel(MirrorWorld.MIRROR_DIMENSION_KEY);
        if (mirror_world == null) {
            context.getSource().sendFailure(Component.literal("Mirror world doesn't exists"));
            return 1;
        }

        normalStorage.save(p);
        Optional<CompoundTag> mirrorData = mirrorStorage.load(p);

        Vec3 destination = mirrorData.map(PlayerStorageManager::extractPos).orElse(p.position());

        if (mirrorData.isPresent()) {
            PlayerStorageManager.applyPlayerData(p, mirrorData.get(), GameType.CREATIVE);
        } else {
            PlayerStorageManager.resetToFreshMirrorState(p);
        }

        TeleportTransition transition = new TeleportTransition(
                mirror_world,
                destination,
                Vec3.ZERO,
                0.0F,
                0.0F,
                TeleportTransition.DO_NOTHING
        );
        p.teleport(transition);
        server.execute(() -> {
            p.connection.send(new ClientboundGameEventPacket(
                    ClientboundGameEventPacket.CHANGE_GAME_MODE, GameType.CREATIVE.getId()
            ));
        });
        context.getSource().sendSuccess(() -> Component.literal("Called /mirror_tp"), false);
        return 1;
    }

    public static int returnFromMirror(CommandContext<CommandSourceStack> context) {
        ServerPlayer p = context.getSource().getPlayer();
        MinecraftServer server = context.getSource().getServer();
        if (p == null) return 1;
        if (!p.level().dimension().equals(MirrorWorld.MIRROR_DIMENSION_KEY)) {
            context.getSource().sendFailure(Component.literal("Not in mirror dimension"));
            return 1;
        }
        ServerLevel overworld = context.getSource().getServer().overworld();

        mirrorStorage.save(p);
        Optional<CompoundTag> normalData = normalStorage.load(p);

        Vec3 destination = normalData
                .map(PlayerStorageManager::extractPos)
                .orElse(p.position());

        normalData.ifPresent(tag -> PlayerStorageManager.applyPlayerData(p, tag, GameType.SURVIVAL));

        TeleportTransition transition = new TeleportTransition(
                overworld,
                destination,
                Vec3.ZERO,
                0.0F,
                0.0F,
                TeleportTransition.DO_NOTHING
        );
        p.teleport(transition);
        server.execute(() -> {
            p.connection.send(new ClientboundGameEventPacket(
                    ClientboundGameEventPacket.CHANGE_GAME_MODE, GameType.SURVIVAL.getId()
            ));
        });
        context.getSource().sendSuccess(() -> Component.literal("Called /mirror_exit"), false);
        return 1;
    }
}
