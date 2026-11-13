package org.langfeld.firetouchlock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CornerSequenceTest {

    private static final float WIDTH = 1280;
    private static final float HEIGHT = 800;
    private static final float INSET = 75;
    private static final float RADIUS = 64;

    private static CornerSequence create() {
        CornerSequence sequence = new CornerSequence(5000);
        sequence.setGeometry(WIDTH, HEIGHT, INSET, RADIUS);
        return sequence;
    }

    @Test
    public void unlocksClockwiseFromTopLeft() {
        CornerSequence sequence = create();
        assertFalse(sequence.onTap(INSET, INSET, 0));
        assertFalse(sequence.onTap(WIDTH - INSET, INSET, 500));
        assertFalse(sequence.onTap(WIDTH - INSET, HEIGHT - INSET, 1000));
        assertTrue(sequence.onTap(INSET, HEIGHT - INSET, 1500));
    }

    @Test
    public void countsTapsNearThePointOnly() {
        CornerSequence sequence = create();
        assertEquals(0, sequence.cornerAt(INSET + 40, INSET + 40));
        assertEquals(2, sequence.cornerAt(WIDTH - INSET - 30, HEIGHT - INSET + 30));
        assertEquals(-1, sequence.cornerAt(INSET + 60, INSET + 60));
        assertEquals(-1, sequence.cornerAt(WIDTH / 2, HEIGHT / 2));
    }

    @Test
    public void counterClockwiseDoesNotUnlock() {
        CornerSequence sequence = create();
        sequence.onTap(INSET, INSET, 0);
        sequence.onTap(INSET, HEIGHT - INSET, 100);
        sequence.onTap(WIDTH - INSET, HEIGHT - INSET, 200);
        assertFalse(sequence.onTap(WIDTH - INSET, INSET, 300));
    }

    @Test
    public void tapElsewhereStartsOver() {
        CornerSequence sequence = create();
        sequence.onTap(INSET, INSET, 0);
        sequence.onTap(WIDTH - INSET, INSET, 100);
        sequence.onTap(WIDTH / 2, HEIGHT / 2, 200);
        sequence.onTap(WIDTH - INSET, HEIGHT - INSET, 300);
        assertFalse(sequence.onTap(INSET, HEIGHT - INSET, 400));
    }

    @Test
    public void topLeftBeginsNewSequence() {
        CornerSequence sequence = create();
        sequence.onTap(INSET, INSET, 0);
        sequence.onTap(WIDTH - INSET, INSET, 100);
        sequence.onTap(INSET, INSET, 200);
        sequence.onTap(WIDTH - INSET, INSET, 300);
        sequence.onTap(WIDTH - INSET, HEIGHT - INSET, 400);
        assertTrue(sequence.onTap(INSET, HEIGHT - INSET, 500));
    }

    @Test
    public void tooSlowStartsOver() {
        CornerSequence sequence = create();
        sequence.onTap(INSET, INSET, 0);
        sequence.onTap(WIDTH - INSET, INSET, 2000);
        sequence.onTap(WIDTH - INSET, HEIGHT - INSET, 4000);
        assertFalse(sequence.onTap(INSET, HEIGHT - INSET, 6000));
    }

    @Test
    public void resetForgetsProgress() {
        CornerSequence sequence = create();
        sequence.onTap(INSET, INSET, 0);
        sequence.onTap(WIDTH - INSET, INSET, 100);
        sequence.onTap(WIDTH - INSET, HEIGHT - INSET, 200);
        sequence.reset();
        assertFalse(sequence.onTap(INSET, HEIGHT - INSET, 300));
    }
}
