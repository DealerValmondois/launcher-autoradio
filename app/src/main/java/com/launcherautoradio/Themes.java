package com.launcherautoradio;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;

// Colour themes: 24 hues 15° apart (res/values/themes.xml). Each activity calls apply() before
// super.onCreate(); accent, background, cards and borders all follow the chosen hue.
final class Themes {
    static final int HUES = 24, DEFAULT = 15; // 225° = the original blue

    static int index(Context c) {
        int i = c.getSharedPreferences("theme", Context.MODE_PRIVATE).getInt("hue", DEFAULT);
        return i >= 0 && i < HUES ? i : DEFAULT;
    }

    static void set(Context c, int i) {
        c.getSharedPreferences("theme", Context.MODE_PRIVATE).edit().putInt("hue", i).apply();
    }

    static int style(Context c, int i) {
        return c.getResources().getIdentifier("Theme.Launcher.H" + i, "style", c.getPackageName());
    }

    static void apply(Activity a) { a.setTheme(style(a, index(a))); }

    static int attr(Context c, int attr) {
        TypedArray t = c.obtainStyledAttributes(new int[]{attr});
        int color = t.getColor(0, Color.GRAY);
        t.recycle();
        return color;
    }

    static int accent(Context c) { return attr(c, R.attr.accentColor); }

    // Accent of any hue, read from its style so the slider shows exactly what each theme gives.
    static int accentOf(Context c, int i) {
        TypedArray t = c.obtainStyledAttributes(style(c, i), new int[]{R.attr.accentColor});
        int color = t.getColor(0, Color.GRAY);
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
