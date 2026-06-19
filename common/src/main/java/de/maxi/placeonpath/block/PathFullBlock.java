package de.maxi.placeonpath.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;

public class PathFullBlock extends Block {

    public PathFullBlock(Properties properties) {
        super(properties);
    }

    /** Pick-block (creative middle-click) yields the normal Dirt Path, never this technical block. */
    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(Blocks.DIRT_PATH);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        // The full-height path only makes sense while a block rests on top of it.
        // Once that block is gone, revert to a normal dirt path.
        if (!level.isClientSide() && level.getBlockState(pos.above()).isAir()) {
            level.setBlock(pos, Blocks.DIRT_PATH.defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}
