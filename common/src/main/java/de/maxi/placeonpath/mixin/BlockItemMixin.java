package de.maxi.placeonpath.mixin;

import de.maxi.placeonpath.config.PathRules;
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

        // Case 1: a block was placed on top of a path.
        //  - blacklisted block -> actively turn the path into dirt (works even for non-solid
        //    blocks like fence gates, which vanilla would never convert on its own).
        //  - otherwise -> make the path below full-height so there is no gap to the block on it.
        BlockPos belowPlaced = placedPos.below();
        BlockState belowState = level.getBlockState(belowPlaced);
        boolean belowIsPath = belowState.is(Blocks.DIRT_PATH) || belowState.is(ModBlocks.FULL_PATH_BLOCK.get());
        if (belowIsPath) {
            if (PathRules.shouldTurnToDirt(placedState)) {
                level.setBlock(belowPlaced, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
            } else if (belowState.is(Blocks.DIRT_PATH)) {
                level.setBlock(belowPlaced, ModBlocks.FULL_PATH_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
            }
        }

        // Case 2: a path was placed directly underneath an existing block.
        //  - blacklisted block above -> the path becomes dirt; otherwise -> full-height variant.
        BlockState aboveState = level.getBlockState(placedPos.above());
        if (placedState.is(Blocks.DIRT_PATH) && !aboveState.isAir()) {
            if (PathRules.shouldTurnToDirt(aboveState)) {
                level.setBlock(placedPos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
            } else {
                level.setBlock(placedPos, ModBlocks.FULL_PATH_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }
}
