package com.launcherautoradio;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

// Analog stopwatch: one turn = 20 s, a tick every 0.2 s, numbers every 2 s,
// a blue hand for the running time and a faint orange mark at the best time.
public class StopwatchDial extends View {
    static final float TURN = 20f;

    final float dp = getResources().getDisplayMetrics().density;
    final Paint face = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint tick = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint num = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint hand = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint best = new Paint(Paint.ANTI_ALIAS_FLAG);
    float seconds, bestSeconds;

    public StopwatchDial(Context c, AttributeSet a) {
        super(c, a);
        face.setColor(Themes.attr(c, R.attr.cardColor));
        tick.setColor(Color.parseColor("#94A3B8"));
        num.setColor(Color.parseColor("#E6EDF7"));
        num.setTextAlign(Paint.Align.CENTER);
        num.setTextSize(20 * getResources().getDisplayMetrics().scaledDensity);
        hand.setColor(Themes.accent(c));
        hand.setStrokeCap(Paint.Cap.ROUND);
        hand.setStrokeWidth(6 * dp);
        best.setColor(Color.parseColor("#F59E0B"));
        best.setStrokeCap(Paint.Cap.ROUND);
        best.setStrokeWidth(5 * dp);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); // the digital time beside it is the readable value
    }

    void set(float s, float bestS) {
        seconds = s;
        bestSeconds = bestS;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f, r = Math.min(cx, cy) - 4 * dp;
        c.drawCircle(cx, cy, r, face);
        for (int i = 0; i < 100; i++) { // 100 ticks of 0.2 s
            boolean major = i % 10 == 0, mid = i % 5 == 0;
            tick.setStrokeWidth((major ? 3 : 1.5f) * dp);
            float a = (float) Math.toRadians(i * 3.6 - 90);
            float outer = r - 10 * dp, inner = outer - (major ? 22 : mid ? 14 : 8) * dp;
            c.drawLine(cx + (float) Math.cos(a) * outer, cy + (float) Math.sin(a) * outer,
                    cx + (float) Math.cos(a) * inner, cy + (float) Math.sin(a) * inner, tick);
            if (major) {
                float nr = inner - 22 * dp;
                c.drawText(String.valueOf(i / 5), cx + (float) Math.cos(a) * nr, cy + (float) Math.sin(a) * nr + num.getTextSize() / 3, num);
            }
        }
        if (bestSeconds > 0) line(c, cx, cy, bestSeconds, r - 12 * dp, r - 40 * dp, best);
        line(c, cx, cy, seconds, r - 14 * dp, -18 * dp, hand);
        c.drawCircle(cx, cy, 10 * dp, hand);
    }

    void line(Canvas c, float cx, float cy, float s, float from, float to, Paint p) {
        float a = (float) Math.toRadians(s / TURN * 360 - 90);
        float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
        c.drawLine(cx + cos * to, cy + sin * to, cx + cos * from, cy + sin * from, p);
    }
}
