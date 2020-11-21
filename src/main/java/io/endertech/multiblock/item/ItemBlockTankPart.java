package io.endertech.multiblock.item;

import java.util.List;

import io.endertech.block.ETBlocks;
import io.endertech.block.ItemBlockBasic;
import io.endertech.multiblock.block.BlockTankPart;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class ItemBlockTankPart extends ItemBlockBasic
{
    public ItemBlockTankPart(Block block)
    {
        super(block);
        this.setMaxDamage(0);
    }

    @Override
    public int getMetadata(int meta)
    {
        return meta;
    }

    @Override
    public String getUnlocalizedName(ItemStack itemstack)
    {
        int meta = 0;
        int damage = itemstack.getItemDamage();

        if (BlockTankPart.isFrame(damage)) meta = 0;
        else if (BlockTankPart.isValve(damage)) meta = 1;
        else if (BlockTankPart.isEnergyInput(damage)) meta = 2;

        return ETBlocks.blockTankPart.getUnlocalizedName() + "." + meta;
    }
    
    @Override
    public void addInformation(ItemStack itemstack, EntityPlayer player, List tooltip, boolean p_77624_4_) {
    	tooltip.add("Min Size: 3x3x3");
    	tooltip.add("Max Size: 9x9x9");
    	super.addInformation(itemstack, player, tooltip, p_77624_4_);
    }
}
