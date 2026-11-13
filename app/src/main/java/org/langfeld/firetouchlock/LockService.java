package org.langfeld.firetouchlock;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

/**
 * Bedienungshilfe, die die Touchsperre schaltet. Sie liest keine Bildschirminhalte, nur die Lautstärketasten:
 * Lauter – Leiser – Lauter – Leiser innerhalb von drei Sekunden schaltet die Sperre an oder aus.
 * Gesperrt liegt eine durchsichtige Fläche über allem, auch über der Navigationsleiste, und die
 * Lautstärketasten sind mitgesperrt. Ein Neustart oder Profilwechsel hebt die Sperre immer auf.
 */
public class LockService extends AccessibilityService {

    private static final String TAG = "FireTouchLock";
    private static final long KEY_SEQUENCE_WINDOW_MILLIS = 3000;

    private final KeySequence keySequence = new KeySequence(KEY_SEQUENCE_WINDOW_MILLIS);
    /** Tasten, deren Herunterdrücken abgefangen wurde; ihr Loslassen wird dann ebenfalls abgefangen. */
    private final Set<Integer> swallowedKeys = new HashSet<>();
    private WindowManager windowManager;
    private LockOverlay overlay;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Nicht gebraucht: Die Sperre hängt nur an den Tasten.
    }

    @Override
    public void onInterrupt() {
        // Nichts zu unterbrechen.
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        if (code != KeyEvent.KEYCODE_VOLUME_UP && code != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return false;
        }
        switch (event.getAction()) {
            case KeyEvent.ACTION_DOWN:
                return onVolumeKeyDown(event);
            case KeyEvent.ACTION_UP:
                return swallowedKeys.remove(code);
            default:
                return overlay != null;
        }
    }

    private boolean onVolumeKeyDown(KeyEvent event) {
        int code = event.getKeyCode();
        boolean swallow = overlay != null;
        if (event.getRepeatCount() == 0) {
            int key = code == KeyEvent.KEYCODE_VOLUME_UP ? KeySequence.UP : KeySequence.DOWN;
            if (keySequence.onPress(key, event.getEventTime())) {
                swallow = true;
                if (overlay == null) {
                    lock();
                } else {
                    unlock();
                }
            }
        }
        if (swallow) {
            swallowedKeys.add(code);
        }
        return swallow;
    }

    private void lock() {
        if (overlay != null || windowManager == null) {
            return;
        }
        LockOverlay view = new LockOverlay(this, this::unlock);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                // Nicht fokussierbar, damit die App darunter (das Video) den Fokus behält;
                // ohne Grenzen, damit die Fläche auch Status- und Navigationsleiste bedeckt.
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        params.setTitle("FireTouchLock");
        try {
            windowManager.addView(view, params);
        } catch (RuntimeException e) {
            Log.e(TAG, "Sperrfläche ließ sich nicht anlegen", e);
            return;
        }
        overlay = view;
        view.showHint();
        Toast.makeText(this, R.string.locked, Toast.LENGTH_SHORT).show();
    }

    private void unlock() {
        if (overlay == null) {
            return;
        }
        removeOverlay();
        Toast.makeText(this, R.string.unlocked, Toast.LENGTH_SHORT).show();
    }

    private void removeOverlay() {
        if (overlay == null) {
            return;
        }
        try {
            windowManager.removeView(overlay);
        } catch (RuntimeException e) {
            Log.w(TAG, "Sperrfläche war schon entfernt", e);
        }
        overlay = null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        removeOverlay();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        removeOverlay();
        super.onDestroy();
    }
}
