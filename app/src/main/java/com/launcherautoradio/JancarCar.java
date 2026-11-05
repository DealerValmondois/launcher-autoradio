package com.launcherautoradio;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

// Client for the head unit's Jancar car service (the same binder interface the stock CAN-bus app uses).
// Transaction codes and formats come from that app's com.jancar.services.car.ICar stub.
class JancarCar implements ServiceConnection {
    static final String DESCRIPTOR = "com.jancar.services.car.ICar";
    static final int TX_REAL_TIME_INFO = 9, TX_OUTSIDE_TEMP_RAW = 24;
    static final int SPEED = 1, ENGINE_RPM = 3; // RealTimeInfo ids

    volatile IBinder car;

    boolean bind(Context c) {
        return c.bindService(new Intent("com.jancar.services.action.car").setPackage("com.jancar.services"),
                this, Context.BIND_AUTO_CREATE);
    }

    @Override public void onServiceConnected(ComponentName n, IBinder b) { car = b; }
    @Override public void onServiceDisconnected(ComponentName n) { car = null; }

    boolean connected() { return car != null; }

    float realTimeInfo(int id) throws RemoteException {
        Parcel d = Parcel.obtain(), r = Parcel.obtain();
        try {
            d.writeInterfaceToken(DESCRIPTOR);
            d.writeInt(id);
            car.transact(TX_REAL_TIME_INFO, d, r, 0);
            r.readException();
            return r.readFloat();
        } finally { d.recycle(); r.recycle(); }
    }

    float outsideTempC() throws RemoteException {
        Parcel d = Parcel.obtain(), r = Parcel.obtain();
        try {
            d.writeInterfaceToken(DESCRIPTOR);
            car.transact(TX_OUTSIDE_TEMP_RAW, d, r, 0);
            r.readException();
            return decodeTemp(r.readInt());
        } finally { d.recycle(); r.recycle(); }
    }

    // Raw format: high byte = value, low byte bit0 = unit (0 °C offset 100, 1 °F offset 60), bits 6-7 = half-degree flag.
    static float decodeTemp(int raw) {
        int value = (raw >> 8) & 255, flags = raw & 255;
        boolean fahrenheit = (flags & 1) != 0, half = ((flags & 192) >> 6) == 2;
        float t = value - (fahrenheit ? 60f : 100f);
        if (half) t += t < 0 ? -0.5f : 0.5f;
        return fahrenheit ? (t - 32) * 5 / 9 : t;
    }

    static {
        // Self-check of the decoder: 11 °C, -4.5 °C, and 50 °F = 10 °C.
        if (decodeTemp(111 << 8) != 11f || decodeTemp((96 << 8) | 0x80) != -4.5f || decodeTemp((110 << 8) | 1) != 10f)
            throw new AssertionError("decodeTemp");
    }
}
