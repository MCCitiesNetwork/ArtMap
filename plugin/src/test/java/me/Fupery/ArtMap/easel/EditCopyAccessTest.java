package me.Fupery.ArtMap.easel;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import java.util.UUID;

import org.junit.Test;

import me.Fupery.ArtMap.Easel.EaselEvent;

public class EditCopyAccessTest {
    private static final UUID ARTIST = UUID.randomUUID();
    private static final UUID OTHER = UUID.randomUUID();

    @Test
    public void originalArtistMayResumeEditCopy() {
        assertTrue(EaselEvent.mayResumeEditCopy(Optional.of(ARTIST), ARTIST, false));
    }

    @Test
    public void otherPlayerMayNotResumeEditCopy() {
        assertFalse(EaselEvent.mayResumeEditCopy(Optional.of(ARTIST), OTHER, false));
    }

    @Test
    public void nobodyMayResumeEditCopyWhoseOriginalIsGone() {
        assertFalse(EaselEvent.mayResumeEditCopy(Optional.empty(), ARTIST, false));
    }

    @Test
    public void adminMayResumeAnyEditCopy() {
        assertTrue(EaselEvent.mayResumeEditCopy(Optional.of(ARTIST), OTHER, true));
        assertTrue(EaselEvent.mayResumeEditCopy(Optional.empty(), OTHER, true));
    }
}
