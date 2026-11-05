package com.launcherautoradio;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
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

        float dp = getResources().getDisplayMetrics().density;
        LinearLayout row = findViewById(R.id.swatches);
        int current = Themes.index(this);
        for (int i = 0; i < Themes.COLORS.length; i++) {
            final int k = i;
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);

            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            dot.setColor(Themes.COLORS[i]);
            if (i == current) dot.setStroke((int) (4 * dp), getColor(R.color.text));
            Button swatch = new Button(this);
            swatch.setBackground(dot);
            swatch.setStateListAnimator(null);
            swatch.setText(i == current ? "✓" : "");
            swatch.setTextColor(0xFFFFFFFF);
            swatch.setTextSize(28);
            swatch.setContentDescription(Themes.NAMES[i] + (i == current ? ", sélectionné" : ""));
            swatch.setOnClickListener(v -> { Themes.set(this, k); recreate(); });
            cell.addView(swatch, new LinearLayout.LayoutParams((int) (80 * dp), (int) (80 * dp)));

            TextView name = new TextView(this);
            name.setText(Themes.NAMES[i]);
            name.setTextColor(getColor(i == current ? R.color.text : R.color.muted));
            name.setTextSize(18);
            name.setPadding(0, (int) (8 * dp), 0, 0);
            name.setImportantForAccessibility(TextView.IMPORTANT_FOR_ACCESSIBILITY_NO);
            cell.addView(name);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            row.addView(cell, lp);
        }
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
