package io.endertech.util.helper;

import java.text.DecimalFormat;

import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fluids.FluidStack;

public class StringHelper
{
    public static final String holdShiftForDetails = EnumChatFormatting.GRAY + LocalisationHelper.localiseString("info.hold_for_details.hold") + " " + EnumChatFormatting.YELLOW + EnumChatFormatting.ITALIC + LocalisationHelper.localiseString("info.hold_for_details.shift") + EnumChatFormatting.RESET + " " + EnumChatFormatting.GRAY + LocalisationHelper.localiseString("info.hold_for_details.for_details") + EnumChatFormatting.RESET;
    public static DecimalFormat twoDP = new DecimalFormat("#.##");

    public static String getEnergyString(int energy)
    {
        if (energy == Integer.MAX_VALUE) return LocalisationHelper.localiseString("info.infinite");

        if (energy >= 1000000)
        {
            return String.valueOf(twoDP.format(energy / 1000000.0)) + "M";
        } else if (energy >= 1000)
        {
            return String.valueOf(energy / 1000) + "k";
        } else
        {
            return String.valueOf(energy);
        }
    }

    public static String getFluidName(FluidStack fluidStack)
    {
        return cofh.lib.util.helpers.StringHelper.getFluidName(fluidStack);
    }
}
