package capslab.mirrorworld.utils.command_restriction;

import capslab.mirrorworld.MirrorWorld;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.function.Predicate;

public class MirrorWorldCommandPredicate {
    private static final Set<String> WHITELIST = Set.of(
            "gamerule", "time", "weather"
    );

    public static void install(MinecraftServer server) {
        CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
        for (CommandNode<CommandSourceStack> node : dispatcher.getRoot().getChildren()) {
            String name = node.getName();
            if (!WHITELIST.contains(name)) continue;

            Predicate<CommandSourceStack> original = node.getRequirement();
            Predicate<CommandSourceStack> patched = source -> original.test(source) || allowInMirror(source);
            patchRequirement(node, patched);
        }
    }

    private static boolean allowInMirror(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        return player != null && player.level().dimension().equals(MirrorWorld.MIRROR_DIMENSION_KEY);
    }

    private static void patchRequirement(CommandNode<CommandSourceStack> node, Predicate<CommandSourceStack> predicate) {
        try {
            Field field = CommandNode.class.getDeclaredField("requirement");
            field.setAccessible(true);
            field.set(node, predicate);
        } catch (ReflectiveOperationException e) {
            MirrorWorld.LOGGER.error("Failed to patch command permission for {}", node.getName(), e);
        }
    }
}
