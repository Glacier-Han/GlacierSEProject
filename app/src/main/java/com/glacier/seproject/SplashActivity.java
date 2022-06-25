package com.glacier.seproject;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;


public class SplashActivity extends AppCompatActivity {

    /**
     * developer : Glacier (한병하 2018111316)
     * name      : smart window controller
     * project   : KNU HUSTAR SE FINAL PROJECT
     * keystore  : glacier key0 glacier
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        //전체화면모드
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                    Intent intent2 = new Intent(getApplication(), MainActivity.class);
                    startActivity(intent2);
                    finish();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        },1500);
    }
}