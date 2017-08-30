package com.alarme;


import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;

import java.util.Arrays;
import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    SharedPreferences settings;
    private static boolean otherSetting = false;
    private static boolean isChangingEmailOrLoc = false;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private boolean isFromSettings = false;







    @Override
    public void onBackPressed() {
       // super.onBackPressed();
        if(otherSetting){
            super.onBackPressed();
            otherSetting = false;
        }else{
            moveTaskToBack(true);
        }
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.

                preference.setSummary(stringValue);



            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());


        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
//            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);


    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
             //   || DataSyncPreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName);
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
                SettingsActivity.this,
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

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        private GoogleAccountCredential mCredential;
        static final int REQUEST_ACCOUNT_PICKER = 1000;
        private SharedPreferences settings;
        GoogleCalendarCall myCal;

        private static final String[] SCOPES = { CalendarScopes.CALENDAR, CalendarScopes.CALENDAR_READONLY };
        /**
         * Set up the {@link android.app.ActionBar}, if the API is available.
         */
        private void setupActionBar() {
            ActionBar actionBar = ((AppCompatPreferenceActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) {
                // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setupActionBar();

            settings = PreferenceManager.getDefaultSharedPreferences(getActivity());

            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);



            mCredential = GoogleAccountCredential.usingOAuth2(
                    getActivity().getApplicationContext(), Arrays.asList(SCOPES))
                    .setBackOff(new ExponentialBackOff());

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
            SwitchPreference dustPref = (SwitchPreference) findPreference("dustInfo");


            findPreference("email").setSummary(settings.getString("email", ""));
            findPreference("userName").setSummary(settings.getString("userName", ""));

            findPreference("email").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    isChangingEmailOrLoc = true;
                    // Start a dialog from which the user can choose an account
                    startActivityForResult(
                            mCredential.newChooseAccountIntent(),
                            REQUEST_ACCOUNT_PICKER);


                    return true;
                }
            });
            myCal = new GoogleCalendarCall(getActivity(), settings.getString("email", ""));
            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("email"));
            bindPreferenceSummaryToValue(findPreference("userName"));

            otherSetting = true;
        }



        @Override
        public void onActivityResult(
                int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            switch(requestCode) {
                case REQUEST_AUTHORIZATION:
                    if (resultCode == RESULT_OK) {
                        //checkAndStart();
                    }
                    break;
                case REQUEST_ACCOUNT_PICKER:
                    if (resultCode == RESULT_OK && data != null &&
                            data.getExtras() != null) {
                        String accountName =
                                data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                        if (accountName != null) {
                            SharedPreferences.Editor editor = settings.edit();
                          //  editor.putString(PREF_ACCOUNT_NAME, accountName);
                            editor.putString("email", accountName);
                            editor.apply();
                            findPreference("email").setSummary(accountName);
                            //mCredential.setSelectedAccountName(accountName);
                            isChangingEmailOrLoc = false;
                            myCal.initializeCredential(accountName);
                            myCal.getResultsFromApi();
                        }
                    }
                    break;

            }
            }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                otherSetting = false;
                this.getActivity().finish();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onHeaderClick(Header header, int position) {
        super.onHeaderClick(header, position);
        if (header.id == R.id.servTerminate) {
            AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
            builder.setTitle("서비스 종료");
            builder.setMessage("정말 종료하시겠습니까?");


// Set up the buttons

            builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    otherSetting = false;

                }
            });

            builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    Toast.makeText(getApplicationContext(), "알라미 서비스를 종료합니다", Toast.LENGTH_SHORT).show();
                    stopService(new Intent(getApplicationContext(), ForegroundService.class));
                    if(android.os.Build.VERSION.SDK_INT >= 21)
                    {
                        moveTaskToBack(true);
                        finishAndRemoveTask();
                        System.exit(0);
                        //     android.os.Process.killProcess(android.os.Process.myPid());
                    }
                    else
                    {

                        moveTaskToBack(true);
                        finishAffinity();
                        finish();
                        System.exit(0);
                        //    android.os.Process.killProcess(android.os.Process.myPid());

                    }

                }
            });

            builder.show();
        }
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment {
        SwitchPreference dustPref;
        SwitchPreference googleCalPref;
        SwitchPreference musicPref;
        Context mContext;

        /**
         * Set up the {@link android.app.ActionBar}, if the API is available.
         */
        private void setupActionBar() {
            ActionBar actionBar = ((AppCompatPreferenceActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) {
                // Show the Up button in the action bar.
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }



        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setupActionBar();

            addPreferencesFromResource(R.xml.pref_notification);
            setHasOptionsMenu(true);
            mContext = getActivity();


            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
//            settings.registerOnSharedPreferenceChangeListener(listener);
            settings.registerOnSharedPreferenceChangeListener(listener);
            dustPref = (SwitchPreference) findPreference("dustInfo");
            dustPref.setChecked(settings.getBoolean("dustInfo", true));
            googleCalPref = (SwitchPreference) findPreference("googleCalInfo");
            googleCalPref.setChecked(settings.getBoolean("googleCalInfo", true));
            musicPref = (SwitchPreference) findPreference("music");
            musicPref.setChecked(settings.getBoolean("music", true));


            findPreference("defaultLoc").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    isChangingEmailOrLoc = true;
                    try {
                        Intent intent =
                                new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                                        .build(getActivity());
                        startActivityForResult(intent, 1);
                    } catch (GooglePlayServicesRepairableException e) {
                        // TODO: Handle the error.
                        isChangingEmailOrLoc = false;
                    } catch (GooglePlayServicesNotAvailableException e) {
                        // TODO: Handle the error.
                        isChangingEmailOrLoc = false;
                    }


                    return true;
                }
            });


            otherSetting = true;

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("defaultLoc"));
        }

        SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                // Implementation
                if (key.equals("dustInfo")) {
                    if(prefs.getBoolean(key, true)) {
                        Toast.makeText(mContext, "미세먼지 정보가 추가되었습니다", Toast.LENGTH_SHORT).show();
                    } else{
                        Toast.makeText(mContext, "미세먼지 정보가 제거되었습니다", Toast.LENGTH_SHORT).show();
                    }
                }
                if (key.equals("googleCalInfo")) {
                    if(prefs.getBoolean(key, true)) {
                        Toast.makeText(mContext, "Google 캘린더 정보가 추가되었습니다\n선택하신 이메일이 캘린더와 동기화되어 있어야 브리핑이 가능합니다", Toast.LENGTH_SHORT).show();
                    } else{
                        Toast.makeText(mContext, "Google 캘린더 정보가 제거되었습니다", Toast.LENGTH_SHORT).show();
                    }
                }
                if (key.equals("music")) {
                    if(prefs.getBoolean(key, true)) {
                        Toast.makeText(mContext, "배경 음악이 추가되었습니다", Toast.LENGTH_SHORT).show();
                    } else{
                        Toast.makeText(mContext, "배경 음악이 제거되었습니다", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };


        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                otherSetting = false;
                this.getActivity().finish();
                return true;
            }

            return super.onOptionsItemSelected(item);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == 1) {
                if (resultCode == RESULT_OK) {
                    Place place = PlaceAutocomplete.getPlace(getActivity(), data);

                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());

                    SharedPreferences.Editor editor = settings.edit();
                    editor.putFloat("lat", Float.parseFloat(String.valueOf(place.getLatLng().latitude)));
                    editor.putFloat("lng", Float.parseFloat(String.valueOf(place.getLatLng().longitude)));
                    editor.putString("defaultLoc", place.getName().toString());
                    editor.apply();

                    findPreference("defaultLoc").setSummary(place.getName());



                  //  Log.i(TAG, "Place: " + place.getName());
                } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                    Status status = PlaceAutocomplete.getStatus(getActivity(), data);
                    // TODO: Handle the error.
           //         Log.i(TAG, status.getStatusMessage());

                } else if (resultCode == RESULT_CANCELED) {
                    // The user canceled the operation.
                }
            }
            isChangingEmailOrLoc = false;

        }
    }


}
