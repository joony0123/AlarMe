package com.alarme;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.os.AsyncTaskCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.google.api.services.calendar.model.Events;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

//import android.text.TextUtils;
//import android.text.method.ScrollingMovementMethod;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Button;
//import android.widget.LinearLayout;
//import android.widget.TextView;

public class GoogleCalendarCall {
    //implements EasyPermissions.PermissionCallbacks
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

    private Context mainContext;
    private List<String> eventAndTime;
    private com.google.api.services.calendar.Calendar mService = null;
    MainActivity mAct = new MainActivity();

    private List<String> twoHourActList = new ArrayList<String>();

    private static Context context;


    public GoogleCalendarCall(Context cont, String accntName){
        mainContext = cont;
        initializeCredential(accntName);
        mProgress = new ProgressDialog(mainContext);

    }

    public List<String> getEventAndTime(){
        return eventAndTime;
    }


    public void runCreateEvent(){
    }


    public void initializeCredential(String accountEmail){

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                mainContext, Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        mCredential.setSelectedAccountName(accountEmail);
    }





    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    public void getResultsFromApi() {
//        if (! isGooglePlayServicesAvailable()) {
//            acquireGooglePlayServices();
//        } else if (mCredential.getSelectedAccountName() == null) {
//            chooseAccount();
//        } else if (! isDeviceOnline()) {
////           mOutputText.setText("No network connection available.");
//        } else {
            MakeRequestTask req = new MakeRequestTask(mCredential);
            AsyncTaskCompat.executeParallel(req);
//        }
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
                apiAvailability.isGooglePlayServicesAvailable(mainContext);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }



    /**
     * An asynchronous task that handles the Google Calendar API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
       // private com.google.api.services.calendar.Calendar mService = null;

        private Exception mLastError = null;

        MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("알라미")
                    .build();
        }

        /**
         * Background task to call Google Calendar API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of the next 10 events from the primary calendar.
         * @return List of Strings describing returned events.
         * @throws IOException
         */
        private List<String> getDataFromApi() throws IOException {
            // List the next 10 events from the primary calendar.
            DateTime now = new DateTime(System.currentTimeMillis());
            Date nowToday = new Date(System.currentTimeMillis());
            Date now2 = new Date();
            Log.d("TIME", now2.toString()); // Fri Apr 14 11:45:53 GMT-04:00 2017
            String show = DateFormat.getTimeInstance().format(nowToday); // 오후 4:22:40
            String show2 = DateFormat.getDateInstance().format(nowToday); // 2017. 4. 7.
            String show3 = DateFormat.getDateTimeInstance().format(nowToday); // 2017. 4. 7. 오후 4:22:40
          //  String show4 = DateFormat.getDateInstance().format(now); 이건 안됌 에러
            Log.d("@@@@@LOOK HERE TIME@@", show);
            Log.d("@@@@@LOOK HERE DATE@@", show2);
            Log.d("@@@@@LOOK HERE DATETIME", show3);
            List<String> eventStrings = new ArrayList<String>();
            Events events = mService.events().list("primary")
                    .setMaxResults(10)
                    .setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();


            List<Event> items = events.getItems();
           // List<Map<String, String>> pairList = new ArrayList<Map<String, String>>();
            String nowDay = now.toString().substring(0, now.toString().indexOf("T"));


            for (Event event : items) {
                DateTime start = event.getStart().getDateTime();
                if (start == null) {
                    // All-day events don't have start times, so just use
                    // the start date.
                    start = event.getStart().getDate();
                }

                eventStrings.add(
                            String.format("%s (%s)", event.getSummary(), start));
            }
            List<String> realStrings = new ArrayList<String>();
            for (String a:eventStrings
                 ) {
          //      Log.d("@@@@", a);
                String day;
                String korTimeSpeech = null;
                String newSpeech = null;
                    day = a.substring(a.indexOf("("), a.indexOf(")"));
                    day = day.substring(1);

                if(day.length() > 16) {
                    int hour = Integer.parseInt(day.substring(11, 13));
                    if(hour < 12) {
                        korTimeSpeech = "오전 ";
                    } else{
                        korTimeSpeech = "오후 ";
                        hour = hour - 12;
                    }
                    korTimeSpeech = korTimeSpeech + hour + "시 " +  day.substring(14, 16) + "분에 ";
                    newSpeech = a.substring(0, a.indexOf("("));
                    newSpeech = korTimeSpeech + newSpeech;
                    //Make it in day format
                    day = day.substring(0, day.indexOf("T"));
                }else {
                   // korTimeSpeech = day.substring(11, 13) + "시 " +  day.substring(14, 16) + "분에 ";
                    newSpeech = a.substring(0, a.indexOf("("));
                    newSpeech = newSpeech;
                }

                if (day.equals(nowDay)) {
                    realStrings.add(newSpeech);
                }
            }

            return realStrings;
        }


        @Override
        protected void onPreExecute() {
            if(mainContext instanceof MainActivity || mainContext instanceof  SettingsActivity) {
                mProgress.setMessage("Google 캘린더 권한 확인 중...");
                mProgress.show();
            }
        }

        @Override
        protected void onPostExecute(List<String> output) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mainContext);

            if(mainContext instanceof MainActivity || mainContext instanceof  SettingsActivity){
                mProgress.hide();
                if(mainContext instanceof MainActivity)
                    Toast.makeText(((MainActivity)mainContext), settings.getString("email", "") + " 권한 확인 완료", Toast.LENGTH_SHORT).show();
                if(mainContext instanceof SettingsActivity)
                   Toast.makeText(((SettingsActivity)mainContext), settings.getString("email", "") + " 권한 확인 완료", Toast.LENGTH_SHORT).show();
            }

            eventAndTime =  new ArrayList<String>();
            eventAndTime = output;

            if(mainContext instanceof MainActivity)
                ((MainActivity) mainContext).checkAndStart();

        }

        @Override
        protected void onCancelled() {
            if(mProgress != null)
                mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    if(mainContext instanceof MainActivity)
                    ((MainActivity)mainContext).showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                    if(mainContext instanceof  SettingsActivity)
                        ((SettingsActivity)mainContext).showGooglePlayServicesAvailabilityErrorDialog(
                                ((GooglePlayServicesAvailabilityIOException) mLastError)
                                        .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    if(mainContext instanceof MainActivity)
                    ((MainActivity)mainContext).startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            ((MainActivity)mainContext).REQUEST_AUTHORIZATION);
                    if(mainContext instanceof SettingsActivity)
                        ((SettingsActivity)mainContext).startActivityForResult(
                                ((UserRecoverableAuthIOException) mLastError).getIntent(),
                                ((SettingsActivity)mainContext).REQUEST_AUTHORIZATION);

                } else {
               //     mOutputText.setText("The following error occurred:\n"
                  //          + mLastError.getMessage());
                }
            } else {
             //   mOutputText.setText("Request cancelled.");
            }
        }

    } // end of private class Maker

}