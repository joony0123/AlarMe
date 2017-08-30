package com.alarme;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;

import java.util.Arrays;
import java.util.List;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {

    private double lng;
    private double lat;
    private CharSequence locationName;
    private SharedPreferences settings;
    private String userName;
    GoogleAccountCredential mCredential;
    //    private TextView mOutputText;
//    private Button mCallApiButton;
    ProgressDialog mProgress;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String BUTTON_TEXT = "Call Google Calendar API";
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { CalendarScopes.CALENDAR, CalendarScopes.CALENDAR_READONLY };

    private List<String> eventAndTime;
    private com.google.api.services.calendar.Calendar mService = null;

    private boolean email = false;
    private boolean loc = false;

    private GoogleCalendarCall myCal;
    private boolean askedAuth = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();



        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        if(!settings.getString("email", "").equals(""))
            mCredential.setSelectedAccountName(settings.getString("email", ""));

        myCal = new GoogleCalendarCall(this, settings.getString("email", ""));

        // Checks if the user needs to add info after installing
        userNameDecider();

    }

    public void userNameDecider(){

        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean dialogShown = settings.getBoolean("userDialogShown", false);

        if (!dialogShown) {
            // AlertDialog code here

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("새로운 사용자님!");
            builder.setMessage("성함이 어떻게 되시나요?");


// Set up the input
            final EditText input = new EditText(this);
            input.setSingleLine();
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
            FrameLayout container = new FrameLayout(this);
            FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
            params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
            input.setLayoutParams(params);
            container.addView(input);
            builder.setCancelable(false);
            builder.setView(container);

            //| InputType.TYPE_TEXT_VARIATION_PASSWORD --> 비번처럼 보이게 함
// Set up the buttons
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    userName = input.getText().toString();


                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("userName", userName);
                    editor.putBoolean("userDialogShown", true);
                    editor.apply();


                    checkAndStart();
                }
            });


            builder.show();


        } else{
            checkAndStart();
        }



    }

    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    public void checkAndStart() {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (!isDeviceOnline()) {
            Toast.makeText(getApplicationContext(), "인터넷이 연결되어 있지 않습니다! 원활한 결과를 위해 꼭 연결해 주세요", Toast.LENGTH_LONG).show();
        } else {
            boolean dialogShown = settings.getBoolean("locDialogShown", false);

            if(!dialogShown) {

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(settings.getString("userName", "") + "님");
                builder.setMessage("마지막으로 위치가 꺼져 있을 시 사용될 기본 위치를 설정해주세요. \n\n확인을 누르시면 뜨는 검색창에 도시를 입력하신 후 나타나는 결과 중 해당 하는 것을 선택해주시면 됩니다. \n\n" +
                        "** 기본 알람이 울릴시 위치기반 서비스가 꺼져있으면 설정해주신 \n위치를 기반으로 날씨를 알려드립니다. \n\n예시 : 서울특별시");
                builder.setCancelable(false);


                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        loc = true;
                        try {
                            Intent intent =
                                    new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                                            .build(MainActivity.this);


                            startActivityForResult(intent, 1);
                        } catch (GooglePlayServicesRepairableException e) {
                            // TODO: Handle the error.
                        } catch (GooglePlayServicesNotAvailableException e) {
                            // TODO: Handle the error.
                        }

                    }
                });
                builder.show();


            } else if(!askedAuth){
//                Intent i = new Intent(MainActivity.this, NavDrawer.class);
//                //i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(i);
                askedAuth= true;
                myCal.initializeCredential(settings.getString("email", ""));
                myCal.getResultsFromApi();


            } else{
              //  askedAuth = false;
                startService(new Intent(getBaseContext(), ForegroundService.class));
                finish();
            }
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    //보류 왜냐면 이건 처음에 시작할 때 써야할듯
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = settings
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("email", accountName);
                editor.apply();

                checkAndStart();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("반갑습니다 " + settings.getString("userName", "") + "님");
                builder.setMessage("이제 구글 캘린더에 연동할 이메일 계정을 선택해 주세요. \n\n **기본 알람 브리핑시 시간과 함께 해당날짜의 일정을 알려드립니다");
                builder.setCancelable(false);

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        email = true;

                        // Start a dialog from which the user can choose an account
                        startActivityForResult(
                                mCredential.newChooseAccountIntent(),
                                REQUEST_ACCOUNT_PICKER);

                    }
                });
                builder.show();
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "이 앱은 귀하의 계정을 접속해야 합니다",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */
    @Override
    public void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
//                    mOutputText.setText(
//                            "This app requires Google Play Services. Please install " +
//                                    "Google Play Services on your device and relaunch this app.");
                } else {
                    checkAndStart();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    email = false;
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {

                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.putString("email", accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);

                        myCal.initializeCredential(accountName);
                        myCal.getResultsFromApi();


                     //   checkAndStart();
                    }


                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    checkAndStart();
                }

                if (resultCode == RESULT_CANCELED){
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("** 주의 **");
                    builder.setMessage(settings.getString("userName", "") + "님, 구글 캘린더 권한을 허용해주시지 않으시면 브리핑시 일정을 들으실 수 없으십니다! " +
                            "\n설정에서 다시 이메일을 선택하시어 권한을 허용하시는 것을 추천드립니다.");
                    builder.setCancelable(false);

                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            checkAndStart();

                        }
                    });
                    builder.show();
                }
                break;
            case 1:
                if (resultCode == RESULT_OK) {
                    loc = false;
                    Place place = PlaceAutocomplete.getPlace(MainActivity.this, data);

                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

                    SharedPreferences.Editor editor = settings.edit();
                    editor.putFloat("lat", Float.parseFloat(String.valueOf(place.getLatLng().latitude)));
                    editor.putFloat("lng", Float.parseFloat(String.valueOf(place.getLatLng().longitude)));
                    editor.putString("defaultLoc", place.getName().toString());
                    editor.putBoolean("locDialogShown", true);
                    editor.apply();

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(settings.getString("userName", "") + "님");
                    builder.setMessage("알라미를 사용하실 모든 기본 설정이 \n완료되었습니다! 설정은 앱에서 언제든 바꾸실 수 있으십니다." +
                                        "\n\n알라미를 사용해주셔서 감사합니다.");
                    builder.setCancelable(false);

                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
//                            Intent i = new Intent(MainActivity.this, NavDrawer.class);
//                            //i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                            startActivity(i);
//                            //SplashScreen.mActivity.finish();
                            startService(new Intent(getBaseContext(), ForegroundService.class));
                            finish();

                        }
                    });
                    builder.show();


                    //  Log.i(TAG, "Place: " + place.getName());
                } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                    Status status = PlaceAutocomplete.getStatus(MainActivity.this, data);
                    // TODO: Handle the error.
                    //         Log.i(TAG, status.getStatusMessage());

                } else if (resultCode == RESULT_CANCELED) {
                    // The user canceled the operation.
                }
                break;
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
//    @Override
//    public void onPermissionsGranted(int requestCode, List<String> list) {
//        // Do nothing.
//    }
//
//    /**
//     * Callback for when a permission is denied using the EasyPermissions
//     * library.
//     * @param requestCode The request code associated with the requested
//     *         permission
//     * @param list The requested permission list. Never null.
//     */
//    @Override
//    public void onPermissionsDenied(int requestCode, List<String> list) {
//        // Do nothing.
//    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
         dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                // dialog dismiss without button press
                moveTaskToBack(true);
                finishAffinity();
                finish();
                System.exit(0);

            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                moveTaskToBack(true);
                finishAffinity();
                finish();
                System.exit(0);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(email) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("반갑습니다 " + settings.getString("userName", "") + "님");
            builder.setMessage("이제 구글 캘린더에 연동할 이메일 계정을 선택해 주세요. \n\n **기본 알람 브리핑시 시간과 함께 해당날짜의 일정을 알려드립니다");
            builder.setCancelable(false);



            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    // Start a dialog from which the user can choose an account
                    startActivityForResult(
                            mCredential.newChooseAccountIntent(),
                            REQUEST_ACCOUNT_PICKER);

                }
            });
            builder.show();
        }
        if(loc){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(settings.getString("userName", "") + "님");
            builder.setMessage("마지막으로 위치가 꺼져 있을 시 사용될 기본 위치를 설정해주세요. \n\n확인을 누르시면 뜨는 검색창에 도시를 입력하신 후 나타나는 결과 중 해당 하는 것을 선택해주시면 됩니다. \n\n" +
                    "** 기본 알람이 울릴시 위치기반 서비스가 꺼져있으면 설정해주신 \n위치를 기반으로 날씨를 알려드립니다. \n\n예시 : 서울특별시");
            builder.setCancelable(false);


            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        Intent intent =
                                new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                                        .build(MainActivity.this);


                        startActivityForResult(intent, 1);
                    } catch (GooglePlayServicesRepairableException e) {
                        // TODO: Handle the error.
                    } catch (GooglePlayServicesNotAvailableException e) {
                        // TODO: Handle the error.
                    }

                }
            });
            builder.show();
        }

    }

    @Override
    public void onBackPressed(){
        moveTaskToBack(true);

    }
}
