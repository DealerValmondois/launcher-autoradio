package com.launcherautoradio;

import android.location.Location;
import android.os.SystemClock;

// Current trip, shared by the home screen and the stats page; lives as long as the launcher process.
final class Trip {
    static float meters, maxKmh;
    static long startMs = SystemClock.elapsedRealtime(), movingMs, startWallMs = System.currentTimeMillis();
    static Location last;

    static void reset() {
        meters = 0;
        maxKmh = 0;
        movingMs = 0;
        last = null;
        startMs = SystemClock.elapsedRealtime();
        startWallMs = System.currentTimeMillis();
    }

    static void update(Location l, float kmh) {
        boolean good = l.getAccuracy() < 30; // ignore poor fixes: they cause fake jumps and speed spikes
        if (last != null && kmh > 3 && good) {
            meters += last.distanceTo(l);
            // Capped so a long GPS dropout doesn't count as one huge moving interval.
            movingMs += Math.min(5000, (l.getElapsedRealtimeNanos() - last.getElapsedRealtimeNanos()) / 1_000_000);
        }
        if (good) maxKmh = Math.max(maxKmh, kmh);
        last = l;
    }

    // meters per millisecond × 3600 = km/h
    static float avgKmh() { return movingMs > 0 ? meters / movingMs * 3600 : 0; }
}
