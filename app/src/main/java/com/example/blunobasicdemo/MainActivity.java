package com.example.blunobasicdemo;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.List;

public class MainActivity  extends BlunoLibrary {
	private Button buttonScan;
	private Button buttonDisconnect;
	private TextView serialReceivedFront;
	private TextView serialReceivedLeft;
	private TextView serialReceivedRight;
	private SeekBar frontThresholdBar;
	private EditText frontThresholdValue;
	private SeekBar sidesThresholdBar;
	private EditText sidesThresholdValue;
	private MsgReceiver msgReceiver;
	private Intent transferIntent = new Intent("com.example.blunobasicdemo.RECEIVER_SERVICE");
    private Intent thresholdIntent = new Intent("com.example.blunobasicdemo.RECEIVER_THRESHOLD");

	private int front  = 0;
	private int left   = 0;
	private int right  = 0;
    private static int thresholdMin = 0;
	private static int thresholdMax = 350;
    private int frontThreshold = 100;
    private int sidesThreshold = 100;
	private boolean editToSeekbar = false;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		onCreateProcess();
        findView();
		setButton();
        setSeekBar();
		setEditText();

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

    public void findView(){
        buttonScan=(Button) findViewById(R.id.buttonScan);
        buttonDisconnect=(Button) findViewById(R.id.buttonDisconnect);
        serialReceivedFront=(TextView) findViewById(R.id.Front);
        serialReceivedLeft=(TextView) findViewById(R.id.Left);
        serialReceivedRight=(TextView) findViewById(R.id.Right);
        frontThresholdBar=(SeekBar) findViewById(R.id.frontThresholdBar);
        frontThresholdValue=(EditText) findViewById(R.id.frontThreshold);
        sidesThresholdBar=(SeekBar) findViewById(R.id.sidesThresholdBar);
        sidesThresholdValue=(EditText) findViewById(R.id.sidesThreshold);
    }

    public void setButton(){
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
                if (isServiceRunning(getApplicationContext(), "com.example.blunobasicdemo.BlunoService")) {
                    Intent intent = new Intent(MainActivity.this, BlunoService.class);
                    stopService(intent);                                        //Alert Dialog for selecting the BLE device
                    mConnectionState = theConnectionState.isToScan;
                    onConectionStateChange(mConnectionState);
                }
            }
        });
    }

    public void setSeekBar(){
        frontThresholdBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				frontThreshold = progress + thresholdMin;
				if (!editToSeekbar)
					frontThresholdValue.setText(String.valueOf(frontThreshold));
				thresholdIntent.putExtra("frontThreshold", frontThreshold);
				thresholdIntent.putExtra("sidesThreshold", sidesThreshold);
				sendBroadcast(thresholdIntent);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});

        sidesThresholdBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				sidesThreshold = progress + thresholdMin;
				if (!editToSeekbar)
					sidesThresholdValue.setText(String.valueOf(sidesThreshold));
				thresholdIntent.putExtra("frontThreshold", frontThreshold);
				thresholdIntent.putExtra("sidesThreshold", sidesThreshold);
				sendBroadcast(thresholdIntent);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});

    }

	public void setEditText(){
		frontThresholdValue.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				try {
					editToSeekbar = true;
					if (Integer.parseInt(s.toString()) > thresholdMax) {
						frontThresholdBar.setProgress(thresholdMax);
						frontThresholdValue.setText(String.valueOf(thresholdMax));
					} else
						frontThresholdBar.setProgress(Integer.parseInt(s.toString()));
					editToSeekbar = false;
				} catch (Exception ex) {
				}
			}
		});

		sidesThresholdValue.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				try {
					editToSeekbar = true;
					if (Integer.parseInt(s.toString()) > thresholdMax) {
						sidesThresholdBar.setProgress(thresholdMax);
						sidesThresholdValue.setText(String.valueOf(thresholdMax));
					} else
						sidesThresholdBar.setProgress(Integer.parseInt(s.toString()));
					editToSeekbar = false;
				} catch (Exception ex) {
				}
			}
		});
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
                transferIntent.putExtra("mDeviceAddress", mDeviceAddress);
				transferIntent.putExtra("connectionState", connectionState);
                thresholdIntent.putExtra("frontThreshold", frontThreshold);
                thresholdIntent.putExtra("sidesThreshold", sidesThreshold);
				sendBroadcast(transferIntent);
                sendBroadcast(thresholdIntent);
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