package io.endertech.block;

import cpw.mods.fml.common.registry.GameRegistry;
import io.endertech.item.ItemBlockChargePad;
import io.endertech.item.ItemBlockHealthPad;
import io.endertech.item.ItemBlockTankController;
import io.endertech.item.ItemBlockTankGlass;
import io.endertech.multiblock.block.BlockMultiblockGlass;
import io.endertech.multiblock.block.BlockTankController;
import io.endertech.multiblock.block.BlockTankPart;
import io.endertech.multiblock.item.ItemBlockTankPart;
import io.endertech.reference.Strings;
import net.minecraft.block.Block;

public class ETBlocks
{
    public static Block blockTankPart;
    public static Block blockTankController;
    public static Block blockMultiblockGlass;
    public static Block blockChargePad;
    public static Block blockHealthPad;

    public static void init()
    {
        blockTankPart = new BlockTankPart();
        blockMultiblockGlass = new BlockMultiblockGlass();
        blockTankController = new BlockTankController();
        blockChargePad = new BlockChargePad();
        blockHealthPad = new BlockHealthPad();

        GameRegistry.registerBlock(blockTankPart, ItemBlockTankPart.class, "endertech." + Strings.Blocks.TANK_PART_NAME);
        GameRegistry.registerBlock(blockMultiblockGlass, ItemBlockTankGlass.class, "endertech." + Strings.Blocks.MULTIBLOCK_GLASS_NAME);
        GameRegistry.registerBlock(blockTankController, ItemBlockTankController.class, "endertech." + Strings.Blocks.TANK_CONTROLLER_NAME);
        GameRegistry.registerBlock(blockChargePad, ItemBlockChargePad.class, Strings.Blocks.CHARGE_PAD);
        GameRegistry.registerBlock(blockHealthPad, ItemBlockHealthPad.class, Strings.Blocks.HEALTH_PAD);

        ((BlockTankPart) blockTankPart).init();
        ((BlockMultiblockGlass) blockMultiblockGlass).init();
        ((BlockTankController) blockTankController).init();
        ((BlockChargePad) blockChargePad).init();
        ((BlockHealthPad) blockHealthPad).init();
    }
}
