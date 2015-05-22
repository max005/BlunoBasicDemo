package com.example.blunobasicdemo;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

public class MainActivity  extends BlunoLibrary {
	private Button buttonScan;
	private Button buttonDisconnect;
	private TextView serialReceivedFront;
	private TextView serialReceivedLeft;
	private TextView serialReceivedRight;
	private MsgReceiver msgReceiver;
	private Intent transferIntent = new Intent("com.example.blunobasicdemo.RECEIVER_SERVICE");

	private int front  = 0;
	private int left   = 0;
	private int right  = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		onCreateProcess();
        buttonScan=(Button) findViewById(R.id.buttonScan);
		buttonDisconnect=(Button) findViewById(R.id.buttonDisconnect);
		serialReceivedFront=(TextView) findViewById(R.id.Front);
		serialReceivedLeft=(TextView) findViewById(R.id.Left);
        serialReceivedRight=(TextView) findViewById(R.id.Right);
		buttonScan.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(!isServiceRunning(getApplicationContext(), "com.example.blunobasicdemo.BlunoService")) {
					onCreateProcess();

					Intent intent = new Intent(MainActivity.this, BlunoService.class);
					startService(intent);
				}
				buttonScanOnClickProcess();                                        //Alert Dialog for selecting the BLE device
			}
		});
		buttonDisconnect.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(isServiceRunning(getApplicationContext(), "com.example.blunobasicdemo.BlunoService")) {
					Intent intent = new Intent(MainActivity.this, BlunoService.class);
					stopService(intent);                                        //Alert Dialog for selecting the BLE device
					mConnectionState = theConnectionState.isToScan;
					onConectionStateChange(mConnectionState);
				}
			}
		});

		msgReceiver = new MsgReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("com.example.blunobasicdemo.RECEIVER_ACTIVITY");
		registerReceiver(msgReceiver, intentFilter);

		Intent intent = new Intent(MainActivity.this, BlunoService.class);
		startService(intent);
	}

	protected void onResume(){
		super.onResume();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}
	
    @Override
    protected void onPause() {
        super.onPause();
	}
	
	protected void onStop() {
		super.onStop();
	}
    
	@Override
    protected void onDestroy() {
        super.onDestroy();
		unregisterReceiver(msgReceiver);
	}

	public class MsgReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent) {
			front  = intent.getIntExtra("front", 0);
			left   = intent.getIntExtra("left", 0);
			right  = intent.getIntExtra("right", 0);
			connectionState = intent.getStringExtra("connectionState");

			mConnectionState = theConnectionState.valueOf(connectionState);
			onConectionStateChange(mConnectionState);
			serialReceivedFront.setText(Integer.toString(front));
			serialReceivedLeft.setText(Integer.toString(left));
			serialReceivedRight.setText(Integer.toString(right));
		}
	}

	public static boolean isServiceRunning(Context context, String serviceClassName){
		final ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
		final List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

		for (ActivityManager.RunningServiceInfo runningServiceInfo : services) {
			if (runningServiceInfo.service.getClassName().equals(serviceClassName)){
				return true;
			}
		}
		return false;
	}

	void buttonScanOnClickProcess()
	{
		switch (mConnectionState) {
			case isNull:
				connectionState="isScanning";
				mConnectionState = theConnectionState.valueOf(connectionState);
				onConectionStateChange(mConnectionState);
				scanLeDevice(true);
				mScanDeviceDialog.show();
				break;
			case isToScan:
				connectionState="isScanning";
				mConnectionState = theConnectionState.valueOf(connectionState);
				onConectionStateChange(mConnectionState);
				scanLeDevice(true);
				mScanDeviceDialog.show();
				break;
			case isScanning:
				break;
			case isConnecting:
				break;
			case isConnected:
				break;
			case isDisconnecting:
				break;
			default:
				break;
		}
	}

	@Override
	public void onConectionStateChange(theConnectionState mConnectionState){
		switch (mConnectionState) {
			case isConnected:
				buttonScan.setText("Connected");
				break;
			case isConnecting:
				buttonScan.setText("Connecting");
				//Intent intent = new Intent(MainActivity.this, BlunoService.class);
				//startService(intent);
				transferIntent.putExtra("mDeviceAddress", mDeviceAddress);
				transferIntent.putExtra("connectionState", connectionState);
				sendBroadcast(transferIntent);
				break;
			case isToScan:
				buttonScan.setText("Scan");
				break;
			case isScanning:
				buttonScan.setText("Scanning");
				break;
			case isDisconnecting:
				buttonScan.setText("isDisconnecting");
				break;
		}
	}
}