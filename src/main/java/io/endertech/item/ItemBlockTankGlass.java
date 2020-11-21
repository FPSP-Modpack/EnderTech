package io.endertech.item;

import java.util.List;

import io.endertech.block.ItemBlockBasic;
import io.endertech.config.GeneralConfig;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class ItemBlockTankGlass extends ItemBlockBasic {

	public ItemBlockTankGlass(Block block) {
		super(block);
	}
    
    @Override
    public void addInformation(ItemStack itemstack, EntityPlayer player, List tooltip, boolean p_77624_4_) {
    	tooltip.add("Min Size: 3x3x3");
    	tooltip.add("Max Size: 9x9x9");
    	super.addInformation(itemstack, player, tooltip, p_77624_4_);
    }

}
