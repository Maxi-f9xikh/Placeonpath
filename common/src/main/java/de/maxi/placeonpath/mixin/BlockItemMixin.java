package de.maxi.placeonpath.mixin;

import de.maxi.placeonpath.config.PathRules;
import de.maxi.placeonpath.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
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
        placeonpath$convertPathUnder(level, placedPos, placedState);

        // A bed is two blocks wide; the click only covers one half, so convert the path
        // under the other half too (otherwise one of the two paths keeps the gap).
        if (placedState.getBlock() instanceof BedBlock) {
            Direction facing = placedState.getValue(BedBlock.FACING);
            BlockPos otherHalf = placedState.getValue(BedBlock.PART) == BedPart.HEAD
                    ? placedPos.relative(facing.getOpposite())
                    : placedPos.relative(facing);
            placeonpath$convertPathUnder(level, otherHalf, placedState);
        }

        // Case 2: a path was placed directly underneath an existing block.
        //  - blacklisted block above -> the path becomes dirt; otherwise -> full-height variant.
        BlockState aboveState = level.getBlockState(placedPos.above());
        if (placedState.is(Blocks.GRASS_PATH) && !aboveState.isAir()) {
            if (PathRules.shouldTurnToDirt(aboveState)) {
                level.setBlock(placedPos, Blocks.DIRT.defaultBlockState(), 3);
            } else {
                level.setBlock(placedPos, ModBlocks.FULL_PATH_BLOCK.get().defaultBlockState(), 3);
            }
        }
    }

    /** Turn the path under {@code abovePos} into dirt (blacklisted) or the full-height variant (otherwise). */
    private static void placeonpath$convertPathUnder(Level level, BlockPos abovePos, BlockState aboveBlock) {
        BlockPos belowPos = abovePos.below();
        BlockState below = level.getBlockState(belowPos);
        if (!(below.is(Blocks.GRASS_PATH) || below.is(ModBlocks.FULL_PATH_BLOCK.get()))) return;
        if (PathRules.shouldTurnToDirt(aboveBlock)) {
            level.setBlock(belowPos, Blocks.DIRT.defaultBlockState(), 3);
        } else if (below.is(Blocks.GRASS_PATH)) {
            level.setBlock(belowPos, ModBlocks.FULL_PATH_BLOCK.get().defaultBlockState(), 3);
        }
    }
}
