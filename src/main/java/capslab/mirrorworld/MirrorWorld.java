package capslab.mirrorworld;

import capslab.mirrorworld.commands.ChunkMarkingCommands;
import capslab.mirrorworld.commands.CopyChunkCommands;
import capslab.mirrorworld.commands.TeleportCommands;
import capslab.mirrorworld.utils.chunkcopy.ChunkCopyManager;
import capslab.mirrorworld.utils.chunkcopy.ChunkMarkingManager;
import capslab.mirrorworld.utils.playerdata.MirrorWorldManager;
import capslab.mirrorworld.utils.playerdata.PlayerStorageManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

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
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			PlayerStorageManager.setupStorages(server);
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			if (handler.player.level().dimension().equals(MirrorWorld.MIRROR_DIMENSION_KEY)) {
				MirrorWorldManager.exitMirror(server, handler.player);
			}
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			boolean diedInMirror = oldPlayer.level().dimension().equals(MirrorWorld.MIRROR_DIMENSION_KEY);
			boolean respawnedInMirror = newPlayer.level().dimension().equals(MirrorWorld.MIRROR_DIMENSION_KEY);
			MinecraftServer server = newPlayer.level().getServer();
			if (diedInMirror || respawnedInMirror) {
				if (respawnedInMirror) {
					ServerLevel overworld = server.overworld();
					BlockPos spawn = overworld.getRespawnData().pos();
					newPlayer.teleport(new TeleportTransition(
							overworld, Vec3.atCenterOf(spawn), Vec3.ZERO, 0.0F, 0.0F, TeleportTransition.DO_NOTHING
					));
				}
				Optional<CompoundTag> normalData = PlayerStorageManager.normalStorage.load(newPlayer);
				normalData.ifPresent(tag -> PlayerStorageManager.applyPlayerData(newPlayer, tag, GameType.SURVIVAL));
				server.execute(() -> {
					newPlayer.connection.send(new ClientboundGameEventPacket(
							ClientboundGameEventPacket.CHANGE_GAME_MODE, GameType.SURVIVAL.getId()
					));
				});
			}
		});

	}

}
