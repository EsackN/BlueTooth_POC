package com.example.bluetooth_poc;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements DevicesOnClick{


    private String[] appPermissions = {Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION};
    private static final int PERMISSION_REQUEST_CODE = 1240;
    private static final int REQUEST_ENABLE_BT = 1241;
    private static final int REQUEST_ENABLE_DISCOVERABLE = 1242;

    // Get the default adapter
    private BluetoothAdapter bluetoothAdapter;

    private BluetoothProfile.ServiceListener profileListener;

    private BluetoothHeadset bluetoothHeadset;

    private RecyclerView rvBTPairedDevices;
    private RecyclerView rvBTAvailableDevices;

    private BTDevicesAdapter btPairedDevicesAdapter;
    private BTDevicesAdapter btAvailableDevicesAdapter;

    private ArrayList<BTData> lstBTPairedDevices = new ArrayList<>();
    private ArrayList<BTData> lstBTAvailableDevices = new ArrayList<>();

    private Switch toggleBT, toggleBTDisc;
    
    private String TAG = "Bluetooth_POC";

    private Button btnScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (checkBTPermissions()) {
            init();
        }
    }

    private boolean checkBTPermissions() {

        List<String> listPermissionsNeeded = new ArrayList<>();

        for (String perm : appPermissions) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, perm)
                    != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(perm);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(MainActivity.this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                    PERMISSION_REQUEST_CODE);
            return false;
        }

        return true;
    }

    @Override
    protected void onResume() {
        // Register for broadcasts when a device is discovered.
        IntentFilter filter = null;
        filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(receiver, filter);
        super.onResume();
    }

    private void init() {

        rvBTPairedDevices = findViewById(R.id.rv_bt_paired_devices);
        rvBTAvailableDevices = findViewById(R.id.rv_bt_available_devices);
        toggleBT = findViewById(R.id.toggle_BT);
        toggleBTDisc = findViewById(R.id.toggle_BT_Disc);
        btnScan = findViewById(R.id.btn_scan);

        btPairedDevicesAdapter = new BTDevicesAdapter(lstBTPairedDevices, MainActivity.this, this);
        rvBTPairedDevices.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        rvBTPairedDevices.setAdapter(btPairedDevicesAdapter);

        btAvailableDevicesAdapter = new BTDevicesAdapter(lstBTAvailableDevices, MainActivity.this, this);
        rvBTAvailableDevices.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        rvBTAvailableDevices.setAdapter(btAvailableDevicesAdapter);

        profileListener = new BluetoothProfile.ServiceListener() {
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if (profile == BluetoothProfile.HEADSET) {
                    bluetoothHeadset = (BluetoothHeadset) proxy;
                }
            }

            public void onServiceDisconnected(int profile) {
                if (profile == BluetoothProfile.HEADSET) {
                    bluetoothHeadset = null;
                }
            }
        };

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter != null) {

            // Establish connection to the proxy.
            bluetoothAdapter.getProfileProxy(MainActivity.this, profileListener, BluetoothProfile.HEADSET);

            if (!bluetoothAdapter.isEnabled()) {
                toggleBT.setChecked(false);
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                toggleBT.setChecked(true);
                showPairedDevices();
            }

            if (!bluetoothAdapter.isDiscovering()) {
                toggleBTDisc.setChecked(false);
            } else {
                toggleBTDisc.setChecked(true);
            }

        } else {
            showDialog("Bluetooth_POC", "Your device not supporting Bluetooth",
                    "", null, "Exit app", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            finish();
                        }
                    }, false);
        }

        toggleBT.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                } else {
                    bluetoothAdapter.disable();
                }
            }
        });

        toggleBTDisc.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    makeYourDeviceDiscoverable();
                } else {
                    bluetoothAdapter.cancelDiscovery();
                }
            }
        });

        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnScan.getText().toString().equalsIgnoreCase("SCAN")){
                    bluetoothAdapter.startDiscovery();
                    btnScan.setText("Scanning. Tap to STOP");
                    lstBTAvailableDevices.clear();
                }else{
                    bluetoothAdapter.cancelDiscovery();
                    btnScan.setText("SCAN");
                }
            }
        });
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

//            To listen the BT is connected or not
            if (action.equals(bluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(bluetoothAdapter.EXTRA_STATE, bluetoothAdapter.ERROR);
                switch (state){
                    case BluetoothAdapter.STATE_OFF:
                        Toast.makeText(getApplicationContext(), "BT Disconnected", Toast.LENGTH_SHORT).show();
                        toggleBT.setChecked(false);
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Toast.makeText(getApplicationContext(), "BT Connected", Toast.LENGTH_SHORT).show();
                        toggleBT.setChecked(true);
                        break;
                }
            }

//            To listen the status of pairing and unpairing
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    Toast.makeText(getApplicationContext(), "BT Device Paired", Toast.LENGTH_SHORT).show();
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
                    Toast.makeText(getApplicationContext(), "BT Device Unpaired", Toast.LENGTH_SHORT).show();
                }
                showPairedDevices();
                btnScan.performClick();
            }

            //To listen the BT devices nearby
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                int deviceBondState =  device.getBondState();
                Log.v(TAG, "Bond Status" + deviceBondState);
                BTData mData = new BTData();
                mData.strBTName = deviceName;
                mData.strBTMACAddress = deviceHardwareAddress;
                mData.btDevice = device;
                if (deviceName != null && !deviceName.equalsIgnoreCase("") && !lstBTAvailableDevices.contains(mData.strBTName)){
                    lstBTAvailableDevices.add(mData);
                }
                setBsetListTAdapter("available", lstBTAvailableDevices);
            }
        }
    };

    private void showPairedDevices() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            lstBTPairedDevices.clear();
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC
                int deviceBondState =  device.getBondState();
                Log.v(TAG, "Name" + deviceName);
                Log.v(TAG, "MAC Address" + deviceHardwareAddress);
                Log.v(TAG, "Bond Status" + deviceBondState);
                BTData mData = new BTData();
                mData.strBTName = deviceName;
                mData.strBTMACAddress = deviceHardwareAddress;
                mData.btDevice = device;
                lstBTPairedDevices.add(mData);
            }

            setBsetListTAdapter("paired", lstBTPairedDevices);
        }
    }

    private void setBsetListTAdapter(String strTag, ArrayList<BTData> lstBTPairedDevices) {
        if (strTag.equalsIgnoreCase("paired")){
            btPairedDevicesAdapter.setList(lstBTPairedDevices);
            btPairedDevicesAdapter.notifyDataSetChanged();
        }else{
            btAvailableDevicesAdapter.setList(lstBTPairedDevices);
            btAvailableDevicesAdapter.notifyDataSetChanged();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            HashMap<String, Integer> permissionResults = new HashMap<>();
            int deniedCount = 0;

            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    permissionResults.put(permissions[i], grantResults[i]);
                    deniedCount++;
                }
            }

            if (deniedCount == 0) {
                init();
            } else {
                showDialog("BLUETOOTH_POC", " You have denied ACCESS_FINE_LOCATION permission. Allow this permission at [SETTINGS] > [PERMISSIONS]",
                        "Go to Settings", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                Intent uIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package",
                                        getPackageName(), null));
                                uIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(uIntent);
                                finish();
                            }
                        }, "No, Exit app", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                finish();
                            }
                        }, false);
            }
        }
    }

    private AlertDialog showDialog(String title, String msg, String positiveLabel, DialogInterface.OnClickListener positiveOnClick,
                                   String negativeLabel, DialogInterface.OnClickListener negativeOnClick, boolean isCancelable) {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setPositiveButton(positiveLabel, positiveOnClick);
        builder.setNegativeButton(negativeLabel, negativeOnClick);
        builder.setCancelable(isCancelable);

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
        return alertDialog;

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(MainActivity.this, "Bluetooth is ENABLED", Toast.LENGTH_LONG).show();
                toggleBT.setChecked(true);
                showPairedDevices();
            } else {
                Toast.makeText(MainActivity.this, "Bluetooth is NOT ENABLED", Toast.LENGTH_LONG).show();
                toggleBT.setChecked(false);
            }
        }
        if (requestCode == REQUEST_ENABLE_DISCOVERABLE) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(MainActivity.this, "Your device is now discoverable for others", Toast.LENGTH_LONG).show();
                toggleBTDisc.setChecked(true);
            } else {
                Toast.makeText(MainActivity.this, "Your device is not discoverable to others", Toast.LENGTH_LONG).show();
                toggleBTDisc.setChecked(false);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }


    private void makeYourDeviceDiscoverable() {
        Intent discoverableIntent =
                new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivityForResult(discoverableIntent, REQUEST_ENABLE_DISCOVERABLE);
    }

    private void pairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unpairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPairedDeviceClick(final  int i) {
        showDialog("BLUETOOTH_POC", "Do you want to unpair with the device "+lstBTPairedDevices.get(i).strBTName + "?",
                "UnPair", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        unpairDevice(lstBTPairedDevices.get(i).btDevice);
                    }
                }, "No", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }, false);
    }

    @Override
    public void onAvailableDeviceClick(final int i) {
        showDialog("BLUETOOTH_POC", "Do you want to pair with the device "+lstBTAvailableDevices.get(i).strBTName + "?",
                "Pair", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        pairDevice(lstBTAvailableDevices.get(i).btDevice);
                    }
                }, "No", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }, false);
    }
}
