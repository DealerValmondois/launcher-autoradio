package com.launcherautoradio;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;

// Accent colour themes. Each activity calls apply() before super.onCreate(); views read accent().
final class Themes {
    static final String[] NAMES = {"Bleu", "Rouge", "Vert", "Orange", "Violet", "Cyan"};
    static final int[] STYLES = {R.style.Theme_Launcher, R.style.Theme_Launcher_Red, R.style.Theme_Launcher_Green,
            R.style.Theme_Launcher_Orange, R.style.Theme_Launcher_Purple, R.style.Theme_Launcher_Cyan};
    // Same values as the themes; dark enough for white text on an accent tile.
    static final int[] COLORS = {0xFF2F6FED, 0xFFD13438, 0xFF218358, 0xFFD9480F, 0xFF7C3AED, 0xFF0369A1};

    static int index(Context c) {
        int i = c.getSharedPreferences("theme", Context.MODE_PRIVATE).getInt("accent", 0);
        return i >= 0 && i < STYLES.length ? i : 0;
    }

    static void set(Context c, int i) {
        c.getSharedPreferences("theme", Context.MODE_PRIVATE).edit().putInt("accent", i).apply();
    }

    static void apply(Activity a) { a.setTheme(STYLES[index(a)]); }

    static int accent(Context c) {
        TypedArray t = c.obtainStyledAttributes(new int[]{R.attr.accentColor});
        int color = t.getColor(0, COLORS[0]);
        t.recycle();
        return color;
    }

    // fraction 0..1 of the way towards white (positive) or black (negative)
    static int shade(int color, float f) {
        int target = f > 0 ? 255 : 0;
        float k = Math.abs(f);
        return Color.rgb((int) (Color.red(color) + (target - Color.red(color)) * k),
                (int) (Color.green(color) + (target - Color.green(color)) * k),
                (int) (Color.blue(color) + (target - Color.blue(color)) * k));
    }
}
