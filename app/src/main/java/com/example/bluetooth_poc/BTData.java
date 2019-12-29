package com.example.bluetooth_poc;

import android.bluetooth.BluetoothDevice;

import java.io.Serializable;

public class BTData implements Serializable {

    String strBTName = "";
    String strBTMACAddress = "";
    BluetoothDevice btDevice;
}
