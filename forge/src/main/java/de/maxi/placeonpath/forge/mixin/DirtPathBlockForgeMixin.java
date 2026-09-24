package de.maxi.placeonpath.forge.mixin;

import de.maxi.placeonpath.config.PathRules;
import de.maxi.placeonpath.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import java.util.Random;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.GrassPathBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GrassPathBlock.class)
public class DirtPathBlockForgeMixin {

    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void placeonpath$alwaysSurvive(BlockState state, LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!PathRules.shouldTurnToDirt(level.getBlockState(pos.above()))) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void placeonpath$keepPath(BlockState state, ServerLevel level, BlockPos pos, Random random, CallbackInfo ci) {
        BlockState above = level.getBlockState(pos.above());
        if (above.isAir()) { ci.cancel(); return; }
        if (PathRules.shouldTurnToDirt(above)) return;
        level.setBlockAndUpdate(pos, ModBlocks.FULL_PATH_BLOCK.get().defaultBlockState());
        ci.cancel();
    }
}
