package capslab.mirrorworld.mixin;

import capslab.mirrorworld.utils.playerdata.MirrorWorldManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class PlayerListRemoveMixin {

    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "remove", at = @At("HEAD"))
    private void mirrorworld$exitMirrorBeforeRemove(ServerPlayer player, CallbackInfo ci) {
        if (player.level().dimension().equals(MirrorWorldManager.MIRROR_DIMENSION_KEY) && player.isAlive()) {
            MirrorWorldManager.exitMirror(this.server, player);
        }
    }
}