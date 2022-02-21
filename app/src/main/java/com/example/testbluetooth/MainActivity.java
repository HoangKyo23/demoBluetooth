package com.example.testbluetooth;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {

    private BluetoothAdapter adapter ;
    private TextView txtStatus;
    private ArrayAdapter<String> btArrayAdapter;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_VISIBLE_BT = 2;
    private int status;
    private List<BluetoothDevice> listFindDevice;
    private final BroadcastReceiver broadcast = new BroadcastReceiver() {


        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                btArrayAdapter.add(device.getName());
                btArrayAdapter.add(device.getAddress());
                btArrayAdapter.notifyDataSetChanged();
                listFindDevice.add(device);
            }
        }
    };
    private int action;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtStatus = findViewById(R.id.txtStatus);
        Button btOn = findViewById(R.id.btOn);
        Button btOff = findViewById(R.id.btOff);
        Button btSearch = findViewById(R.id.btSearch);
        Button btList = findViewById(R.id.btList);
        ListView lstView = findViewById(R.id.lstDevices);
        status = 0;
        action = 0;

        adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            btOn.setEnabled(false);
            btOff.setEnabled(false);
            btSearch.setEnabled(false);
            btList.setEnabled(false);
            txtStatus.setText("Status: Bluetooth is not Supported on your device");
            return;
        }

        btArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        lstView.setAdapter(btArrayAdapter);

        listFindDevice = new ArrayList<>();

        lstView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                BluetoothDevice selectedDevice = listFindDevice.get(position);
                try {
                    Method method = null;
                    if (action == 2) {
                        method = selectedDevice.getClass().getMethod(
                                "createBond", (Class[]) null);
                        Log.d("createBond", "aaa");
                    } else if (action == 1) {
                        method = selectedDevice.getClass().getMethod(
                                "removeBond", (Class[]) null);
                        clickTolistDevice(view);
                        Log.d("removeBond", "bbb");
                    }
                    try {
                        if (method != null) {
                            method.invoke(selectedDevice, (Object[]) null);
                        }
                    } catch (IllegalAccessException | IllegalArgumentException
                            | InvocationTargetException e) {
                        Log.d("Pair", e.getLocalizedMessage());
                    }
                } catch (NoSuchMethodException e) {
                    Log.d("Method", e.getLocalizedMessage());
                }
            }
        });
    }



    @SuppressLint("MissingPermission")
    public void clickToTurnon(View view) {
        if (!adapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BT);
            Toast.makeText(getApplicationContext(), "Bluetooth is turn on",
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Bluetooth is already on",
                    Toast.LENGTH_LONG).show();
        }
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT ||
                requestCode == REQUEST_VISIBLE_BT) {
            if (adapter.isEnabled()) {
                txtStatus.setText("Status: Enabled");
            } else {
                txtStatus.setText("Status: Disable");
            }
        }
    }





    @SuppressLint("MissingPermission")
    public void clickToTurnoff(View view) {
        adapter.disable();
        txtStatus.setText("Status: Disable");
        Toast.makeText(getApplicationContext(), "Bluetooth is turn off",
                Toast.LENGTH_LONG).show();
    }


    @SuppressLint("MissingPermission")
    public void clickTovisible(View view) {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(intent, REQUEST_VISIBLE_BT);
    }



    @SuppressLint("MissingPermission")
    public void clickTolistDevice(View view) {
        Set<BluetoothDevice> pairDevices = null;
        btArrayAdapter.clear();
        listFindDevice.clear();
        pairDevices = adapter.getBondedDevices();
        for (BluetoothDevice d : pairDevices) {
            btArrayAdapter.add(d.getName() + "\n" + d.getAddress());
            listFindDevice.add(d);
        }
        btArrayAdapter.notifyDataSetChanged();
        action = 1;
    }

    public void clickToSearch(View view) {
        btArrayAdapter.clear();
        action = 2;
        final ProgressDialog dialog = ProgressDialog.show(this,
                "Please wait ...", "Searching new bluetooth device ...", true);
        dialog.setCancelable(true);
        new Thread(new Runnable() {
            private boolean result = false;

            
            @SuppressLint("MissingPermission")
            @Override
            public void run() {
                if (adapter.isDiscovering()) {
                    adapter.cancelDiscovery();
                    Log.d("bluetooth", "canceldiscovering");
                    dialog.dismiss();
                } else {
                    result = adapter.startDiscovery();
                    registerReceiver(broadcast, new IntentFilter(
                            BluetoothDevice.ACTION_FOUND));
                    Log.d("bluetooth", "discovering");
                    status = 1;
                    while (!result) {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                        }
                    }
                    dialog.dismiss();
                }
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (status > 0) {
            unregisterReceiver(broadcast);
            status = 0;
        }
    }

    public void clickToTransfer(View view) {
        // String uri = "file:///sdcard/MYDBS/horizontal.png";
        File sdCard = Environment.getExternalStorageDirectory();
        String realPath = sdCard.getAbsolutePath();
        File directory = new File(realPath + "/MYDBS");
        File file = new File(directory, "soanbai.txt");
        Log.d("file", file.exists() + "");
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.setComponent(new ComponentName("com.android.bluetooth",
                "com.android.bluetooth.opp.BluetoothoppLauncherActivity"));
        // File file - new File(uci);
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));

    }


}