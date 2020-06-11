package com.NSL.ECGCertification;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class PrefActivity extends AppCompatActivity {
    public static final String PREF_NAME = "hr";
    public static final String PREF_KEY_CALIBRATION_TIME= "PREF_KEY_CALIBRATION_TIME";
    public static final String PREF_KEY_R_MAX = "PREF_KEY_R_MAX";
    public static final String PREF_KEY_R_MIN = "PREF_KEY_R_MIN";
    public static final String PREF_KEY_S_MAX = "PREF_KEY_S_MAX";
    public static final String PREF_KEY_S_MIN = "PREF_KEY_S_MIN";
    public static final String PREF_KEY_T_MAX = "PREF_KEY_T_MAX";
    public static final String PREF_KEY_T_MIN = "PREF_KEY_T_MIN";
    public static final String PREF_KEY_P_MAX = "PREF_KEY_P_MAX";
    public static final String PREF_KEY_P_MIN = "PREF_KEY_P_MIN";
    public static final String PREF_KEY_Q_MAX = "PREF_KEY_Q_MAX";
    public static final String PREF_KEY_Q_MIN = "PREF_KEY_Q_MIN";
    public static Context mContext_pref;
    public static boolean key_gen=false;
    private EditText calibrationTimeEt;
    InputMethodManager imm;
    private Toast toast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pref);
        mContext_pref=this;
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); // 전체창

        calibrationTimeEt = (EditText)findViewById(R.id.calibrationTimeEt);
        findViewById(R.id.cert_start).setOnClickListener(mClickListener);
        findViewById(R.id.cert_init).setOnClickListener(mClickListener1);
        setUp();
        imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);

    }

    public void setKey_gen() {
        key_gen=false;
    }

    Button.OnClickListener mClickListener= new View.OnClickListener() {
        public void onClick(View v) {
            SharedPreferences prefs= ((MainActivity) MainActivity.mContext).getPrefs();

            if(prefs.getInt(PrefActivity.PREF_KEY_P_MAX, 1)!=0) {
                toast=Toast.makeText(getApplicationContext(), "Warning : Data Exist", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER,0,0);
                ViewGroup group = (ViewGroup)toast.getView();
                TextView messageText=(TextView)group.getChildAt(0);
                messageText.setBackgroundResource(R.drawable.back);
                messageText.setTextSize(TypedValue.COMPLEX_UNIT_DIP,30);
                toast.show();

                key_gen=false;
            }
            else {
                Intent intent = new Intent(PrefActivity.this, RegActivity.class);
                startActivity(intent);
                key_gen = true;
            }
            finish();

        }
    };


    Button.OnClickListener mClickListener1= new View.OnClickListener() {
        public void onClick(View v) {
            key_gen=false;
            ((MainActivity) MainActivity.mContext).sethilow();
            SharedPreferences.Editor editor = ((MainActivity) MainActivity.mContext).getPrefs().edit();
            editor.putInt(PrefActivity.PREF_KEY_R_MAX, 0);
            editor.putInt(PrefActivity.PREF_KEY_R_MIN, 5000);
            editor.putInt(PrefActivity.PREF_KEY_S_MAX, 0);
            editor.putInt(PrefActivity.PREF_KEY_S_MIN, 5000);
            editor.putInt(PrefActivity.PREF_KEY_T_MAX, 0);
            editor.putInt(PrefActivity.PREF_KEY_T_MIN, 5000);
            editor.putInt(PrefActivity.PREF_KEY_P_MAX, 0);
            editor.putInt(PrefActivity.PREF_KEY_P_MIN, 5000);
            editor.putInt(PrefActivity.PREF_KEY_Q_MAX, 0);
            editor.putInt(PrefActivity.PREF_KEY_Q_MIN, 5000);
            editor.commit();
            toast=Toast.makeText(getApplicationContext(), "Initial Status", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER,0,0);
            ViewGroup group = (ViewGroup)toast.getView();
            TextView messageText=(TextView)group.getChildAt(0);
            messageText.setBackgroundResource(R.drawable.back);
            messageText.setTextSize(TypedValue.COMPLEX_UNIT_DIP,30);
            toast.show();

            Log.e("버튼","초기화 완료");


        }
    };

    private void setUp()
    {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        calibrationTimeEt.setText(prefs.getString(PREF_KEY_CALIBRATION_TIME, "120"));
    }


    private void save() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_KEY_CALIBRATION_TIME, calibrationTimeEt.getText().toString());
        editor.commit();
        Log.e("prefs", prefs.getString(PREF_KEY_CALIBRATION_TIME, ""));
    }

    protected void onStop() {
        super.onStop();
        save();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
