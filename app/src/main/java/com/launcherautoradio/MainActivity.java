package com.launcherautoradio;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.View;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Locale;

public class MainActivity extends Activity implements LocationListener {
    static final String APK_URL = "https://github.com/DealerValmondois/launcher-autoradio/releases/download/latest/app-debug.apk";
    // Tiles whose app the user picks; index = request code.
    static final String[] KEYS = {"carplay", "music", "phone"};
    static final int[] TILES = {R.id.tile_carplay, R.id.tile_music, R.id.tile_phone};
    static final String[] DIRS = {"N", "NE", "E", "SE", "S", "SO", "O", "NO"};

    LocationManager lm;
    Location last;
    float tripMeters;
    TextView speed, heading, trip;
    Chronometer duration;
    View locAccess;
    volatile boolean updating;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        lm = getSystemService(LocationManager.class);
        speed = findViewById(R.id.speed);
        heading = findViewById(R.id.heading);
        trip = findViewById(R.id.trip);
        duration = findViewById(R.id.duration);
        locAccess = findViewById(R.id.loc_access);

        try { // show the build number so it's easy to tell which version is installed
            ((TextView) findViewById(R.id.update)).append(" · v" + getPackageManager().getPackageInfo(getPackageName(), 0).versionCode);
        } catch (Exception ignored) {}
        findViewById(R.id.update).setOnClickListener(v -> update());

        for (int i = 0; i < KEYS.length; i++) {
            final int k = i;
            findViewById(TILES[k]).setOnClickListener(v -> launch(k));
            findViewById(TILES[k]).setOnLongClickListener(v -> { pick(k); return true; });
        }
        findViewById(R.id.tile_nav).setOnClickListener(v -> open(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="))));
        findViewById(R.id.tile_apps).setOnClickListener(v -> open(new Intent(this, AppsActivity.class)));
        findViewById(R.id.tile_settings).setOnClickListener(v -> open(new Intent(Settings.ACTION_SETTINGS)));
        findViewById(R.id.reset).setOnClickListener(v -> resetTrip());
        locAccess.setOnClickListener(v -> askLocation());

        resetTrip();
        if (!startGps()) askLocation();
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
        tripMeters = 0;
        last = null;
        duration.setBase(SystemClock.elapsedRealtime());
        duration.start();
        showTrip();
    }

    void showTrip() {
        trip.setText(String.format(Locale.FRANCE, "%.1f km", tripMeters / 1000));
    }

    @Override
    public void onLocationChanged(Location l) {
        float kmh = l.hasSpeed() ? l.getSpeed() * 3.6f : 0;
        speed.setText(String.valueOf(Math.round(kmh)));
        if (l.hasBearing() && kmh > 5) heading.setText(DIRS[Math.round(l.getBearing() / 45f) % 8]);
        // Count distance only when moving with a decent fix, so GPS jitter while parked adds nothing.
        if (last != null && kmh > 3 && l.getAccuracy() < 30) tripMeters += last.distanceTo(l);
        last = l;
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

    // --- Self update ---

    // Streams the latest APK straight into the system installer (DownloadManager is often missing on head units).
    void update() {
        if (updating) return;
        updating = true;
        Toast.makeText(this, "Téléchargement de la mise à jour…", Toast.LENGTH_LONG).show();
        new Thread(() -> {
            try {
                PackageInstaller pi = getPackageManager().getPackageInstaller();
                int id = pi.createSession(new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL));
                PackageInstaller.Session session = pi.openSession(id);
                try (InputStream in = new URL(APK_URL).openStream(); OutputStream out = session.openWrite("apk", 0, -1)) {
                    copy(in, out);
                    session.fsync(out);
                }
                Intent status = new Intent(this, MainActivity.class).setAction("install");
                session.commit(PendingIntent.getActivity(this, 0, status,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE).getIntentSender());
                session.close();
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Échec de la mise à jour : " + e, Toast.LENGTH_LONG).show());
            } finally {
                updating = false;
            }
        }).start();
    }

    static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[65536];
        for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
    }

    // Installer result: ask the user to confirm, or report a failure.
    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        if (!"install".equals(i.getAction())) return;
        int st = i.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        if (st == PackageInstaller.STATUS_PENDING_USER_ACTION) open((Intent) i.getParcelableExtra(Intent.EXTRA_INTENT));
        else if (st != PackageInstaller.STATUS_SUCCESS)
            Toast.makeText(this, "Échec : " + i.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE), Toast.LENGTH_LONG).show();
    }
}
