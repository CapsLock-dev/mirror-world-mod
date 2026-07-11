package capslab.mirrorworld.commands;

import capslab.mirrorworld.MirrorWorld;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

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
        Player p = context.getSource().getPlayer();
        if (p == null) return 1;
        ServerLevel mirror_world = context.getSource().getServer().getLevel(MirrorWorld.MIRROR_DIMENSION_KEY);
        if (mirror_world == null) {
            context.getSource().sendFailure(Component.literal("Mirror world doesn't exists"));
            return 1;
        }

        TeleportTransition transition = new TeleportTransition(
                mirror_world,
                p.position(),
                Vec3.ZERO,  // velocity
                0.0F,       // yaw
                0.0F,       // pitch
                TeleportTransition.DO_NOTHING
        );
        p.teleport(transition);

        context.getSource().sendSuccess(() -> Component.literal("Called /mirror_tp"), false);
        return 1;
    }

    public static int returnFromMirror(CommandContext<CommandSourceStack> context) {
        Player p = context.getSource().getPlayer();
        if (p == null) return 1;
        ServerLevel mirror_world = context.getSource().getServer().overworld();

        TeleportTransition transition = new TeleportTransition(
                mirror_world,
                p.position(),
                Vec3.ZERO,  // velocity
                0.0F,       // yaw
                0.0F,       // pitch
                TeleportTransition.DO_NOTHING
        );
        p.teleport(transition);

        context.getSource().sendSuccess(() -> Component.literal("Called /mirror_exit"), false);
        return 1;
    }
}
