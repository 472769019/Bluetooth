package com.cccg.example.bluetooth;

/**
 * Created by CCCG-黄文镔 on 2017/8/3.
 */

public class BtDevice {

    private String deviceName;
    private String deviceAddress;
    private String nickName="";

    BtDevice(String name, String address){
        deviceName=name;
        deviceAddress=address;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }
}
