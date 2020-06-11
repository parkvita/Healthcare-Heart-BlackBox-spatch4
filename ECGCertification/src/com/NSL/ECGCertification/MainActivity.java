package com.NSL.ECGCertification;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.Legend.LegendForm;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.components.YAxis.AxisDependency;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.NSL.ECGCertification.R.id.r;
import static com.NSL.ECGCertification.R.id.t;


public class MainActivity extends AppCompatActivity implements OnChartValueSelectedListener {
    /**
     * 그래프 상단의 Entry 보여줄 넓이
     */
    private final int GRAPH_WIDTH = 600;

    private LineChart mChart;
    private TextView ecgTv;
    private ImageView lightIv;
    private TextView pTv, qTv, rTv, sTv, tTv;
    private TextView hrTv, hrTv1, systolicTv, diastolicTv;
    private ImageView heart_gif;
    public static Context mContext;

    /**
     * ble데이터 두묶음 저장
     */
    private ArrayList<Integer> tempDatas;  // 전체 데이터 측정기록 축적
    private ArrayList<Integer> tempDatas_1; // PQRST 한주기 탐색용 데이터셋
    /**
     * RSTPQ 배열을 담음
     * 최초 켈리브레이션에 사용됨
     */
    private CalibList<int[]> calibList;

    /**
     * 실시간 rstpq 를 담아 비교하는 리스트
     */
    private ArrayList<int[]> pickUpList;
    private PickUp pickUp;
    private SharedPreferences prefs;
    private String p_str, q_str, r_str, s_str, t_str;
    private String DBp_str, DBq_str, DBr_str, DBs_str, DBt_str;
    private int tem_d = 0;
    private boolean hilow = true;
    private int[] ecg_high = new int[5];
    private int[] ecg_low = new int[5];
    public int constR = 2520, constQ = 1985, constS = 2017, constT = 2060; // 교수님이전 : constR=2800,constQ=2350,constS=2230,constT=2330 // 교수님최신 : constR=2430,constQ=1985,constS=2017,constT=2060 // 허재욱 : constR=2520,constQ=1985,constS=2017,constT=2060
    private int systolic, diastolic;
    private int DB_HR, DBsystolic, DBdiastolic;
    private int cert_count = 0, cert_ok = 0;
    private boolean cert_check = false, cert_key = false;
    private Toast toast;
    public static int PublicHR;
    public static ArrayList<Integer> PublicrawdataECG;

    public static boolean Spatch2setting = false;
    public static boolean Movesensesetting = false;

    public static boolean fff = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); // 전체창
        setContentView(R.layout.activity_main);

        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0); // 위치 권한

        findViewById(R.id.cert).setOnClickListener(mClickListener2);
        ecgTv = (TextView) findViewById(R.id.ecgTv);
        lightIv = (ImageView) findViewById(R.id.lightIv);
        heart_gif = (ImageView) findViewById(R.id.gif_image);
        GlideDrawableImageViewTarget gifimage = new GlideDrawableImageViewTarget(heart_gif);
        Glide.with(this).load(R.drawable.heart_motion).into(gifimage);

        calibList = new CalibList<>();
        pickUpList = new ArrayList<>();
        tempDatas = new ArrayList<>();
        tempDatas_1 = new ArrayList<>();
        prefs = getSharedPreferences(PrefActivity.PREF_NAME, MODE_PRIVATE);
        pickUp = new PickUp(getApplicationContext());
        ecgTv = (TextView) findViewById(R.id.ecgTv);
        pTv = (TextView) findViewById(R.id.p);
        qTv = (TextView) findViewById(R.id.q);
        rTv = (TextView) findViewById(r);
        sTv = (TextView) findViewById(R.id.s);
        tTv = (TextView) findViewById(t);
        hrTv = (TextView) findViewById(R.id.HeartRate);
        hrTv1 = (TextView) findViewById(R.id.HeartRate1);
        diastolicTv = (TextView) findViewById(R.id.diastolicTv1);
        systolicTv = (TextView) findViewById(R.id.systolicTv1);

        setUpChart();
        if (PublicrawdataECG != null) {
            class NewRunnable implements Runnable {
                @Override
                public void run() {
                    while (true) {
                        if (!fff) {
                            // 현재 시간 구하기
                            long now = System.currentTimeMillis();
                            Date edate = new Date(now);
                            // 시간 출력될 포맷 설정
                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                            String formatDate = simpleDateFormat.format(edate);

                            // 현재 시간 구하기
                            long now2 = System.currentTimeMillis();
                            Date edate2 = new Date(now2);
                            // 시간 출력될 포맷 설정
                            SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("hh:mm:ss");
                            String formatDate2 = simpleDateFormat2.format(edate2);

                            // DB에 데이터 추가
                            final DBHelper dbHelper = new DBHelper(getApplicationContext(), "ECGDATA.db", null, 1);
                            String date = formatDate;
                            String time = formatDate2;
                            String ECGdatap = DBp_str;
                            String ECGdataq = DBq_str;
                            String ECGdatar = DBr_str;
                            String ECGdatas = DBs_str;
                            String ECGdatat = DBt_str;
                            int HRBPM = DB_HR;
                            int BPsystolic = DBsystolic;
                            int BPdiastolic = DBdiastolic;
                            ArrayList<Integer> RawdataECG = PublicrawdataECG;

                            dbHelper.insertECGDATA(date, time, ECGdatap, ECGdataq, ECGdatar, ECGdatas, ECGdatat, HRBPM, BPsystolic, BPdiastolic);
                            dbHelper.insertECGrawdata(RawdataECG);

                            for (final Integer n : PublicrawdataECG) { //그래프그리기
                                runOnUiThread(new Runnable() { //그래프 그리기
                                    @Override
                                    public void run() {

                                        addEntry((float) n);

                                        if (tempDatas_1.size() >= 625) { // 125 x 5
                                            int[] calibArray = PickUp.toIntArray(tempDatas_1);
                                            pickUp.setList(calibArray);

                                            pickUp.setOnFindListener(new PickUp.OnFindListener() {
                                                @Override
                                                public void onFind(Integer[] rstpq, int times) {
                                                }
                                            });
                                            pickUp.find();

                                            if (pickUp.y_data[0] != tem_d) {
                                                tem_d = pickUp.y_data[0];
                                                // String.format("%3f",pickUp.y_data[3]/(float)1000)
                                                if (Movesensesetting) { // 무브센스볼트계산
                                                    p_str = String.format("%.3f", pickUp.y_data[3] / (float) 1000) + "V \n" + p_str;
                                                    Log.i("TAG", "P data:" + pickUp.y_data[3]);
                                                    q_str = String.format("%.3f", pickUp.y_data[4] / (float) 800) + "V \n" + q_str;
                                                    Log.i("TAG", "Q data:" + pickUp.y_data[4]);
                                                    r_str = String.format("%.3f", pickUp.y_data[0] / (float) 1450) + "V \n" + r_str;
                                                    Log.i("TAG", "R data:" + pickUp.y_data[0]);
                                                    s_str = String.format("%.3f", pickUp.SS / (float) 700) + "V \n" + s_str;
                                                    Log.i("TAG", "S data:" + pickUp.SS);
                                                    t_str = String.format("%.3f", pickUp.y_data[2] / (float) 1000) + "V \n" + t_str;
                                                }
                                                if (Spatch2setting) { // 에스패치센스볼트계산
                                                    p_str = String.format("%.3f", pickUp.y_data[3] / (float) 1000) + "V \n" + p_str;
                                                    Log.i("TAG", "P data:" + pickUp.y_data[3]);
                                                    q_str = String.format("%.3f", pickUp.y_data[4] / (float) 1000) + "V \n" + q_str;
                                                    Log.i("TAG", "Q data:" + pickUp.y_data[4]);
                                                    r_str = String.format("%.3f", pickUp.y_data[0] / (float) 1000) + "V \n" + r_str;
                                                    Log.i("TAG", "R data:" + pickUp.y_data[0]);
                                                    s_str = String.format("%.3f", pickUp.SS / (float) 1000) + "V \n" + s_str;
                                                    Log.i("TAG", "S data:" + pickUp.SS);
                                                    t_str = String.format("%.3f", pickUp.y_data[2] / (float) 1000) + "V \n" + t_str;
                                                }
                                                Log.i("TAG", "T data:" + pickUp.y_data[2]);
                                                Log.i("QRS", "QRS 1.R-Q:" + (pickUp.y_data[0] - pickUp.y_data[4]));
                                                Log.i("QRS", "QRS 2.R-S:" + (pickUp.y_data[0] - pickUp.y_data[1]));
                                                Log.i("QRS", "QRS 3.QRS:" + (pickUp.y_data[1] + pickUp.y_data[0] + pickUp.y_data[4]));

                                                rTv.setText(r_str);
                                                sTv.setText(s_str);
                                                tTv.setText(t_str);
                                                pTv.setText(p_str);
                                                qTv.setText(q_str);

                                                // DB 저장용
                                                DBr_str = String.format("%.3f", pickUp.y_data[0] / (float) 1000);
                                                DBs_str = String.format("%.3f", pickUp.SS / (float) 1000);
                                                DBt_str = String.format("%.3f", pickUp.y_data[2] / (float) 1000);
                                                DBp_str = String.format("%.3f", pickUp.y_data[3] / (float) 1000);
                                                DBq_str = String.format("%.3f", pickUp.y_data[4] / (float) 1000);
                                                DB_HR = PublicHR;

                                                if (Movesensesetting) { // 무브센스혈압계산
                                                    systolic = 30 * (pickUp.y_data[0] - pickUp.y_data[4]) / (constR - constQ) + 10 * (pickUp.y_data[2] - pickUp.SS) / (constT - constS);
                                                    diastolic = systolic - 41; // 재욱 : 9 , 4  선우형 : 30 , 10
                                                }
                                                if (Spatch2setting) { // 에스패치혈압계산
                                                    //constR  Q. .S .const T
                                                    // [0] =R , [1] = S , [2] = T , [3] =P , [4] =Q
                                                    //diastolic = 80 * (pickUp.y_data[0] - pickUp.y_data[4]) / (constR - constQ); //R-P
                                                    systolic = 80 * (pickUp.y_data[0] - pickUp.y_data[4]) / (constR - constQ) + 40 * (pickUp.y_data[2] - pickUp.SS) / (constT - constS);
                                                    diastolic = systolic - 41;
                                                }

                                                if (PublicHR < 220) { // 심박수 220이상 무시
                                                    hrTv1.setText(String.valueOf(PublicHR) + " BPM");
                                                }
                                                if (systolic < 25000) { // 250이상 무시
                                                    systolicTv.setText(String.valueOf(systolic) + "mmHg");
                                                    DBsystolic = systolic; // DB 저장용
                                                }
                                                if (diastolic < 18000) { // 180이상 무시
                                                    diastolicTv.setText(String.valueOf(diastolic) + "mmHg");
                                                    DBdiastolic = diastolic; // DB 저장용
                                                }

                                                if (PrefActivity.key_gen) { // 등록
                                                    Log.e("버튼 실행 확인", "등록");
                                                    Log.e("Certification", "Certification Key Generating...");

                                                    if (hilow) { // high,low 값 초기화
                                                        for (int j = 0; j < 5; j++)
                                                            ecg_high[j] = ecg_low[j] = pickUp.y_data[j];
                                                        tempDatas.clear();
                                                        hilow = false;
                                                    }

                                                    if (tempDatas.size() <= Integer.parseInt(prefs.getString(PrefActivity.PREF_KEY_CALIBRATION_TIME, "120")) * 250) {
                                                        for (int j = 0; j < 5; j++) {
                                                            if (ecg_high[j] < pickUp.y_data[j])
                                                                ecg_high[j] = pickUp.y_data[j];
                                                            if (ecg_low[j] > pickUp.y_data[j])
                                                                ecg_low[j] = pickUp.y_data[j];
                                                        }

                                                        SharedPreferences.Editor editor = prefs.edit();
                                                        if (prefs.getInt(PrefActivity.PREF_KEY_R_MAX, 0) < ecg_high[0])
                                                            editor.putInt(PrefActivity.PREF_KEY_R_MAX, (Integer) ecg_high[0]);
                                                        if (prefs.getInt(PrefActivity.PREF_KEY_R_MIN, 0) > ecg_low[0])
                                                            editor.putInt(PrefActivity.PREF_KEY_R_MIN, (Integer) ecg_low[0]);
                                                        if (prefs.getInt(PrefActivity.PREF_KEY_S_MAX, 0) < ecg_high[1])
                                                            editor.putInt(PrefActivity.PREF_KEY_S_MAX, (Integer) ecg_high[1]);
                                                        if (prefs.getInt(PrefActivity.PREF_KEY_S_MIN, 0) > ecg_low[1])
                                                            editor.putInt(PrefActivity.PREF_KEY_S_MIN, (Integer) ecg_low[1]);
                                                        if (prefs.getInt(PrefActivity.PREF_KEY_T_MAX, 0) < ecg_high[2])
                                                            editor.putInt(PrefActivity.PREF_KEY_T_MAX, (Integer) ecg_high[2]);
                                                        if (prefs.getInt(PrefActivity.PREF_KEY_T_MIN, 0) > ecg_low[2])
                                                            editor.putInt(PrefActivity.PREF_KEY_T_MIN, (Integer) ecg_low[2]);
                                                        if (prefs.getInt(PrefActivity.PREF_KEY_P_MAX, 0) < ecg_high[3])
                                                            editor.putInt(PrefActivity.PREF_KEY_P_MAX, (Integer) ecg_high[3]);
                                                        if (prefs.getInt(PrefActivity.PREF_KEY_P_MIN, 0) > ecg_low[3])
                                                            editor.putInt(PrefActivity.PREF_KEY_P_MIN, (Integer) ecg_low[3]);
                                                        if (prefs.getInt(PrefActivity.PREF_KEY_Q_MAX, 0) < ecg_high[4])
                                                            editor.putInt(PrefActivity.PREF_KEY_Q_MAX, (Integer) ecg_high[4]);
                                                        if (prefs.getInt(PrefActivity.PREF_KEY_Q_MIN, 0) > ecg_low[4])
                                                            editor.putInt(PrefActivity.PREF_KEY_Q_MIN, (Integer) ecg_low[4]);
                                                        editor.commit();

                                                        Log.e("high-low", tempDatas.size() + "   High : " + prefs.getInt(PrefActivity.PREF_KEY_P_MAX, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_Q_MAX, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_R_MAX, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_S_MAX, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_T_MAX, 0));
                                                        Log.e("high-low", " Low : " + prefs.getInt(PrefActivity.PREF_KEY_P_MIN, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_Q_MIN, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_R_MIN, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_S_MIN, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_T_MIN, 0));
                                                    } else {
                                                        ((PrefActivity) PrefActivity.mContext_pref).setKey_gen();
                                                        ((RegActivity) RegActivity.mContext_reg).finish();

                                                        Toast.makeText(getApplicationContext(), "Registration", Toast.LENGTH_LONG).show();

                                                        Log.e("버튼-등록", "high : " + prefs.getInt(PrefActivity.PREF_KEY_P_MAX, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_Q_MAX, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_R_MAX, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_S_MAX, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_T_MAX, 0));
                                                    }
                                                }

                                                if (cert_key) { // 인증
                                                    Log.e("버튼 실행 확인", "인증");
                                                    if (prefs.getInt(PrefActivity.PREF_KEY_P_MAX, 0) != 0) {//데이터 등록됨
                                                        cert_check = true;
                                                        if (prefs.getInt(PrefActivity.PREF_KEY_R_MAX, 0) + 20 < pickUp.y_data[0] || prefs.getInt(PrefActivity.PREF_KEY_R_MIN, 0) - 20 > pickUp.y_data[0])
                                                            cert_check = false;
                                                        if (prefs.getInt(PrefActivity.PREF_KEY_S_MAX, 0) + 20 < pickUp.y_data[1] || prefs.getInt(PrefActivity.PREF_KEY_S_MIN, 0) - 20 > pickUp.y_data[1])
                                                            cert_check = false;
                                                        if (prefs.getInt(PrefActivity.PREF_KEY_P_MAX, 0) + 20 < pickUp.y_data[3] || prefs.getInt(PrefActivity.PREF_KEY_P_MIN, 0) - 20 > pickUp.y_data[3])
                                                            cert_check = false;
                                                        if (prefs.getInt(PrefActivity.PREF_KEY_T_MAX, 0) + 20 < pickUp.y_data[2] || prefs.getInt(PrefActivity.PREF_KEY_T_MIN, 0) - 20 > pickUp.y_data[2])
                                                            cert_check = false;
                                                        if (prefs.getInt(PrefActivity.PREF_KEY_Q_MAX, 0) + 20 < pickUp.y_data[4] || prefs.getInt(PrefActivity.PREF_KEY_Q_MIN, 0) - 20 > pickUp.y_data[4])
                                                            cert_check = false;
                                                        Log.e("버튼-인증중", "high" + prefs.getInt(PrefActivity.PREF_KEY_P_MAX, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_Q_MAX, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_R_MAX, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_S_MAX, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_T_MAX, 0));
                                                        Log.e("버튼-인증중", " Low : " + prefs.getInt(PrefActivity.PREF_KEY_P_MIN, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_Q_MIN, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_R_MIN, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_S_MIN, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_T_MIN, 0));
                                                        Log.e("버튼-인증중", "" + pickUp.y_data[3] + " " + pickUp.y_data[4] + " " + pickUp.y_data[0] + " " + pickUp.y_data[1] + " " + pickUp.y_data[2] + " cert_ok " + cert_ok + " cert_cout" + cert_count);
                                                        if (cert_check)
                                                            cert_ok++;

                                                        if (cert_ok == 3) {
                                                            ((CertActivity) CertActivity.mContext_cert).finish();
                                                            cert_count = 0;
                                                            cert_ok = 0;
                                                            cert_key = false;
                                                            toast = Toast.makeText(getApplicationContext(), "Accept", Toast.LENGTH_LONG);
                                                            toast.setGravity(Gravity.CENTER, 0, 0);
                                                            ViewGroup group = (ViewGroup) toast.getView();
                                                            TextView messageText = (TextView) group.getChildAt(0);
                                                            messageText.setBackgroundResource(R.drawable.back);
                                                            messageText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 30);
                                                            toast.show();

                                                            Log.e("버튼-인증성공", prefs.getInt(PrefActivity.PREF_KEY_P_MAX, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_Q_MAX, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_R_MAX, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_S_MAX, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_T_MAX, 0));
                                                            Log.e("버튼-인증성공", " Low : " + prefs.getInt(PrefActivity.PREF_KEY_P_MIN, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_Q_MIN, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_R_MIN, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_S_MIN, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_T_MIN, 0));
                                                        }

                                                        if (cert_count == 5) {
                                                            ((CertActivity) CertActivity.mContext_cert).finish();
                                                            cert_count = 0;
                                                            cert_ok = 0;
                                                            cert_key = false;
                                                            toast = Toast.makeText(getApplicationContext(), "Reject", Toast.LENGTH_LONG);
                                                            toast.setGravity(Gravity.CENTER, 0, 0);
                                                            ViewGroup group = (ViewGroup) toast.getView();
                                                            TextView messageText = (TextView) group.getChildAt(0);
                                                            messageText.setBackgroundResource(R.drawable.back);
                                                            messageText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 30);
                                                            toast.show();
                                                        }
                                                        cert_count++;

                                                    } else {
                                                        ((CertActivity) CertActivity.mContext_cert).finish();
                                                        toast = Toast.makeText(getApplicationContext(), "Warning : Register Data First", Toast.LENGTH_LONG);
                                                        toast.setGravity(Gravity.CENTER, 0, 0);
                                                        ViewGroup group = (ViewGroup) toast.getView();
                                                        TextView messageText = (TextView) group.getChildAt(0);
                                                        messageText.setBackgroundResource(R.drawable.back);
                                                        messageText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 30);
                                                        toast.show();
                                                        cert_key = false;
                                                    }
                                                }
                                            }

                                            if (tempDatas.size() > 2500) {
                                                if ((systolic >= 180 && diastolic >= 120 && PublicHR >= 200) || (systolic <= 50 && diastolic <= 33 && PublicHR <= 30)) {
                                                    Log.e("light-result", pickUp.RED + "");
                                                    lightIv.setBackgroundResource(R.drawable.red_light);
                                                } else if ((systolic >= 90 && systolic <= 139) && (diastolic >= 60 && diastolic <= 89)) {
                                                    Log.e("light-result", pickUp.GREEN + "");
                                                    lightIv.setBackgroundResource(R.drawable.green_light);
                                                } else {
                                                    Log.e("light-result", pickUp.ORANGE + "");
                                                    lightIv.setBackgroundResource(R.drawable.orange_light);
                                                }
                                            }
                                            tempDatas_1.clear();
                                        }
                                    }
                                });
                                tempDatas.add(n);
                                tempDatas_1.add(n);
                            }

                            try {
                                Thread.sleep(10);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            fff = true;
                        }
                    }
                }
            }
            NewRunnable nr = new NewRunnable();
            Thread t = new Thread(nr);
            t.start();
        }
    }


    Button.OnClickListener mClickListener2 = new View.OnClickListener() {
        public void onClick(View v) {
            cert_key = true;
            SharedPreferences prefs = ((MainActivity) MainActivity.mContext).getPrefs();
            Log.e("버튼", "   High : " + prefs.getInt(PrefActivity.PREF_KEY_P_MAX, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_Q_MAX, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_R_MAX, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_S_MAX, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_T_MAX, 0));
            Log.e("버튼", " Low : " + prefs.getInt(PrefActivity.PREF_KEY_P_MIN, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_Q_MIN, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_R_MIN, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_S_MIN, 0) + " " + prefs.getInt(PrefActivity.PREF_KEY_T_MIN, 0));
            Intent intent = new Intent(MainActivity.this, CertActivity.class);
            startActivity(intent);
            Log.e("버튼 눌림", "버튼이 눌림");
        }
    };

    // public int() {return }
    public SharedPreferences getPrefs() {
        return prefs;
    }

    public void sethilow() {
        hilow = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) { // 메뉴

        switch (item.getItemId()) {
            /* case R.id.Connect: {
                break;
            } */

            case R.id.Preference: {
                Intent intent = new Intent(this, PrefActivity.class);
                startActivity(intent);
            }
            break;

            case R.id.Movesense: {
                Intent intent = new Intent(this, com.NSL.ECGCertification.Movesense.MovesenseActivity.class);
                startActivity(intent);
                //finish();
            }
            break;

            case R.id.Spatch2: {
                Intent intent = new Intent(this, com.NSL.ECGCertification.Spatch2.Spatch2.class);
                startActivity(intent);
                //finish();
            }
            break;

            /*case R.id.Loadecgdata: {
                Intent intent = new Intent(this, DBActivity.class);
                startActivity(intent);
            }
            break;
            */
        }
        return true;
    }


    private void setUpChart() //차트셋업
    {
        mChart = (LineChart) findViewById(R.id.chart1);
        mChart.setOnChartValueSelectedListener(this);

        // enable description text
        mChart.getDescription().setEnabled(true);

        // enable touch gestures
        mChart.setTouchEnabled(true);

        // enable scaling and dragging
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(false);

        // if disabled, scaling can be done on x- and y-axis separately
        mChart.setPinchZoom(true);

        // set an alternative background color
        mChart.setBackgroundColor(Color.BLACK);

        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);

        // add empty data
        mChart.setData(data);

        // get the legend (only possible after setting data)
        Legend l = mChart.getLegend();

        // modify the legend ...
        l.setForm(LegendForm.LINE);
        l.setTextColor(Color.WHITE);

        XAxis xl = mChart.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        if (Spatch2setting) {
            leftAxis.setAxisMaximum(3000); //3000
            leftAxis.setAxisMinimum(1500); //1700
        }
        if (Movesensesetting) {
            leftAxis.setAxisMaximum(5000);
            leftAxis.setAxisMinimum(500);
        } else {
            leftAxis.setAxisMaximum(3000);
            leftAxis.setAxisMinimum(1500);
        }
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    private void addEntry(float value) {
        LineData data = mChart.getData();
        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);
            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }
            data.addEntry(new Entry(set.getEntryCount(), value), 0);
            updateChart(data);
        }
    }

    private void updateChart(LineData data) // 차트업데이트
    {
        data.notifyDataChanged();
        mChart.notifyDataSetChanged();
        mChart.setVisibleXRangeMaximum(GRAPH_WIDTH);
        // mChart.setVisibleYRange(30, AxisDependency.LEFT);
        mChart.moveViewToX(data.getEntryCount());
        // mChart.moveViewTo(data.getXValCount()-7, 55f,
        // AxisDependency.LEFT);
    }


    private LineDataSet createSet() { // 그래프선 설정
        LineDataSet set = new LineDataSet(null, "ECG data (Voltage)");
        set.setAxisDependency(AxisDependency.LEFT);
        set.setColor(Color.RED);
        set.setCircleColor(Color.WHITE);
        set.setLineWidth(1f);
        set.setCircleRadius(3f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.WHITE);
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        return set;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) { // 권한
        if (requestCode == 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.e("permission grant", "permission is granted");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {
    }

    @Override
    public void onNothingSelected() {
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finishAffinity();
        System.exit(0);
    }
}
