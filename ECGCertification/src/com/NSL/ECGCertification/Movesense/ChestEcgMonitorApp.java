package com.NSL.ECGCertification.Movesense;

import android.app.Application;
import android.content.Context;

import com.NSL.ECGCertification.R;
import com.NSL.ECGCertification.Movesense.bluetooth.BluetoothStatusMonitor;
import com.NSL.ECGCertification.Movesense.bluetooth.MdsRx;
import com.NSL.ECGCertification.Movesense.bluetooth.RxBle;
import com.NSL.ECGCertification.Movesense.utils.Util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Application for making all initializations
 */
public class ChestEcgMonitorApp extends Application {


    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize RxBleWrapper
        RxBle.Instance.initialize(this);

        // Copy necessary configuration file to proper place
        copyRawResourceToFile(R.raw.kompostisettings, "KompostiSettings.xml");

        // Initialize MDS
        MdsRx.Instance.initialize(this);

        BluetoothStatusMonitor.INSTANCE.initBluetoothStatus();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

    }

    /**
     * Copy raw resource file to file.
     *
     * @param resourceId Resource id.
     * @param fileName   Target file name.
     */
    private void copyRawResourceToFile(int resourceId, String fileName) {
        InputStream in = null;
        FileOutputStream out = null;
        try {
            in = getResources().openRawResource(resourceId);
            out = openFileOutput(fileName, Context.MODE_PRIVATE);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not copy configuration file to: " + fileName);
        } finally {
            Util.safeClose(out);
            Util.safeClose(in);
        }
    }
}
