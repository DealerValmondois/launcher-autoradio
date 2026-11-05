package com.launcherautoradio;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

// Horizontal compass band: the scale slides under a fixed centre marker, 180° visible, fading at the edges.
public class CompassTape extends View {
    static final float VISIBLE = 180;
    static final String[] NAMES = {"N", "NE", "E", "SE", "S", "SO", "O", "NO"};

    final float dp = getResources().getDisplayMetrics().density;
    final Paint tick = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint marker = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint fade = new Paint();
    final Path tri = new Path();
    float shown;
    boolean hasFix;
    ValueAnimator anim;

    public CompassTape(Context c, AttributeSet a) {
        super(c, a);
        tick.setStrokeWidth(2 * dp);
        label.setTextAlign(Paint.Align.CENTER);
        label.setTextSize(18 * getResources().getDisplayMetrics().scaledDensity);
        marker.setColor(Themes.accent(c));
        fade.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        setLayerType(LAYER_TYPE_HARDWARE, null); // needed for the edge fade
        setContentDescription("Cap inconnu");
    }

    public void setBearing(float deg) {
        hasFix = true;
        setContentDescription("Cap " + NAMES[Math.round(deg / 45f) % 8]);
        float from = shown, delta = ((deg - from) % 360 + 540) % 360 - 180; // shortest way round
        if (anim != null) anim.cancel();
        anim = ValueAnimator.ofFloat(from, from + delta).setDuration(600);
        anim.addUpdateListener(v -> { shown = (float) v.getAnimatedValue(); invalidate(); });
        anim.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        fade.setShader(new LinearGradient(0, 0, w, 0,
                new int[]{0x00000000, 0xFF000000, 0xFF000000, 0x00000000}, new float[]{0, .2f, .8f, 1}, Shader.TileMode.CLAMP));
        tri.reset();
        tri.moveTo(w / 2f - 8 * dp, 0);
        tri.lineTo(w / 2f + 8 * dp, 0);
        tri.lineTo(w / 2f, 10 * dp);
        tri.close();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), px = w / VISIBLE;
        int first = (int) Math.floor((shown - VISIBLE / 2) / 15) * 15;
        for (int a = first; a <= shown + VISIBLE / 2 + 15; a += 15) {
            float x = w / 2 + (a - shown) * px;
            int n = ((a % 360) + 360) % 360;
            boolean named = n % 45 == 0;
            tick.setColor(n == 0 ? marker.getColor() : Color.parseColor("#94A3B8"));
            c.drawLine(x, 14 * dp, x, (named ? 26 : 20) * dp, tick);
            if (named) {
                label.setColor(n == 0 ? Themes.shade(marker.getColor(), .35f) : Color.parseColor("#E6EDF7"));
                label.setFakeBoldText(n % 90 == 0);
                c.drawText(NAMES[n / 45], x, h - 6 * dp, label);
            }
        }
        c.drawRect(0, 0, w, h, fade);
        if (hasFix) c.drawPath(tri, marker);
    }
}
