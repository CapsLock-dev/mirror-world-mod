package capslab.mirrorworld;

import capslab.mirrorworld.commands.ChunkMarkingCommands;
import capslab.mirrorworld.commands.CopyChunkCommands;
import capslab.mirrorworld.commands.TeleportCommands;
import capslab.mirrorworld.utils.chunkcopy.ChunkCopyManager;
import capslab.mirrorworld.utils.chunkcopy.ChunkMarkingManager;
import capslab.mirrorworld.utils.playerdata.MirrorWorldManager;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MirrorWorld implements ModInitializer {
	public static final String MOD_ID = "mirrorworld";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Command registration
		TeleportCommands.registerCommands();
		CopyChunkCommands.registerCommands();
		ChunkMarkingCommands.registerCommands();

		// Event listeners
		ChunkCopyManager.registerEventListeners();
		ChunkMarkingManager.registerEventListeners();
		MirrorWorldManager.registerEventListeners();
	}

}
