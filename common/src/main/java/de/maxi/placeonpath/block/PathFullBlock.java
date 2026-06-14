package de.maxi.placeonpath.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class PathFullBlock extends Block {

    public PathFullBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide() && fromPos.equals(pos.above())) {
            if (level.getBlockState(pos.above()).isAir()) {
                level.setBlock(pos, Blocks.GRASS_PATH.defaultBlockState(), 3);
            }
        }
    }
}
