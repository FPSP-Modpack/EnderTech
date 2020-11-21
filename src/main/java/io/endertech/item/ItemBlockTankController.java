package io.endertech.item;

import java.util.List;

import io.endertech.block.ItemBlockBasic;
import io.endertech.config.GeneralConfig;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class ItemBlockTankController extends ItemBlockBasic {

	public ItemBlockTankController(Block block) {
		super(block);
	}
	
	@Override
	public void addInformation(ItemStack itemstack, EntityPlayer player, List tooltip, boolean p_77624_4_) {
		tooltip.add("Capacity: (x-1)*(y-1)*(z-1)*1000*" + GeneralConfig.tankStorageMultiplier);
		super.addInformation(itemstack, player, tooltip, p_77624_4_);
	}

}
