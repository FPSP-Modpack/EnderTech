package io.endertech.config;

import java.io.File;

import io.endertech.util.helper.LocalisationHelper;
import io.endertech.util.helper.LogHelper;
import net.minecraftforge.common.config.Configuration;

public class ItemConfig
{
    public static int itemExchangerBaseCost;
    public static int itemExchangerRadiusCost;
    public static int itemExchangerHardnessCost;
    public static int itemExchangerMaxRadius;
    public static int itemExchangerMinimumCost;
    public static int itemExchangerMaximumCost;
    public static boolean itemExchangerSilkTouch;
    private static Configuration itemConfig;

    protected static void init(File configFile)
    {
        itemConfig = new Configuration(configFile);
        try
        {
            itemConfig.load();
            itemExchangerBaseCost = itemConfig.get("item.general", "Exchanger.BaseCost", 2048).getInt();
            itemExchangerRadiusCost = itemConfig.get("item.general", "Exchanger.RadiusCost", 128).getInt(128);
            itemExchangerHardnessCost = itemConfig.get("item.general", "Exchanger.HardnessCost", 64).getInt(64);
            itemExchangerMinimumCost = itemConfig.get("item.general", "Exchanger.MinimumCost", 2048).getInt(2048);
            itemExchangerMaximumCost = itemConfig.get("item.general", "Exchanger.MaximumCost", 16384).getInt(16384);
            itemExchangerMaxRadius = itemConfig.get("item.general", "Exchanger.MaxRadius", 8).getInt(8);
            itemExchangerSilkTouch = itemConfig.get("item.general", "Exchanger.SilkTouch", true).getBoolean(true);

            if (itemExchangerMaxRadius < 1)
            {
                itemExchangerMaxRadius = 1;
            }
            if (itemExchangerMaxRadius > 64)
            {
                itemExchangerMaxRadius = 64;
            }
        } catch (Exception e)
        {
            LogHelper.error(LocalisationHelper.localiseString("error.config.item.load"));
        } finally
        {
            itemConfig.save();
        }
    }
}