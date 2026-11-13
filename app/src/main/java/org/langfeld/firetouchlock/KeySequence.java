package org.langfeld.firetouchlock;

/**
 * Erkennt die Tastenfolge Lauter – Leiser – Lauter – Leiser, die die Touchsperre an- und ausschaltet.
 * Reine Logik ohne Android, damit sie sich mit JUnit prüfen lässt.
 */
final class KeySequence {

    static final int UP = 1;
    static final int DOWN = 2;

    private static final int[] PATTERN = {UP, DOWN, UP, DOWN};

    private final long windowMillis;
    private int matched;
    private long startMillis;

    /** @param windowMillis Zeit, in der die ganze Folge ab dem ersten Druck fertig sein muss */
    KeySequence(long windowMillis) {
        this.windowMillis = windowMillis;
    }

    /**
     * Meldet einen Tastendruck (nur das erste Herunterdrücken, keine Wiederholungen beim Halten).
     *
     * @return true, wenn dieser Druck die Folge abschließt
     */
    boolean onPress(int key, long timeMillis) {
        if (matched > 0 && timeMillis - startMillis > windowMillis) {
            matched = 0;
        }
        if (key != PATTERN[matched]) {
            // Falsche Taste: von vorn. Sie kann aber selbst der Anfang einer neuen Folge sein.
            matched = 0;
            if (key != PATTERN[0]) {
                return false;
            }
        }
        if (matched == 0) {
            startMillis = timeMillis;
        }
        matched++;
        if (matched == PATTERN.length) {
            matched = 0;
            return true;
        }
        return false;
    }
}
