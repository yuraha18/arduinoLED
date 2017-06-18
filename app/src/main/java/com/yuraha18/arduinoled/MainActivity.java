package com.yuraha18.arduinoled;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    // mac address arduino bluetooth module
    private final String MAC = "98:D3:32:30:B2:4E";

    Button fstRed;
    Button fstGreen;
    Button fstBlue;
    Button scdRed;
    Button scdGreen;
    Button scdBlue;
    final int RECIEVE_MESSAGE = 1;
    Handler h;
    private StringBuilder sb = new StringBuilder();
    BluetoothSocket clientSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnConnect = (Button) findViewById(R.id.connect);// create connect button for hand connecting to arduino
        btnConnect.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                turnOnBluetooth();
            }
        });
        initButtons();
        turnOnBluetooth();//turn on bluetooth after start app

        /* get responce from arduino about changing LED state  */
         h = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case RECIEVE_MESSAGE:
                        byte[] readBuf = (byte[]) msg.obj;
                        String strIncom = new String(readBuf, 0, msg.arg1);
                        sb.append(strIncom);												// get string
                        int endOfLineIndex = sb.indexOf("\r\n");							// on the end of string
                        if (endOfLineIndex > 0) { 											// get result
                            String result = sb.substring(0, endOfLineIndex);				//
                            sb.delete(0, sb.length());

                            changeBtnState(result);

                        }
                        break;
                }
            };
        };
       ConnectedThread mConnectedThread = new ConnectedThread(clientSocket);
        mConnectedThread.start();
    }

    private void changeBtnState(String result) {
        String off = getResources().getString(R.string.off);
        String on = getResources().getString(R.string.on);
        String text;
        int res = Integer.parseInt(result);// get int number of pin on arduino and command (0-1)

        if (res%2 == 0)
            text = off;
        else
            text = on;

        setBtnText(text, res);
    }

    /* change button text after changing led state on arduino
    * there are 6 leds with 0-1 state*/
    private void setBtnText(String text, int res) {
        if (res/10 == 2)
            fstRed.setText(text);
        else if (res/10 == 3)
            fstGreen.setText(text);
        else if (res/10 == 4)
            fstBlue.setText(text);
        else if (res/10 == 5)
            scdRed.setText(text);
        else if (res/10 == 6)
            scdGreen.setText(text);
        else if (res/10 == 7)
            scdBlue.setText(text);
    }

    private void turnOnBluetooth() {
        //turn on bluetooth.
        String enableBT = BluetoothAdapter.ACTION_REQUEST_ENABLE;
        startActivityForResult(new Intent(enableBT), 0);

        //get default bluetooth adapter
        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        String connectResult ;
        try{
            // try connect to our module, using its MAC address
            BluetoothDevice device = bluetooth.getRemoteDevice(MAC);

            //init connect
            Method m = device.getClass().getMethod(
                    "createRfcommSocket", new Class[] {int.class});

            clientSocket = (BluetoothSocket) m.invoke(device, 1);
            clientSocket.connect();
           connectResult ="CONNECTED";

        } catch (IOException e) {
            connectResult =("BLUETOOTH"+ e.getMessage());
        } catch (SecurityException e) {
            connectResult =("BLUETOOTH"+ e.getMessage());
        } catch (NoSuchMethodException e) {
            connectResult =("BLUETOOTH"+ e.getMessage());
        } catch (IllegalArgumentException e) {
            connectResult =("BLUETOOTH"+ e.getMessage());
        } catch (IllegalAccessException e) {
            connectResult =("BLUETOOTH"+ e.getMessage());
        } catch (InvocationTargetException e) {
            connectResult =("BLUETOOTH"+ e.getMessage());
        }

        //show message about connecting state
        Toast.makeText(getApplicationContext(), connectResult, Toast.LENGTH_LONG).show();
    }

    /* init whole buttons*/
    private void initButtons() {
        fstRed = (Button) findViewById(R.id.fstR);
        fstBlue = (Button) findViewById(R.id.fstB);
        fstGreen = (Button) findViewById(R.id.fstG);
        scdBlue = (Button)  findViewById(R.id.scdB);
        scdGreen = (Button) findViewById(R.id.scdG) ;
        scdRed = (Button)  findViewById(R.id.scdR);

        //set onClickListeners
        fstRed.setOnClickListener(this);
        fstBlue.setOnClickListener(this);
        fstGreen.setOnClickListener(this);
        scdBlue.setOnClickListener(this);
        scdGreen.setOnClickListener(this);
        scdRed.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        String off = getResources().getString(R.string.off);
        //try send data
        try {
            OutputStream outStream = clientSocket.getOutputStream();

            int value = 0;

            //depend on clicked button - make different int
            // 0 - off
            // 1 - on
            // 20, 30... mean pins on arduino. Controller will make /10 and know which pin must use
            // Example 31 mean that 3 pin will be on
            if (v == fstRed)
                value = (fstRed.getText().equals(off) ? 1 : 0) + 20;
            else if (v == fstGreen)
                value = (fstGreen.getText().equals(off) ? 1 : 0) + 30;
            else if (v == fstBlue)
                value = (fstBlue.getText().equals(off) ? 1 : 0) + 40;
            else if (v == scdRed)
                value = (scdRed.getText().equals(off) ? 1 : 0) + 50;
            else if (v == scdGreen)
                value = (scdGreen.getText().equals(off) ? 1 : 0) + 60;
            else if (v == scdBlue)
                value = (scdBlue.getText().equals(off) ? 1 : 0) + 70;

            //write data
            outStream.write(value);
        } catch (IOException e) {
            //show message
            Toast.makeText(this, getResources().getString(R.string.cantConnectToArduino), Toast.LENGTH_LONG).show();
            Log.d("BLUETOOTH", e.getMessage());
        }
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;

        public ConnectedThread(BluetoothSocket socket) {

            InputStream tmpIn = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
        }

        public void run() {
            byte[] buffer = new byte[256];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    System.out.println("result "+bytes);
                    // send result to handler
                    h.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer).sendToTarget();		// Отправляем в очередь сообщений Handler
                } catch (IOException e) {
                    break;
                }
            }
        }
    }
}