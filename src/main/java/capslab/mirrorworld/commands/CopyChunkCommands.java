package capslab.mirrorworld.commands;

import capslab.mirrorworld.utils.chunkcopy.ChunkCopyManager;
import capslab.mirrorworld.utils.chunkcopy.ChunkMarkingManager;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Set;

public class CopyChunkCommands {

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("copy_chunk")
                    .executes(CopyChunkCommands::copyChunk));
        });
    }

    public static int copyChunk(CommandContext<CommandSourceStack> context) {
        ServerPlayer p = context.getSource().getPlayer();
        if (p == null) {return 1;}

        Set<ChunkPos> chunks = ChunkMarkingManager.getMarked(p.getUUID());
        if (chunks == null || chunks.isEmpty()) {
            context.getSource().sendFailure(Component.literal("No marked chunks"));
            return 1;
        }
        Queue<ChunkPos> chunkPosQueue = new ArrayDeque<>(chunks);

        boolean success = ChunkCopyManager.addJob(p.getUUID(), chunkPosQueue);

        if (success) {
            ChunkMarkingManager.unmarkAllChunks(p.getUUID());
            context.getSource().sendSuccess(() -> Component.literal("Chunk copying queued"), false);
        } else {
            context.getSource().sendFailure(Component.literal("You already have a chunk copying job in progress"));
        }

        return 1;
    }
}
