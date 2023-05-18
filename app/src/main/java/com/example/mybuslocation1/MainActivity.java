package com.example.mybuslocation1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {
    private BackgroundThread thread;
    String BusID = "";
    TextView textView;
    Spinner spinner;



    GoogleMap map;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("TAG", "onCreate 실행");
        textView=findViewById(R.id.textView);
        spinner=findViewById(R.id.spinner);

        LatLngBounds bounds = new LatLngBounds(
                new LatLng(34.911593, 126.718137), // 광주광역시 좌측 상단 좌표
                new LatLng(35.232851, 126.636366)  // 광주광역시 우측 하단 좌표
        );

        SupportMapFragment mapFragment =  (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap googleMap) {
                Log.d("TAG", "onMapReady 실행");

                googleMap.setLatLngBoundsForCameraTarget(bounds);

                // 최대 확대
                googleMap.setMinZoomPreference(10.5f);

                // 최초 위치
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(35.128724, 126.866018), 12.0f);
                googleMap.moveCamera(cameraUpdate);

                map = googleMap;
            }
        });


        try {
            InputStream is = getAssets().open("busList.txt");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);

            // JSON 파싱
            JSONObject jsonObject = new JSONObject(json);
            JSONObject response = jsonObject.getJSONObject("response");
            JSONObject body = response.getJSONObject("body");
            JSONObject items = body.getJSONObject("items");
            JSONArray itemList = items.getJSONArray("item");

            // routeno값을 저장할 ArrayList 생성
            ArrayList<String> routeNos = new ArrayList<>();

            // 스피너에 뿌릴 데이터 생성
            for (int i = 0; i < itemList.length(); i++) {
                JSONObject item = itemList.getJSONObject(i);
                String routeNo = item.getString("routeno");
                routeNos.add(routeNo);
            }

            // 스피너 생성
            Spinner spinner = findViewById(R.id.spinner);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, routeNos);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            spinner.setSelection(0, false);

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    // 선택된 아이템에 해당하는 routeid 값을 BusID 변수에 저장
                    String selectedRouteNo = parent.getItemAtPosition(position).toString();
                    for (int i = 0; i < itemList.length(); i++) {
                        try{
                            JSONObject item = itemList.getJSONObject(i);
                            if (selectedRouteNo.equals(item.getString("routeno"))) {
                                if (thread != null) {
                                    try {
                                        thread.stopThread();
                                        thread.interrupt();
                                        thread = null;
                                        Log.d("TAG", "새 스레드 감지, 기존 스레드 종료");} catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                }
                                String routeid = item.getString("routeid");
                                BusID = routeid;
                                textView.setText(BusID);
                                Log.d("TAG", "BusID : "+BusID);
                                thread = new BackgroundThread();
                                thread.start();
                                break;
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });



        } catch (Exception e) {
            e.printStackTrace();
        }



    }




    @Override
    protected void onDestroy() {
        super.onDestroy();

        thread.stopThread();
    }


    class BackgroundThread extends Thread {
        private volatile boolean running = true;
        public void run() {
            while (running) {
                try {
                    generateURL(BusID);
                    while (running) {
                        busInfo(BusID);
                        //Thread.sleep(15000);
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.d("TAG", "BackgroundThread 실행");
                //코드
            }
        }
        public void stopThread() {
            running = false;
        }


    }


    public String generateURL(String BusID) {
        try {
            StringBuilder urlBuilder = new StringBuilder("http://apis.data.go.kr/1613000/BusLcInfoInqireService/getRouteAcctoBusLcList");
            urlBuilder.append("?" + URLEncoder.encode("serviceKey", "UTF-8") + "=cAXk80%2BPqgzUG0x24Z9Wt4iUdB03NpSnjLnji9YiSonMZNhbthkocv9s1YxTuWSwLzbOQKdVlQha9%2FYIAfzR7w%3D%3D");
            urlBuilder.append("&" + URLEncoder.encode("pageNo", "UTF-8") + "=" + URLEncoder.encode("1", "UTF-8"));
            urlBuilder.append("&" + URLEncoder.encode("numOfRows", "UTF-8") + "=" + URLEncoder.encode("50", "UTF-8"));
            urlBuilder.append("&" + URLEncoder.encode("_type", "UTF-8") + "=" + URLEncoder.encode("json", "UTF-8"));
            urlBuilder.append("&" + URLEncoder.encode("cityCode", "UTF-8") + "=" + URLEncoder.encode("24", "UTF-8"));
            urlBuilder.append("&" + URLEncoder.encode("routeId", "UTF-8") + "=" + URLEncoder.encode(BusID, "UTF-8")); //여기 버그있음!!!! BusID에 값이 지정되지 않음. static 변수의 특성으로 보임.
            Log.d("TAG", "urlBuilder : "+urlBuilder);

            return urlBuilder.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }


    }



    public void busInfo(String BusID) {
        try {
            StringBuilder urlBuilder = new StringBuilder("https://apis.data.go.kr/1613000/BusLcInfoInqireService/getRouteAcctoBusLcList");
            urlBuilder.append("?" + URLEncoder.encode("serviceKey", "UTF-8") + "********");     // * 대신 api 키 입력
            urlBuilder.append("&" + URLEncoder.encode("pageNo", "UTF-8") + "=" + URLEncoder.encode("1", "UTF-8"));
            urlBuilder.append("&" + URLEncoder.encode("numOfRows", "UTF-8") + "=" + URLEncoder.encode("50", "UTF-8"));
            urlBuilder.append("&" + URLEncoder.encode("_type", "UTF-8") + "=" + URLEncoder.encode("json", "UTF-8"));
            urlBuilder.append("&" + URLEncoder.encode("cityCode", "UTF-8") + "=" + URLEncoder.encode("24", "UTF-8"));
            urlBuilder.append("&" + URLEncoder.encode("routeId", "UTF-8") + "=" + URLEncoder.encode(BusID, "UTF-8"));


            URL url = new URL(urlBuilder.toString());

            int i = 0;
            while (true) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        map.clear();
                    }
                });

                gpsParser(url);
                Log.d("TAG", "20초마다 갱신.\n"+ (i + 1) + "번 불러옴");
                Thread.sleep(20000);
                i++;

            }

            //gpsParser(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void gpsParser(URL url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-type", "application/json");
            System.out.println("Response code: " + conn.getResponseCode());
            BufferedReader rd;
            if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
                rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            } else {
                rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            }
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            rd.close();
            conn.disconnect();
            //System.out.println(sb.toString());
            parsejson(sb);
        } catch (Exception e) {
            e.printStackTrace();

        }
    }



    public void parsejson(StringBuilder jsonString) {
        StringBuilder sb = new StringBuilder(jsonString);

        JsonElement jsonElement = JsonParser.parseString(sb.toString());
        JsonObject jsonObj = jsonElement.getAsJsonObject();

        JsonObject responseObj = jsonObj.getAsJsonObject("response");

        JsonObject bodyObj = responseObj.getAsJsonObject("body");

        JsonObject itemsObj = bodyObj.getAsJsonObject("items");

        JsonElement itemArray = itemsObj.get("item");



        for (JsonElement element: itemArray.getAsJsonArray()) {
            JsonObject itemObj = element.getAsJsonObject();
            double gpslati = itemObj.get("gpslati").getAsDouble();
            double gpslong = itemObj.get("gpslong").getAsDouble();
            String nodeid = itemObj.get("nodeid").getAsString();
            String nodenm = itemObj.get("nodenm").getAsString();
            int nodeord = itemObj.get("nodeord").getAsInt();
            String routenm = itemObj.get("routenm").getAsString();
            String routetp = itemObj.get("routetp").getAsString();
            String vehicleno = itemObj.get("vehicleno").getAsString();

            System.out.println();
            System.out.println("gpslati: " + gpslati);
            System.out.println("gpslong: " + gpslong);
            System.out.println("nodeid: " + nodeid);
            System.out.println("nodenm: " + nodenm);
            System.out.println("nodeord: " + nodeord);
            System.out.println("routenm: " + routenm);
            System.out.println("routetp: " + routetp);
            System.out.println("vehicleno: " + vehicleno);

            MarkerOptions markerOptions = new MarkerOptions();
            LatLng latLng = new LatLng(gpslati, gpslong);
            markerOptions.position(latLng);
            markerOptions.title(routenm);
            markerOptions.snippet(nodenm + ", " + vehicleno);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    map.addMarker(markerOptions);
                }
            });

        }

    }
}