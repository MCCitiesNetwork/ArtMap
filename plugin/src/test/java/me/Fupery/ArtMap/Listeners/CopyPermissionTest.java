package me.Fupery.ArtMap.Listeners;

import static org.junit.Assert.assertEquals;

import java.util.Optional;
import java.util.UUID;

import org.junit.Test;

public class CopyPermissionTest {
    private static final UUID ARTIST = UUID.randomUUID();
    private static final UUID OTHER = UUID.randomUUID();

    @Test
    public void artistMayCopyTheirSavedArtwork() {
        assertEquals(CopyPermission.ARTIST, CopyPermission.decide(true, Optional.of(ARTIST), ARTIST));
    }

    @Test
    public void otherPlayerMayNotCopySavedArtwork() {
        assertEquals(CopyPermission.DENY, CopyPermission.decide(true, Optional.of(ARTIST), OTHER));
    }

    @Test
    public void unsavedArtMapItemCannotBeCopiedEvenByItsArtist() {
        assertEquals(CopyPermission.DENY, CopyPermission.decide(true, Optional.empty(), ARTIST));
    }

    @Test
    public void vanillaMapIsLeftAlone() {
        assertEquals(CopyPermission.VANILLA, CopyPermission.decide(false, Optional.empty(), OTHER));
    }
}
