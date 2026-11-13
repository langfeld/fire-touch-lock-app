package org.langfeld.firetouchlock;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;

/**
 * Durchsichtige Fläche über dem ganzen Bildschirm, die jede Berührung abfängt.
 * Bei einer Berührung erscheinen dezent die vier Eckpunkte zum Entsperren und ein Schloss in der Mitte;
 * das Schloss liegt auf einem halbdurchsichtigen dunklen Kreis, damit es auch auf hellem Bild zu sehen ist.
 * Die Punkte zeigen absichtlich keinen Fortschritt, damit ein Kind die Reihenfolge nicht durch Ausprobieren findet.
 */
@SuppressLint("ViewConstructor")
final class LockOverlay extends View {

    private static final long HINT_MILLIS = 3000;
    private static final long FADE_MILLIS = 600;
    private static final long SEQUENCE_TIMEOUT_MILLIS = 5000;
    private static final float INSET_DP = 56;
    private static final float TOUCH_RADIUS_DP = 48;
    private static final float DOT_RADIUS_DP = 9;
    private static final float OUTLINE_DP = 1.5f;
    private static final float LOCK_SIZE_DP = 40;
    private static final float LOCK_BACKDROP_RADIUS_DP = 32;

    private final Runnable onUnlock;
    private final CornerSequence corners = new CornerSequence(SEQUENCE_TIMEOUT_MILLIS);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint backdropPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Drawable lock;
    private final float density;
    private final Runnable fadeOutTask = this::fadeOut;
    private ValueAnimator fade;
    private float hintAlpha;

    LockOverlay(Context context, Runnable onUnlock) {
        super(context);
        this.onUnlock = onUnlock;
        density = context.getResources().getDisplayMetrics().density;
        dotPaint.setColor(Color.WHITE);
        outlinePaint.setColor(Color.BLACK);
        backdropPaint.setColor(Color.BLACK);
        lock = context.getDrawable(R.drawable.ic_lock);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        corners.setGeometry(width, height, INSET_DP * density, TOUCH_RADIUS_DP * density);
        int size = Math.round(LOCK_SIZE_DP * density);
        int left = (width - size) / 2;
        int top = (height - size) / 2;
        lock.setBounds(left, top, left + size, top + size);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            int index = event.getActionIndex();
            if (corners.onTap(event.getX(index), event.getY(index), event.getEventTime())) {
                // Erst nach der laufenden Berührung entfernen, nicht mitten in ihr.
                post(onUnlock);
                return true;
            }
            showHint();
        }
        // Alles abfangen, damit nichts bei der App darunter ankommt.
        return true;
    }

    /** Zeigt Eckpunkte und Schloss für ein paar Sekunden. */
    void showHint() {
        removeCallbacks(fadeOutTask);
        if (fade != null) {
            fade.cancel();
        }
        hintAlpha = 1f;
        invalidate();
        postDelayed(fadeOutTask, HINT_MILLIS);
    }

    private void fadeOut() {
        // Wer länger nichts antippt, beginnt bei den Eckpunkten von vorn.
        corners.reset();
        fade = ValueAnimator.ofFloat(hintAlpha, 0f);
        fade.setDuration(FADE_MILLIS);
        fade.addUpdateListener(animator -> {
            hintAlpha = (float) animator.getAnimatedValue();
            invalidate();
        });
        fade.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(fadeOutTask);
        if (fade != null) {
            fade.cancel();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (hintAlpha <= 0f) {
            return;
        }
        float dot = DOT_RADIUS_DP * density;
        float outline = OUTLINE_DP * density;
        outlinePaint.setAlpha(Math.round(60 * hintAlpha));
        dotPaint.setAlpha(Math.round(120 * hintAlpha));
        for (int i = 0; i < CornerSequence.CORNERS; i++) {
            float x = corners.cornerX(i);
            float y = corners.cornerY(i);
            canvas.drawCircle(x, y, dot + outline, outlinePaint);
            canvas.drawCircle(x, y, dot, dotPaint);
        }
        backdropPaint.setAlpha(Math.round(80 * hintAlpha));
        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, LOCK_BACKDROP_RADIUS_DP * density, backdropPaint);
        lock.setAlpha(Math.round(170 * hintAlpha));
        lock.draw(canvas);
    }
}
