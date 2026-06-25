package de.maxi.placeonpath.mixin;

import de.maxi.placeonpath.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Let a pressure plate sit on a (full-height) path — 1.16.5 has no support-shape hook. */
@Mixin(BasePressurePlateBlock.class)
public class PressurePlateBlockMixin {

    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void placeonpath$plateOnPath(BlockState state, LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockState below = level.getBlockState(pos.below());
        if (below.is(Blocks.GRASS_PATH) || below.is(ModBlocks.FULL_PATH_BLOCK.get())) {
            cir.setReturnValue(true);
        }
    }
}
