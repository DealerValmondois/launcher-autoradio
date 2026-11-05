package com.launcherautoradio;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;

// Round speedometer: 240° arc filled with a blue gradient, ticks every 10 km/h, big animated number in the middle.
public class SpeedGauge extends View {
    static final float MAX = 200, START = 150, SWEEP = 240;

    final float dp = getResources().getDisplayMetrics().density;
    final float stroke = 14 * dp;
    final Paint track = stroke(Color.parseColor("#1E2A44"), stroke);
    final Paint arc = stroke(Color.WHITE, stroke);
    final Paint tick = stroke(Color.parseColor("#94A3B8"), 2 * dp);
    final Paint big = text(Color.parseColor("#E6EDF7"), 72, true);
    final Paint small = text(Color.parseColor("#94A3B8"), 13, false);
    final Paint unit = text(Color.parseColor("#94A3B8"), 18, false);
    final RectF oval = new RectF();
    final int accent = Themes.accent(getContext());
    float shown, cx, cy, r;
    boolean hasFix;
    ValueAnimator anim;

    public SpeedGauge(Context c, AttributeSet a) {
        super(c, a);
        setContentDescription("Vitesse inconnue");
    }

    Paint stroke(int color, float width) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(width);
        p.setColor(color);
        return p;
    }

    Paint text(int color, float sp, boolean bold) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(sp * getResources().getDisplayMetrics().scaledDensity);
        p.setFakeBoldText(bold);
        return p;
    }

    public void setSpeed(float kmh) {
        hasFix = true;
        setContentDescription(Math.round(kmh) + " km/h");
        if (anim != null) anim.cancel();
        anim = ValueAnimator.ofFloat(shown, kmh).setDuration(700);
        anim.addUpdateListener(v -> { shown = (float) v.getAnimatedValue(); invalidate(); });
        anim.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        // The arc's lowest points sit at sin(30°) below the centre, so it needs 1.5 radii of height.
        r = Math.min(w / 2f - stroke, (h - 2 * stroke) / 1.5f);
        cx = w / 2f;
        cy = stroke + r;
        oval.set(cx - r, cy - r, cx + r, cy + r);
        SweepGradient g = new SweepGradient(cx, cy,
                new int[]{Themes.shade(accent, -.25f), accent, Themes.shade(accent, .5f)},
                new float[]{0, SWEEP / 720, SWEEP / 360});
        Matrix m = new Matrix();
        m.setRotate(START, cx, cy);
        g.setLocalMatrix(m);
        arc.setShader(g);
    }

    @Override
    protected void onDraw(Canvas c) {
        c.drawArc(oval, START, SWEEP, false, track);
        if (shown > 0.5f) c.drawArc(oval, START, SWEEP * Math.min(shown, MAX) / MAX, false, arc);

        for (int v = 0; v <= MAX; v += 10) {
            double a = Math.toRadians(START + SWEEP * v / MAX);
            boolean major = v % 20 == 0;
            float outer = r - stroke - 4 * dp, inner = outer - (major ? 12 : 6) * dp;
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            c.drawLine(cx + cos * outer, cy + sin * outer, cx + cos * inner, cy + sin * inner, tick);
            if (v % 40 == 0) {
                float lr = inner - 14 * dp;
                c.drawText(String.valueOf(v), cx + cos * lr, cy + sin * lr + small.getTextSize() / 3, small);
            }
        }

        c.drawText(hasFix ? String.valueOf(Math.round(shown)) : "—", cx, cy + big.getTextSize() / 3, big);
        c.drawText("km/h", cx, cy + big.getTextSize() / 3 + unit.getTextSize() * 1.6f, unit);
    }
}
