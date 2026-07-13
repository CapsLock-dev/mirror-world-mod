package capslab.mirrorworld.mixin;

import capslab.mirrorworld.MirrorWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndPortalBlock.class)
public class EndPortalMixin {
    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void onEntityCollision(BlockState blockState, Level level, BlockPos blockPos, Entity entity, InsideBlockEffectApplier insideBlockEffectApplier, boolean bl, CallbackInfo ci) {
        if (level.dimension().equals(MirrorWorld.MIRROR_DIMENSION_KEY)) {
            ci.cancel();
        }
    }
}