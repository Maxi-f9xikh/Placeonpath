package de.maxi.placeonpath.mixin;

import de.maxi.placeonpath.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** canSurvive lives in FaceAttachedHorizontalDirectionalBlock (parent of ButtonBlock/LeverBlock). */
@Mixin(FaceAttachedHorizontalDirectionalBlock.class)
public class ButtonBlockMixin {

    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void placeonpath$buttonOnPath(BlockState state, LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (state.getValue(BlockStateProperties.ATTACH_FACE) != AttachFace.FLOOR) return;
        BlockState below = level.getBlockState(pos.below());
        if (below.is(Blocks.GRASS_PATH) || below.is(ModBlocks.FULL_PATH_BLOCK.get())) {
            cir.setReturnValue(true);
        }
    }
}
