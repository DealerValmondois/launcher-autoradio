package com.launcherautoradio;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Chronometer;
import android.widget.Toast;

public class MainActivity extends Activity implements LocationListener {
    static final String APK_URL = "https://github.com/DealerValmondois/launcher-autoradio/releases/download/latest/app-debug.apk";
    // Tiles whose app the user picks; index = request code.
    static final String[] KEYS = {"carplay", "music", "phone"};
    static final int[] TILES = {R.id.tile_carplay, R.id.tile_music, R.id.tile_phone};

    LocationManager lm;
    SpeedGauge speed;
    CompassTape heading;
    Odometer trip;
    Chronometer duration;
    View locAccess;
    int themeShown;

    @Override
    protected void onCreate(Bundle b) {
        Themes.apply(this);
        themeShown = Themes.index(this);
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        lm = getSystemService(LocationManager.class);
        speed = findViewById(R.id.speed);
        heading = findViewById(R.id.heading);
        trip = findViewById(R.id.trip);
        duration = findViewById(R.id.duration);
        locAccess = findViewById(R.id.loc_access);

        for (int i = 0; i < KEYS.length; i++) {
            final int k = i;
            findViewById(TILES[k]).setOnClickListener(v -> launch(k));
            findViewById(TILES[k]).setOnLongClickListener(v -> { pick(k); return true; });
        }
        findViewById(R.id.tile_nav).setOnClickListener(v -> open(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="))));
        findViewById(R.id.tile_apps).setOnClickListener(v -> open(new Intent(this, AppsActivity.class)));
        findViewById(R.id.tile_settings).setOnClickListener(v -> open(new Intent(this, SettingsActivity.class)));
        trip.setOnClickListener(v -> open(new Intent(this, TripActivity.class)));
        findViewById(R.id.accel).setOnClickListener(v -> open(new Intent(this, AccelActivity.class)));
        trip.setOnLongClickListener(v -> {
            resetTrip();
            Toast.makeText(this, "Trajet remis à zéro", Toast.LENGTH_SHORT).show();
            return true;
        });
        locAccess.setOnClickListener(v -> askLocation());

        duration.setBase(Trip.startMs);
        duration.start();
        showTrip();
        if (!startGps()) askLocation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Themes.index(this) != themeShown) { recreate(); return; } // colour changed in Réglages
        AppsActivity.preload(this); // keeps the app list ready (and fresh after installs) for an instant Applis menu
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        lm.removeUpdates(this);
    }

    // --- GPS: speed, heading, trip ---

    void askLocation() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] res) {
        startGps();
    }

    // Listens for the whole life of the launcher so the trip keeps counting while CarPlay is in front.
    boolean startGps() {
        boolean ok = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        locAccess.setVisibility(ok ? View.GONE : View.VISIBLE);
        // A crash here would lock the user out of their home screen, so a missing GPS only shows a message.
        if (ok) try { lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this); }
        catch (IllegalArgumentException e) { Toast.makeText(this, "GPS introuvable", Toast.LENGTH_LONG).show(); }
        return ok;
    }

    void resetTrip() {
        Trip.reset();
        duration.setBase(Trip.startMs);
        duration.start();
        showTrip();
    }

    void showTrip() {
        trip.setValue(Trip.meters / 1000);
    }

    @Override
    public void onLocationChanged(Location l) {
        float kmh = l.hasSpeed() ? l.getSpeed() * 3.6f : 0;
        speed.setSpeed(kmh);
        if (l.hasBearing() && kmh > 5) heading.setBearing(l.getBearing());
        // Count distance only when moving with a decent fix, so GPS jitter while parked adds nothing.
        Trip.update(l, kmh);
        showTrip();
    }

    // Abstract before Android 11: implemented so older units don't crash when GPS is toggled.
    @Override public void onProviderEnabled(String p) {}
    @Override public void onProviderDisabled(String p) {}
    @Override public void onStatusChanged(String p, int s, Bundle e) {}

    // --- Tiles ---

    void launch(int k) {
        String pkg = getPreferences(MODE_PRIVATE).getString(KEYS[k], null);
        Intent i = pkg == null ? null : getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) pick(k); else open(i);
    }

    void pick(int k) {
        Toast.makeText(this, R.string.pick_app, Toast.LENGTH_LONG).show();
        startActivityForResult(new Intent(this, AppsActivity.class).putExtra("pick", true), k);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req >= 0 && req < KEYS.length && res == RESULT_OK && data != null)
            getPreferences(MODE_PRIVATE).edit().putString(KEYS[req], data.getStringExtra("pkg")).apply();
    }

    void open(Intent i) {
        try { startActivity(i); }
        catch (ActivityNotFoundException e) { Toast.makeText(this, R.string.no_app, Toast.LENGTH_SHORT).show(); }
    }

}
