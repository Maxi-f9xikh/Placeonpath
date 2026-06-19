package de.maxi.placeonpath.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public class BlockBehaviourMixin {

    /**
     * Make the vanilla dirt path report a full, sturdy support face so blocks that need solid
     * ground (doors, rails, redstone, ...) can be placed on it. Only the support shape changes;
     * the visual and collision shape stay the normal 15/16-high path.
     */
    @Inject(method = "getBlockSupportShape", at = @At("HEAD"), cancellable = true)
    private void placeonpath$pathIsSturdy(BlockState state, BlockGetter level, BlockPos pos,
                                          CallbackInfoReturnable<VoxelShape> cir) {
        if (state.is(Blocks.DIRT_PATH)) {
            cir.setReturnValue(Shapes.block());
        }
    }
}
