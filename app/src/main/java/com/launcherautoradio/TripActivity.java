package com.launcherautoradio;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

// Trip stats page: big cards refreshed every second from the shared Trip.
public class TripActivity extends Activity {
    static final String[] LABELS = {"Distance", "Durée", "En mouvement", "Vitesse moyenne", "Vitesse max", "Départ"};
    final TextView[] values = new TextView[LABELS.length];
    final Handler tick = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle b) {
        Themes.apply(this);
        super.onCreate(b);
        setContentView(R.layout.activity_trip);
        findViewById(R.id.back).setOnClickListener(v -> finish());
        findViewById(R.id.reset).setOnClickListener(v -> { Trip.reset(); show(); });

        float dp = getResources().getDisplayMetrics().density;
        GridLayout grid = findViewById(R.id.grid);
        for (int i = 0; i < LABELS.length; i++) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setBackgroundResource(R.drawable.tile);
            int pad = (int) (24 * dp);
            card.setPadding(pad, pad, pad, pad);

            TextView label = new TextView(this);
            label.setText(LABELS[i]);
            label.setTextColor(getColor(R.color.muted));
            label.setTextSize(18);
            card.addView(label);

            values[i] = new TextView(this);
            values[i].setTextColor(getColor(R.color.text));
            values[i].setTextSize(32);
            values[i].getPaint().setFakeBoldText(true);
            card.addView(values[i]);

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, 1f), GridLayout.spec(GridLayout.UNDEFINED, 1f));
            lp.width = 0;
            lp.height = 0;
            int m = (int) (10 * dp);
            lp.setMargins(m, m, m, m);
            grid.addView(card, lp);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        tick.post(new Runnable() {
            @Override public void run() { show(); tick.postDelayed(this, 1000); }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        tick.removeCallbacksAndMessages(null);
    }

    void show() {
        findViewById(R.id.trace).invalidate();
        values[0].setText(String.format(Locale.FRANCE, "%.1f km", Trip.meters / 1000));
        values[1].setText(hm(SystemClock.elapsedRealtime() - Trip.startMs));
        values[2].setText(hm(Trip.movingMs));
        values[3].setText(Math.round(Trip.avgKmh()) + " km/h");
        values[4].setText(Math.round(Trip.maxKmh) + " km/h");
        values[5].setText(DateFormat.format("HH:mm", Trip.startWallMs));
    }

    static String hm(long ms) {
        long min = ms / 60000;
        return min >= 60 ? String.format(Locale.FRANCE, "%d h %02d", min / 60, min % 60) : min + " min";
    }
}
