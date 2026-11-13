package org.langfeld.firetouchlock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KeySequenceTest {

    private static final int UP = KeySequence.UP;
    private static final int DOWN = KeySequence.DOWN;

    @Test
    public void detectsSequenceWithinWindow() {
        KeySequence sequence = new KeySequence(3000);
        assertFalse(sequence.onPress(UP, 0));
        assertFalse(sequence.onPress(DOWN, 400));
        assertFalse(sequence.onPress(UP, 800));
        assertTrue(sequence.onPress(DOWN, 1200));
    }

    @Test
    public void ignoresSlowSequence() {
        KeySequence sequence = new KeySequence(3000);
        sequence.onPress(UP, 0);
        sequence.onPress(DOWN, 1000);
        sequence.onPress(UP, 2000);
        assertFalse(sequence.onPress(DOWN, 3500));
    }

    @Test
    public void wrongKeyStartsOverButCanBeginNewSequence() {
        KeySequence sequence = new KeySequence(3000);
        sequence.onPress(UP, 0);
        // Zweimal Lauter: falsch, aber der zweite Druck beginnt eine neue Folge.
        sequence.onPress(UP, 100);
        sequence.onPress(DOWN, 200);
        sequence.onPress(UP, 300);
        assertTrue(sequence.onPress(DOWN, 400));
    }

    @Test
    public void needsFullSequenceAgainAfterSuccess() {
        KeySequence sequence = new KeySequence(3000);
        sequence.onPress(UP, 0);
        sequence.onPress(DOWN, 100);
        sequence.onPress(UP, 200);
        assertTrue(sequence.onPress(DOWN, 300));
        assertFalse(sequence.onPress(UP, 400));
        assertFalse(sequence.onPress(DOWN, 500));
    }

    @Test
    public void repeatedSameKeyNeverMatches() {
        KeySequence sequence = new KeySequence(3000);
        for (int i = 0; i < 10; i++) {
            assertFalse(sequence.onPress(DOWN, i * 100L));
            assertFalse(sequence.onPress(DOWN, i * 100L + 50));
        }
    }
}
