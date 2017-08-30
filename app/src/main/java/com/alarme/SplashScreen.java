package com.alarme;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class SplashScreen extends Activity {

    private com.wang.avi.AVLoadingIndicatorView avi;


    @Override
    protected void onCreate(Bundle savedInstanceState) {



        super.onCreate(savedInstanceState);

        if (!isTaskRoot()) {
            finish();
            return;
        }



        setContentView(R.layout.activity_splash_screen);
        avi = (com.wang.avi.AVLoadingIndicatorView) findViewById(R.id.avi);



        startAnim();

        new Handler().postDelayed(new Runnable() {

			/*
			 * Showing splash screen with a timer. This will be useful when you
			 * want to show case your app logo / company
			 */

            @Override
            public void run() {
                // This method will be executed once the timer is over
                // Start your app main activity
              //  startService(new Intent(getBaseContext(), ForegroundService.class));

                Intent i = new Intent(getApplicationContext(), MainActivity.class);
                //i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);



                // close this activity
                finish();
            }
        }, 3000);


    }


    void startAnim(){
        avi.show();
        // or avi.smoothToShow();
    }

    void stopAnim(){
        avi.hide();
        // or avi.smoothToHide();
    }
}
