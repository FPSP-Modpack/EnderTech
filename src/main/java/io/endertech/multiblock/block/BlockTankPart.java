package io.endertech.multiblock.block;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cofh.api.block.IDismantleable;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.endertech.EnderTech;
import io.endertech.block.BlockET;
import io.endertech.multiblock.IMultiblockPart;
import io.endertech.multiblock.MultiblockControllerBase;
import io.endertech.multiblock.controller.ControllerTank;
import io.endertech.multiblock.tile.TileTankEnergyInput;
import io.endertech.multiblock.tile.TileTankPart;
import io.endertech.multiblock.tile.TileTankValve;
import io.endertech.reference.Strings;
import io.endertech.util.BlockCoord;
import io.endertech.util.IOutlineDrawer;
import io.endertech.util.helper.LocalisationHelper;
import io.endertech.util.helper.LogHelper;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.common.util.ForgeDirection;

public class BlockTankPart extends BlockET implements ITileEntityProvider, IOutlineDrawer, IDismantleable
{
    public static final int FRAME_METADATA_BASE = 0; // Frame metadata
    public static final int FRAME_CORNER = 1;
    public static final int FRAME_CENTER = 2;
    public static final int FRAME_VERTICAL = 3;
    public static final int FRAME_EASTWEST = 4;
    public static final int FRAME_NORTHSOUTH = 5;

    public static final int VALVE_BASE = 6;

    public static final int ENERGY_INPUT_BASE = 7;

    public static final String TEXTURE_BASE = "endertech:enderTankPart";

    public static ItemStack itemBlockTankFrame;
    public static ItemStack itemBlockTankValve;
    public static ItemStack itemBlockTankEnergyInput;

    private static String[] _subBlocks = new String[] {"frameDefault", "frameCorner", "frameCenter", "frameVertical", "frameEastWest", "frameNorthSouth", "valve", "energyInput"};

    private IIcon[] _icons = new IIcon[_subBlocks.length];

    public BlockTankPart()
    {
        super(Material.iron);
        setHardness(10.0f);
        setResistance(20.0f);
        this.setCreativeTab(EnderTech.tabET);
        this.setBlockName(Strings.Blocks.TANK_PART_NAME);
        this.setBlockTextureName(TEXTURE_BASE);
    }

    public static boolean isFrame(int metadata) { return metadata >= FRAME_METADATA_BASE && metadata <= FRAME_NORTHSOUTH; }

    public static boolean isValve(int metadata) { return metadata == VALVE_BASE; }

    public static boolean isEnergyInput(int metadata) { return metadata == ENERGY_INPUT_BASE; }

    public static boolean onTankBlockActivated(World world, int x, int y, int z, EntityPlayer player, int faceHit, float par7, float par8, float par9)
    {
        if (player.isSneaking())
        {
            return false;
        }

        if (!world.isRemote)
        {
            ItemStack currentEquippedItem = player.getCurrentEquippedItem();

            if (currentEquippedItem == null)
            {
                TileEntity te = world.getTileEntity(x, y, z);
                if (te instanceof IMultiblockPart)
                {
                    MultiblockControllerBase controller = ((IMultiblockPart) te).getMultiblockController();
                    if (controller != null)
                    {
                        player.addChatComponentMessage(new ChatComponentText("Ender Tank"));
                        List<String> tooltipLines = controller.getWailaBody();
                        for (String line : tooltipLines)
                            player.addChatComponentMessage(new ChatComponentText(" " + line));

                        Exception e = controller.getLastValidationException();
                        if (e != null)
                        {
                            player.addChatComponentMessage(new ChatComponentText(LocalisationHelper.localiseString("info.multiblock.construction.last_failure_reason.pre")));
                            player.addChatComponentMessage(new ChatComponentText(e.getMessage()));
                        }
                    } else
                    {
                        player.addChatComponentMessage(new ChatComponentText(LocalisationHelper.localiseString("info.multiblock.construction.no_controller")));
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static boolean canPlaceTankPartAt(World world, int x, int y, int z)
    {
        Set<Integer> randomNumbers = new HashSet<Integer>();
        for (ForgeDirection neighbour : ForgeDirection.VALID_DIRECTIONS)
        {
            TileEntity tile = world.getTileEntity(x + neighbour.offsetX, y + neighbour.offsetY, z + neighbour.offsetZ);
            if (tile instanceof TileTankPart)
            {
                ControllerTank controller = ((TileTankPart) tile).getTankController();
                if (controller != null)
                {
                    boolean hasContents = controller.tank.getFluidAmount() > 0 || controller.getStoredEnergy() > 0;
                    if (hasContents)
                    {
                        int randomNumber = controller.getRandomNumber();

                        if (!randomNumbers.contains(randomNumber))
                        {
                            randomNumbers.add(randomNumber);
                        }

                        if (randomNumbers.size() > 1)
                        {
                            LogHelper.info(LocalisationHelper.localiseString("info.multiblock.tank.connecting_destructive", new BlockCoord(x, y, z).toString()));
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    public static boolean isLastPartWithContents(World world, BlockCoord coord)
    {
        TileEntity tile = world.getTileEntity(coord.x, coord.y, coord.z);
        if (!(tile instanceof TileTankPart)) return false;

        ControllerTank controller = ((TileTankPart) tile).getTankController();
        if (controller == null || (controller.getStoredEnergy() <= 0 && controller.tank.getFluidAmount() <= 0))
            return false;

        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS)
        {
            TileEntity neighbourTile = world.getTileEntity(coord.x + direction.offsetX, coord.y + direction.offsetY, coord.z + direction.offsetZ);
            if (neighbourTile instanceof TileTankPart) return false;
        }

        return true;
    }

    public static boolean canDismantleTankBlock(EntityPlayer player, World world, int x, int y, int z)
    {
        boolean dismantleWouldBeDestructive = isLastPartWithContents(world, new BlockCoord(x, y, z));
        if (dismantleWouldBeDestructive)
            player.addChatComponentMessage(new ChatComponentText(LocalisationHelper.localiseString("warning.tank.dismantle_loss")));

        return !dismantleWouldBeDestructive;
    }

    public void init()
    {
        TileTankPart.init();
        TileTankValve.init();
        TileTankEnergyInput.init();

        itemBlockTankFrame = new ItemStack(this, 1, FRAME_METADATA_BASE);
        itemBlockTankValve = new ItemStack(this, 1, VALVE_BASE);
        itemBlockTankEnergyInput = new ItemStack(this, 1, ENERGY_INPUT_BASE);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata)
    {
        if (metadata >= FRAME_METADATA_BASE && metadata <= FRAME_NORTHSOUTH) return new TileTankPart();
        if (metadata == VALVE_BASE) return new TileTankValve();
        if (metadata == ENERGY_INPUT_BASE) return new TileTankEnergyInput();

        throw new IllegalArgumentException("Unrecognized metadata: " + metadata);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int faceHit, float par7, float par8, float par9)
    {
        boolean overridden = super.onBlockActivated(world, x, y, z, player, faceHit, par7, par8, par9);

        if (!overridden)
            overridden = BlockTankPart.onTankBlockActivated(world, x, y, z, player, faceHit, par7, par8, par9);

        return overridden;
    }

    @Override
    public boolean isOpaqueCube()
    {
        return true;
    }

    @Override
    public boolean renderAsNormalBlock()
    {
        return true;
    }

    public ItemStack getTankFrameItemStack()
    {
        return new ItemStack(this, 1, FRAME_METADATA_BASE);
    }

    public ItemStack getTankValveItemStack() { return new ItemStack(this, 1, VALVE_BASE); }

    public ItemStack getTankEnergyInputItemStack() { return new ItemStack(this, 1, ENERGY_INPUT_BASE); }

    @Override
    public void getSubBlocks(Item item, CreativeTabs par2CreativeTabs, List par3List)
    {
        par3List.add(getTankFrameItemStack());
        par3List.add(getTankValveItemStack());
        par3List.add(getTankEnergyInputItemStack());
    }

    @Override
    public boolean drawOutline(DrawBlockHighlightEvent event)
    {
        BlockCoord target = new BlockCoord(event.target.blockX, event.target.blockY, event.target.blockZ);
        World world = event.player.worldObj;

        TileEntity tile = world.getTileEntity(target.x, target.y, target.z);
        if (tile instanceof TileTankPart)
        {
            return ((TileTankPart) tile).drawOutline(event);
        }

        return false;
    }

    @Override
    public int damageDropped(int meta)
    {
        if (isFrame(meta)) return FRAME_METADATA_BASE;
        else if (isValve(meta)) return VALVE_BASE;
        else if (isEnergyInput(meta)) return ENERGY_INPUT_BASE;

        return FRAME_METADATA_BASE;
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z)
    {
        return canPlaceTankPartAt(world, x, y, z);
    }

    @Override
    public IIcon getIcon(int side, int metadata)
    {
        // Casing block
        switch (metadata)
        {
            case FRAME_METADATA_BASE:
                return _icons[FRAME_METADATA_BASE];

            case FRAME_CORNER:
                return _icons[FRAME_CORNER];

            case FRAME_CENTER:
                return _icons[FRAME_CENTER];

            case FRAME_VERTICAL:
                // Vertical block
                if (side == 0 || side == 1)
                {
                    return _icons[FRAME_METADATA_BASE];
                } else
                {
                    return _icons[metadata];
                }
            case FRAME_EASTWEST:
                // X-aligned block (e/w)
                if (side == 4 || side == 5)
                {
                    return _icons[FRAME_METADATA_BASE];
                } else
                {
                    return _icons[metadata];
                }
            case FRAME_NORTHSOUTH:
                // Z-aligned block (n/s)
                if (side == 2 || side == 3)
                {
                    return _icons[FRAME_METADATA_BASE];
                } else if (side == 4 || side == 5)
                {
                    // I hate everything
                    return _icons[FRAME_EASTWEST];
                } else
                {
                    return _icons[metadata];
                }
            case VALVE_BASE:
                return _icons[VALVE_BASE];

            case ENERGY_INPUT_BASE:
                return _icons[ENERGY_INPUT_BASE];

            default:
                if (side == 0 || side == 1)
                {
                    return _icons[FRAME_METADATA_BASE];
                } else
                {
                    metadata = Math.max(0, Math.min(6, metadata));
                    return _icons[metadata];
                }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister)
    {
        this.blockIcon = iconRegister.registerIcon(TEXTURE_BASE);

        for (int i = 0; i < _subBlocks.length; ++i)
        {
            _icons[i] = iconRegister.registerIcon(TEXTURE_BASE + "." + _subBlocks[i]);
        }
    }

    @Override
    public ArrayList<ItemStack> dismantleBlock(EntityPlayer player, World world, int x, int y, int z, boolean returnDrops)
    {
        return BlockET.dismantleBlockInWorld(player, world, x, y, z, returnDrops);
    }

    @Override
    public boolean canDismantle(EntityPlayer player, World world, int x, int y, int z)
    {
        return canDismantleTankBlock(player, world, x, y, z);
    }

    @Override
	public boolean canCreatureSpawn(EnumCreatureType type, IBlockAccess world, int x, int y, int z)
    {
        return false;
    }
}
