package com.example.bluetooth_poc;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

class BTDevicesAdapter extends RecyclerView.Adapter<BTDevicesAdapter.BTViewHolder> {

    ArrayList<BTData> lstBTDevices;
    Activity mActivity;
    DevicesOnClick clickListener;

    public BTDevicesAdapter(ArrayList<BTData> lstBTDevices, Activity mActivity, DevicesOnClick clickListener) {
        this.lstBTDevices = lstBTDevices;
        this.mActivity = mActivity;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public BTViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View photoView = LayoutInflater.from(mActivity).inflate(R.layout.item_bt_devices, parent, false);
        BTViewHolder viewHolder = new BTViewHolder(photoView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull BTViewHolder holder, int position) {
        holder.tvBTName.setText(lstBTDevices.get(position).strBTName);
        holder.tvBTMacAddress.setText(lstBTDevices.get(position).strBTMACAddress);

        holder.lnrBTDevice.setTag(position);
        holder.lnrBTDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = Integer.parseInt(v.getTag().toString());
                if (lstBTDevices.get(pos).btDevice.getBondState() != BluetoothDevice.BOND_BONDED){
                    clickListener.onAvailableDeviceClick(Integer.parseInt(v.getTag().toString()));
                }else{
                    clickListener.onPairedDeviceClick(Integer.parseInt(v.getTag().toString()));
                }
            }
        });

    }

    @Override
    public int getItemCount() {
        return lstBTDevices.size();
    }

    public void setList(ArrayList<BTData> lstBTDevices) {
        this.lstBTDevices = lstBTDevices;
    }

    public static class BTViewHolder extends RecyclerView.ViewHolder {
        public TextView tvBTName;
        public TextView tvBTMacAddress;
        public LinearLayout lnrBTDevice;

        public BTViewHolder(View itemView) {
            super(itemView);
            this.tvBTName = itemView.findViewById(R.id.tv_bt_name);
            this.tvBTMacAddress = itemView.findViewById(R.id.tv_bt_mac);
            this.lnrBTDevice = itemView.findViewById(R.id.lnrBTDevice);
        }
    }
}
