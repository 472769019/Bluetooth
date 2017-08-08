package com.cccg.example.bluetooth;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * Created by CCCG-黄文镔 on 2017/8/3.
 */

public class BtListAdapter extends ArrayAdapter<BtDevice> {
    private int layoutId;

    public BtListAdapter(Context context,int layoutId){
        super(context,layoutId);
        this.layoutId=layoutId;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        BtDevice device=getItem(position);
        View view= LayoutInflater.from(getContext()).inflate(layoutId,parent,false);
        TextView deviceName=(TextView) view.findViewById(R.id.tv_device_name);
        if(device.getNickName().isEmpty()) {
            deviceName.setText(device.getDeviceName());
        }else {
            deviceName.setText(device.getNickName());
        }
        return view;
    }
}
