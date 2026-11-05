package com.launcherautoradio;

import android.location.Location;
import android.os.SystemClock;

// Current trip, shared by the home screen and the stats page; lives as long as the launcher process.
final class Trip {
    static float meters, maxKmh;
    static long startMs = SystemClock.elapsedRealtime(), movingMs, startWallMs = System.currentTimeMillis();
    static Location last;
    // Route trace: x/y in metres from the first point, one point per 10 m (about 80 KB per hour of driving).
    static float[] xy = new float[4096];
    static int points;
    static double lat0, lon0;

    static void reset() {
        meters = 0;
        maxKmh = 0;
        movingMs = 0;
        last = null;
        points = 0;
        startMs = SystemClock.elapsedRealtime();
        startWallMs = System.currentTimeMillis();
    }

    static void update(Location l, float kmh) {
        boolean good = l.getAccuracy() < 30; // ignore poor fixes: they cause fake jumps and speed spikes
        if (last != null && kmh > 3 && good) {
            meters += last.distanceTo(l);
            addPoint(l);
            // Capped so a long GPS dropout doesn't count as one huge moving interval.
            movingMs += Math.min(5000, (l.getElapsedRealtimeNanos() - last.getElapsedRealtimeNanos()) / 1_000_000);
        }
        if (good) maxKmh = Math.max(maxKmh, kmh);
        last = l;
    }

    static void addPoint(Location l) {
        if (points == 0) { lat0 = l.getLatitude(); lon0 = l.getLongitude(); }
        float x = (float) ((l.getLongitude() - lon0) * 111320 * Math.cos(Math.toRadians(lat0)));
        float y = (float) ((l.getLatitude() - lat0) * 110540);
        if (points > 0 && Math.hypot(x - xy[2 * points - 2], y - xy[2 * points - 1]) < 10) return;
        if (2 * points + 2 > xy.length) xy = java.util.Arrays.copyOf(xy, xy.length * 2);
        xy[2 * points] = x;
        xy[2 * points + 1] = y;
        points++;
    }

    // meters per millisecond × 3600 = km/h
    static float avgKmh() { return movingMs > 0 ? meters / movingMs * 3600 : 0; }
}
