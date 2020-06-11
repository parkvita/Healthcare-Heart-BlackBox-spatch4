package com.NSL.ECGCertification.Spatch2;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.NSL.ECGCertification.MainActivity;
import com.NSL.ECGCertification.R;
import com.slsi.bp2_packet_structure.Packet_v0;
import com.slsi.spatch2_library.SPATCH2_Manager;
import com.slsi.spatch2_library.SPATCH2_Utils;


import static android.content.ContentValues.TAG;
import static com.NSL.ECGCertification.MainActivity.Spatch2setting;
import static com.NSL.ECGCertification.MainActivity.fff;

public class Spatch2 extends AppCompatActivity {

    public static final String KEY_NAME = "hr";
    public static final String KEY_DEVICE_ID = "KEY_DEVICE_ID";

    public static Context mContext;
    private int pri_minor;
    public int pri_minor2 = 0;
    private EditText deviceName;
    private boolean firststart = true;

    ProgressDialog dialog;

    // S-PATCH2
    private SPATCH2_Manager mSPATCH2_Manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); // 전체창

        setContentView(R.layout.activity_spatch2);

        deviceName = (EditText) findViewById(R.id.deviceName);
        findViewById(R.id.spatchconnect).setOnClickListener(mClickListener);
        setUp();
        setUpSPatch();
    }

    Button.OnClickListener mClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            save();
            pri_minor2 = 1;
            loading();
        }
    };

    public void loading() {
        dialog = new ProgressDialog(this, R.style.StyledDialog);
        dialog.setMessage("Connecting...");
        dialog.setIndeterminate(true);
        dialog.setCancelable(true);
        dialog.show();
    }

    private void setUp() {
        SharedPreferences connect = getSharedPreferences(KEY_NAME, MODE_PRIVATE);
        deviceName.setText(connect.getString(KEY_DEVICE_ID, ""));
    }

    private void save() {
        SharedPreferences connect = getSharedPreferences(KEY_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = connect.edit();
        editor.putString(KEY_DEVICE_ID, deviceName.getText().toString());
        editor.commit();
        Log.e("prefs", connect.getString(KEY_DEVICE_ID, ""));
    }


    private void setUpSPatch() {
        mSPATCH2_Manager = new SPATCH2_Manager(this);
        mSPATCH2_Manager.setRangingListener(new SPATCH2_Manager.RangingListener() {
            @Override
            public void onSPATCH2Discovered() {
            }

            @Override
            public void onSPATCH2Lost() {
            }

            @Override
            public void onSPATCH2Immediate(String macAddr) {
                SharedPreferences connect = getSharedPreferences(KEY_NAME, MODE_PRIVATE);
                if (pri_minor == Integer.parseInt(connect.getString(KEY_DEVICE_ID, "")) && pri_minor2 == 1) {
                    mSPATCH2_Manager.stopRanging();
                    mSPATCH2_Manager.startMonitor();
                }
            }

            @Override
            public boolean onSPATCH2Detected(BluetoothDevice btDevice, byte[] scanRecord, SPATCH2_Utils.Proximity proximity) {
                boolean isValidSPatch = false;
                String deviceName = btDevice.getName();
                String address = btDevice.getAddress();
                Log.d(TAG, "SPatch2 Detected: " + deviceName + ", " + address + ", " + proximity);

                if (deviceName != null) {
                    if (deviceName.equals("S-PATCH2") & scanRecord != null) {
                        int major = ((scanRecord[25] << 8) & 0x0000ff00) + (scanRecord[26] & 0x000000ff);
                        int minor = ((scanRecord[27] << 8) & 0x0000ff00) + (scanRecord[28] & 0x000000ff);

                        Log.d(TAG, "SPatch2 [" + major + ", " + minor + "]");

                        int targetScanMinor = 27;
                        if ((major == 0x00005348) & (minor == targetScanMinor))
                            isValidSPatch = true;
                        if ((major == 20545) & (minor == targetScanMinor)) isValidSPatch = true;
                        pri_minor = minor;
                    }
                }
                if (isValidSPatch && proximity == SPATCH2_Utils.Proximity.IMMEDIATE) {
                    Log.d(TAG, "Immediate SPatch2 is detected");
                    SharedPreferences connect = getSharedPreferences(KEY_NAME, MODE_PRIVATE);
                    if (pri_minor == Integer.parseInt(connect.getString(KEY_DEVICE_ID, "")) && pri_minor2 == 1) {
                        mSPATCH2_Manager.stopRanging();
                        mSPATCH2_Manager.startMonitor();
                    }
                }
                return isValidSPatch;
            }
        });

        mSPATCH2_Manager.setMonitorListener(new SPATCH2_Manager.MonitorListener() {
            @Override
            public void receivedECGData(Packet_v0 packet) { // ECG 전송부분

                if (fff) {
                    MainActivity.PublicHR = packet.data; // 심박수
                    MainActivity.PublicrawdataECG = packet.rawData; // ECG
                    Log.i("TAG", "Heart Rate: " + packet.data);
                    Log.i("TAG", "ECGdata: " + packet.rawData);
                    fff = false;
                }

                if (firststart) {
                    firststart = false;
                    Spatch2setting = true;
                    startActivity(new Intent(Spatch2.this, MainActivity.class));
                    dialog.dismiss();
                    //finish();
                }
            }

            @Override
            public void receivedSKTData(Packet_v0 packet) {
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();

        // Check if device supports Bluetooth Low Energy.
        if (!mSPATCH2_Manager.hasBluetooth()) {
            Toast.makeText(this, "Device does not have Bluetooth Low Energy", Toast.LENGTH_LONG).show();
            return;
        }

        // If Bluetooth is not enabled, let user enable it.
        if (!mSPATCH2_Manager.isBluetoothEnabled()) {
            final int REQUEST_ENABLE_BT = 1;
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        mSPATCH2_Manager.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy: ");
        save();
        mSPATCH2_Manager.stopRanging();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

}
