package com.launcherautoradio;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

// Launcher settings: accent colour, shortcut to Android settings, self-update.
public class SettingsActivity extends Activity {
    static final String APK_URL = "https://github.com/DealerValmondois/launcher-autoradio/releases/download/latest/app-debug.apk";
    volatile boolean updating;

    @Override
    protected void onCreate(Bundle b) {
        Themes.apply(this);
        super.onCreate(b);
        setContentView(R.layout.activity_settings);
        findViewById(R.id.back).setOnClickListener(v -> finish());
        findViewById(R.id.android_settings).setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
            catch (ActivityNotFoundException e) { Toast.makeText(this, R.string.no_app, Toast.LENGTH_SHORT).show(); }
        });

        Button update = findViewById(R.id.update);
        try { // show the build number so it's easy to tell which version is installed
            update.append(" · v" + getPackageManager().getPackageInfo(getPackageName(), 0).versionCode);
        } catch (Exception ignored) {}
        update.setOnClickListener(v -> update());

        // Rainbow track built from the real accents of the 24 themes; the theme switches when the finger lifts.
        int[] rainbow = new int[Themes.HUES];
        for (int i = 0; i < Themes.HUES; i++) rainbow[i] = Themes.accentOf(this, i);
        GradientDrawable track = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, rainbow);
        track.setCornerRadius(16 * getResources().getDisplayMetrics().density);
        SeekBar hue = findViewById(R.id.hue);
        View preview = findViewById(R.id.preview);
        hue.setProgressDrawable(track);
        hue.setProgress(Themes.index(this));
        hue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                preview.getBackground().mutate().setTint(rainbow[p]);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {
                if (s.getProgress() != Themes.index(SettingsActivity.this)) {
                    Themes.set(SettingsActivity.this, s.getProgress());
                    recreate();
                }
            }
        });
    }

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
                Intent status = new Intent(this, SettingsActivity.class).setAction("install");
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
        if (st == PackageInstaller.STATUS_PENDING_USER_ACTION) startActivity((Intent) i.getParcelableExtra(Intent.EXTRA_INTENT));
        else if (st != PackageInstaller.STATUS_SUCCESS)
            Toast.makeText(this, "Échec : " + i.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE), Toast.LENGTH_LONG).show();
    }
}
