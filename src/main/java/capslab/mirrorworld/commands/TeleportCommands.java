package capslab.mirrorworld.commands;

import capslab.mirrorworld.MirrorWorld;
import capslab.mirrorworld.utils.playerdata.MirrorWorldManager;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

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
        if (!p.level().dimension().equals(server.overworld().dimension())) {
            context.getSource().sendFailure(Component.literal("Player must be in the overworld to enter mirror dimension"));
            return 1;
        }
        MirrorWorldManager.Result res = MirrorWorldManager.enterMirror(server, p);
        switch (res) {
            case OVERWORLD_DATA_MISSING -> context.getSource().sendFailure(Component.literal("Overworld data doesn't exists"));
            case MIRROR_WORLD_UNAVAILABLE -> context.getSource().sendFailure(Component.literal("Mirror world doesn't exists"));
            case SUCCESS -> context.getSource().sendSuccess(() -> Component.literal("Teleported to mirror world"), false);
        }
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
        MirrorWorldManager.Result res = MirrorWorldManager.exitMirror(server, p);
        switch (res) {
            case OVERWORLD_DATA_MISSING -> context.getSource().sendFailure(Component.literal("Overworld data doesn't exists"));
            case MIRROR_WORLD_UNAVAILABLE -> context.getSource().sendFailure(Component.literal("Mirror world doesn't exists"));
            case SUCCESS -> context.getSource().sendSuccess(() -> Component.literal("Teleported to overworld"), false);
        }
        return 1;
    }
}
