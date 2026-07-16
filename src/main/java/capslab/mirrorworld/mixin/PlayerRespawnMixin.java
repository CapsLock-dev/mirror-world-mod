package capslab.mirrorworld.mixin;

import capslab.mirrorworld.MirrorWorld;
import capslab.mirrorworld.utils.playerdata.MirrorWorldManager;
import capslab.mirrorworld.utils.playerdata.PlayerStorageManager;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class PlayerRespawnMixin {

    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "setRespawnPosition", at = @At("TAIL"))
    private void mirrorworld$setRespawnConfig(ServerPlayer.RespawnConfig config, boolean bl, CallbackInfo ci) {
        if (config == null) return;
        ServerPlayer self = (ServerPlayer)(Object)this;
        ResourceKey<Level> dim = config.respawnData().dimension();
        if (dim.equals(MirrorWorldManager.MIRROR_DIMENSION_KEY)) {
            self.setAttached(PlayerStorageManager.MIRROR_RESPAWN_ATTACHMENT, config);
        } else {
            self.setAttached(PlayerStorageManager.OVERWORLD_RESPAWN_ATTACHMENT, config);
        }
    }

    @Inject(method="getRespawnConfig", at = @At("HEAD"))
    public ServerPlayer.@Nullable RespawnConfig getRespawnConfig(CallbackInfoReturnable<ServerPlayer.RespawnConfig> cir) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        ResourceKey<Level> dim = self.level().dimension();
        if (dim.equals(MirrorWorldManager.MIRROR_DIMENSION_KEY)) {
            return self.getAttached(PlayerStorageManager.MIRROR_RESPAWN_ATTACHMENT);
        } else {
            return self.getAttached(PlayerStorageManager.OVERWORLD_RESPAWN_ATTACHMENT);
        }
    }

}