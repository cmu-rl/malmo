package com.microsoft.Malmo.MissionHandlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import com.microsoft.Malmo.MissionHandlerInterfaces.IWantToQuit;
import com.microsoft.Malmo.Schemas.AgentQuitFromCraftingItem;
import com.microsoft.Malmo.Schemas.BlockOrItemSpecWithDescription;
import com.microsoft.Malmo.Schemas.MissionInit;

/**
 * @author Cayden Codel, Carnegie Mellon University
 *
 *         Gives agents rewards when items are crafted. Handles variants and colors.
 */
public class AgentQuitFromCraftingItemImplementation extends HandlerBase implements IWantToQuit {

	private AgentQuitFromCraftingItem params;
	private HashMap<String, Integer> craftedItems;
	private List<ItemQuitMatcher> matchers;
	private String quitCode = "";
	private boolean wantToQuit = false;
	private boolean callCraft = true;

	public static class ItemQuitMatcher extends RewardForItemBase.ItemMatcher {
		String description;

		ItemQuitMatcher(BlockOrItemSpecWithDescription spec) {
			super(spec);
			this.description = spec.getDescription();
		}

		String description() {
			return this.description;
		}
	}

	@Override
	public boolean parseParameters(Object params) {
		if (!(params instanceof AgentQuitFromCraftingItem))
			return false;

		this.params = (AgentQuitFromCraftingItem) params;
		this.matchers = new ArrayList<ItemQuitMatcher>();
		for (BlockOrItemSpecWithDescription bs : this.params.getItem())
			this.matchers.add(new ItemQuitMatcher(bs));
		return true;
	}

	@Override
	public boolean doIWantToQuit(MissionInit missionInit) {
		return this.wantToQuit;
	}

	@Override
	public String getOutcome() {
		return this.quitCode;
	}

	@Override
	public void prepare(MissionInit missionInit) {
		MinecraftForge.EVENT_BUS.register(this);
		craftedItems = new HashMap<String, Integer>();
	}

	@Override
	public void cleanup() {
		MinecraftForge.EVENT_BUS.unregister(this);
	}

	@SubscribeEvent
	public void onItemCraft(PlayerEvent.ItemCraftedEvent event) {
		if (callCraft)
			checkForMatch(event.crafting);

		callCraft = !callCraft;
	}

	/**
	 * Checks whether the ItemStack matches a variant stored in the item list. If
	 * so, returns true, else returns false.
	 *
	 * @param is The item stack
	 * @return If the stack is allowed in the item matchers and has color or
	 * variants enabled, returns true, else false.
	 */
	private boolean getVariant(ItemStack is) {
		for (ItemQuitMatcher matcher : matchers) {
			if (matcher.allowedItemTypes.contains(is.getItem().getUnlocalizedName())) {
				if (matcher.matchSpec.getColour() != null && matcher.matchSpec.getColour().size() > 0)
					return true;
				if (matcher.matchSpec.getVariant() != null && matcher.matchSpec.getVariant().size() > 0)
					return true;
			}
		}

		return false;
	}

	private int getCraftedItemCount(ItemStack is) {
		boolean variant = getVariant(is);

		if (variant)
			return (craftedItems.get(is.getUnlocalizedName()) == null) ? 0 : craftedItems.get(is.getUnlocalizedName());
		else
			return (craftedItems.get(is.getItem().getUnlocalizedName()) == null) ? 0
					: craftedItems.get(is.getItem().getUnlocalizedName());
	}

	private void addCraftedItemCount(ItemStack is) {
		boolean variant = getVariant(is);

		int prev = (craftedItems.get(is.getUnlocalizedName()) == null ? 0
				: craftedItems.get(is.getUnlocalizedName()));
		if (variant)
			craftedItems.put(is.getUnlocalizedName(), prev + is.getCount());
		else
			craftedItems.put(is.getItem().getUnlocalizedName(), prev + is.getCount());
	}

	private void checkForMatch(ItemStack is) {
		int savedCrafted = getCraftedItemCount(is);
		if (is != null) {
			for (ItemQuitMatcher matcher : this.matchers) {
				if (matcher.matches(is)) {
					if (savedCrafted != 0) {
						if (is.getCount() + savedCrafted >= matcher.matchSpec.getAmount()) {
							this.quitCode = matcher.description();
							this.wantToQuit = true;
						}
					} else if (is.getCount() >= matcher.matchSpec.getAmount()) {
						this.quitCode = matcher.description();
						this.wantToQuit = true;
					}
				}
			}

			addCraftedItemCount(is);
		}
	}
}
