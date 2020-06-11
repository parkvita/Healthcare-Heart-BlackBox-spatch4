package com.NSL.ECGCertification.Movesense;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.NSL.ECGCertification.MainActivity;
import com.NSL.ECGCertification.R;
import com.NSL.ECGCertification.Movesense.model.EcgInfoResponse;
import com.NSL.ECGCertification.Movesense.model.EcgModel;
import com.NSL.ECGCertification.Movesense.model.HeartRate;
import com.NSL.ECGCertification.Movesense.utils.FormatHelper;
import com.google.gson.Gson;
import com.movesense.mds.Mds;
import com.movesense.mds.MdsException;
import com.movesense.mds.MdsNotificationListener;
import com.movesense.mds.MdsResponseListener;
import com.movesense.mds.MdsSubscription;
import com.movesense.mds.internal.connectivity.BleManager;
import com.movesense.mds.internal.connectivity.MovesenseConnectedDevices;
import com.movesense.mds.internal.connectivity.MovesenseDevice;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.RxBleScanResult;

import com.NSL.ECGCertification.Movesense.bluetooth.MdsRx;
import com.NSL.ECGCertification.Movesense.bluetooth.RxBle;
import com.NSL.ECGCertification.Movesense.model.MdsConnectedDevice;
import com.NSL.ECGCertification.Movesense.model.MdsDeviceInfoNewSw;
import com.NSL.ECGCertification.Movesense.model.MdsDeviceInfoOldSw;
import com.NSL.ECGCertification.Movesense.model.RxBleDeviceWrapper;
import com.NSL.ECGCertification.Movesense.utils.ThrowableToastingAction;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

import static com.NSL.ECGCertification.MainActivity.Movesensesetting;
import static com.NSL.ECGCertification.MainActivity.PublicHR;
import static com.NSL.ECGCertification.MainActivity.PublicrawdataECG;
import static com.NSL.ECGCertification.MainActivity.fff;

public class MovesenseActivity extends AppCompatActivity implements MovesenseContract.View, View.OnClickListener, BleManager.IBleConnectionMonitor {

    @BindView(R.id.movesense_recyclerView)
    RecyclerView mMovesenseRecyclerView;
    @BindView(R.id.movesense_infoTv)
    TextView mMovesenseInfoTv;
    @BindView(R.id.movesense_progressBar)
    ProgressBar mMovesenseProgressBar;

    private MovesenseContract.Presenter mMovesensePresenter;
    private ArrayList<RxBleDeviceWrapper> mMovesenseModels;
    private CompositeSubscription scanningSubscriptions;
    private CompositeSubscription connectedDevicesSubscriptions;

    private final String TAG = MovesenseActivity.class.getSimpleName();
    private MovesenseAdapter mMovesenseAdapter;

    private final String ECG_VELOCITY_PATH = "Meas/ECG/";
    private final String ECG_VELOCITY_INFO_PATH = "/Meas/ECG/Info";
    public static final String URI_EVENTLISTENER = "suunto://MDS/EventListener";
    private final String HEART_RATE_PATH = "Meas/Hr";

    private MdsSubscription mdsSubscriptionHr;
    private MdsSubscription mdsSubscriptionEcg;

    private boolean firststart = true;

    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movesense);
        ButterKnife.bind(this);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); // 전체창

        scanningSubscriptions = new CompositeSubscription();
        connectedDevicesSubscriptions = new CompositeSubscription();

        mMovesensePresenter = new MovesensePresenter(this,
                (BluetoothManager) getSystemService(BLUETOOTH_SERVICE));

        mMovesensePresenter.onCreate();

        mMovesenseModels = new ArrayList<>();

        mMovesenseAdapter = new MovesenseAdapter(mMovesenseModels, this);
        mMovesenseRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mMovesenseRecyclerView.setAdapter(mMovesenseAdapter);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        if (!bluetoothAdapter.isEnabled()) {
            // Bluetooth is not enable so run
            bluetoothAdapter.enable();
        }

        startScanning();

    }

    @Override
    public void displayScanResult(RxBleDevice bluetoothDevice, int rssi) {
        Log.d(TAG, "displayScanResult: " + bluetoothDevice.getName());
        mMovesenseAdapter.add(new RxBleDeviceWrapper(rssi, bluetoothDevice));

        mMovesenseAdapter.notifyDataSetChanged();
    }

    @Override
    public void displayErrorMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void registerReceiver(BroadcastReceiver broadcastReceiver) {
        registerReceiver(broadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    @Override
    public boolean checkLocationPermissionIsGranted() {
        return true;
    }

    @Override
    public void setPresenter(MovesenseContract.Presenter presenter) {
        mMovesensePresenter = presenter;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mMovesensePresenter.onBluetoothResult(requestCode, resultCode, data);
    }

    private void startScanning() {
        Log.d(TAG, "START SCANNING !!!");
        // Start scanning
        scanningSubscriptions.add(RxBle.Instance.getClient().scanBleDevices()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<RxBleScanResult>() {
                    @Override
                    public void call(RxBleScanResult rxBleScanResult) {
                        Log.d(TAG, "call: SCANNED: " + rxBleScanResult.getBleDevice().getName() + " " + rxBleScanResult.getBleDevice().getMacAddress()
                                + " rssi: " + rxBleScanResult.getRssi());
                        RxBleDevice rxBleDevice = rxBleScanResult.getBleDevice();

                        if (rxBleDevice.getName() != null && rxBleDevice.getName().contains("Movesense")
                                && !mMovesenseModels.contains(rxBleDevice)) {

                            Log.d(TAG, "call: Add to list " + rxBleScanResult.getBleDevice().getName());
                            mMovesenseAdapter.add(new RxBleDeviceWrapper(rxBleScanResult.getRssi(), rxBleDevice));
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.e(TAG, "scanBleDevices(): ", throwable);
                    }
                }));
    }

    public void loading() {
        dialog = new ProgressDialog(this, R.style.StyledDialog);
        dialog.setMessage("Connecting...");
        dialog.setIndeterminate(true);
        dialog.setCancelable(true);
        dialog.show();
    }


    @Override
    public void onClick(View v) {
        final RxBleDevice rxBleDevice = (RxBleDevice) v.getTag();
        Log.d(TAG, "Connecting to : " + rxBleDevice.getName() + " " + rxBleDevice.getMacAddress());

        //mMovesenseProgressBar.setVisibility(View.GONE);

        Mds.builder().build(this).connect(rxBleDevice.getMacAddress(), null);

        // We are in connecting progress we don't need to scan anymore
        scanningSubscriptions.unsubscribe();
        mMovesensePresenter.stopScanning();

        loading();

        // Monitor for connected devices
        connectedDevicesSubscriptions.add(MdsRx.Instance.connectedDeviceObservable()
                .subscribe(new Action1<MdsConnectedDevice>() {
                    @Override
                    public void call(MdsConnectedDevice mdsConnectedDevice) {
                        // Stop refreshing
                        if (mdsConnectedDevice.getConnection() != null) {
                            Log.e(TAG, "Connected " + mdsConnectedDevice.toString());

                            // Add connected device
                            if (mdsConnectedDevice.getDeviceInfo() instanceof MdsDeviceInfoNewSw) {
                                MdsDeviceInfoNewSw mdsDeviceInfoNewSw = (MdsDeviceInfoNewSw) mdsConnectedDevice.getDeviceInfo();
                                Log.d(TAG, "instanceof MdsDeviceInfoNewSw: " + mdsDeviceInfoNewSw.getAddressInfoNew().get(0).getAddress()
                                        + " : " + mdsDeviceInfoNewSw.getDescription() + " : " + mdsDeviceInfoNewSw.getSerial()
                                        + " : " + mdsDeviceInfoNewSw.getSw());
                                MovesenseConnectedDevices.addConnectedDevice(new MovesenseDevice(
                                        mdsDeviceInfoNewSw.getAddressInfoNew().get(0).getAddress(),
                                        mdsDeviceInfoNewSw.getDescription(),
                                        mdsDeviceInfoNewSw.getSerial(),
                                        mdsDeviceInfoNewSw.getSw()));
                            } else if (mdsConnectedDevice.getDeviceInfo() instanceof MdsDeviceInfoOldSw) {
                                MdsDeviceInfoOldSw mdsDeviceInfoOldSw = (MdsDeviceInfoOldSw) mdsConnectedDevice.getDeviceInfo();
                                Log.d(TAG, "instanceof MdsDeviceInfoOldSw: " + mdsDeviceInfoOldSw.getAddressInfoOld()
                                        + " : " + mdsDeviceInfoOldSw.getDescription() + " : " + mdsDeviceInfoOldSw.getSerial()
                                        + " : " + mdsDeviceInfoOldSw.getSw());
                                MovesenseConnectedDevices.addConnectedDevice(new MovesenseDevice(
                                        mdsDeviceInfoOldSw.getAddressInfoOld(),
                                        mdsDeviceInfoOldSw.getDescription(),
                                        mdsDeviceInfoOldSw.getSerial(),
                                        mdsDeviceInfoOldSw.getSw()));
                            }

                            Log.e(TAG, "List size(): " + MovesenseConnectedDevices.getConnectedDevices().size());
                            connectedDevicesSubscriptions.unsubscribe();

                            // We have a new SdsDevice
                            //startActivity(new Intent(MovesenseActivity.this, EcgActivityGraphView.class));


                            Mds.builder().build(MovesenseActivity.this).get(MdsRx.SCHEME_PREFIX
                                            + MovesenseConnectedDevices.getConnectedDevice(0).getSerial() + ECG_VELOCITY_INFO_PATH,
                                    null, new MdsResponseListener() {
                                        @Override
                                        public void onSuccess(String data) {
                                            Log.d(TAG, "onSuccess()1: " + data);


                                            EcgInfoResponse infoResponse = new Gson().fromJson(data, EcgInfoResponse.class);
                                        }

                                        @Override
                                        public void onError(MdsException error) {
                                            Log.e(TAG, "onError(): ", error);

                                        }
                                    });

                            mdsSubscriptionHr = Mds.builder().build(MovesenseActivity.this).subscribe(URI_EVENTLISTENER,
                                    FormatHelper.formatContractToJson(MovesenseConnectedDevices.getConnectedDevice(0).getSerial(),
                                            HEART_RATE_PATH)
                                    , new MdsNotificationListener() {
                                        @Override
                                        public void onNotification(String data) {
                                            Log.e(TAG, "Heart rate onNotification() : " + data);
                                            HeartRate heartRate = new Gson().fromJson(data, HeartRate.class);

                                            if (heartRate != null) {

                                                PublicHR = Integer.parseInt(String.valueOf(Math.round((60.0 / heartRate.body.rrData[0]) * 1000)));
                                            }
                                        }

                                        @Override
                                        public void onError(MdsException error) {
                                            Log.e(TAG, "Heart rate error", error);
                                        }
                                    });


                            String subscribedSampleRate = String.valueOf(125);
                            mdsSubscriptionEcg = Mds.builder().build(MovesenseActivity.this).subscribe(URI_EVENTLISTENER,
                                    FormatHelper.formatContractToJson(MovesenseConnectedDevices.getConnectedDevice(0)
                                            .getSerial(), ECG_VELOCITY_PATH + subscribedSampleRate), new MdsNotificationListener() {
                                        @Override
                                        public void onNotification(String data) {
                                            Log.d(TAG, "onSuccess()2: " + data);


                                            final EcgModel ecgModel = new Gson().fromJson(
                                                    data, EcgModel.class);

                                            final int[] ecgSamples = ecgModel.getBody().getData();
                                            final int ecgSampleRate = 125;
                                            final float sampleInterval = (float) 1000 / ecgSampleRate;

                                            if (ecgModel.getBody() != null) {
                                                try { // ECG 전송부분
                                                    if (fff) {
                                                        PublicrawdataECG = new ArrayList<Integer>();
                                                        for (int intValue : ecgSamples) {
                                                            int intValue2 = (intValue + 4200) / 2;  // -값을 2100 근사치로
                                                            PublicrawdataECG.add(intValue2);
                                                        }
                                                        PublicrawdataECG.size();
                                                        Log.d(TAG, "PublicrawdataECG: " + PublicrawdataECG);
                                                        fff = false;
                                                    }

                                                    if (firststart) {
                                                        firststart = false;
                                                        Movesensesetting = true;
                                                        startActivity(new Intent(MovesenseActivity.this, MainActivity.class));
                                                        //finish();
                                                        dialog.dismiss();
                                                    }

                                                } catch (IllegalArgumentException e) {
                                                    Log.e(TAG, "GraphView error ", e);
                                                }
                                            }
                                        }

                                        @Override
                                        public void onError(MdsException error) {
                                            Log.e(TAG, "onError(): ", error);

                                            Toast.makeText(MovesenseActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();

                                        }
                                    });
                        } else {
                            Log.e(TAG, "DISCONNECT");
                        }
                    }
                }, new ThrowableToastingAction(this)));
    }


    @Override
    protected void onPause() {
        super.onPause();
        mMovesensePresenter.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy: ");
        mMovesensePresenter.onDestroy();
        connectedDevicesSubscriptions.unsubscribe();
        scanningSubscriptions.unsubscribe();

        BleManager.INSTANCE.removeBleConnectionMonitorListener(this);
    }


    @Override
    public void onDisconnect(String s) {
        Log.d(TAG, "onDisconnect: " + s);
    }

    @Override
    public void onConnect(RxBleDevice rxBleDevice) {
        Log.e(TAG, "onConnect: " + rxBleDevice.getName() + " " + rxBleDevice.getMacAddress());
    }

    @Override
    public void onConnectError(String s, Throwable throwable) {

    }
}
