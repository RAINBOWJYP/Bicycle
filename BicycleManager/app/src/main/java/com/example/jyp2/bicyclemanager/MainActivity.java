package com.example.jyp2.bicyclemanager;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private static final String TAG = "MainActivity";
    private static final boolean D = true;

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    public String readMessage;

    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_CONNECT_DEVICE2 = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    private TextView tvfrontpsi, tvbackpsi, tvfrontbar, tvbackbar;
    private Button bt_Start,bt_End;


    //Cal and tab
    private EditText bodyedit,bikeedit;
    private TabHost tabHost;
    private EditText mOutEditText;
    private TextView tvgps, frontbar,frontpsi,backbar,backpsi;
    private Button mSendButton, button1, button2;

    private double result, result2;
    private String mConnectedDeviceName = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private StringBuffer mOutStringBuffer;
    private ServerActivity1 mServerActivity1 = null;
    private ServerActivity2 mServerActivity2 = null;


    ArrayAdapter<String> roadAdapter, weatherAdapter, tireAdapter;
    Spinner RoadSpin, WeatherSpin, TireSpin;





    //Map
    private GoogleApiClient mGoogleApiClient = null;
    private GoogleMap mGoogleMap = null;
    private Marker currentMarker = null;

    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 2002;
    private static final int UPDATE_INTERVAL_MS = 5000;  // 2초
    private static final int FASTEST_UPDATE_INTERVAL_MS = 5000; // 1초

    private AppCompatActivity mActivity;
    boolean askPermissionOnceAgain = false;
    boolean mRequestingLocationUpdates = false;
    boolean mMoveMapByUser = true;
    boolean mMoveMapByAPI = true;

    boolean walkState = false;
    private ArrayList<LatLng> points;
    private Polyline polyline;


    LocationRequest locationRequest = new LocationRequest()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(UPDATE_INTERVAL_MS)
            .setFastestInterval(FASTEST_UPDATE_INTERVAL_MS);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (D) Log.e(TAG, "++++++++++++++++++++ON CREATE++++++++++++++++++++");

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "블루투스를 지원하지 않습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mServerActivity1 = new ServerActivity1(this,mHandler);
        mServerActivity2 = new ServerActivity2(this,mHandler2);

        bt_Start = (Button) findViewById(R.id.bt_start);
        bt_End = (Button) findViewById(R.id.bt_end);
        bt_End.setEnabled(false);

        //tab
        tabHost = (TabHost) findViewById(R.id.tabhost);
        tabHost.setup();
        TabHost.TabSpec tab1 = tabHost.newTabSpec("1").setContent(R.id.tab1).setIndicator("현재 공기압");
        TabHost.TabSpec tab2 = tabHost.newTabSpec("2").setContent(R.id.tab2).setIndicator("적정 공기압");
        TabHost.TabSpec tab3 = tabHost.newTabSpec("3").setContent(R.id.tab3).setIndicator("속도계");
        tabHost.addTab(tab1);
        tabHost.addTab(tab2);
        tabHost.addTab(tab3);


        //properpsi
        final String[] data1 = getResources().getStringArray(R.array.road);
        final String[] data2 = getResources().getStringArray(R.array.weather);
        final String[] data3 = getResources().getStringArray(R.array.tire);

        roadAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, data1);
        weatherAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, data2);
        tireAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, data3);

        RoadSpin = (Spinner) findViewById(R.id.spinner1);
        WeatherSpin = (Spinner) findViewById(R.id.spinner2);
        TireSpin = (Spinner) findViewById(R.id.tirespin);

        RoadSpin.setAdapter(roadAdapter);
        WeatherSpin.setAdapter(weatherAdapter);
        TireSpin.setAdapter(tireAdapter);

        TireSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        frontbar = (TextView) findViewById(R.id.frontbar);
        frontpsi = (TextView) findViewById(R.id.frontpsi);
        backbar = (TextView) findViewById(R.id.backbar);
        backpsi = (TextView) findViewById(R.id.backpsi);

        //init();

        //Map
        mActivity = this;

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        points = new ArrayList<LatLng>();

        tvgps=(TextView)findViewById(R.id.tvgps);
    }

    public void onBtnCal(View v){
        bodyedit = (EditText) findViewById(R.id.bodyedit);
        bikeedit = (EditText) findViewById(R.id.bikeedit);

        double bodydata = 0;
        double bikedata = 0;

        try{
            bodydata = Double.parseDouble(bodyedit.getText().toString());
            bikedata = Double.parseDouble(bikeedit.getText().toString());
        }catch (Exception e){
            Toast.makeText(getApplicationContext(), "숫자만 입력해주세요", Toast.LENGTH_SHORT).show();
        }

        double frontcal = Calculation.Calculate(bodydata, bikedata);
        double backcal = frontcal*0.9;

        String frontresult = String.format("%.1f",frontcal);
        String backresult = String.format("%.1f",backcal);

        frontpsi.setText(frontresult+"psi");
        backpsi.setText(backresult+"psi");


    }

    public void onBtnStart(View v){
        walkState = true;
        bt_Start.setEnabled(false);
        bt_End.setEnabled(true);
    }

    public void onBtnEnd(View v){
        walkState = false;
        bt_Start.setEnabled(true);
        bt_End.setEnabled(false);
    }

    public void onStart() {
        if(mGoogleApiClient != null && mGoogleApiClient.isConnected() == false){

            Log.d(TAG, "onStart: mGoogleApiClient connect");
            mGoogleApiClient.connect();
        }

        super.onStart();
        if (D) Log.e(TAG, "+++++++++ON START+++++++++");


        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

    }


    public synchronized void onResume() {
        super.onResume();
        if (D) Log.e(TAG, "+ON RESUME+");

        if (mServerActivity1 != null) {
            if (mServerActivity1.getState() == ServerActivity1.STATE_NONE) {
                mServerActivity1.start();
            }
        }

        if (mServerActivity2 != null) {
            if(mServerActivity2.getState() ==ServerActivity2.STATE_NONE) {
                mServerActivity2.start();
            }
        }


        if (mGoogleApiClient.isConnected()) {
            Log.d(TAG, "onResume: call startLocationUpdates");
            if (!mRequestingLocationUpdates) startLocationUpdates();
        }

        //앱정보에서 퍼미션을 허가했는지 검사
        if (askPermissionOnceAgain) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                askPermissionOnceAgain = false;

                checkPermissions();
            }
        }

    }



    @Override
    public synchronized void onPause() {
        super.onPause();

        //Map
        //위치 업데이트 중지
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {

            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

            mGoogleApiClient.disconnect();
        }

        if (D) Log.e(TAG, "- ON PAUSE -");

    }

    @Override
    public void onStop() {

        if (D) Log.e(TAG, "-- ON STOP --");

        //Map
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

        if (mRequestingLocationUpdates) {

            Log.d(TAG, "onStop : call stopLocationUpdates");
            stopLocationUpdates();
        }

        if ( mGoogleApiClient.isConnected()) {

            Log.d(TAG, "onStop : mGoogleApiClient disconnect");
            mGoogleApiClient.disconnect();
        }

        super.onStop();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mServerActivity1 != null) mServerActivity1.stop();
        if (mServerActivity2 != null) mServerActivity2.stop();
        Log.d(TAG, "++++++++++++++++++++ON DESTROY++++++++++++++++++++");

        //Map
        if (mGoogleApiClient != null) {
            mGoogleApiClient.unregisterConnectionCallbacks(this);
            mGoogleApiClient.unregisterConnectionFailedListener(this);

            if (mGoogleApiClient.isConnected()) {
                LocationServices.FusedLocationApi
                        .removeLocationUpdates(mGoogleApiClient, this);
                mGoogleApiClient.disconnect();
            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        if (D) Log.d(TAG, "onActivityResult: " + resultCode);

        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                if (resultCode == Activity.RESULT_OK) {
                    //디바이스 Mac 주소 얻기
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    mServerActivity1.connect(device);
                }
                break;
            case REQUEST_CONNECT_DEVICE2:
                if (resultCode == Activity.RESULT_OK) {
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    mServerActivity2.connect(device);
                }
                break;
            case GPS_ENABLE_REQUEST_CODE:

                //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (ActivityCompat.checkSelfPermission(this,
                                    Manifest.permission.ACCESS_FINE_LOCATION)
                                    == PackageManager.PERMISSION_GRANTED) {

                                mGoogleMap.setMyLocationEnabled(true);
                            }
                        }
                        else  mGoogleMap.setMyLocationEnabled(true);

                        return;
                    }
                }
                else{
                    setCurrentLocation(null, "위치정보 가져올 수 없음",
                            "위치 퍼미션과 GPS 활성 요부 확인하세요");
                }

                break;

        }
    }

    public void onbtnModule1(View v) {
        Intent intent;
        intent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(intent, REQUEST_CONNECT_DEVICE);
    }

    public void onbtnModule2(View view) {
        Intent intent;
        intent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(intent, REQUEST_CONNECT_DEVICE2);
    }

    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
            startActivity(discoverableIntent);
        }
    }


    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case MESSAGE_STATE_CHANGE:

                    if (D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        //블루투스 연결이 되었을 경우
                        case ServerActivity1.STATE_CONNECTED:
                            Toast.makeText(getApplicationContext(), "연결 성공", Toast.LENGTH_LONG).show();
                            // mConversationArrayAdapter.clear();
                            break;

                        case ServerActivity1.STATE_CONNECTING:
                            Toast.makeText(getApplicationContext(), "연결 중 입니다", Toast.LENGTH_LONG).show();
                            break;

                        case ServerActivity1.STATE_LISTEN:
                        case ServerActivity1.STATE_NONE:
                            //Toast.makeText(getApplicationContext(),"연결되지 않았습니다",Toast.LENGTH_LONG).show();
                            break;
                    }
                    break;

                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    break;

                case MESSAGE_READ:
                    tvfrontpsi = (TextView) findViewById(R.id.tvfrontpsi);
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    readMessage = new String(readBuf, 0, msg.arg1);
                    //init();
                    try{
                        result = Double.parseDouble(readMessage);}
                    catch (NumberFormatException e){}
                    String strNum = String.format("%.2f",result);
                    tvfrontpsi.setText(""+ strNum);

                    break;

                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;

                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private final Handler mHandler2 = new Handler(){
        public void handleMessage(Message msg){
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    Log.i(TAG, "MESSAGE_STATE_CHANGE ===> " + msg.arg1);
                    switch (msg.arg1) {
                        case ServerActivity2.STATE_CONNECTED:
                            Toast.makeText(MainActivity.this, "연결 성공", Toast.LENGTH_SHORT).show();
                            break;
                        case ServerActivity2.STATE_CONNECTING:
                            Toast.makeText(MainActivity.this, "연결 중 입니다", Toast.LENGTH_SHORT).show();
                            break;
                        case ServerActivity2.STATE_LISTEN:
                            break;
                        case ServerActivity2.STATE_NONE:
                            Toast.makeText(MainActivity.this, "도리도리", Toast.LENGTH_SHORT).show();
                    }
                    break;

                case MESSAGE_WRITE:
                    break;

                case MESSAGE_READ:
                    tvbackpsi = (TextView) findViewById(R.id.tvbackpsi);
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    try{
                        result2 = Double.parseDouble(readMessage);}
                    catch (NumberFormatException e){}
                    String strNum = String.format("%.2f",result2);
                    tvbackpsi.setText(""+ strNum);
                    break;

                case MESSAGE_DEVICE_NAME:
                    String mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), mConnectedDeviceName + "에 연결되었습니다.", Toast.LENGTH_SHORT).show();
                    break;

                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;

            }
        }
    };



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.bluetooth_chat, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.discoverable:
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
        }
        return false;
    }
    private void startLocationUpdates() {
        if (!checkLocationServicesStatus()) {
            Log.d(TAG, "startLocationUpdates: call showDialogForLocationServiceSetting");
            showDialogForLocationServiceSetting();
        } else {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            {
                Log.d(TAG, "startLocationUpdates: 퍼미션 안가지고 있음");
                return;
            }
        }

        Log.d(TAG, "startLocationUpdates : call FusedLocationApi.requestLocationUpdates");
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
        mRequestingLocationUpdates = true;
    }

    private void stopLocationUpdates() {

        try {
            Log.d(TAG, "stopLocationUpdates: LocationServices.FusedLocationApi.removeLocationUpdates");
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mRequestingLocationUpdates = false;
        }catch(IllegalStateException e){
            Log.d(TAG, "stopLocationUpdates: "+e.toString());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d( TAG, "onLocationChanged");



        double latitude = location.getLatitude();
        double longitude  = location.getLongitude();
        LatLng latlng = new LatLng(latitude,longitude);

        //여기에 if문을 써서
        if(walkState == true) {
            points.add(latlng);

            reDrawPath();
        }

        if(location.hasSpeed()){
            double speed = location.getSpeed();
            String strSpeed = String.format("%.2f",speed);

            try{
                double realSpeed = Double.parseDouble(strSpeed);
                tvgps.setText("speed : " + realSpeed*3.6);
            }catch (NumberFormatException e){}
        }

        String markerTitle = getCurrentAddress(location);

        String markerSnippet = "위도:" + String.valueOf(location.getLatitude())
                + " 경도:" + String.valueOf(location.getLongitude());

        //LatLng currentlatlng = new LatLng(location.getLatitude(),location.getLongitude());

        //현재 위치에 마커 생성하고 이동
        setCurrentLocation(location, markerTitle, markerSnippet);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady: ");

        mGoogleMap = googleMap;

        //런타임 퍼미션 요청 대화상자나 GPS 활성 요청 대화상자 보이기전에
        //지도의 초기위치를 서울로 이동
        setDefaultLocation();

        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);
        mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        //현재위치로 돌아오는 버튼
        mGoogleMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {

            @Override
            public boolean onMyLocationButtonClick() {
                Log.d(TAG, "onMyLocationButtonClick: 위치에 따른 카메라 이동 활성화");
                mMoveMapByAPI = true;
                mGoogleApiClient.reconnect();
                return true;
            }
        });

        mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener(){
            @Override
            public void onMapClick(LatLng latLng) {
                Log.d(TAG, "onMapClick: ");
            }
        });

        mGoogleMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener(){
            @Override
            public void onCameraMoveStarted(int i) {
                if(mMoveMapByUser == true && mRequestingLocationUpdates){
                    Log.d(TAG, "onCameraMove : 위치에 따른 카메라 이동 비활성화");
                    mMoveMapByAPI = false;
                }
                mMoveMapByUser = true;
            }
        });


        mGoogleMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener(){
            @Override
            public void onCameraMove() {

            }
        });
    }

    protected synchronized void buildGoogleApiClient() {

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();

    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if ( mRequestingLocationUpdates == false ) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

                if (hasFineLocationPermission == PackageManager.PERMISSION_DENIED) {

                    ActivityCompat.requestPermissions(mActivity,
                            new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

                } else {

                    Log.d(TAG, "onConnected : 퍼미션 가지고 있음");
                    Log.d(TAG, "onConnected : call startLocationUpdates");
                    startLocationUpdates();
                    mGoogleMap.setMyLocationEnabled(true);
                }

            }else{

                Log.d(TAG, "onConnected : call startLocationUpdates");
                startLocationUpdates();
                mGoogleMap.setMyLocationEnabled(true);
            }
        }
    }
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed");
        setDefaultLocation();
    }



    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended");
        if ( cause ==  CAUSE_NETWORK_LOST )
            Log.e(TAG, "onConnectionSuspended(): Google Play services " +
                    "connection lost.  Cause: network lost.");
        else if (cause == CAUSE_SERVICE_DISCONNECTED )
            Log.e(TAG,"onConnectionSuspended():  Google Play services " +
                    "connection lost.  Cause: service disconnected");
    }

    public String getCurrentAddress(Location location){

        //GPS를 주소로 변환
        Geocoder geocoder = new Geocoder(this,Locale.getDefault());

        List<Address> addresses;

        //인터넷으로부터 현재 위치에 대한 주소를 가져오지 못할 때 지오코드 사용 불가 에러가 뜸
        try {
            addresses = geocoder.getFromLocation(location.getLatitude(),location.getLongitude(),1);
        } catch (IOException e) {
            Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_SHORT).show();
            return "지오코더 서비스 사용불가";
        } catch (IllegalArgumentException e2){
            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_SHORT).show();
            return "잘못된 GPS 좌표";
        }

        if(addresses == null || addresses.size()==0){
            Toast.makeText(this, "주소 미발견", Toast.LENGTH_SHORT).show();
            return "주소 미발견";
        }else{
            Address address = addresses.get(0);
            return address.getAddressLine(0).toString();
        }

    }


    public boolean checkLocationServicesStatus(){
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }


    public void setCurrentLocation(Location location, String markerTitle, String markerSnippet) {

        mMoveMapByUser = false;

        if(currentMarker != null) currentMarker.remove();


        LatLng currentLatlng = new LatLng(location.getLatitude(),location.getLongitude());

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(currentLatlng);
        markerOptions.title(markerTitle);
        markerOptions.snippet(markerSnippet);
        markerOptions.draggable(true);
        markerOptions.icon(BitmapDescriptorFactory
                .defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        currentMarker = mGoogleMap.addMarker(markerOptions);

        if ( mMoveMapByAPI ) {

            Log.d( TAG, "setCurrentLocation :  mGoogleMap moveCamera "
                    + location.getLatitude() + " " + location.getLongitude() ) ;
            // CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(currentLatLng, 15);
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(currentLatlng);
            mGoogleMap.moveCamera(cameraUpdate);
        }

    }


    public void reDrawPath(){
        PolylineOptions options = new PolylineOptions().width(5).color(Color.BLACK).geodesic(true);
        for(int i = 0; i < points.size(); i++){
            LatLng point = points.get(i);
            options.add(point);
        }

        polyline = mGoogleMap.addPolyline(options);
    }

    public void setDefaultLocation() {

        mMoveMapByUser = false;


        //디폴트 위치, Seoul
        LatLng DEFAULT_LOCATION = new LatLng(37.56, 126.97);
        String markerTitle = "위치정보 가져올 수 없음";
        String markerSnippet = "위치 퍼미션과 GPS 활성 요부 확인하세요";


        if (currentMarker != null) currentMarker.remove();

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(DEFAULT_LOCATION);
        markerOptions.title(markerTitle);
        markerOptions.snippet(markerSnippet);
        markerOptions.draggable(true);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        currentMarker = mGoogleMap.addMarker(markerOptions);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 15);
        mGoogleMap.moveCamera(cameraUpdate);

    }

    //여기부터는 런타임 퍼미션 처리을 위한 메소드들
    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermissions() {
        boolean fineLocationRationale = ActivityCompat
                .shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (hasFineLocationPermission == PackageManager
                .PERMISSION_DENIED && fineLocationRationale )
            showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");

        else if (hasFineLocationPermission
                == PackageManager.PERMISSION_DENIED && !fineLocationRationale ) {
            showDialogForPermissionSetting("퍼미션 거부 + Don't ask again(다시 묻지 않음) " +
                    "체크 박스를 설정한 경우로 설정에서 퍼미션 허가해야합니다.");
        }

        else if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED) {

            if (mGoogleApiClient == null) {
                buildGoogleApiClient();
            }

            mGoogleMap.setMyLocationEnabled(true);


        }
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (permsRequestCode
                == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION && grantResults.length > 0) {

            boolean permissionAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

            if (permissionAccepted) {

                if (mGoogleApiClient == null) {
                    buildGoogleApiClient();
                }

                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {

                    mGoogleMap.setMyLocationEnabled(true);
                }


            }
            else {

                checkPermissions();
            }
        }
    }


    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                ActivityCompat.requestPermissions( mActivity,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        });

        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        builder.create().show();
    }

    private void showDialogForPermissionSetting(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(true);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                askPermissionOnceAgain = true;

                Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + mActivity.getPackageName()));
                myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
                myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mActivity.startActivity(myAppSettings);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        builder.create().show();
    }


    //여기부터는 GPS 활성화를 위한 메소드들
    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
                + "위치 설정을 수정하실래요?" );
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id){
                dialog.cancel();
            }
        });
        builder.create().show();
    }



}
