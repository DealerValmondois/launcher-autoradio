package com.launcherautoradio;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
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
import java.util.List;

public class MainActivity extends Activity {
    static final int PICK_CARPLAY = 1;

    MediaSessionManager msm;
    ComponentName listener;
    MediaController controller;
    TextView title, artist;
    ImageView art;
    ImageButton play;
    View access;

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

        findViewById(R.id.tile_carplay).setOnClickListener(v -> launchCarPlay());
        findViewById(R.id.tile_carplay).setOnLongClickListener(v -> { pickCarPlay(); return true; });
        findViewById(R.id.tile_nav).setOnClickListener(v -> open(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="))));
        findViewById(R.id.tile_music).setOnClickListener(v -> open(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC)));
        findViewById(R.id.tile_phone).setOnClickListener(v -> open(new Intent(Intent.ACTION_DIAL)));
        findViewById(R.id.tile_apps).setOnClickListener(v -> open(new Intent(this, AppsActivity.class)));
        findViewById(R.id.tile_settings).setOnClickListener(v -> open(new Intent(Settings.ACTION_SETTINGS)));
        access.setOnClickListener(v -> open(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));

        findViewById(R.id.prev).setOnClickListener(v -> { if (controller != null) controller.getTransportControls().skipToPrevious(); });
        findViewById(R.id.next).setOnClickListener(v -> { if (controller != null) controller.getTransportControls().skipToNext(); });
        play.setOnClickListener(v -> {
            if (controller == null) return;
            if (isPlaying()) controller.getTransportControls().pause();
            else controller.getTransportControls().play();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        PlaybackState s = controller == null ? null : controller.getPlaybackState();
        return s != null && s.getState() == PlaybackState.STATE_PLAYING;
    }

    void showMedia() {
        MediaMetadata m = controller == null ? null : controller.getMetadata();
        Bitmap bmp = null;
        if (m == null) {
            title.setText(R.string.no_music);
            artist.setText("");
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

    void launchCarPlay() {
        String pkg = getPreferences(MODE_PRIVATE).getString("carplay", null);
        Intent i = pkg == null ? null : getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) pickCarPlay(); else open(i);
    }

    void pickCarPlay() {
        Toast.makeText(this, R.string.pick_carplay, Toast.LENGTH_LONG).show();
        startActivityForResult(new Intent(this, AppsActivity.class).putExtra("pick", true), PICK_CARPLAY);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == PICK_CARPLAY && res == RESULT_OK && data != null)
            getPreferences(MODE_PRIVATE).edit().putString("carplay", data.getStringExtra("pkg")).apply();
    }

    void open(Intent i) {
        try { startActivity(i); }
        catch (ActivityNotFoundException e) { Toast.makeText(this, R.string.no_app, Toast.LENGTH_SHORT).show(); }
    }
}
