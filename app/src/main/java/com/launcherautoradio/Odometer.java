package com.launcherautoradio;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import java.util.Locale;

// Mechanical-style odometer: 4 drums for km plus an inverted drum for tenths; drums roll into the next digit.
public class Odometer extends View {
    static final int DRUMS = 5;

    final float dp = getResources().getDisplayMetrics().density;
    final Paint box = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint digit = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint shade = new Paint();
    final Paint unit = new Paint(Paint.ANTI_ALIAS_FLAG);
    final RectF r = new RectF();
    float km, shown;
    ValueAnimator anim;

    public Odometer(Context c, AttributeSet a) {
        super(c, a);
        digit.setTextAlign(Paint.Align.CENTER);
        digit.setFakeBoldText(true);
        unit.setColor(Color.parseColor("#94A3B8"));
        unit.setTextSize(16 * getResources().getDisplayMetrics().scaledDensity);
        setValue(0);
    }

    public void setValue(float newKm) {
        km = newKm;
        setContentDescription(String.format(Locale.FRANCE, "Trajet %.1f kilomètres", km));
        if (anim != null) anim.cancel();
        if (km < shown) { shown = km; invalidate(); return; } // reset: jump, don't roll backwards
        anim = ValueAnimator.ofFloat(shown, km).setDuration(900);
        anim.addUpdateListener(v -> { shown = (float) v.getAnimatedValue(); invalidate(); });
        anim.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        // Dark band at top and bottom of each drum, like the curve of a real wheel.
        shade.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{0xCC000000, 0x00000000, 0x00000000, 0xCC000000}, new float[]{0, .3f, .7f, 1}, Shader.TileMode.CLAMP));
        digit.setTextSize(h * 0.62f);
    }

    @Override
    protected void onDraw(Canvas c) {
        float h = getHeight(), gap = 3 * dp, unitW = unit.measureText(" km") + gap;
        float w = (getWidth() - unitW - gap * (DRUMS - 1)) / DRUMS;
        float tenths = (shown * 10) % 100000;

        float carry = tenths - (float) Math.floor(tenths); // the tenths drum rolls continuously
        for (int k = 0; k < DRUMS; k++) { // k = 0 is the tenths drum, drawn rightmost
            int d = (int) (Math.floor(tenths / Math.pow(10, k)) % 10);
            float x = (DRUMS - 1 - k) * (w + gap);
            boolean dec = k == 0;
            r.set(x, 0, x + w, h);
            box.setColor(dec ? Color.parseColor("#E6EDF7") : Color.parseColor("#05080F"));
            digit.setColor(dec ? Color.parseColor("#0B1220") : Color.parseColor("#E6EDF7"));
            c.drawRoundRect(r, 4 * dp, 4 * dp, box);

            c.save();
            c.clipRect(r);
            float base = h / 2 + digit.getTextSize() / 3 - carry * h;
            c.drawText(String.valueOf(d), x + w / 2, base, digit);
            c.drawText(String.valueOf((d + 1) % 10), x + w / 2, base + h, digit);
            c.drawRect(r, shade);
            c.restore();

            carry = d == 9 ? carry : 0; // the next drum only turns while this one goes from 9 to 0
        }
        c.drawText(" km", DRUMS * (w + gap), h / 2 + unit.getTextSize() / 3, unit);
    }
}
