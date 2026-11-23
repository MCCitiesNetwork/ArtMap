package me.Fupery.ArtMap.Listeners;

import java.util.Optional;
import java.util.UUID;

/**
 * Whether a player may copy a filled map in a crafting or cartography table.
 */
enum CopyPermission {
    /** Not an ArtMap map, so vanilla copying applies. */
    VANILLA,
    /** A saved artwork, and the player is its artist. */
    ARTIST,
    /** An ArtMap map the player may not copy. */
    DENY;

    /**
     * @param artMapItem  True if the map carries ArtMap lore or is an unsaved ArtMap canvas.
     * @param savedArtist The artist of the saved artwork with this map ID, if there is one.
     * @param crafter     The player copying the map.
     */
    static CopyPermission decide(boolean artMapItem, Optional<UUID> savedArtist, UUID crafter) {
        if (savedArtist.isPresent()) {
            return savedArtist.get().equals(crafter) ? ARTIST : DENY;
        }
        return artMapItem ? DENY : VANILLA;
    }
}
