package io.endertech.handler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import io.endertech.config.ItemConfig;
import io.endertech.item.ItemExchanger;
import io.endertech.util.BlockCoord;
import io.endertech.util.Exchange;
import io.endertech.util.Geometry;
import io.endertech.util.helper.BlockHelper;
import io.endertech.util.helper.LogHelper;
import io.endertech.util.inventory.InventoryHelper;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

public class WorldEventHandler
{
    // Do exchanges per dimension
    public static Map<Integer, Set<Exchange>> exchanges = new HashMap();

    public static void queueExchangeRequest(World world, BlockCoord origin, int radius, Block source, int sourceMeta, ItemStack target, EntityPlayer player, int hotbar_id, ForgeDirection orientation)
    {
        if (target.isItemEqual(new ItemStack(source, 1, sourceMeta)))
        {
            return;
        }

        int dimensionId = world.provider.dimensionId;
        Set<Exchange> queue = exchanges.get(dimensionId);

        if (queue == null)
        {
            exchanges.put(dimensionId, new LinkedHashSet());
            queue = exchanges.get(dimensionId);
        }

        queue.add(new Exchange(origin, radius, source, sourceMeta, target, player, hotbar_id, orientation));
        world.playSoundAtEntity(player, "mob.endermen.portal", 1.0F, 1.0F);
        exchanges.put(dimensionId, queue);
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event)
    {
        exchangeTick(event.world);
    }

    private void exchangeTick(World world)
    {
        int dimensionId = world.provider.dimensionId;
        Set<Exchange> queue = exchanges.get(dimensionId);
        if (queue == null || queue.size() == 0) return;

        checkAndPerformExchanges(queue, world);
    }

    private void checkAndPerformExchanges(Set<Exchange> queue, World world)
    {
        Set<Exchange> removals = new HashSet<Exchange>();
        for (Exchange exchange : queue)
        {
            ItemStack exchangerStack = exchange.player.inventory.getStackInSlot(exchange.hotbar_id);
            boolean cullExchange = false;
            if (exchangerStack == null || exchangerStack.getItem() == null || !(exchangerStack.getItem() instanceof ItemExchanger))
                cullExchange = true;

            if (cullExchange)
            {
                removals.add(exchange);
                continue;
            }

            ItemExchanger exchanger = (ItemExchanger) exchangerStack.getItem();

            exchange.currentRadiusTicks--;
            if (exchange.currentRadiusTicks > 0) continue;

            Set<BlockCoord> blocks = Geometry.squareSet(exchange.currentRadius - 1, exchange.origin, exchange.orientation);
            boolean stop = false;
            for (BlockCoord blockCoord : blocks)
            {
                if (stop) break;
                ExchangeResult result = checkAndPerformExchange(exchange, exchanger, exchangerStack, world, new BlockCoord(blockCoord.x, blockCoord.y, blockCoord.z));
                switch (result)
                {
                    case FAIL_ENERGY:
                    case FAIL_NO_SOURCE_BLOCKS:
                    case FAIL_INVENTORY_SPACE:
                    case FAIL_SOURCE_NOT_CONSUMED:
                    case FAIL_NULL_ITEM:
                        stop = true;
                        removals.add(exchange);
                        break;
                }
            }

            exchange.currentRadius++;
            if (exchange.currentRadius > exchange.radius) removals.add(exchange);
            else exchange.currentRadiusTicks = Exchange.radiusTicksDefault;
        }

        for (Exchange removal : removals)
            queue.remove(removal);
    }

    private int calculateExchangeCost(Exchange exchange, Block worldBlock, World world, BlockCoord blockCoord)
    {
        int baseCost = ItemConfig.itemExchangerBaseCost;
        int radiusCost = ItemConfig.itemExchangerRadiusCost;
        double blockHardness = worldBlock.getBlockHardness(world, blockCoord.x, blockCoord.y, blockCoord.z);
        if (blockHardness < 1) blockHardness = 1;
        if (blockHardness > 50) blockHardness = 50;

        int hardnessCost = ((int) blockHardness) * ItemConfig.itemExchangerHardnessCost;

        int exchangeCost = baseCost + (radiusCost * exchange.currentRadius) + hardnessCost;
        if (exchangeCost < ItemConfig.itemExchangerMinimumCost) exchangeCost = ItemConfig.itemExchangerMinimumCost;
        if (exchangeCost > ItemConfig.itemExchangerMaximumCost) exchangeCost = ItemConfig.itemExchangerMaximumCost;

        //LogHelper.info("Exchange cost: " + exchangeCost);
        return exchangeCost;
    }

    private ExchangeResult checkAndPerformExchange(Exchange exchange, ItemExchanger exchanger, ItemStack exchangerStack, World world, BlockCoord blockCoord)
    {
        Block block = world.getBlock(blockCoord.x, blockCoord.y, blockCoord.z);

        if (!Exchange.blockSuitableForExchange(blockCoord, world, exchange.source, exchange.sourceMeta, exchange.target, exchangerStack, exchange.currentRadius - 1))
            return ExchangeResult.FAIL_BLOCK_NOT_REPLACEABLE;

        int exchangeCost = this.calculateExchangeCost(exchange, block, world, blockCoord);
        if (exchanger.extractEnergy(exchange.player.inventory.getStackInSlot(exchange.hotbar_id), exchangeCost, true) < exchangeCost)
            return ExchangeResult.FAIL_ENERGY;

        int sourceSlot = InventoryHelper.findFirstItemStack(exchange.player.inventory, exchange.target);

        boolean isCreativeExchanger = ItemExchanger.isCreative(exchangerStack);

        if (sourceSlot < 0 && !isCreativeExchanger)
        {
            return ExchangeResult.FAIL_NO_SOURCE_BLOCKS;
        }

        if (!isCreativeExchanger) {
            ItemStack beforeConsumedStack = exchange.player.inventory.getStackInSlot(sourceSlot);
            int beforeConsumedSize = (beforeConsumedStack == null) ? 0 : beforeConsumedStack.stackSize;

            if (beforeConsumedSize == 1 && (exchange.player instanceof FakePlayer)) {
                LogHelper.debug("Not consuming item as it is the last in a Fake Player's stack: " + exchange.player.getPosition(1.0F));

                return ExchangeResult.FAIL_SOURCE_NOT_CONSUMED;
            }

            List<ItemStack> droppedItems = this.harvestBlockWithSilkTouchIfRequired(block, exchange, blockCoord);
            if (droppedItems == null) {
                LogHelper.warn("Player " + exchange.player.getDisplayName() + " tried to exchange something, but the droppedItems were null - bailing the entire exchange: " + block.toString());
                return ExchangeResult.FAIL_NULL_ITEM;
            }

            boolean containsNullItemInDroppedItems = false;
            for (ItemStack droppedItem : droppedItems) {
                if (droppedItem.getItem() == null) {
                    LogHelper.warn("Player " + exchange.player.getDisplayName() + " tried to exchange a null item - bailing the entire exchange: " + droppedItem.toString());
                    containsNullItemInDroppedItems = true;
                    break;
                }
            }

            if (containsNullItemInDroppedItems) {
                return ExchangeResult.FAIL_NULL_ITEM;
            }

            boolean canPutItemsInInventory = InventoryHelper.canPutItemStacksInToInventory(exchange.player.inventory, droppedItems);
            if (!canPutItemsInInventory) {
                return ExchangeResult.FAIL_INVENTORY_SPACE;
            }

            boolean didPutItemsInInventory = InventoryHelper.checkAndPutItemStacksInToInventory(exchange.player.inventory, droppedItems);
            if (didPutItemsInInventory)
            {
                ItemStack itemsConsumed = InventoryHelper.consumeItem(exchange.player.inventory, sourceSlot);

                ItemStack afterConsumedStack = exchange.player.inventory.getStackInSlot(sourceSlot);
                int afterConsumedSize = (afterConsumedStack == null) ? 0 : afterConsumedStack.stackSize;

                if (itemsConsumed == null || itemsConsumed.stackSize < 1 || afterConsumedSize >= beforeConsumedSize) {
                    // Didn't actually reduce the stack size. Bail.
                    return ExchangeResult.FAIL_SOURCE_NOT_CONSUMED;
                }

                performExchange(exchange, blockCoord, exchanger, world, exchangeCost);
            } else return ExchangeResult.FAIL_INVENTORY_SPACE;
        } else performExchange(exchange, blockCoord, exchanger, world, exchangeCost);

        return ExchangeResult.SUCCESS;
    }

    private List<ItemStack> harvestBlockWithSilkTouchIfRequired(Block block, Exchange exchange, BlockCoord blockCoord) {
        boolean canSilkTouch = block.canSilkHarvest(exchange.player.worldObj, exchange.player, blockCoord.x, blockCoord.y, blockCoord.z, exchange.sourceMeta);

        if (canSilkTouch && ItemConfig.itemExchangerSilkTouch) {
            return BlockHelper.createSilkTouchStack(block, exchange.sourceMeta);
        } else {
            return block.getDrops(exchange.player.worldObj, blockCoord.x, blockCoord.y, blockCoord.z, exchange.sourceMeta, 0);
        }
    }

    private void performExchange(Exchange exchange, BlockCoord blockCoord, ItemExchanger exchanger, World world, int exchangeCost)
    {
        world.setBlock(blockCoord.x, blockCoord.y, blockCoord.z, Block.getBlockFromItem(exchange.target.getItem()), exchange.target.getItemDamage(), 3);
        exchanger.extractEnergy(exchange.player.inventory.getStackInSlot(exchange.hotbar_id), exchangeCost, false);
        world.playAuxSFX(2001, blockCoord.x, blockCoord.y, blockCoord.z, Block.getIdFromBlock(exchange.source) + (exchange.sourceMeta << 12));
    }

    public static enum ExchangeResult
    {
        FAIL_ENERGY,
        FAIL_MISMATCH,
        FAIL_NO_SOURCE_BLOCKS,
        FAIL_SOURCE_NOT_CONSUMED,
        FAIL_INVENTORY_SPACE,
        FAIL_NULL_ITEM,
        FAIL_BLOCK_NOT_REPLACEABLE,
        FAIL_BLOCK_NOT_EXPOSED,
        SUCCESS
    }
}
