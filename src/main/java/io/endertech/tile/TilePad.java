package io.endertech.tile;

import java.awt.Color;
import java.util.Random;

import cofh.api.energy.IEnergyContainerItem;
import cofh.api.energy.IEnergyHandler;
import cofh.api.tileentity.IReconfigurableFacing;
import cofh.lib.util.helpers.EnergyHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.endertech.EnderTech;
import io.endertech.block.BlockPad;
import io.endertech.config.GeneralConfig;
import io.endertech.network.PacketETBase;
import io.endertech.util.IChargeableFromSlot;
import io.endertech.util.IOutlineDrawer;
import io.endertech.util.RGBA;
import io.endertech.util.helper.LocalisationHelper;
import io.endertech.util.helper.NBTHelper;
import io.endertech.util.helper.RenderHelper;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.common.util.ForgeDirection;

public abstract class TilePad extends TileInventory implements IReconfigurableFacing, IEnergyHandler, IOutlineDrawer, IChargeableFromSlot
{
    public static final short TICKS_PER_UPDATE = 20;
    public static final int INVENTORY_SIZE = 1;
    public short ticksSinceLastUpdate = 0;
    public boolean isActive = false;
    public int storedEnergy = 0;
    public boolean isCreative = false;

    public TilePad()
    {
        super();

        this.inventory = new ItemStack[INVENTORY_SIZE];
    }

    @Override
	public String getName()
    {
        Block block = this.worldObj.getBlock(this.xCoord, this.yCoord, this.zCoord);
        int blockMeta = this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord);

        return LocalisationHelper.localiseString(block.getUnlocalizedName() + "." + blockMeta + ".name");
    }

    public static void writeDefaultTag(NBTTagCompound nbtTagCompound)
    {
        nbtTagCompound.setInteger("Energy", 0);
        NBTHelper.writeInventoryToNBT(nbtTagCompound, new ItemStack[INVENTORY_SIZE]);
    }

    @Override
    public int getFacing()
    {
        return this.getOrientation().ordinal();
    }

    @Override
    public boolean allowYAxisFacing()
    {
        return true;
    }

    @Override
    public boolean rotateBlock()
    {
        int orientation = this.getFacing();
        orientation++;
        if (orientation >= ForgeDirection.VALID_DIRECTIONS.length) orientation = 0;

        return this.setFacing(orientation);
    }

    @Override
    public boolean setFacing(int side)
    {
        if (side == this.getOrientation().ordinal()) return false;
        else
        {
            this.setOrientation(side);
            this.sendDescriptionPacket();
            return true;
        }
    }

    @Override
    public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate)
    {
        boolean canReceive = from != this.getOrientation();
        if (!canReceive) return 0;

        return this.receiveEnergy(maxReceive, simulate);
    }

    @Override
    public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate)
    {
        return 0;
    }

    public int extractEnergy(int maxExtract, int meta, boolean simulate)
    {
        int energyExtracted = Math.min(this.storedEnergy, Math.min(this.getMaxSendRate(meta), maxExtract));

        if (!simulate)
        {
            this.storedEnergy -= energyExtracted;
        }

        return energyExtracted;
    }

    @Override
    public int getEnergyStored(ForgeDirection from)
    {
        return this.storedEnergy;
    }

    @Override
    public int getMaxEnergyStored(ForgeDirection from)
    {
        int blockMeta = this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord);
        return this.getMaxEnergyStored(blockMeta);
    }

    @Override
    public boolean canConnectEnergy(ForgeDirection from)
    {
        return from != this.getOrientation();
    }

    @Override
    public boolean hasItemState()
    {
        return true;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbtTagCompound)
    {
        super.readFromNBT(nbtTagCompound);

        this.readStateFromNBT(nbtTagCompound);
    }

    @Override
    public void readStateFromNBT(NBTTagCompound nbtTagCompound)
    {
        if (nbtTagCompound.hasKey("Energy")) this.storedEnergy = nbtTagCompound.getInteger("Energy");

        if (nbtTagCompound.hasKey("Inventory"))
        {
            this.inventory = NBTHelper.readInventoryFromNBT(nbtTagCompound, INVENTORY_SIZE);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbtTagCompound)
    {
        super.writeToNBT(nbtTagCompound);

        this.writeStateToNBT(nbtTagCompound);
    }

    @Override
    public void writeStateToNBT(NBTTagCompound nbtTagCompound)
    {
        nbtTagCompound.setInteger("Energy", this.storedEnergy);
        NBTHelper.writeInventoryToNBT(nbtTagCompound, this.inventory);
    }

    @Override
    public PacketETBase getPacket()
    {
        PacketETBase packet = super.getPacket();
        packet.addBool(this.isActive);
        packet.addInt(this.storedEnergy);
        packet.addInventory(this.inventory);

        return packet;
    }

    @Override
    public void handleTilePacket(PacketETBase tilePacket, boolean isServer)
    {
        super.handleTilePacket(tilePacket, isServer);

        boolean isActive = tilePacket.getBool();
        int storedEnergy = tilePacket.getInt();
        ItemStack[] inventory = tilePacket.getInventory(INVENTORY_SIZE);

        if (!isServer)
        {
            this.isActive = isActive;
            this.storedEnergy = storedEnergy;
            this.inventory = inventory;
        }
    }

    public AxisAlignedBB getAABBInFront(int distance)
    {
        ForgeDirection orientation = this.getOrientation();
        return AxisAlignedBB.getBoundingBox(this.xCoord, this.yCoord, this.zCoord, this.xCoord + 1, this.yCoord + 1, this.zCoord + 1).expand(Math.abs(orientation.offsetX) * (distance - 1), Math.abs(orientation.offsetY) * (distance - 1), Math.abs(orientation.offsetZ) * (distance - 1)).offset(orientation.offsetX, orientation.offsetY, orientation.offsetZ);
    }

    @Override
    public boolean hasGui()
    {
        return true;
    }

    @Override
	public int getChargeSlot()
    {
        return this.inventory.length - 1;
    }

    @Override
	public boolean hasChargeSlot() { return true; }

    @Override
	public void chargeFromGUISlot()
    {
        if (this.isCreative) return;

        int chargeSlot = getChargeSlot();
        ItemStack chargeItemStack = this.inventory[chargeSlot];
        if (!this.hasChargeSlot() || !EnergyHelper.isEnergyContainerItem(chargeItemStack)) return;

        int meta = this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord);
        int chargeAmount = Math.min(this.getMaxReceiveRate(meta), this.getMaxEnergyStored(meta) - this.getEnergyStored());

        IEnergyContainerItem energyContainerItem = (IEnergyContainerItem) chargeItemStack.getItem();
        if (energyContainerItem == null) return;

        int extractedAmount = energyContainerItem.extractEnergy(chargeItemStack, chargeAmount, false);
        this.receiveEnergy(extractedAmount, false);

        if (chargeItemStack.stackSize <= 0) this.inventory[chargeSlot] = null;
    }

    @Override
    public boolean drawOutline(DrawBlockHighlightEvent event)
    {
        if (GeneralConfig.debugRender)
        {
            AxisAlignedBB front = this.getAABBInFront(2);
            RenderHelper.renderAABBOutline(event.context, event.player, front, RGBA.Red.setAlpha(0.6f), 2.0f, event.partialTicks);
            return true;
        } else
        {
            return false;
        }
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate)
    {
        return 0;
    }

    @Override
    public int getEnergyStored()
    {
        return this.storedEnergy;
    }

    @SideOnly(Side.CLIENT)
    protected float[] getRainbowParticleColour(Random rand)
    {
        final float hue = rand.nextFloat();
        final float saturation = 0.9f;
        final float luminance = 1.0f;
        Color color = Color.getHSBColor(hue, saturation, luminance);

        float r = color.getRed() / 255.0F;
        float g = color.getBlue() / 255.0F;
        float b = color.getGreen() / 255.0F;

        return new float[] {r, g, b};
    }

    protected boolean isItemInChargeSlotTuberous()
    {
        int slot = this.getChargeSlot();
        ItemStack itemStack = this.inventory[slot];
        if (itemStack == null) return false;

        Item item = itemStack.getItem();
        if (item == null) return false;

        if (item == EnderTech.capacitor && itemStack.getItemDamage() == 1) return true;

        return false;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate)
    {
        if (this.isCreative) return 0;

        int blockMeta = this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord);
        int energyReceived = Math.min(this.getMaxEnergyStored(blockMeta) - this.storedEnergy, Math.min(this.getMaxReceiveRate(blockMeta), maxReceive));

        if (!simulate)
        {
            this.storedEnergy += energyReceived;
        }

        return energyReceived;
    }

    @Override
    public int getMaxEnergyStored()
    {
        int blockMeta = this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord);
        return getMaxEnergyStored(blockMeta);
    }

    public IIcon getFrontIcon()
    {
        Block block = this.worldObj.getBlock(this.xCoord, this.yCoord, this.zCoord);

        if (!(block instanceof BlockPad)) return null;

        int blockMeta = this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord);
        BlockPad blockPad = (BlockPad) block;

        if (this.isActive) return blockPad.getActiveIcon(blockMeta);
        else return blockPad.getInactiveIcon(blockMeta);
    }

    public double calculateEfficiencyForEntity(Entity entity)
    {
        double xDiff = Math.abs(((this.xCoord + 0.5D) - entity.posX) * orientation.offsetX);
        double yDiff = Math.abs(((this.yCoord + 0.5D) - entity.posY) * orientation.offsetY);
        double zDiff = Math.abs(((this.zCoord + 0.5D) - entity.posZ) * orientation.offsetZ);
        double distance = xDiff + yDiff + zDiff - 0.5;
        if (orientation == ForgeDirection.DOWN) distance -= 1.5;
        if (distance < 0.3) distance = 0;
        if (distance > 1.5) distance = 1.5;

        double efficiency = ((2.0 - distance) / 2.0);
        if (distance < 0.9) efficiency += 0.2;
        efficiency = Math.max(0.5, efficiency);
        efficiency = Math.min(1, efficiency);

        return efficiency;
    }

    public abstract int getMaxEnergyStored(int meta);

    public abstract int getMaxReceiveRate(int meta);

    public abstract int getMaxSendRate(int meta);

    @SideOnly(Side.CLIENT)
    public abstract void spawnParticles(int meta);

    @SideOnly(Side.CLIENT)
    public abstract float[] getParticleColour(Random rand);

    @SideOnly(Side.CLIENT)
    public abstract int getParticleMaxAge();

    @SideOnly(Side.CLIENT)
    public abstract double[] getParticleVelocity();

    @SideOnly(Side.CLIENT)
    public abstract int getParticleCount(int meta);

    @SideOnly(Side.CLIENT)
    public abstract float getParticleSizeModifier(int meta);
}