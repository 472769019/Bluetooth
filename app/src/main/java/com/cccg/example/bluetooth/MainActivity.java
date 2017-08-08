package com.cccg.example.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE=0;
    private static final int REQUEST_DISCOVERABLE=1;
    private BluetoothAdapter mBtAdapter;
    private BtListAdapter mPairedDevicesArrayAdapter;
    private BtListAdapter mNewDevicesArrayAdapter;
    private BtListAdapter tempPairedDevicesArrayAdapter;
    private BtListAdapter tempNewDevicesArrayAdapter;
    private byte[] message="发送的数据".getBytes();
    private BluetoothChatService mChatService = null;

    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_WRITE = 2;
    public static final int MESSAGE_TOAST = 3;
    public static final String TOAST = "toast";

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //当搜索到蓝牙设备
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter.add(new BtDevice(device.getName(),device.getAddress()));
                    //tempNewDevicesArrayAdapter.add(new BtDevice(device.getName(),device.getAddress()));
                }else{
                    mPairedDevicesArrayAdapter.add(new BtDevice(device.getName(),device.getAddress()));
                    //tempPairedDevicesArrayAdapter.add(new BtDevice(device.getName(),device.getAddress()));
                }
            //当搜索停止时
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                /*mPairedDevicesArrayAdapter=tempPairedDevicesArrayAdapter;
                mNewDevicesArrayAdapter=tempNewDevicesArrayAdapter;
                Toast.makeText(MainActivity.this,"一轮搜索结束\n已配对设备数："+mPairedDevicesArrayAdapter.getCount()+"\n未配对设备数："+mNewDevicesArrayAdapter.getCount(),Toast.LENGTH_SHORT).show();
                tempPairedDevicesArrayAdapter.clear();
                tempNewDevicesArrayAdapter.clear();
                doDiscovery();*/
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("蓝牙设备列表");
        mPairedDevicesArrayAdapter = new BtListAdapter(this,R.layout.device_list_item);
        mNewDevicesArrayAdapter = new BtListAdapter(this,R.layout.device_list_item);
        tempPairedDevicesArrayAdapter = new BtListAdapter(this,R.layout.device_list_item);
        tempNewDevicesArrayAdapter = new BtListAdapter(this,R.layout.device_list_item);
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                BtDevice device=mPairedDevicesArrayAdapter.getItem(position);
                connectDevice(device);
                connectDevice(device);
            }
        });
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                BtDevice device=mNewDevicesArrayAdapter.getItem(position);
                BluetoothDevice btDevice=mBtAdapter.getRemoteDevice(device.getDeviceAddress());
                btDevice.createBond();
                if(btDevice.getBondState()==BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter.remove(device);
                    mPairedDevicesArrayAdapter.add(device);
                }else {
                    Toast.makeText(MainActivity.this,"蓝牙配对失败",Toast.LENGTH_SHORT).show();
                }
            }
        });
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!mBtAdapter.isEnabled()) {
            Intent enable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enable,REQUEST_ENABLE);
        }
        Intent discoverable = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(discoverable,REQUEST_DISCOVERABLE);
        Button refresh=(Button) findViewById(R.id.btn_refresh);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPairedDevicesArrayAdapter.clear();
                mNewDevicesArrayAdapter.clear();
                doDiscovery();
            }
        });
        Button sendMessage=(Button) findViewById(R.id.btn_send_message);
        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage(message,MESSAGE_WRITE);
            }
        });
        Button setName=(Button) findViewById(R.id.btn_set_name);
        final EditText etSetName=(EditText) findViewById(R.id.et_set_name);
        setName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBtAdapter.setName(etSetName.getText().toString());
                etSetName.setText("");
                Toast.makeText(MainActivity.this,"修改成功",Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mChatService=new BluetoothChatService(mHandler);
        if (mChatService != null) {
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                mChatService.start();
            }
        }
        doDiscovery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }
        this.unregisterReceiver(mReceiver);
    }

    private void doDiscovery() {
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }
        mBtAdapter.startDiscovery();
    }

    private void connectDevice(BtDevice btDevice) {
        BluetoothDevice device = mBtAdapter.getRemoteDevice(btDevice.getDeviceAddress());
        mChatService.connect(device);
    }

    private void sendMessage(byte[] message, int type) {
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, "设备未连接", Toast.LENGTH_SHORT).show();
            return;
        }
        if (message.length > 0) {
            mChatService.write(message, type);
        }
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    String writeMessage = new String(writeBuf);
                    Toast.makeText(MainActivity.this,writeMessage,Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Toast.makeText(MainActivity.this,readMessage,Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(MainActivity.this,msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
}