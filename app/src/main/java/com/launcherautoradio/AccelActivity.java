package com.launcherautoradio;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

// Full-screen 0-100 km/h timer. Arms when stopped, starts on the first movement, stops at 100 km/h.
// GPS gives about one fix per second, so start and finish are interpolated between fixes.
public class AccelActivity extends Activity implements LocationListener {
    enum State { WAIT_STOP, ARMED, RUNNING, DONE }
    static final float MOVE = 1, TARGET = 100; // km/h

    LocationManager lm;
    SharedPreferences prefs;
    StopwatchDial dial;
    TextView time, status, speedText, bestText;
    final Handler tick = new Handler(Looper.getMainLooper());
    State state = State.WAIT_STOP;
    long startNs, prevNs;
    float prevKmh = -1, best, result;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_accel);
        lm = getSystemService(LocationManager.class);
        prefs = getSharedPreferences("accel", MODE_PRIVATE);
        best = prefs.getFloat("best", 0);
        dial = findViewById(R.id.dial);
        time = findViewById(R.id.time);
        status = findViewById(R.id.status);
        speedText = findViewById(R.id.speed);
        bestText = findViewById(R.id.best);
        findViewById(R.id.back).setOnClickListener(v -> finish());
        bestText.setOnLongClickListener(v -> {
            best = 0;
            prefs.edit().remove("best").apply();
            Toast.makeText(this, "Record effacé", Toast.LENGTH_SHORT).show();
            show(0);
            return true;
        });
        show(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            status.setText("Autorise la localisation sur l'écran d'accueil");
            return;
        }
        try { lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this); }
        catch (IllegalArgumentException e) { status.setText("GPS introuvable"); }
    }

    @Override
    protected void onPause() {
        super.onPause();
        lm.removeUpdates(this);
        tick.removeCallbacksAndMessages(null);
    }

    @Override
    public void onLocationChanged(Location l) {
        float kmh = l.hasSpeed() ? l.getSpeed() * 3.6f : 0;
        long ns = l.getElapsedRealtimeNanos();
        speedText.setText(Math.round(kmh) + " km/h");
        switch (state) {
            case WAIT_STOP:
            case DONE:
                if (kmh < MOVE) state = State.ARMED;
                break;
            case ARMED:
                if (kmh >= MOVE) {
                    startNs = cross(MOVE, kmh, ns);
                    state = State.RUNNING;
                    tick.post(this::run);
                }
                break;
            case RUNNING:
                if (kmh < MOVE) { state = State.ARMED; tick.removeCallbacksAndMessages(null); }
                else if (kmh >= TARGET) {
                    result = (cross(TARGET, kmh, ns) - startNs) / 1e9f;
                    state = State.DONE;
                    tick.removeCallbacksAndMessages(null);
                    if (best == 0 || result < best) {
                        best = result;
                        prefs.edit().putFloat("best", best).apply();
                        Toast.makeText(this, "Nouveau record !", Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
        prevKmh = kmh;
        prevNs = ns;
        show(state == State.RUNNING ? (SystemClock.elapsedRealtimeNanos() - startNs) / 1e9f : state == State.DONE ? result : 0);
    }

    // Moment the speed crossed `target`, linearly between the previous fix and this one.
    long cross(float target, float kmh, long ns) {
        if (prevKmh < 0 || kmh <= prevKmh) return ns;
        return prevNs + (long) ((target - prevKmh) / (kmh - prevKmh) * (ns - prevNs));
    }

    void run() {
        show((SystemClock.elapsedRealtimeNanos() - startNs) / 1e9f);
        tick.postDelayed(this::run, 30);
    }

    void show(float s) {
        dial.set(s, best);
        time.setText(String.format(Locale.FRANCE, "%.2f s", s));
        status.setText(state == State.WAIT_STOP ? "Arrête-toi pour armer le chrono"
                : state == State.ARMED ? "Prêt : démarre quand tu veux"
                : state == State.RUNNING ? "Chrono lancé…" : "Terminé ! Arrête-toi pour recommencer");
        bestText.setText(best > 0 ? String.format(Locale.FRANCE, "Record : %.2f s", best) : "Pas encore de record");
    }

    @Override public void onProviderEnabled(String p) {}
    @Override public void onProviderDisabled(String p) {}
    @Override public void onStatusChanged(String p, int s, Bundle e) {}
}
