package de.maxi.placeonpath.mixin;

import de.maxi.placeonpath.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.16.5 has no support-shape hook, so a door's lower half won't accept a path as ground.
 * Allow the bottom half to survive when it rests on a (full-height) path.
 */
@Mixin(DoorBlock.class)
public class DoorBlockMixin {

    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void placeonpath$doorsOnPath(BlockState state, LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (state.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER) return; // only the bottom half rests on the path
        BlockState below = level.getBlockState(pos.below());
        if (below.is(Blocks.GRASS_PATH) || below.is(ModBlocks.FULL_PATH_BLOCK.get())) {
            cir.setReturnValue(true);
        }
    }
}
