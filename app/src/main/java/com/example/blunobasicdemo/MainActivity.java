package com.example.blunobasicdemo;

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

public class MainActivity  extends BlunoLibrary {
	private Button buttonScan;
	private TextView serialReceivedFrontLeft;
	private TextView serialReceivedFrontRight;
	private TextView serialReceivedLeft;
	private TextView serialReceivedRight;
	private MsgReceiver msgReceiver;
	private Intent transferIntent = new Intent("com.example.blunobasicdemo.RECEIVER_SERVICE");

	private int frontLeft  = 0;
	private int frontRight = 0;
	private int left         = 0;
	private int right        = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		onCreateProcess();
        buttonScan=(Button) findViewById(R.id.buttonScan);
		serialReceivedFrontLeft=(TextView) findViewById(R.id.ForwardLeft);
		serialReceivedFrontRight=(TextView) findViewById(R.id.ForwardRight);
		serialReceivedLeft=(TextView) findViewById(R.id.Left);
        serialReceivedRight=(TextView) findViewById(R.id.Right);
		buttonScan.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				buttonScanOnClickProcess();                                        //Alert Dialog for selecting the BLE device
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
		Intent intent = new Intent(MainActivity.this, BlunoService.class);
		stopService(intent);
	}

	public class MsgReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent) {
			frontLeft  = intent.getIntExtra("forwardLeft", 0);
			frontRight = intent.getIntExtra("forwardRight", 0);
			left         = intent.getIntExtra("left", 0);
			right        = intent.getIntExtra("right", 0);
			connectionState = intent.getStringExtra("connectionState");

			mConnectionState = theConnectionState.valueOf(connectionState);
			onConectionStateChange(mConnectionState);
			serialReceivedFrontLeft.setText(Integer.toString(frontLeft));
			serialReceivedFrontRight.setText(Integer.toString(frontRight));
			serialReceivedLeft.setText(Integer.toString(left));
			serialReceivedRight.setText(Integer.toString(right));
		}
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