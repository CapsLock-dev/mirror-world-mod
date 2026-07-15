package capslab.mirrorworld.commands;

import capslab.mirrorworld.utils.chunkcopy.ChunkMarkingManager;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public class ChunkMarkingCommands {
    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("mark_chunk")
                    .executes(ChunkMarkingCommands::markChunk));
            dispatcher.register(Commands.literal("unmark_chunk")
                    .executes(ChunkMarkingCommands::unmarkChunk));
        });
    }

    public static int markChunk(CommandContext<CommandSourceStack> context) {
        ServerPlayer p = context.getSource().getPlayer();
        if (p == null) return 1;
        ChunkPos pos = new ChunkPos(p.blockPosition());

        boolean success = ChunkMarkingManager.markChunk(p, pos);

        if (success) {
            context.getSource().sendSuccess(() -> Component.literal("Chunk marked"), false);
        } else {
            context.getSource().sendFailure(Component.literal("Chunk already marked"));
        }
        return 1;
    }

    public static int unmarkChunk(CommandContext<CommandSourceStack> context) {
        ServerPlayer p = context.getSource().getPlayer();
        if (p == null) return 1;
        ChunkPos pos = new ChunkPos(p.blockPosition());

        boolean success = ChunkMarkingManager.unmarkChunk(p, pos);

        if (success) {
            context.getSource().sendSuccess(() -> Component.literal("Chunk unmarked"), false);
        } else {
            context.getSource().sendFailure(Component.literal("Chunk is not marked"));
        }
        return 1;
    }
}
