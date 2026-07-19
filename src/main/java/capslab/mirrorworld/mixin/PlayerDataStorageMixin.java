package capslab.mirrorworld.mixin;

import capslab.mirrorworld.utils.playerdata.MirrorWorldManager;
import capslab.mirrorworld.utils.playerdata.PlayerStorageManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerDataStorage.class)
public class PlayerDataStorageMixin {

    @Inject(method = "save", at = @At("HEAD"), cancellable = true)
    private void mirrorworld$redirectSave(Player player, CallbackInfo ci) {
        var attachment = player.getAttached(PlayerStorageManager.PLAYERDATA_ORIGIN_ATTACHMENT);
        if (player.level().dimension().equals(MirrorWorldManager.MIRROR_DIMENSION_KEY) || (attachment != null && attachment.equals("mirror"))) {
            PlayerStorageManager.mirrorStorage.save((ServerPlayer) player);
            ci.cancel();
        }
    }
}