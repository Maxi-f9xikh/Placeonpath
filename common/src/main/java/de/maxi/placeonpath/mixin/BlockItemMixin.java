package de.maxi.placeonpath.mixin;

import de.maxi.placeonpath.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockItemMixin {

    @Inject(method = "useOn", at = @At("RETURN"))
    private void onBlockPlaced(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        // BlockItem#place returns CONSUME on the server and SUCCESS on the client,
        // so we check consumesAction() instead of comparing against SUCCESS directly.
        if (!cir.getReturnValue().consumesAction()) return;

        Level level = context.getLevel();
        if (level.isClientSide()) return;

        BlockPos placedPos = context.getClickedPos().relative(context.getClickedFace());
        BlockState placedState = level.getBlockState(placedPos);

        // Only react if a block actually ended up being placed.
        if (placedState.isAir()) return;

        // Case 1: a block was placed on top of a path -> make the path below full-height
        // so there is no visible gap to the block resting on it.
        BlockPos belowPlaced = placedPos.below();
        if (level.getBlockState(belowPlaced).is(Blocks.DIRT_PATH)) {
            level.setBlock(belowPlaced, ModBlocks.FULL_PATH_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
        }

        // Case 2: a path was placed directly underneath an existing block -> swap it for
        // the full-height variant as well, instead of leaving a normal (shorter) path.
        if (placedState.is(Blocks.DIRT_PATH) && !level.getBlockState(placedPos.above()).isAir()) {
            level.setBlock(placedPos, ModBlocks.FULL_PATH_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}
