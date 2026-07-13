package capslab.mirrorworld.mixin;

import capslab.mirrorworld.MirrorWorld;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelData;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class PlayerRespawnMixin {

    @Shadow
    @Final
    private MinecraftServer server;

    @ModifyVariable(method = "setRespawnPosition", at = @At("HEAD"), argsOnly = true)
    private ServerPlayer.RespawnConfig mirrorworld$redirectMirrorSpawn(ServerPlayer.RespawnConfig respawnConfig) {
        if (respawnConfig != null
                && respawnConfig.respawnData().dimension().equals(MirrorWorld.MIRROR_DIMENSION_KEY)) {
            ServerLevel mirrorworld = this.server.getLevel(MirrorWorld.MIRROR_DIMENSION_KEY);
            LevelData.RespawnData safeRespawnData = LevelData.RespawnData.of(
                    MirrorWorld.MIRROR_DIMENSION_KEY,
                    mirrorworld.getRespawnData().pos(),
                    respawnConfig.respawnData().yaw(),      // keep whatever they set
                    respawnConfig.respawnData().pitch()
            );

            return new ServerPlayer.RespawnConfig(safeRespawnData, respawnConfig.forced());
        }
        return respawnConfig;
    }
}