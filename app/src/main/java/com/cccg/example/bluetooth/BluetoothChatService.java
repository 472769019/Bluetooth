

package com.cccg.example.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BluetoothChatService {
	private static final String NAME_SECURE = "BluetoothChatSecure";
	private static final UUID MY_UUID_SECURE = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
	private final BluetoothAdapter mAdapter;
	private final Handler mHandler;
	private AcceptThread mSecureAcceptThread;
	private ConnectThread mConnectThread;
	private ConnectedThread mConnectedThread;
	private int mState;
	public static final int STATE_NONE = 0;
	public static final int STATE_LISTEN = 1;
	public static final int STATE_CONNECTING = 2;
	public static final int STATE_CONNECTED = 3;

	public BluetoothChatService(Handler handler) {
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mState = STATE_NONE;
		mHandler = handler;
	}

	private synchronized void setState(int state) {
		mState = state;
	}

	public synchronized int getState() {
		return mState;
	}

	public synchronized void start() {
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		setState(STATE_LISTEN);
		if (mSecureAcceptThread == null) {
			mSecureAcceptThread = new AcceptThread();
			mSecureAcceptThread.start();
		}
	}

	public synchronized void connect(BluetoothDevice device) {
		if (mState == STATE_CONNECTING) {
			if (mConnectThread != null) {
				mConnectThread.cancel();
				mConnectThread = null;
			}
		}
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
		setState(STATE_CONNECTING);
	}

	public synchronized void connected(BluetoothSocket socket) {
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		if (mSecureAcceptThread != null) {
			mSecureAcceptThread.cancel();
			mSecureAcceptThread = null;
		}
		mConnectedThread = new ConnectedThread(socket);
		mConnectedThread.start();
		setState(STATE_CONNECTED);
	}

	public synchronized void stop() {
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		if (mSecureAcceptThread != null) {
			mSecureAcceptThread.cancel();
			mSecureAcceptThread = null;
		}
		setState(STATE_NONE);
	}

	public void write(byte[] out, int type) {
		ConnectedThread r;
		synchronized (this) {
			if (mState != STATE_CONNECTED)
				return;
			r = mConnectedThread;
		}
		r.write(out, type);
	}

	private void connectionFailed() {
		Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(MainActivity.TOAST, "设备无法连接");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
		BluetoothChatService.this.start();
	}

	private void connectionLost() {
		Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(MainActivity.TOAST, "连接已断开");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
		BluetoothChatService.this.start();
	}

	private class AcceptThread extends Thread {
		private final BluetoothServerSocket mmServerSocket;
		public AcceptThread() {
			BluetoothServerSocket tmp = null;
			try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, MY_UUID_SECURE);
			} catch (IOException e) {
				e.printStackTrace();
			}
			mmServerSocket = tmp;
		}

		public void run() {
			BluetoothSocket socket = null;
			while (mState != STATE_CONNECTED) {
				try {
					socket = mmServerSocket.accept();
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}
				if (socket != null) {
					synchronized (BluetoothChatService.this) {
						switch (mState) {
						case STATE_CONNECTING:
							connected(socket);
							break;
						case STATE_CONNECTED:
							try {
								socket.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
							break;
						}
					}
				}
			}
		}

		public void cancel() {
			try {
				mmServerSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		public ConnectThread(BluetoothDevice device) {
			BluetoothSocket tmp = null;
			try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
			} catch (IOException e) {
				e.printStackTrace();
			}
			mmSocket = tmp;
		}

		public void run() {
			mAdapter.cancelDiscovery();
			try {
				mmSocket.connect();
			} catch (IOException e) {
				try {
					mmSocket.close();
				} catch (IOException e2) {
					e.printStackTrace();
				}
				connectionFailed();
				return;
			}

			synchronized (BluetoothChatService.this) {
				mConnectThread = null;
			}

			connected(mmSocket);
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
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
				e.printStackTrace();
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			byte[] buffer = new byte[1024];
			int bytes = 0;
			while (true) {
				try {
					bytes = mmInStream.read(buffer);
					Log.i("bytes", bytes + "");
					if (bytes < 989) {
						mHandler.obtainMessage(MainActivity.MESSAGE_READ,bytes, -1, buffer).sendToTarget();
					}
				} catch (IOException e) {
					connectionLost();
					BluetoothChatService.this.start();
					return;
				}
			}
		}

		public void write(byte[] buffer, int type) {
			try {
				mmOutStream.write(buffer);
				mHandler.obtainMessage(type, -1, -1, buffer).sendToTarget();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
