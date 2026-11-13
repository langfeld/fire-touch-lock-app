package org.langfeld.firetouchlock;

/**
 * Erkennt das Antippen der vier Eckpunkte im Uhrzeigersinn, oben links beginnend.
 * Jeder Tipp daneben oder in falscher Reihenfolge beginnt von vorn.
 * Reine Logik ohne Android, damit sie sich mit JUnit prüfen lässt.
 */
final class CornerSequence {

    /** Anzahl der Punkte; Reihenfolge: oben links, oben rechts, unten rechts, unten links. */
    static final int CORNERS = 4;

    private final long timeoutMillis;
    private float width;
    private float height;
    private float inset;
    private float radius;
    private int progress;
    private long startMillis;

    /** @param timeoutMillis Zeit, in der alle vier Punkte ab dem ersten angetippt sein müssen */
    CornerSequence(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * Legt die Lage der Punkte fest und beginnt von vorn.
     *
     * @param inset  Abstand der Punkte von den Bildschirmrändern
     * @param radius Umkreis um jeden Punkt, in dem ein Tipp zählt
     */
    void setGeometry(float width, float height, float inset, float radius) {
        this.width = width;
        this.height = height;
        this.inset = inset;
        this.radius = radius;
        reset();
    }

    float cornerX(int index) {
        return index == 1 || index == 2 ? width - inset : inset;
    }

    float cornerY(int index) {
        return index >= 2 ? height - inset : inset;
    }

    /** @return Nummer des getroffenen Punkts oder -1 */
    int cornerAt(float x, float y) {
        for (int i = 0; i < CORNERS; i++) {
            float dx = x - cornerX(i);
            float dy = y - cornerY(i);
            if (dx * dx + dy * dy <= radius * radius) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Meldet einen Tipp (jedes Aufsetzen eines Fingers).
     *
     * @return true, wenn dieser Tipp die Folge abschließt
     */
    boolean onTap(float x, float y, long timeMillis) {
        if (progress > 0 && timeMillis - startMillis > timeoutMillis) {
            progress = 0;
        }
        int corner = cornerAt(x, y);
        if (corner != progress) {
            // Daneben oder falsche Reihenfolge: von vorn. Oben links beginnt gleich eine neue Folge.
            progress = 0;
            if (corner != 0) {
                return false;
            }
        }
        if (progress == 0) {
            startMillis = timeMillis;
        }
        progress++;
        if (progress == CORNERS) {
            progress = 0;
            return true;
        }
        return false;
    }

    void reset() {
        progress = 0;
    }
}
