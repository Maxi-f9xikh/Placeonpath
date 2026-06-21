package de.maxi.placeonpath.mixin;

import de.maxi.placeonpath.config.PathRules;
import de.maxi.placeonpath.registry.ModBlocks;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.DirtPathBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DirtPathBlock.class)
public class DirtPathBlockMixin {

    /**
     * Vanilla turns the path into plain dirt when a solid block sits on top of it.
     * It does this via {@code canSurvive} returning false (which both rejects placing a
     * path under a block and schedules the turn-to-dirt tick). We force it to always
     * return true so the path is never converted to dirt and can always be placed.
     * The actual swap to the full-height variant (to close the gap) is done by
     * {@link BlockItemMixin} when a block is placed.
     */
    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void placeonpath$alwaysSurvive(BlockState state, LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        // Keep the path alive, unless the block above is blacklisted (then let vanilla decide -> dirt).
        if (!PathRules.shouldTurnToDirt(level.getBlockState(pos.above()))) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Safety net: should the turn-to-dirt tick ever still be scheduled (e.g. by another
     * mod), redirect it to our full-height path block instead of letting it become dirt.
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void placeonpath$keepPath(BlockState state, ServerLevel level, BlockPos pos, Random random, CallbackInfo ci) {
        BlockState above = level.getBlockState(pos.above());
        if (above.isAir()) { ci.cancel(); return; }
        if (PathRules.shouldTurnToDirt(above)) return;   // blacklisted -> let vanilla turnToDirt run
        level.setBlockAndUpdate(pos, ModBlocks.FULL_PATH_BLOCK.get().defaultBlockState());
        ci.cancel();
    }
}
