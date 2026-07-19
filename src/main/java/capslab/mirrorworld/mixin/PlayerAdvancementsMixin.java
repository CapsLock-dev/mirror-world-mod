package capslab.mirrorworld.mixin;

import capslab.mirrorworld.utils.playerdata.MirrorWorldManager;
import capslab.mirrorworld.utils.playerdata.PlayerStorageManager;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {

    @Shadow
    private ServerPlayer player;

    @Inject(method = "award", at = @At("HEAD"), cancellable = true)
    private void mirrorworld$blockMirrorAdvancements(AdvancementHolder advancementHolder, String string, CallbackInfoReturnable<Boolean> cir) {
        var attachment = player.getAttached(PlayerStorageManager.PLAYERDATA_ORIGIN_ATTACHMENT);
        if (player.level().dimension().equals(MirrorWorldManager.MIRROR_DIMENSION_KEY) || (attachment != null && attachment.equals("mirror"))) {
            cir.setReturnValue(false);
        }
    }
}