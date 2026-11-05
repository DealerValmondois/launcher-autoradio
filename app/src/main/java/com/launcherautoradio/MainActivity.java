package com.launcherautoradio;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.util.List;

public class MainActivity extends Activity {
    // Tiles whose app the user picks; index = request code.
    static final String APK_URL = "https://github.com/DealerValmondois/launcher-autoradio/releases/download/latest/app-debug.apk";
    static final String APK_MIME = "application/vnd.android.package-archive";
    static final String[] KEYS = {"carplay", "music", "phone"};
    static final int[] TILES = {R.id.tile_carplay, R.id.tile_music, R.id.tile_phone};

    MediaSessionManager msm;
    ComponentName listener;
    MediaController controller;
    TextView title, artist;
    ImageView art;
    ImageButton play;
    View access;
    AudioManager am;
    String bTitle, bArtist; // from legacy "metachanged" broadcasts, used when no media session exists

    DownloadManager dm;
    long downloadId = -1;

    final BroadcastReceiver downloaded = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (i.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -2) != downloadId) return;
            Uri apk = dm.getUriForDownloadedFile(downloadId); // null if the download failed
            downloadId = -1;
            if (apk == null) { Toast.makeText(c, "Échec du téléchargement", Toast.LENGTH_LONG).show(); return; }
            open(new Intent(Intent.ACTION_VIEW).setDataAndType(apk, APK_MIME)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    };

    final BroadcastReceiver meta = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (i.hasExtra("track")) { bTitle = i.getStringExtra("track"); bArtist = i.getStringExtra("artist"); }
            showMedia();
        }
    };

    final MediaController.Callback cb = new MediaController.Callback() {
        @Override public void onMetadataChanged(MediaMetadata m) { showMedia(); }
        @Override public void onPlaybackStateChanged(PlaybackState s) { showMedia(); }
        @Override public void onSessionDestroyed() { bind(null); }
    };
    final MediaSessionManager.OnActiveSessionsChangedListener sessions = this::bind;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        msm = getSystemService(MediaSessionManager.class);
        listener = new ComponentName(this, MediaListener.class);
        title = findViewById(R.id.title);
        artist = findViewById(R.id.artist);
        art = findViewById(R.id.art);
        play = findViewById(R.id.play);
        access = findViewById(R.id.access);

        am = getSystemService(AudioManager.class);
        dm = getSystemService(DownloadManager.class);
        IntentFilter done = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(downloaded, done, Context.RECEIVER_EXPORTED);
        else registerReceiver(downloaded, done);
        findViewById(R.id.update).setOnClickListener(v -> update());
        for (int i = 0; i < KEYS.length; i++) {
            final int k = i;
            findViewById(TILES[k]).setOnClickListener(v -> launch(k));
            findViewById(TILES[k]).setOnLongClickListener(v -> { pick(k); return true; });
        }
        findViewById(R.id.tile_nav).setOnClickListener(v -> open(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="))));
        findViewById(R.id.tile_apps).setOnClickListener(v -> open(new Intent(this, AppsActivity.class)));
        findViewById(R.id.tile_settings).setOnClickListener(v -> open(new Intent(Settings.ACTION_SETTINGS)));
        access.setOnClickListener(v -> open(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));

        findViewById(R.id.prev).setOnClickListener(v -> {
            if (controller != null) controller.getTransportControls().skipToPrevious();
            else mediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        });
        findViewById(R.id.next).setOnClickListener(v -> {
            if (controller != null) controller.getTransportControls().skipToNext();
            else mediaKey(KeyEvent.KEYCODE_MEDIA_NEXT);
        });
        play.setOnClickListener(v -> {
            if (controller == null) mediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
            else if (isPlaying()) controller.getTransportControls().pause();
            else controller.getTransportControls().play();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter f = new IntentFilter("com.android.music.metachanged");
        f.addAction("com.android.music.playstatechanged");
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(meta, f, Context.RECEIVER_EXPORTED);
        else registerReceiver(meta, f);
        try {
            msm.addOnActiveSessionsChangedListener(sessions, listener);
            bind(msm.getActiveSessions(listener));
            access.setVisibility(View.GONE);
        } catch (SecurityException e) { // notification access not granted yet
            access.setVisibility(View.VISIBLE);
            bind(null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(meta);
        msm.removeOnActiveSessionsChangedListener(sessions);
        bind(null);
    }

    void bind(List<MediaController> list) {
        if (controller != null) controller.unregisterCallback(cb);
        controller = (list == null || list.isEmpty()) ? null : list.get(0);
        if (controller != null) controller.registerCallback(cb);
        showMedia();
    }

    boolean isPlaying() {
        if (controller == null) return am.isMusicActive();
        PlaybackState s = controller.getPlaybackState();
        return s != null && s.getState() == PlaybackState.STATE_PLAYING;
    }

    void showMedia() {
        MediaMetadata m = controller == null ? null : controller.getMetadata();
        Bitmap bmp = null;
        if (m == null) {
            if (bTitle != null) title.setText(bTitle); else title.setText(R.string.no_music);
            artist.setText(bArtist);
        } else {
            title.setText(m.getString(MediaMetadata.METADATA_KEY_TITLE));
            artist.setText(m.getString(MediaMetadata.METADATA_KEY_ARTIST));
            bmp = m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
            if (bmp == null) bmp = m.getBitmap(MediaMetadata.METADATA_KEY_ART);
        }
        if (bmp != null) {
            art.setScaleType(ImageView.ScaleType.CENTER_CROP);
            art.setImageBitmap(bmp);
        } else {
            art.setScaleType(ImageView.ScaleType.CENTER);
            art.setImageResource(R.drawable.ic_music);
        }
        play.setImageResource(isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(downloaded);
    }

    void update() {
        if (downloadId != -1) return; // already downloading
        new File(getExternalFilesDir(null), "update.apk").delete();
        downloadId = dm.enqueue(new DownloadManager.Request(Uri.parse(APK_URL))
                .setMimeType(APK_MIME)
                .setTitle("Launcher Auto")
                .setDestinationInExternalFilesDir(this, null, "update.apk"));
        Toast.makeText(this, "Téléchargement de la mise à jour…", Toast.LENGTH_LONG).show();
    }

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

    // Fallback when the player exposes no media session: send a media key like the steering-wheel buttons.
    void mediaKey(int code) {
        long t = SystemClock.uptimeMillis();
        am.dispatchMediaKeyEvent(new KeyEvent(t, t, KeyEvent.ACTION_DOWN, code, 0));
        am.dispatchMediaKeyEvent(new KeyEvent(t, t, KeyEvent.ACTION_UP, code, 0));
        play.postDelayed(this::showMedia, 500);
    }

    void open(Intent i) {
        try { startActivity(i); }
        catch (ActivityNotFoundException e) { Toast.makeText(this, R.string.no_app, Toast.LENGTH_SHORT).show(); }
    }
}
