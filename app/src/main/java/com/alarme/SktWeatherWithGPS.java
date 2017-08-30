package com.alarme;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.os.AsyncTaskCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;

import static android.content.Context.LOCATION_SERVICE;


/**
 * Created by Joon.Y.K on 2017-03-27.
 */

public class SktWeatherWithGPS {
    LocationManager locationManager;
    //현재 GPS 사용유무
    boolean isGPSEnabled = false;
    //네트워크 사용유무
    boolean isNetworkEnabled = false;
    //GPS 상태값
    boolean isGetLocation = false;
    Location location;

    double lat; // 위도
    double lng; // 경도

    //최소 GPS 정보 업데이트 거리 1000미터
    private static final long MIN_DISTANCE_CHANGE_FORUPDATES = 1000;

    //최소 업데이트 시간 1분
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1;
    Context mContext;

    private String response;
    private String dustGrade;

    int corePoolSize = 60;
    int maximumPoolSize = 80;
    int keepAliveTime = 10;
    private SharedPreferences settings;
    String sktKorWeatherURl;
    String skDustUrl;
    private String rainTimeAndResp;
    private boolean raining = false;
    private String cityName;
    private boolean coldYes = false;
    private String weatherCond;
    private String weatherDesc;
    private boolean isKorea;


    public boolean getColdYes(){
        return coldYes;
    }


    public SktWeatherWithGPS(Context mContext) {
        this.mContext = mContext;
    }

    public String getResponse(){


//        if(isKorea && settings.getBoolean("dustInfo", true)){
//            response += " 미세먼지 등급은 " + dustGrade;
//        }
//
//
//        if(raining){
//            response += rainTimeAndResp;
//        }


        return response;
    }

    public Location getLocation(){
        try {
            locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);

            //GPS 정보 가져오기
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            //current network state
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            //For some reason, it detects my network is disabled, even if wifi is turned on
            if(!isGPSEnabled && !isNetworkEnabled){

            } else {
                this.isGetLocation = true;
                //get location from network
                if(isNetworkEnabled){
                    try{
                        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FORUPDATES, locationListener);
                    // Security Exception Required
                } catch (SecurityException e)
                {
                    e.printStackTrace();
                }


                    if(locationManager != null){
                        try {
                       location =  locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        // Security Exception Required
                    } catch (SecurityException e)
                    {
                        e.printStackTrace();
                    }

                        if(location != null){
                            //save lat and long
                            lat = location.getLatitude();
                            lng = location.getLongitude();
                        }
                    }
                }
                if(isGPSEnabled){
                    if(location == null){

                        try {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FORUPDATES, locationListener);
                        // Security Exception Required
                    } catch (SecurityException e)
                    {
                        e.printStackTrace();
                    }

                        if(locationManager != null){
                            try{
                           location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                // Security Exception Required
                            } catch (SecurityException e)
                            {
                                e.printStackTrace();
                            }

                                if(location != null){
                                lat = location.getLatitude();
                                lng = location.getLongitude();
                            }
                        }
                    }
                }
            }

        } catch (Exception e){
            e.printStackTrace();
        }
        return location;
    }

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location){
            //Toast("onLocationChanged");
            getWeatherData(location.getLatitude(), location.getLongitude());
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras){
            //Toast("onStatusChanged");
        }

        @Override
        public void onProviderEnabled(String provider){
            //Toast("onProviderEnabled");
        }

        @Override
        public void onProviderDisabled(String provider){
            //Toast("onProviderDisabled");
        }
    };

    public void getWeatherData(final double latitude, final double longitude){
        settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        lat = latitude;
        lng = longitude;

        Random rand = new Random();
        int choose = rand.nextInt(2);
        String openKey = "3376cfd36d634389d9fac1bedde3308c";
        String key1 = "38eb8fe8-0479-3ff0-a9bf-e48cc5812e0b";
        String key2 = "ed80f5cd-e65e-35ea-bca1-fd3e83263397";
        if(choose == 1) {
            openKey = "cdbbf78206bbaec827c2142635a59035";
            key1 = "ed80f5cd-e65e-35ea-bca1-fd3e83263397";
            key2 = "38eb8fe8-0479-3ff0-a9bf-e48cc5812e0b";
        }



        String url = "http://api.openweathermap.org/data/2.5/weather?lat=" + latitude + "&lon=" + longitude + "&units=metric&lang=kr&appid=" + openKey;
        //Get Celsius Temp Weather
        // String url = "http://api.openweathermap.org/data/2.5/weather?lat=" + lat + "&lon=" + lng + "&units=metric&appid=3376cfd36d634389d9fac1bedde3308c";
        skDustUrl = "http://apis.skplanetx.com/weather/dust?version=1&lat=" + latitude + "&lon=" + longitude + "&appKey=" + key1;
        sktKorWeatherURl = "http://apis.skplanetx.com/weather/current/hourly?version=1&lat=" + latitude + "&lon=" + longitude + "&appKey=" + key2;

        isKorea = false;
        //Always reset raining boolean
        raining = false;
        ReceiveCityTask receiveCityTask = new ReceiveCityTask();
        AsyncTaskCompat.executeParallel(receiveCityTask, url);
    }

    private class ReceiveCityTask extends AsyncTask<String, Void, JSONObject> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected JSONObject doInBackground(String... datas) {
            try {

                // for debug worker thread
                if (android.os.Debug.isDebuggerConnected())
                    android.os.Debug.waitForDebugger();

                // create the file with the given file path
                File file = new File(datas[0]);
                HttpURLConnection conn = (HttpURLConnection) new URL(datas[0]).openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.connect();

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    InputStreamReader reader = new InputStreamReader(is);
                    BufferedReader in = new BufferedReader(reader);

                    String readed;
                    while ((readed = in.readLine()) != null) {
                        JSONObject jObject = new JSONObject(readed);
                        //jObject.getJSONArray("weather").getJSONObject(0).getString("icon");

                        return jObject;
                    }
                } else {
                    return null;
                }
                return null;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            if (result != null) {
                String name = "";
                String weatherTemp = "";

                try {
                    weatherTemp = result.getJSONArray("weather").getJSONObject(0).getString("main");
                    weatherDesc = result.getJSONArray("weather").getJSONObject(0).getString("description");
                    name = result.getString("name");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                cityName = name;
                weatherCond = weatherTemp;

                Random rand = new Random();
                int choose = rand.nextInt(2);
                String key = "ed80f5cd-e65e-35ea-bca1-fd3e83263397";
                if(choose == 0)
                    key = "38eb8fe8-0479-3ff0-a9bf-e48cc5812e0b";

                //Get Global First. If KO then do another
                ReceiveGlobalWeatherTask receiveGlobalWeatherTask = new ReceiveGlobalWeatherTask();
                String globalWeatherURL = "http://apis.skplanetx.com/gweather/forecast/short?version=1&timezone=local&lat=" + lat + "&lon=" + lng +"&appKey=" + key;
                AsyncTaskCompat.executeParallel(receiveGlobalWeatherTask, globalWeatherURL);


            }
        }
    }

        private class ReceiveGlobalWeatherTask extends AsyncTask<String, Void, JSONObject>{
        @Override
        protected void onPreExecute(){
            super.onPreExecute();
        }

        @Override
        protected JSONObject doInBackground(String... datas){
            try{

                // for debug worker thread
                if(android.os.Debug.isDebuggerConnected())
                    android.os.Debug.waitForDebugger();

                // create the file with the given file path
                File file = new File(datas[0]);
                HttpURLConnection conn = (HttpURLConnection) new URL(datas[0]).openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.connect();

                if(conn.getResponseCode() == HttpURLConnection.HTTP_OK){
                    InputStream is = conn.getInputStream();
                    InputStreamReader reader = new InputStreamReader(is);
                    BufferedReader in = new BufferedReader(reader);

                    String readed;
                    while((readed = in.readLine()) != null){
                        JSONObject jObject = new JSONObject(readed);
                        //jObject.getJSONArray("weather").getJSONObject(0).getString("icon");

                        return jObject;
                    }
                } else{
                    return null;
                }
                return null;
            } catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject result) {

            if (result != null) {
                String iconName = "";
                String nowTemp = "";
                String maxTemp = "";
                String minTemp = "";

                String humidity = "";
                String speed = "";
                String main = "";
                String description = "";

                String countyName = "";
                int code;

                try {


                        ArrayList<String> tempList = new ArrayList<>();
                        ArrayList<String> percpList = new ArrayList<>();
                        ArrayList<String> percpTypeList = new ArrayList<>();
                        ArrayList<String> percpStartList = new ArrayList<>();

                        for (int i = 0; i < 8; i++) {
                            tempList.add(result.getJSONObject("gweather").getJSONArray("forecastDays").getJSONObject(0).getJSONArray("forecast").getJSONObject(i).getJSONObject("temperature").getString("tc"));
                            percpList.add(result.getJSONObject("gweather").getJSONArray("forecastDays").getJSONObject(0).getJSONArray("forecast").getJSONObject(i).getJSONObject("precipitation").getString("value"));
                            percpTypeList.add(result.getJSONObject("gweather").getJSONArray("forecastDays").getJSONObject(0).getJSONArray("forecast").getJSONObject(i).getJSONObject("precipitation").getString("type"));
                            percpStartList.add(result.getJSONObject("gweather").getJSONArray("forecastDays").getJSONObject(0).getJSONArray("forecast").getJSONObject(i).getJSONObject("time").getString("from"));
                        }

                        //code = Integer.parseInt(result.getJSONObject("gweather").getJSONArray("forecastDays").getJSONObject(0).getJSONArray("forecast").getJSONObject(0).getJSONObject("sky").getString("code"));

                        //Deal with rain for today
                        Calendar cal = Calendar.getInstance();
                        Date now = cal.getTime();
                        for (int i = 0; i < 8; i++) {
                            double percpGrade = Double.parseDouble(percpList.get(i));
                            if(!percpTypeList.get(i).equals("3") && percpGrade > 0){
                                SimpleDateFormat convert = new SimpleDateFormat("HH:mm:ss");
                                SimpleDateFormat dateAndTime = new SimpleDateFormat("dd'T'HH:mm");
                                String GMT = percpStartList.get(i).substring(16);
                                String[] GMTPartial = GMT.split(":");
                                if(GMTPartial[0].length() < 3){
                                    GMTPartial[0] = GMTPartial[0].substring(0, 1) + "0" + GMTPartial[0].substring(1);
                                }
                                GMT = GMTPartial[0] + ":" + GMTPartial[1];
                                dateAndTime.setTimeZone(TimeZone.getTimeZone("GMT" + GMT));

                                //cal.add(Calendar.MINUTE, 90);

                                //현재의 Dest 타임 구하기
                                String destDate = dateAndTime.format(now).substring(0, 2);
                                String destTime = dateAndTime.format(now).substring(5);

                                if(percpStartList.get(i).substring(8, 10).equals(destDate)){
                                    String rainTime2 = percpStartList.get(i).substring(11, 16);

                                    rainTime2 = rainTime2.substring(0, 2);

                                    if(Integer.parseInt(rainTime2) > 12){
                                        rainTime2 = "오후 " + String.valueOf(Integer.parseInt(rainTime2) - 12) + "시";
                                    } else {
                                        rainTime2 = "오전 " + Integer.parseInt(rainTime2) + "시";
                                    }


                                    raining = true;
                                    rainTimeAndResp = "약 " + rainTime2 + "경 부터 ";
                                    if(percpGrade <= 0.5)
                                        rainTimeAndResp += "약하게 ";
                                    if(percpGrade > 5 )
                                        rainTimeAndResp += "세차게 ";

                                    rainTimeAndResp += "비가 올 가능성이 있으니 그 때 우산 챙기세요. ";

                                    break;

                                }

                            }
                        }
                        // override result if it is raining currently
                        if(weatherCond.equals("Rain") || weatherCond.equals("Thunderstorm")) {
                            raining = true;
                            rainTimeAndResp = " 현재 비가 오고 있으니 외출시 우산 꼭 챙기세요. ";
                        }

                        //After rain is dealt, decide whether to transfer to korean weather calc
                        if (result.getJSONObject("gweather").getJSONArray("forecastDays").getJSONObject(0).getJSONObject("location").getString("country").equals("KR")) {
                            isKorea = true;
                            //Execute DustTask. Weather task is executed in postExecute of this execution. So in sequence.
                            ReceiveDustTask receiveDustTask = new ReceiveDustTask();

                            AsyncTaskCompat.executeParallel(receiveDustTask, skDustUrl);

                        } else {
                        //Deal with max min temp for today
                        minTemp = tempList.get(0);
                        maxTemp = tempList.get(0);

                            cal = Calendar.getInstance();
                            now = cal.getTime();

                            SimpleDateFormat dateAndTime = new SimpleDateFormat("dd'T'HH:mm");
                            String GMT = percpStartList.get(1).substring(16);
                            String[] GMTPartial = GMT.split(":");
                            if(GMTPartial[0].length() < 3){
                                GMTPartial[0] = GMTPartial[0].substring(0, 1) + "0" + GMTPartial[0].substring(1);
                            }
                            GMT = GMTPartial[0] + ":" + GMTPartial[1];
                            dateAndTime.setTimeZone(TimeZone.getTimeZone("GMT" + GMT));

                            //cal.add(Calendar.MINUTE, 90);

                            //현재의 Dest 타임 구하기
                            String destDate = dateAndTime.format(now).substring(0, 2);
                            String destTime = dateAndTime.format(now).substring(5);

                        for (int i = 0; i < tempList.size(); i++) {

                            if(percpStartList.get(i).substring(8, 10).equals(destDate)){
                                if(Float.parseFloat(minTemp) > Float.parseFloat(tempList.get(i))){
                                    minTemp = tempList.get(i);
                                }
                                if(Float.parseFloat(maxTemp) < Float.parseFloat(tempList.get(i))){
                                    maxTemp = tempList.get(i);
                                }
                            }
                        }
                        nowTemp = tempList.get(0);

                        description = weatherDesc;


                        Float nowTempF = Float.parseFloat(nowTemp);
                        nowTemp = String.format("%.1f", nowTempF);
                        if (nowTemp.endsWith("0")) {
                            nowTemp = nowTemp.substring(0, nowTemp.length() - 2);
                        }

                        Float minTempF = Float.parseFloat(minTemp);
                        minTemp = String.format("%.1f", minTempF);
                        if (minTemp.endsWith("0")) {
                            minTemp = minTemp.substring(0, minTemp.length() - 2);
                        }
                        Float maxTempF = Float.parseFloat(maxTemp);
                        maxTemp = String.format("%.1f", maxTempF);
                        if (maxTemp.endsWith("0")) {
                            maxTemp = maxTemp.substring(0, maxTemp.length() - 2);
                        }

                        String minMaxTempResponse = ", 도로 일교차가 작겠습니다. ";
                        coldYes = false;

                        if (Float.parseFloat(maxTemp) - Float.parseFloat(minTemp) >= 7) {
                            minMaxTempResponse = ", 도로 오늘은 일교차가 조금 크겠습니다. ";
                            if(Float.parseFloat(maxTemp) - Float.parseFloat(minTemp) >= 10) {
                                minMaxTempResponse = ", 도로 오늘은 일교차가 크겠습입니다. ";
                                coldYes = true;
                            }
                        }

                        //final String msg = description + " 습도" + humidity +"%, 풍속 " + speed + "m/s" + " 온도 현재:" + nowTemp + " / 최저:" + minTemp + " / 최고:" + maxTemp;


                        response = cityName + " " + countyName + "의 날씨는 " + description + "이며 온도는 약 " + nowTemp + "도입니다."
                                + "최고 " + maxTemp + ", 도 최저 " + minTemp + minMaxTempResponse;
                        if(raining){
                            response += rainTimeAndResp;
                        }



                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }



        private String transferKorWeather(int code){

            if(code == 800){
                return "맑음";
            }
            else if(200 <= code && code <= 232){
                return "천둥번개";
            }
            else if(300 <= code && code <= 321){
                return "흐리고 비";
            }
            else if(500 <= code && code <= 531){
                return "구름 많고 비";
            }
            else if(600 <= code && code <= 622){
                return "구름 많고 눈 또는 비";
            }
            else if(701 <= code && code <= 761){
                return "안개 또는 먼지바람";
            }
            else if(702 == code){
                return "화산재";
            } else if(code == 771){
                return "스콜";
            }
            else if(code == 781){
                return "토네이도";
            }
            else if(801 <= code && code <= 804){
                return "구름 많음";
            }
            else if(701 <= code && code <= 761){
                return "안개 또는 먼지바람";
            }
            else if(900 <= code && code <= 962){
                return "태풍 또는 강한 바람";
            }
            return "맑음";
        }

    } // end of class ReceiveWeather Task

    private class ReceiveWeatherTask extends AsyncTask<String, Void, JSONObject>{
        @Override
        protected void onPreExecute(){
            super.onPreExecute();
        }

        @Override
        protected JSONObject doInBackground(String... datas){
            try{

                // for debug worker thread
                if(android.os.Debug.isDebuggerConnected())
                    android.os.Debug.waitForDebugger();

                // create the file with the given file path
                File file = new File(datas[0]);
                HttpURLConnection conn = (HttpURLConnection) new URL(datas[0]).openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.connect();

                if(conn.getResponseCode() == HttpURLConnection.HTTP_OK){
                    InputStream is = conn.getInputStream();
                    InputStreamReader reader = new InputStreamReader(is);
                    BufferedReader in = new BufferedReader(reader);

                    String readed;
                    while((readed = in.readLine()) != null){
                        JSONObject jObject = new JSONObject(readed);
                        //jObject.getJSONArray("weather").getJSONObject(0).getString("icon");

                        return jObject;
                    }
                } else{
                    return null;
                }
                return null;
            } catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject result){
            if(result != null){
                String iconName = "";
                String nowTemp = "";
                String maxTemp = "";
                String minTemp = "";

                String humidity = "";
                String speed = "";
                String main = "";
                String description = "";
                String cityName = "";
                String countyName = "";

                try{
                  //  iconName =
                    nowTemp = result.getJSONObject("weather").getJSONArray("hourly").getJSONObject(0).getJSONObject("temperature").getString("tc");
//                    humidity = result.getJSONObject("main").getString("humidity");
                    minTemp = result.getJSONObject("weather").getJSONArray("hourly").getJSONObject(0).getJSONObject("temperature").getString("tmin");
                    maxTemp = result.getJSONObject("weather").getJSONArray("hourly").getJSONObject(0).getJSONObject("temperature").getString("tmax");
                    //speed = result.getJSONObject("main").getString("speed");
//                    main = result.getJSONArray("weather").getJSONObject(0).getString("main");
                    cityName = result.getJSONObject("weather").getJSONArray("hourly").getJSONObject(0).getJSONObject("grid").getString("city");
                    countyName  = result.getJSONObject("weather").getJSONArray("hourly").getJSONObject(0).getJSONObject("grid").getString("county");

                   description = result.getJSONObject("weather").getJSONArray("hourly").getJSONObject(0).getJSONObject("sky").getString("name");
                } catch (JSONException e){
                    e.printStackTrace();
                }
               // description = transferKorWeather(description);


                // city distinguisher
                if(cityName.equals("경기") ||cityName.equals("충남") ||cityName.equals("충북") ||cityName.equals("전남") ||
                        cityName.equals("전북") || cityName.equals("경기") || cityName.equals("강원")){
                    cityName = "";
                }


                Float nowTempF = Float.parseFloat(nowTemp);
                nowTemp = String.format("%.1f", nowTempF);
                if(nowTemp.endsWith("0")){
                    nowTemp = nowTemp.substring(0, nowTemp.length() - 2);
                }

                Float minTempF = Float.parseFloat(minTemp);
                minTemp = String.format("%.1f", minTempF);
                if(minTemp.endsWith("0")){
                    minTemp = minTemp.substring(0, minTemp.length() - 2);
                }
                Float maxTempF = Float.parseFloat(maxTemp);
                maxTemp = String.format("%.1f", maxTempF);
                if(maxTemp.endsWith("0")){
                    maxTemp = maxTemp.substring(0, maxTemp.length() - 2);
                }

                if(dustGrade.equals("약간나쁨") || dustGrade.equals("약간 나쁨")){
                    dustGrade = "약간 나쁨으로 마스크 착용을 추천드립니다. ";
                }else if(dustGrade.equals("나쁨")){
                    dustGrade = "나쁨으로 가급적이면 대외 활동을 피하시고 마스크 착용을 꼭 하세요. ";
                }else if(dustGrade.equals("매우나쁨") || dustGrade.equals("매우 나쁨")){
                    dustGrade = "매우 나쁨으로 마스크 착용을 필수이며 대외 활동이 있으시다면 취소하시길 권고드립니다. ";
                }else if(dustGrade.equals("좋음") || dustGrade.equals("보통")){
                    dustGrade = dustGrade + "입니다. ";
                }



                //final String msg = description + " 습도" + humidity +"%, 풍속 " + speed + "m/s" + " 온도 현재:" + nowTemp + " / 최저:" + minTemp + " / 최고:" + maxTemp;


                String minMaxTempResponse = ", 도로 일교차가 작겠습니다. ";
                coldYes = false;

                if (Float.parseFloat(maxTemp) - Float.parseFloat(minTemp) >= 7) {
                    minMaxTempResponse = ", 도로 오늘은 일교차가 조금 크겠습니다. ";
                    if(Float.parseFloat(maxTemp) - Float.parseFloat(minTemp) >= 10) {
                        minMaxTempResponse = ", 도로 오늘은 일교차가 크겠습니다. ";
                        coldYes = true;
                    }
                }

                //final String msg = description + " 습도" + humidity +"%, 풍속 " + speed + "m/s" + " 온도 현재:" + nowTemp + " / 최저:" + minTemp + " / 최고:" + maxTemp;


                response = cityName + " " + countyName + "의 날씨는 " + description + "이며 온도는 약 " + nowTemp + "도입니다."
                        + "최고 " + maxTemp + ", 도 최저 " + minTemp + minMaxTempResponse;


                if(settings.getBoolean("dustInfo", true)){
            response += " 미세먼지 등급은 " + dustGrade;
        }


        if(raining){
            response += rainTimeAndResp;
        }


            }
        }



    } // end of class ReceiveWeather Task

    private class ReceiveDustTask extends AsyncTask<String, Void, JSONObject> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected JSONObject doInBackground(String... datas) {
            try {

                // for debug worker thread
                if (android.os.Debug.isDebuggerConnected())
                    android.os.Debug.waitForDebugger();

                // create the file with the given file path
                File file = new File(datas[0]);
                HttpURLConnection conn = (HttpURLConnection) new URL(datas[0]).openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.connect();

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    InputStreamReader reader = new InputStreamReader(is);
                    BufferedReader in = new BufferedReader(reader);

                    String readed;
                    while ((readed = in.readLine()) != null) {
                        JSONObject jObject = new JSONObject(readed);
                        //jObject.getJSONArray("weather").getJSONObject(0).getString("icon");

                        return jObject;
                    }
                } else {
                    return null;
                }
                return null;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            if (result != null) {
                String cityName = "";
                String grade = "";

                try {
                    grade = result.getJSONObject("weather").getJSONArray("dust").getJSONObject(0).getJSONObject("pm10").getString("grade");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                dustGrade = grade;

                ReceiveWeatherTask receiveWeatherTask = new ReceiveWeatherTask();
                AsyncTaskCompat.executeParallel(receiveWeatherTask, sktKorWeatherURl);



            }
        }
    }

} // end of class SktWeatherWithGPS.java
