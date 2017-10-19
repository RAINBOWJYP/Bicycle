package com.example.jyp2.bicyclemanager;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

/**
 * Created by jyp on 2017-05-12.
 */

public class DeviceListActivity extends Activity {


    // Debugging
    private static final String TAG = "DeviceListActivity";
    private static final boolean D = true;

    // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    // Member fields
    private BluetoothAdapter mBtAdapter;

    private ArrayAdapter<String> mNewDevicesArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.device_list);

        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);

        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doDiscovery();
                v.setVisibility(View.GONE);
            }
        });

        ArrayAdapter<String> mPairedDevicesArrayAdapter;

        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);//어댑터 객체 생성
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices); //객체 참조
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickedListener);

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> pariedDevices = mBtAdapter.getBondedDevices(); //정보가져오기
        //getBondedDevices는 페어링된 단말의 MAC주소를 가져온다.


        if (pariedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pariedDevices) {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            mPairedDevicesArrayAdapter.add(noDevices);
        }

        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickedListener);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
    }

    private void doDiscovery() {
        if (D) Log.d(TAG, "doDiscovery()");

        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);
        setTitle("단말을 검색중입니다...");

        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);//subtitle

        //만약 discovering이 이미되어있다면 멈추거라
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        //BluetoothAdapter로부터 discover를 요청하기
        mBtAdapter.startDiscovery();

    }

    protected void onDestroy(){
        super.onDestroy();

        //Make sure we are not doing discovery anymore
        if(mBtAdapter!=null){
            mBtAdapter.cancelDiscovery();
        }
        this.unregisterReceiver(mReceiver);
    }

    private AdapterView.OnItemClickListener mDeviceClickedListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mBtAdapter.cancelDiscovery();

            //Get the device MAC address, which is the last 17chars in the View
            String info = ((TextView) view).getText().toString();
            String address = info.substring(info.length() - 17);

            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            //아마 여기서 address를 넘겨주는 것 같아!!!!!!!!!!★★★address를 넘겨준다구!!!!★★★★★★
            //set result and finish this activity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle("연결할 단말을 선택하세요");
                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = "단말을 찾을 수 없습니다.".toString();
                    mNewDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };

}
