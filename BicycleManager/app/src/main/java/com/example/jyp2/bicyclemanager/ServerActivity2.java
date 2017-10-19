package com.example.jyp2.bicyclemanager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by jyp on 2017-08-02.
 */

public class ServerActivity2 {

    private final String TAG = "ServerActivity2";

    private static final String NAME = "Bluetooth";

    private final BluetoothAdapter mBluetoothAdapter;
    private final Handler mhandler;
    private int mState;
    private int mNewState;
    private final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    public ServerActivity2(Context context, Handler handler) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mhandler = handler;
        mState = STATE_NONE;
        mNewState = mState;
    }

    private synchronized void setState() {
        mState = getState();
        Log.d(TAG, "setState() " + mNewState + " -> " + mState);
        mNewState = mState;

        mhandler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, mNewState, -1).sendToTarget();
    }

    public synchronized int getState() {
        return mState;
    }

    public synchronized void start() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
    }

    public synchronized void connect(BluetoothDevice device){
        Log.d(TAG, "connect to : "+device);

        if(mState == STATE_CONNECTING){
            if(mConnectThread != null){
                mConnectThread.cancel();
                mConnectThread=null;
            }
        }

        if(mConnectedThread != null){
            mConnectedThread.cancel();
            mConnectedThread=null;
        }

        mConnectThread = new ConnectThread(device);
        mConnectThread.start();

        setState();
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device){

        if(mConnectThread != null){
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if(mConnectedThread != null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if(mAcceptThread != null){
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        Message msg = mhandler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.DEVICE_NAME,device.getName());
        msg.setData(bundle);
        mhandler.sendMessage(msg);

        setState();
    }


    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        mState = STATE_NONE;
        setState();
    }

    public void write(byte[] out){

        ConnectedThread r;
        synchronized (this) {
            if( mState != STATE_CONNECTED) return;
            r=mConnectedThread;
        }
        r.write(out);
    }

    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;


            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, uuid);

            } catch (IOException e) {
                Log.e(TAG, "listen() failed", e);
            }
            mmServerSocket = tmp;
            mState = STATE_LISTEN;
        }


        public void run() {
            Log.d(TAG, "BEGIN mAcceptThread" + this);
            setName("AcceptThread");

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (ServerActivity2.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread ");

        }

        public void cancel() {
            Log.d(TAG, "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            try {
                tmp = device.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread create() failed", e);
            }

            mmSocket = tmp;
            mState = STATE_CONNECTING;
        }

        @Override
        public void run() {

            mBluetoothAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException e1) {
                    Log.e(TAG, "unable to close(), socket during connection failure", e1);
                }
                connectionFailed();
                return;
            }
            synchronized (ServerActivity2.this){
                mConnectThread = null;
            }

            connected(mmSocket,mmDevice);
            mhandler.obtainMessage(0,mmSocket).sendToTarget();

            Log.d(TAG, "Connect Success");
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect, socket failed ", e);
            }
        }
    }


    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;
        }

        @Override
        public void run() {

            byte[] buffer = new byte[1024];
            int bytes;

            while(mState == STATE_CONNECTED) {
                try {
                    bytes = mmInStream.read(buffer);

                    mhandler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }


        public void write(byte[] buffer){

            try {
                mmOutStream.write(buffer);

                mhandler.obtainMessage(MainActivity.MESSAGE_WRITE,-1,-1,buffer).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }

    }

    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mhandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "연결 실패(connectionFailed)");
        msg.setData(bundle);
        mhandler.sendMessage(msg);

        mState = STATE_NONE;
        setState();
        // Start the service over to restart listening mode
        ServerActivity2.this.start();
    }

    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mhandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "연결이 끊어졌어!(connectionLost)");
        msg.setData(bundle);
        mhandler.sendMessage(msg);


        mState = STATE_NONE;
        setState();
        // Start the service over to restart listening mode
        ServerActivity2.this.start();
    }
}
