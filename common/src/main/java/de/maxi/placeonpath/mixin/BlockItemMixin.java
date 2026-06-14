package de.maxi.placeonpath.mixin;

import de.maxi.placeonpath.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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

        // Only react if a block actually ended up resting on top of the path block.
        if (level.getBlockState(placedPos).isAir()) return;

        BlockPos belowPlaced = placedPos.below();
        if (level.getBlockState(belowPlaced).is(Blocks.DIRT_PATH)) {
            // Replace the slightly-shorter vanilla dirt path with our full-height variant
            // so there is no visible gap to the block placed on top of it. This also
            // neutralises the vanilla scheduled tick that would otherwise turn the path
            // into plain dirt, because that tick only runs while the block is a DirtPathBlock.
            level.setBlock(belowPlaced, ModBlocks.FULL_PATH_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}
