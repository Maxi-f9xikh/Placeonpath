package de.maxi.placeonpath.registry;

import de.maxi.placeonpath.Placeonpath;
import de.maxi.placeonpath.block.PathFullBlock;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Placeonpath.MOD_ID, Registries.BLOCK);

    public static final RegistrySupplier<Block> FULL_PATH_BLOCK = BLOCKS.register("path_full_block",
            () -> new PathFullBlock(
                    BlockBehaviour.Properties.of(Material.DIRT)
                            .strength(0.65f)
                            .sound(SoundType.GRAVEL)
            ));

    public static void init() {
        BLOCKS.register();
    }
}
