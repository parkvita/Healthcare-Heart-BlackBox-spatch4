package com.NSL.ECGCertification;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.ImageView;
import android.content.Context;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;

public class CertActivity extends AppCompatActivity {
    private ImageView cert_gif;
    public static Context mContext_cert;

    ProgressDialog dialog;

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cert);
        mContext_cert=this;

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); // 전체창


        cert_gif=(ImageView)findViewById(R.id.gif_certimage);
        GlideDrawableImageViewTarget gifimage = new GlideDrawableImageViewTarget(cert_gif);
        Glide.with(this).load(R.drawable.temp_cert).into(gifimage);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}

