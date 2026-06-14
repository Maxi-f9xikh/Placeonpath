package de.maxi.placeonpath.registry;

import de.maxi.placeonpath.Placeonpath;
import de.maxi.placeonpath.block.PathFullBlock;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Placeonpath.MOD_ID, Registries.BLOCK);

    public static final ResourceKey<Block> FULL_PATH_KEY = ResourceKey.create(
            Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Placeonpath.MOD_ID, "path_full_block"));

    public static final RegistrySupplier<Block> FULL_PATH_BLOCK = BLOCKS.register("path_full_block",
            () -> new PathFullBlock(
                    BlockBehaviour.Properties.of()
                            .setId(FULL_PATH_KEY)
                            .mapColor(MapColor.DIRT)
                            .strength(0.65f)
                            .sound(SoundType.GRAVEL)
            ));

    public static void init() {
        BLOCKS.register();
    }
}
