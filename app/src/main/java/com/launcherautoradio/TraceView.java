package com.launcherautoradio;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

// Draws the current trip as a glowing line, scaled to fit, with a start dot and a "you are here" dot.
public class TraceView extends View {
    final float dp = getResources().getDisplayMetrics().density;
    final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint hint = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Path path = new Path();

    public TraceView(Context c, AttributeSet a) {
        super(c, a);
        int accent = Themes.accent(c);
        for (Paint p : new Paint[]{line, glow}) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            p.setColor(Themes.shade(accent, .3f));
        }
        line.setStrokeWidth(4 * dp);
        glow.setStrokeWidth(14 * dp);
        glow.setAlpha(60);
        hint.setColor(Color.parseColor("#94A3B8"));
        hint.setTextAlign(Paint.Align.CENTER);
        hint.setTextSize(18 * getResources().getDisplayMetrics().scaledDensity);
        setContentDescription("Tracé du trajet");
    }

    @Override
    protected void onDraw(Canvas c) {
        int n = Trip.points;
        float[] p = Trip.xy;
        if (n < 2) {
            c.drawText("Le tracé apparaîtra en roulant", getWidth() / 2f, getHeight() / 2f, hint);
            return;
        }
        float minX = p[0], maxX = p[0], minY = p[1], maxY = p[1];
        for (int i = 1; i < n; i++) {
            minX = Math.min(minX, p[2 * i]); maxX = Math.max(maxX, p[2 * i]);
            minY = Math.min(minY, p[2 * i + 1]); maxY = Math.max(maxY, p[2 * i + 1]);
        }
        float pad = 24 * dp, w = getWidth() - 2 * pad, h = getHeight() - 2 * pad;
        float scale = Math.min(w / Math.max(maxX - minX, 50), h / Math.max(maxY - minY, 50)); // 50 m minimum span
        float ox = pad + (w - (maxX - minX) * scale) / 2, oy = pad + (h - (maxY - minY) * scale) / 2;

        path.reset();
        for (int i = 0; i < n; i++) { // y grows north, screen y grows down
            float x = ox + (p[2 * i] - minX) * scale, y = oy + (maxY - p[2 * i + 1]) * scale;
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        c.drawPath(path, glow);
        c.drawPath(path, line);

        dot.setColor(Color.parseColor("#94A3B8"));
        c.drawCircle(ox + (p[0] - minX) * scale, oy + (maxY - p[1]) * scale, 6 * dp, dot);
        float ex = ox + (p[2 * n - 2] - minX) * scale, ey = oy + (maxY - p[2 * n - 1]) * scale;
        dot.setColor(line.getColor());
        dot.setAlpha(70);
        c.drawCircle(ex, ey, 16 * dp, dot);
        dot.setAlpha(255);
        c.drawCircle(ex, ey, 8 * dp, dot);
    }
}
