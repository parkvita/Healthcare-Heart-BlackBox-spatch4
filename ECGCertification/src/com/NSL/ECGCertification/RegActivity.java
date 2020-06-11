package com.NSL.ECGCertification;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;

public class RegActivity extends AppCompatActivity {
    private ImageView cert_gif;
    public static Context mContext_reg;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reg);
        mContext_reg = this;

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); // 전체창

        cert_gif = (ImageView) findViewById(R.id.gif_regimage);
        GlideDrawableImageViewTarget gifimage = new GlideDrawableImageViewTarget(cert_gif);
        Glide.with(this).load(R.drawable.temp_reg).into(gifimage);

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        PrefActivity.key_gen=false;
    }
}