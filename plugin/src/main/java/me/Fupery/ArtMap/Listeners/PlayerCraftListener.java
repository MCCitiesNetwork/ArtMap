package me.Fupery.ArtMap.Listeners;

import java.sql.SQLException;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.CartographyInventory;
import org.bukkit.inventory.ItemStack;

import me.Fupery.ArtMap.ArtMap;
import me.Fupery.ArtMap.api.Config.Lang;
import me.Fupery.ArtMap.api.Exception.ArtMapException;
import me.Fupery.ArtMap.IO.MapArt;
import me.Fupery.ArtMap.Recipe.ArtItem;
import me.Fupery.ArtMap.Utils.ItemUtils;

class PlayerCraftListener implements RegisteredListener {

    @EventHandler
    public void onPlayerCraftEvent(CraftItemEvent event) {
        ItemStack result = event.getCurrentItem();

        // Tuinity fork sends nulls sometimes so handle it.
        if (result == null || result.getType() != Material.FILLED_MAP) {
            return;
        }
        Optional<MapArt> art = checkCopy(result, event);
        if (art.isEmpty()) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        ItemStack artworkItem = art.get().getMapItem();
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            onShiftClick(artworkItem, player, event);
        } else {
            result.setItemMeta(artworkItem.getItemMeta());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event == null || !(event.getInventory() instanceof CartographyInventory)) {
            return;
        }
        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType() != Material.FILLED_MAP) {
            return;
        }
        checkCopy(result, event).ifPresent(art -> result.setItemMeta(art.getMapItem().getItemMeta()));
    }

    /**
     * Decide whether the clicking player may copy this map, and cancel the event if not.
     *
     * @return The saved artwork when the player is its artist, otherwise empty.
     */
    private Optional<MapArt> checkCopy(ItemStack result, InventoryClickEvent event) {
        Optional<MapArt> art;
        boolean artMapItem;
        try {
            OptionalInt mapId = ItemUtils.getMapID(result);
            if (mapId.isEmpty()) {
                return Optional.empty();
            }
            art = ArtMap.instance().getArtDatabase().getArtwork(mapId.getAsInt());
            artMapItem = ArtItem.isArtwork(result)
                    || ArtMap.instance().getArtDatabase().containsUnsavedArtwork(mapId.getAsInt());
        } catch (SQLException | ArtMapException e) {
            ArtMap.instance().getLogger().log(Level.SEVERE, "Database error!", e);
            event.getWhoClicked().sendMessage("Error Retrieving Artwork check logs.");
            deny(event);
            return Optional.empty();
        }
        Optional<UUID> artist = art.map(MapArt::getArtist);
        switch (CopyPermission.decide(artMapItem, artist, event.getWhoClicked().getUniqueId())) {
            case ARTIST:
                return art;
            case DENY:
                Lang.NO_CRAFT_PERM.send(event.getWhoClicked());
                deny(event);
                return Optional.empty();
            default:
                return Optional.empty();
        }
    }

    private void deny(InventoryClickEvent event) {
        event.setResult(Event.Result.DENY);
        event.setCancelled(true);
    }

    private void onShiftClick(ItemStack artworkItem, Player player, CraftItemEvent event) {
        event.setCancelled(true);

        int i = 0;
        ItemStack[] items = event.getInventory().getMatrix();
        for (ItemStack item : items) {

            if (item != null) {
                i += item.getAmount();
            }
        }
        event.getInventory().setMatrix(new ItemStack[items.length]);
        artworkItem.setAmount(i);
        ItemUtils.giveItem(player, artworkItem);
    }

	@Override
    public void unregister() {
        CraftItemEvent.getHandlerList().unregister(this);
    }
}