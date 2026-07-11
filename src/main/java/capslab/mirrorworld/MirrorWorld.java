package capslab.mirrorworld;

import capslab.mirrorworld.commands.ChunkMarkingCommands;
import capslab.mirrorworld.commands.CopyChunkCommands;
import capslab.mirrorworld.commands.TeleportCommands;
import capslab.mirrorworld.utils.ChunkCopyManager;
import capslab.mirrorworld.utils.ChunkMarkingManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MirrorWorld implements ModInitializer {
	public static final String MOD_ID = "mirrorworld";
	public static final ResourceKey<Level> MIRROR_DIMENSION_KEY = ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("mirrorworld", "mirror_world_dim"));
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Fabric world! ");
		TeleportCommands.registerCommands();
		CopyChunkCommands.registerCommands();
		ChunkMarkingCommands.registerCommands();
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			ChunkMarkingManager.highlightMarkedChunks(server);
			ChunkCopyManager.processJob(server, 1);
		});
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

}
