package com.alarme;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.AlarmClock;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.NotificationCompat;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ForegroundService extends Service {

    public static final String ALARM_ALERT_ACTION = "com.samsung.sec.android.clockpackage.alarm.ALARM_ALERT";
    public static final String ALARM_SNOOZE_ACTION = "com.samsung.sec.android.clockpackage.alarm.ALARM_SNOOZE";
    public static final String SAMSUNG_ALARM_STOPPED_IN_ALERT_ACTION = "com.samsung.sec.android.clockpackage.alarm.ALARM_STOPPED_IN_ALERT";
    public static final String SAMSUNG_ALARM_STOP_ACTION = "com.samsung.sec.android.clockpackage.alarm.ALARM_STOP";
    public static final String NEXUS_ALARM_STOP_ACTION = "com.android.deskclock.ALARM_DISMISS";
    public static final String LG_ALARM_STOP_ACTION = "com.lge.clock.ALARM_DONE";
    public static final String GOOGLE_ALARM_STOP_ACTION = "com.google.android.deskclock.ALARM_DISMISS";
    public static final String MOTOROLA_ALARM_STOP_ACTION = "com.motorola.blur.alarmclock.ALARM_DONE";
    public static final String ZTE_ALARM_STOP_ACTION = "zte.com.cn.alarmclock.ALARM_DONE";
    public static final String SONY_ALARM_STOP_ACTION = "com.sonyericsson.alarm.ALARM_DONE";
    public static final String HTC_ALARM_STOP_ACTION = "com.htc.android.ALARM_DONE";
    public static final String HTC2_ALARM_STOP_ACTION = "com.htc.android.worldclock.ALARM_DONE";


    private SktWeatherWithGPS weather;
    private TTSManager ttsManager;
    private SharedPreferences settings;
    private GoogleCalendarCall myCal;
    private MediaPlayer mPlayer2;
    private boolean briefMode = false;

    private BroadcastReceiver clearReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            String action = intent.getAction();
            if (action.equals("CLEAR")) {
                ttsManager.shutDown();
                if(mPlayer2 != null)
                    mPlayer2.stop();

                NavDrawer.backToOriginal();

                NotificationManager mNotificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);


                int notifyID = 1337;
                NotificationCompat.Builder mBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.drawable.alarme)
                        .setContentTitle("알라미")
                        .setContentText("대기 중")
                        .setPriority(Notification.PRIORITY_MAX);

                Intent intent2 = new Intent(getApplicationContext(), SplashScreen.class);
                intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent2, PendingIntent.FLAG_UPDATE_CURRENT);

                mBuilder.setContentIntent(contentIntent);


                mNotificationManager.notify(
                        notifyID,
                        mBuilder.build());

                Toast.makeText(getApplicationContext(), "알라미 브리핑이 취소되었습니다", Toast.LENGTH_SHORT).show();


            }



        }
    };



    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            // Initiate TTS
            ttsManager = new TTSManager();
            ttsManager.init(getApplicationContext());


            NavDrawer.logoText.setText("데이터 수집 중");
            NavDrawer.avi.setIndicatorColor(Color.BLUE);

             updateBriefNot("브리핑 준비 중");





            //dialog box for quitting
   //         showQuitDialog();

            Toast.makeText(getApplicationContext(), "알라미입니다", Toast.LENGTH_LONG).show();
                weather = new SktWeatherWithGPS(context);
                weather.getLocation();
                myCal.initializeCredential(settings.getString("email", ""));
                myCal.getResultsFromApi();
               // weather.getWeatherData(weather.lat, weather.lng);


                // wait till it collects location data
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(weather.location == null && settings.getFloat("lat", 0) == 0) {
                            Toast.makeText(getApplicationContext(), "위치를 켜주시거나 기본 위치를 설정해주세요!", Toast.LENGTH_LONG).show();
                        }else {
                            if (weather.location == null) {
                                weather.getWeatherData(settings.getFloat("lat", 0), settings.getFloat("lng", 0));
                            } else {
                                weather.getWeatherData(weather.lat, weather.lng);
                            }

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if(weather.getResponse() != null) {
                                        String speechCal = " 오늘은 특정한 일정이 없으시네요.";
                                        if (myCal.getEventAndTime() == null || myCal.getEventAndTime().isEmpty()) {

                                        } else {
                                            speechCal = "그리고 오늘은 ";
                                            for (int i = 0; i < myCal.getEventAndTime().size(); i++) {
                                                speechCal += "\n " + myCal.getEventAndTime().get(i);
                                            }
                                            speechCal += "가 있으시네요.";
                                        }

                                        if (!settings.getBoolean("googleCalInfo", true)) {
                                            speechCal = "";
                                        }

                                        String finalSpeech = "";

//                                    Pattern pat = Pattern.compile("등산");
//                                    Matcher matcher = pat.matcher(speechCal);
//                                    if(matcher.find())
//                                        speechCal += " 오늘 등산 하시려는 것 같은데 조심하세요.";


                                        Calendar cal = Calendar.getInstance();
                                        int hour = cal.get(Calendar.HOUR_OF_DAY);
                                        int minute = cal.get(Calendar.MINUTE);


                                        String greetings = "";
                                        Random rand = new Random();
                                        if (hour < 12) {
                                            int n = rand.nextInt(4);
                                            if (n == 0) {
                                                greetings = settings.getString("userName", "") + "님, 안녕히 주무셨나요?";
                                            } else if (n == 1) {
                                                greetings = "좋은 아침입니다 " + settings.getString("userName", "") + "님. ";
                                            } else if (n == 2) {
                                                greetings = settings.getString("userName", "") + "님, 잘 주무셨나요?";
                                            } else if (n == 3) {
                                                greetings = settings.getString("userName", "") + "님, 편안히 주무셨나요?";
                                            }
                                        } else {
                                            int n = rand.nextInt(3);
                                            if (n == 0) {
                                                greetings = settings.getString("userName", "") + "님, 잘 쉬셨나요?";
                                            } else if (n == 1) {
                                                greetings = settings.getString("userName", "") + "님, 편안히 쉬셨나요?";
                                            } else if (n == 2) {
                                                greetings = settings.getString("userName", "") + "님, 낮잠 잘 주무셨나요?";
                                            }
                                        }

                                        String finalTime = "오전 " + hour + "시";
                                        if (hour == 0) {
                                            hour = 12;
                                            finalTime = "오전 12시";
                                        } else if (hour > 12) {
                                            hour -= 12;
                                            finalTime = "오후 " + hour + "시";
                                        }
                                        if (minute == 30)
                                            finalTime += " 반으";
                                        else
                                            finalTime += " " + minute + "분";


                                        finalSpeech = greetings + " 현재 " + finalTime + "로 " + weather.getResponse() + speechCal;
                                        if (weather.getColdYes())
                                            finalSpeech += " 감기 조심하시고 ";

                                        int n = rand.nextInt(2);
                                        if (n == 0)
                                            finalSpeech += " 좋은 하루 보내십시오";
                                        else
                                            finalSpeech += " 좋은 하루 되십시오";

                                        if(ttsManager != null){
                                            updateBriefNot("브리핑 중");
                                            NavDrawer.logoText.setText("브리핑 중");
                                            NavDrawer.avi.setIndicatorColor(Color.MAGENTA);
                                            if(settings.getBoolean("music", true)) {
                                                mPlayer2 = MediaPlayer.create(getApplicationContext(), R.raw.iron_man_lights_up);
                                                mPlayer2.setVolume(0.4f, 0.4f);
                                                mPlayer2.start();

                                                mPlayer2.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                                    public void onCompletion(MediaPlayer mp) {
                                                        NavDrawer.backToOriginal();
                                                        updateNotification();
                                                    }
                                                });
                                            }
                                            ttsManager.initQueue(finalSpeech);
                                        }


                                    } // if weather is not null
                                    else{
                                        if(ttsManager != null){
                                            NavDrawer.backToOriginal();
                                            ttsManager.initQueue(settings.getString("userName", "") + "님, 죄송합니다. 알 수 없는 오류로 인해 기상정보를 가져오지 못했습니다. ");
                                            updateNotification();
                                        }
                                    }




                                }
                            }, 7500);

                        }
                    }
                }, 1000);


                // myCal.getResultsFromApi();
             //   isCollectingData = true;

                //   sleeping = false;



        }
    };


    public ForegroundService() {
    }

    public void updateBriefNot(String state){
        Intent clear = new Intent();
        clear.setAction("CLEAR");
        PendingIntent pendingClear = PendingIntent.getBroadcast(getApplicationContext(), 12345, clear, PendingIntent.FLAG_UPDATE_CURRENT);
        //  notif.addAction(R.drawable.back_dialog, "Yes", pendingIntentYes);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);


        int notifyID = 1337;
        NotificationCompat.Builder mBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.alarme)
                .setContentTitle("알라미")
                .setContentText(state)
                .addAction(R.drawable.places_ic_clear, "브리핑 취소", pendingClear)
                .setPriority(Notification.PRIORITY_MAX);

        Intent intent2 = new Intent(this, SplashScreen.class);
        intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent2, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(contentIntent);




        mNotificationManager.notify(
                notifyID,
                mBuilder.build());





    }


    public void updateNotification(){
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);


        int notifyID = 1337;
        NotificationCompat.Builder mBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.alarme)
                .setContentTitle("알라미")
                .setContentText("대기 중")
                .setPriority(Notification.PRIORITY_MAX);

        Intent intent2 = new Intent(this, SplashScreen.class);
        intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent2, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(contentIntent);


        mNotificationManager.notify(
                notifyID,
                mBuilder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        // Initiate TTS
        ttsManager = new TTSManager();
        ttsManager.init(this);

        myCal = new GoogleCalendarCall(getApplicationContext(), settings.getString("email", ""));

        IntentFilter filter = new IntentFilter(AlarmClock.ACTION_DISMISS_ALARM);
        filter.addAction(SAMSUNG_ALARM_STOPPED_IN_ALERT_ACTION);
        filter.addAction(SAMSUNG_ALARM_STOP_ACTION);
        filter.addAction(NEXUS_ALARM_STOP_ACTION);
        filter.addAction(LG_ALARM_STOP_ACTION);
        filter.addAction(GOOGLE_ALARM_STOP_ACTION);
        filter.addAction(MOTOROLA_ALARM_STOP_ACTION);
        filter.addAction(ZTE_ALARM_STOP_ACTION);
        filter.addAction(SONY_ALARM_STOP_ACTION);
        filter.addAction(HTC_ALARM_STOP_ACTION);
        filter.addAction(HTC2_ALARM_STOP_ACTION);



        registerReceiver(broadcastReceiver, filter);

        IntentFilter filter2 = new IntentFilter("CLEAR");
        filter2.addAction("TOUCHED");

        registerReceiver(clearReceiver, filter2);


        NotificationCompat.Builder mBuilder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.drawable.alarme)
                        .setContentTitle("알라미")
                        .setContentText("대기 중")
                        .setOngoing(true)
                        .setPriority(Notification.PRIORITY_MAX);

        Intent intent2 = new Intent(this, SplashScreen.class);
        intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent2, PendingIntent.FLAG_UPDATE_CURRENT);


        mBuilder.setContentIntent(contentIntent);


        Toast.makeText(this, "알라미가 대기합니다", Toast.LENGTH_SHORT).show();



        Intent i = new Intent(this, NavDrawer.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);



        startForeground(1337, mBuilder.build());


        return START_NOT_STICKY;

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

}
