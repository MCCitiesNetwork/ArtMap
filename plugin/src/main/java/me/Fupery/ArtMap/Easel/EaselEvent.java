package me.Fupery.ArtMap.Easel;

import java.sql.SQLException;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.Fupery.ArtMap.ArtMap;
import me.Fupery.ArtMap.Canvas.CanvasSize;
import me.Fupery.ArtMap.api.Config.Lang;
import me.Fupery.ArtMap.IO.MapArt;
import me.Fupery.ArtMap.IO.Database.Map;
import me.Fupery.ArtMap.Recipe.ArtItem;
import me.Fupery.ArtMap.Recipe.ArtMaterial;
import me.Fupery.ArtMap.Utils.ItemUtils;
import me.Fupery.ArtMap.api.Easel.ClickType;
import me.Fupery.ArtMap.api.Exception.ArtMapException;
import net.md_5.bungee.api.ChatColor;

public final class EaselEvent {
	private final Easel easel;
	private final ClickType click;
	private final Player player;

	public EaselEvent(Easel easel, ClickType click, Player player) {
		this.easel = easel;
		this.click = click;
		this.player = player;
	}

	public void callEvent() {
		if (!player.hasPermission("artmap.artist")) {
			Lang.NO_PERM.send(player);
			return;
		}
		if (easel.isBeingUsed()) {
			Lang.ActionBar.ELSE_USING.send(player);
			easel.playEffect(EaselEffect.USE_DENIED);
			return;
		}
		if (ArtMap.instance().getPreviewManager().endPreview(player))
			return;

		switch (click) {
			case LEFT_CLICK:
				Lang.ActionBar.EASEL_HELP.send(player);
				return;
			case RIGHT_CLICK:
				if (easel.getItem().getType() == Material.FILLED_MAP) {
					try {
						OptionalInt mapId = ItemUtils.getMapID(easel.getItem());
						if (mapId.isPresent()) {
							CanvasSize size = CanvasSize.defaultSize();
							Optional<Canvas> mounted = Canvas.getCanvas(easel.getItem());
							if (mounted.isPresent()) {
								size = mounted.get().getSize();
							} else {
								size = ArtMap.instance().getArtDatabase().getMapCanvasSize(mapId.getAsInt());
							}
							ArtMap.instance().getArtistHandler().addPlayer(player, easel,
								new Map(mapId.getAsInt()),
								EaselPart.getYawOffset(easel.getFacing()),
								size);
						} else {
							ArtMap.instance().getLogger().warning("Broken map on easel for player " + player.getName());
							Lang.ERROR_ON_EASEL.send(player);
						}
					} catch (Exception e) {
						ArtMap.instance().getLogger().log(Level.SEVERE, "Failure having player mount the Easel!", e);
					}
					return;
				} else if (easel.getItem().getType() != Material.AIR) {
					try {
						easel.removeItem();
					} catch (SQLException | ArtMapException e) {
						ArtMap.instance().getLogger().log(Level.SEVERE, "Unexpected item on Easel!", e);
					}
					return;
				}
				ItemStack itemInHand = player.getInventory().getItemInMainHand();
				ArtMaterial material = ArtMaterial.getCraftItemType(itemInHand);

				if (CanvasSize.isCanvasMaterial(material)) {
					placeBlankCanvas(itemInHand, CanvasSize.fromArtMaterial(material));
					return;
				}
				if (ArtItem.isArtwork(itemInHand)) {
					placeArtworkForEdit(itemInHand);
					return;
				}
				Lang.ActionBar.NEED_CANVAS.send(player);
				easel.playEffect(EaselEffect.USE_DENIED);
				return;

		case SHIFT_RIGHT_CLICK:
			easel.breakEasel();
		}
	}

	private void placeBlankCanvas(ItemStack itemInHand, CanvasSize size) {
		if (easel.getItem().getType() != Material.AIR) {
			return;
		}
		ItemStack removed = itemInHand.clone();
		removed.setAmount(1);
		java.util.HashMap<Integer, ItemStack> leftover = player.getInventory().removeItem(removed);
		if (!leftover.isEmpty()) {
			return;
		}
		Map map = null;
		try {
			map = ArtMap.instance().getArtDatabase().createMap();
		} catch (NoSuchFieldException | IllegalAccessException e) {
			returnCanvasToPlayer(removed);
			player.sendMessage(
				ChatColor.RED + " Severe Error.  Pleae contact a server Admin! " + ChatColor.RESET);
			ArtMap.instance().getLogger().log(Level.SEVERE, "Error creating map!", e);
			return;
		}
		if (map == null) {
			returnCanvasToPlayer(removed);
			player.sendMessage(
					ChatColor.RED + " Severe Error.  Pleae contact a server Admin! " + ChatColor.RESET);
			return;
		}
		try {
			map.update(player);
			mountCanvas(removed, new Canvas(map, player.getName(), size));
		} catch (Exception e) {
			returnCanvasToPlayer(removed);
			player.sendMessage("Error placing canvas on the Easel!");
			ArtMap.instance().getLogger().log(Level.SEVERE, "Error placing canvas on easel!", e);
		}
	}

	private void placeArtworkForEdit(ItemStack itemInHand) {
		ArtMap.instance().getScheduler().ASYNC.run(() -> {
			Optional<MapArt> art;
			int id;
			boolean unsaved;
			boolean editCopy;
			Optional<MapArt> original;
			CanvasSize requiredSize;
			try {
				id = ItemUtils.getMapID(itemInHand).orElseThrow(() -> new ArtMapException("Artwork does not have a mapview!"));
				art = ArtMap.instance().getArtDatabase().getArtwork(id);
				unsaved = ArtMap.instance().getArtDatabase().containsUnsavedArtwork(id);
				// An unsaved "Artwork Copy" belongs to the artwork it was copied from, found by its title.
				editCopy = art.isEmpty() && ArtItem.isCopyArtwork(itemInHand);
				original = editCopy
						? ArtMap.instance().getArtDatabase().getArtwork(itemInHand.getItemMeta().getDisplayName())
						: Optional.empty();
				requiredSize = ArtMap.instance().getArtDatabase().getMapCanvasSize(id);
				if (itemInHand.hasItemMeta()) {
					CanvasSize loreSize = Canvas.parseCanvasSize(itemInHand.getItemMeta());
					if (loreSize != CanvasSize.defaultSize()) {
						requiredSize = loreSize;
					}
				}
			} catch (Exception e) {
				player.sendMessage("Error placing art on the Easel!");
				ArtMap.instance().getLogger().log(Level.SEVERE, "Error placing art on easel for edit!", e);
				return;
			}
			final int finalId = id;
			final CanvasSize finalSize = requiredSize;
			ArtMap.instance().getScheduler().SYNC.run(() -> {
				if (easel.getItem().getType() != Material.AIR) {
					return;
				}
				ItemStack currentItem = player.getInventory().getItemInMainHand();
				if (!ArtItem.isArtwork(currentItem)) {
					return;
				}
				OptionalInt currentId;
				try {
					currentId = ItemUtils.getMapID(currentItem);
				} catch (ArtMapException e) {
					return;
				}
				if (currentId.isEmpty() || currentId.getAsInt() != finalId) {
					return;
				}
				ItemStack removed = currentItem.clone();
				removed.setAmount(1);
				java.util.HashMap<Integer, ItemStack> leftover = player.getInventory().removeItem(removed);
				if (!leftover.isEmpty()) {
					return;
				}
				if (art.isPresent()) {
					if (!player.getUniqueId().equals(art.get().getArtistPlayer().getUniqueId()) && !player.hasPermission("artmap.admin")) {
						returnCanvasToPlayer(removed);
						Lang.ActionBar.NO_EDIT_PERM.send(player);
						easel.playEffect(EaselEffect.USE_DENIED);
						return;
					}
					try {
						Canvas canvas = new Canvas.CanvasCopy(art.get().getMap().cloneMap(), art.get(), finalSize);
						mountCanvas(removed, canvas);
					} catch (Exception e) {
						returnCanvasToPlayer(removed);
						player.sendMessage("Error placing art on the Easel!");
						ArtMap.instance().getLogger().log(Level.SEVERE, "Error placing art on easel for edit!", e);
					}
				} else if (editCopy && unsaved) {
					if (!mayResumeEditCopy(original.map(MapArt::getArtist), player.getUniqueId(),
							player.hasPermission("artmap.admin"))) {
						returnCanvasToPlayer(removed);
						Lang.ActionBar.NO_EDIT_PERM.send(player);
						easel.playEffect(EaselEffect.USE_DENIED);
						return;
					}
					try {
						// Mount it as a copy of the original so saving under the original title updates it.
						Canvas canvas = original.isPresent()
								? new Canvas.CanvasCopy(new Map(finalId), original.get(), finalSize)
								: new Canvas(finalId, player.getName(), finalSize);
						mountCanvas(removed, canvas);
					} catch (Exception e) {
						returnCanvasToPlayer(removed);
						player.sendMessage("Error placing art on the Easel!");
						ArtMap.instance().getLogger().log(Level.SEVERE, "Error placing art on easel for edit!", e);
					}
				} else if (unsaved) {
					try {
						Canvas canvas = new Canvas(finalId, player.getName(), finalSize);
						mountCanvas(removed, canvas);
					} catch (Exception e) {
						returnCanvasToPlayer(removed);
						player.sendMessage("Error placing art on the Easel!");
						ArtMap.instance().getLogger().log(Level.SEVERE, "Error placing art on easel for edit!", e);
					}
				} else {
					returnCanvasToPlayer(removed);
					Lang.ActionBar.NEED_CANVAS.send(player);
					easel.playEffect(EaselEffect.USE_DENIED);
				}
			});
		});
	}

	/**
	 * Whether a player may put an unsaved "Artwork Copy" back on an easel.
	 *
	 * @param originalArtist The artist of the saved artwork the copy was made from, if it still exists.
	 * @param player         The player placing the copy.
	 * @param admin          True if the player has artmap.admin.
	 */
	public static boolean mayResumeEditCopy(Optional<UUID> originalArtist, UUID player, boolean admin) {
		return admin || originalArtist.map(player::equals).orElse(false);
	}

	private void returnCanvasToPlayer(ItemStack removed) {
		java.util.HashMap<Integer, ItemStack> addLeftover = player.getInventory().addItem(removed);
		if (!addLeftover.isEmpty()) {
			easel.getLocation().getWorld().dropItemNaturally(easel.getLocation(), addLeftover.get(0));
		}
	}

	private void mountCanvas(ItemStack itemInHand, Canvas canvas) {
		if (!easel.mountCanvas(canvas)) {
			returnCanvasToPlayer(itemInHand);
		}
	}
}
