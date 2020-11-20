package io.endertech.integration.waila;

import java.util.List;

import io.endertech.tile.TileET;
import io.endertech.util.IETWailaProvider;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaRegistrar;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class GenericWailaProvider implements IWailaDataProvider
{
    public static void callbackRegister(IWailaRegistrar registrar)
    {
        registrar.registerHeadProvider(new GenericWailaProvider(), TileET.class);
        registrar.registerBodyProvider(new GenericWailaProvider(), TileET.class);
    }

    @Override
    public ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config)
    {
        TileEntity tile = accessor.getTileEntity();
        if (tile instanceof IETWailaProvider)
        {
            return ((IETWailaProvider) tile).getWailaStack();
        }

        return null;
    }

    @Override
    public List<String> getWailaHead(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config)
    {
        TileEntity tile = accessor.getTileEntity();
        if (tile instanceof IETWailaProvider)
        {
            return ((IETWailaProvider) tile).getWailaHead(itemStack, currenttip);
        }

        return currenttip;
    }

    @Override
    public List<String> getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config)
    {
        TileEntity tile = accessor.getTileEntity();
        if (tile instanceof IETWailaProvider)
        {
            return ((IETWailaProvider) tile).getWailaBody(itemStack, currenttip);
        }

        return currenttip;
    }

    @Override
    public List<String> getWailaTail(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config)
    {
        TileEntity tile = accessor.getTileEntity();
        if (tile instanceof IETWailaProvider)
        {
            return ((IETWailaProvider) tile).getWailaTail(itemStack, currenttip);
        }

        return currenttip;
    }

    @Override
    public NBTTagCompound getNBTData(EntityPlayerMP player, TileEntity te, NBTTagCompound tag, World world, int x, int y, int z) {
        return tag;
    }
}