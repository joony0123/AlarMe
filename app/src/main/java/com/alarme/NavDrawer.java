package com.alarme;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.NotificationCompat;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

public class NavDrawer extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static com.wang.avi.AVLoadingIndicatorView avi;
    public static TextView logoText;
    public static int origAviCol;

    SharedPreferences settings;
    private boolean starting = false;

    public static void backToOriginal() {
        avi.setIndicatorColor(origAviCol);
        logoText.setText("A C T I V E");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isTaskRoot()) {
            stopService(new Intent(getApplicationContext(), ForegroundService.class));
            finish();
            return;
        }


        setContentView(R.layout.activity_nav_drawer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());


        avi = (com.wang.avi.AVLoadingIndicatorView) findViewById(R.id.avi2);
        avi.show();
        origAviCol = avi.getIndicator().getColor();

        logoText = (TextView) findViewById(R.id.logoText);



        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


  //      updateNotification();

    }


    @Override
    protected void onResume() {
        super.onResume();
        if(starting) {
            TextView name = (TextView) findViewById(R.id.name);
            name.setText(settings.getString("userName", "") + "님");
            TextView emailLayout = (TextView) findViewById(R.id.emailLayout);
            emailLayout.setText(settings.getString("email", ""));
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            moveTaskToBack(true);
         //   super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.nav_drawer, menu);


        TextView name = (TextView) findViewById(R.id.name);
        name.setText(settings.getString("userName", "") + "님");
        TextView emailLayout = (TextView) findViewById(R.id.emailLayout);
        emailLayout.setText(settings.getString("email", ""));
        starting = true;

        return true;
    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.GeneralPreferenceFragment.class.getName());
            startActivity(intent);

            // Handle the camera action
        } else if (id == R.id.nav_gallery) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.NotificationPreferenceFragment.class.getName());
            startActivity(intent);

        } else if (id == R.id.nav_slideshow) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("서비스 종료");
            builder.setMessage("정말로 종료하시겠습니까?");


// Set up the buttons
            builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    Toast.makeText(getApplicationContext(), "알라미 서비스를 종료합니다", Toast.LENGTH_SHORT).show();
                    stopService(new Intent(getApplicationContext(), ForegroundService.class));
                    if (android.os.Build.VERSION.SDK_INT >= 21) {
                        moveTaskToBack(true);
                        finishAndRemoveTask();
                        System.exit(0);
                        //     android.os.Process.killProcess(android.os.Process.myPid());
                    } else {
                        moveTaskToBack(true);
                        finishAffinity();
                        finish();
                        System.exit(0);
                        //    android.os.Process.killProcess(android.os.Process.myPid());

                    }

                }
            });
            builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    //  otherSetting = false;

                }
            });

            builder.show();
        }


//        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
//        drawer.closeDrawer(GravityCompat.START);
            return true;
        }

}
