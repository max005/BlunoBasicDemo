package com.example.blunobasicdemo;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by HuangYuChang on 15/5/6.
 */
public class BlunoService extends Service {
    public static final String SerialPortUUID="0000dfb1-0000-1000-8000-00805f9b34fb";
    public static final String CommandUUID="0000dfb2-0000-1000-8000-00805f9b34fb";
    public static final String ModelNumberStringUUID="00002a24-0000-1000-8000-00805f9b34fb";
    private Handler handler = new Handler();
    private Intent transferIntent = new Intent("com.example.blunobasicdemo.RECEIVER_ACTIVITY");
    private Context serviceContext=this;
    private MsgReceiver msgReceiver;
    private ThresholdReceiver thresholdReceiver;
    private DeleteReceiver deleteReceiver;
    private int mBaudrate=115200;
    BluetoothLeService mBluetoothLeService;
    public String connectionState;
    private enum theConnectionState{
        isToScan, isScanning, isConnecting, isConnected, isDisconnecting
    }

    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private static BluetoothGattCharacteristic mSCharacteristic, mModelNumberCharacteristic,
                    mSerialPortCharacteristic, mCommandCharacteristic;

    private String mDeviceAddress;
    private String mPassword="AT+PASSWOR=DFRobot\r\n";
    private String mBaudrateBuffer = "AT+CURRUART="+mBaudrate+"\r\n";

    public boolean mConnected = false;
    private final static String TAG = BlunoService.class.getSimpleName();

    private int front  = 100;
    private int left       = 100;
    private int right      = 100;
    private int frontTemp = 100;
    private int leftTemp = 100;
    private int rightTemp = 100;
    private int frontThreshold = 50;
    private int sidesThreshold = 50;
    //private int postedNotificationCount = 0;
    private theConnectionState mConnectionState;
    protected enum warningState{
        left, right, front, twoSide,frontAndLeft, frontAndRight, allDirection, safe, others
    }
    private warningState mWarningState;
    private String mWarningText;
    private int mWarningCount = 0;
    private boolean turnOff = false;
    private static final int mWarningCountThreshold = 30;
    private Uri soundUri;
    private long[] vibrate = {0, 100, 500, 100, 500};
    //private static final String EXTRA_VOICE_REPLY = "extra_voice_reply";

    private Runnable mConnectingOverTimeRunnable=new Runnable(){

        @Override
        public void run() {
            if(connectionState=="isConnecting") {
                connectionState = "isToScan";
                transferIntent.putExtra("connectionState", connectionState);
                sendBroadcast(transferIntent);
            }
            mConnectionState = theConnectionState.valueOf(connectionState);
            onConectionStateChange(mConnectionState);
            mBluetoothLeService.close();
        }};

    private Runnable mDisonnectingOverTimeRunnable=new Runnable(){

        @Override
        public void run() {
            if(connectionState=="isDisconnecting") {
                transferIntent.putExtra("connectionState", connectionState);
                sendBroadcast(transferIntent);
            }
            mConnectionState = theConnectionState.valueOf(connectionState);
            onConectionStateChange(mConnectionState);
            mBluetoothLeService.close();
        }};

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            System.out.println("mServiceConnection onServiceConnected");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            System.out.println("mServiceConnection onServiceDisconnected");
            mBluetoothLeService = null;
        }
    };

    @Override
    public void onCreate(){
        super.onCreate();
        Log.i("BlunoService", "Create");
        onCreateProcess();
        serialBegin(115200);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        Log.i("BlunoService", "Start");
        onStartProcess();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        Log.i("BlunoService", "Destroy");
        onDestroyProcess();
    }

    @Override
    public IBinder onBind(Intent arg0){
        return null;
    }

    public void onCreateProcess() {
        Intent gattServiceIntent = new Intent(serviceContext, BluetoothLeService.class);
        serviceContext.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        msgReceiver = new MsgReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.example.blunobasicdemo.RECEIVER_SERVICE");
        registerReceiver(msgReceiver, intentFilter);

        thresholdReceiver = new ThresholdReceiver();
        IntentFilter thresholdIntentFilter = new IntentFilter();
        thresholdIntentFilter.addAction("com.example.blunobasicdemo.RECEIVER_THRESHOLD");
        registerReceiver(thresholdReceiver, thresholdIntentFilter);

        deleteReceiver = new DeleteReceiver();
        IntentFilter deleteIntentFilter = new IntentFilter();
        deleteIntentFilter.addAction("com.example.blunobasicdemo.RECEIVER_DELETE");
        registerReceiver(deleteReceiver, deleteIntentFilter);

        System.out.println("BlunoService onCreate");
    }

    public void onStartProcess() {
        System.out.println("BlunoService onStart");
        serviceContext.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    public void onDestroyProcess() {
        System.out.println("BlunoService onDestroy");
        serviceContext.unregisterReceiver(mGattUpdateReceiver);
        connectionState="isToScan";
        transferIntent.putExtra("connectionState", connectionState);
        sendBroadcast(transferIntent);
        mConnectionState = theConnectionState.valueOf(connectionState);
        onConectionStateChange(mConnectionState);
        unregisterReceiver(msgReceiver);
        if(mBluetoothLeService!=null)
        {
            mBluetoothLeService.disconnect();
            handler.removeCallbacks(mDisonnectingOverTimeRunnable);
            mBluetoothLeService.close();
        }
        mSCharacteristic=null;
        serviceContext.unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }


    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @SuppressLint("DefaultLocale")
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            System.out.println("mGattUpdateReceiver->onReceive->action=" + action);
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                handler.removeCallbacks(mConnectingOverTimeRunnable);

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                connectionState = "isToScan";
                transferIntent.putExtra("connectionState", connectionState);
                sendBroadcast(intent);
                mConnectionState = theConnectionState.valueOf(connectionState);
                onConectionStateChange(mConnectionState);
                handler.removeCallbacks(mDisonnectingOverTimeRunnable);
                mBluetoothLeService.close();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                for (BluetoothGattService gattService : mBluetoothLeService.getSupportedGattServices()) {
                    System.out.println("ACTION_GATT_SERVICES_DISCOVERED  "+
                            gattService.getUuid().toString());
                }
                getGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                if(mSCharacteristic==mModelNumberCharacteristic)
                {
                    if (intent.getStringExtra(BluetoothLeService.EXTRA_DATA).toUpperCase().startsWith("DF BLUNO")) {
                        mBluetoothLeService.setCharacteristicNotification(mSCharacteristic, false);
                        mSCharacteristic=mCommandCharacteristic;
                        mSCharacteristic.setValue(mPassword);
                        mBluetoothLeService.writeCharacteristic(mSCharacteristic);
                        mSCharacteristic.setValue(mBaudrateBuffer);
                        mBluetoothLeService.writeCharacteristic(mSCharacteristic);
                        mSCharacteristic=mSerialPortCharacteristic;
                        mBluetoothLeService.setCharacteristicNotification(mSCharacteristic, true);
                        connectionState = "isConnected";
                        transferIntent.putExtra("connectionState", connectionState);
                        sendBroadcast(intent);
                        mConnectionState = theConnectionState.valueOf(connectionState);
                        onConectionStateChange(mConnectionState);
                    }
                    else {
                        Toast.makeText(serviceContext, "Please select DFRobot devices",Toast.LENGTH_SHORT).show();
                        connectionState = "isToScan";
                        transferIntent.putExtra("connectionState", connectionState);
                        sendBroadcast(intent);
                        mConnectionState = theConnectionState.valueOf(connectionState);
                        onConectionStateChange(mConnectionState);
                    }
                }
                else if (mSCharacteristic==mSerialPortCharacteristic) {
                    onSerialReceived(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                }
                System.out.println("displayData "+intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    private void getGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        mModelNumberCharacteristic=null;
        mSerialPortCharacteristic=null;
        mCommandCharacteristic=null;
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();
            System.out.println("displayGattServices + uuid="+uuid);

            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                uuid = gattCharacteristic.getUuid().toString();
                if(uuid.equals(ModelNumberStringUUID)){
                    mModelNumberCharacteristic=gattCharacteristic;
                    System.out.println("mModelNumberCharacteristic  "+mModelNumberCharacteristic.getUuid().toString());
                }
                else if(uuid.equals(SerialPortUUID)){
                    mSerialPortCharacteristic = gattCharacteristic;
                    System.out.println("mSerialPortCharacteristic  "+mSerialPortCharacteristic.getUuid().toString());
//                    updateConnectionState(R.string.comm_establish);
                }
                else if(uuid.equals(CommandUUID)){
                    mCommandCharacteristic = gattCharacteristic;
                    System.out.println("mSerialPortCharacteristic  "+mSerialPortCharacteristic.getUuid().toString());
//                    updateConnectionState(R.string.comm_establish);
                }
            }
            mGattCharacteristics.add(charas);
        }

        if (mModelNumberCharacteristic==null || mSerialPortCharacteristic==null || mCommandCharacteristic==null) {
            Toast.makeText(serviceContext, "Please select DFRobot devices",Toast.LENGTH_SHORT).show();
            connectionState = "isToScan";
            transferIntent.putExtra("connectionState", connectionState);
            sendBroadcast(transferIntent);
            mConnectionState = theConnectionState.valueOf(connectionState);
            onConectionStateChange(mConnectionState);
        }
        else {
            mSCharacteristic=mModelNumberCharacteristic;
            mBluetoothLeService.setCharacteristicNotification(mSCharacteristic, true);
            mBluetoothLeService.readCharacteristic(mSCharacteristic);
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    public void serialBegin(int baud){
        mBaudrate=baud;
        mBaudrateBuffer = "AT+CURRUART="+mBaudrate+"\r\n";
    }

    public void onConectionStateChange(theConnectionState mConnectionState){
        switch (mConnectionState) {											//Four connection state
            case isConnected:
                break;
            case isConnecting:
                break;
            case isToScan:
                break;
            case isScanning:
                break;
            case isDisconnecting:
                break;
            default:
                break;
        }
    }

    public void onSerialReceived(String theString){
        String[] buffer = theString.split(",");
        Log.i("BlunoService", "onSerialRcecived");
        try{
            front  = Integer.parseInt(buffer[0]);
            left   = Integer.parseInt(buffer[1]);
            right  = Integer.parseInt(buffer[2]);
            stateProcess();
            transferIntent.putExtra("front", front);
            transferIntent.putExtra("left", left);
            transferIntent.putExtra("right", right);
            sendBroadcast(transferIntent);
            Log.i("BlunoService", "Warning Count = " + mWarningCount);

        }catch(Exception e){
            Log.e("BlunoService", "[Error onSerialReceived]: "+e.toString());
        }
    }

    public void stateProcess(){
        if(turnOff == true) {
            if(mWarningCount >= mWarningCountThreshold)
                mWarningCount++;
            else
                mWarningCount = mWarningCountThreshold;
        }

        if((left > sidesThreshold && right > sidesThreshold && front > frontThreshold) || mWarningCount >= mWarningCountThreshold){
            mWarningState = warningState.safe;
            //turnOff = true;
            if(mWarningCount >= (mWarningCountThreshold+1) && (Math.abs(left-leftTemp) > 10 || Math.abs(right-rightTemp) > 10 ||
                    Math.abs(front-frontTemp) > 20)){
                mWarningCount = 0;
                turnOff = false;
            }
            leftTemp = left;
            rightTemp = right;
            frontTemp = front;
        }
        else if(left < sidesThreshold && right > sidesThreshold && front > frontThreshold){
            if(mWarningState != warningState.left)
                mWarningCount = 0;
            mWarningState = warningState.left;
            mWarningText = "左方危險, 注意";
            mWarningCount += 1;
            soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.warning_left);
            vibrate = new long[]{0, 100, 500, 100, 500};
            //postNotifications();
            myNotification();
        }
        else if(left > sidesThreshold && right < sidesThreshold && front > frontThreshold){
            if(mWarningState != warningState.right)
                mWarningCount = 0;
            mWarningState = warningState.right;
            mWarningText = "右方危險, 注意";
            soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.warning_right);
            vibrate = new long[]{0, 500, 500, 100, 500};
            mWarningCount += 1;
            //postNotifications();
            myNotification();
        }
        else if(left < sidesThreshold && right < sidesThreshold && front > frontThreshold){
            if(mWarningState != warningState.twoSide)
                mWarningCount = 0;
            mWarningState = warningState.twoSide;
            mWarningText = "兩側危險, 注意";
            mWarningCount += 1;
            soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.warning_twosides);
            vibrate = new long[]{0, 1000, 500, 100, 500};
            //postNotifications();
            myNotification();
        }
        else if(left > sidesThreshold && right > sidesThreshold && front < frontThreshold){
            if(mWarningState != warningState.front)
                mWarningCount = 0;
            mWarningState = warningState.front;
            mWarningText = "前方危險, 注意";
            mWarningCount += 1;
            soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.warning_front);
            vibrate = new long[]{0, 0, 500, 500, 500};
            //postNotifications();
            myNotification();
        }

        else if(left < sidesThreshold && right < sidesThreshold && front < frontThreshold){
            if(mWarningState != warningState.allDirection)
                mWarningCount = 0;
            mWarningState = warningState.allDirection;
            mWarningText = "密集區域, 注意";
            mWarningCount += 1;
            soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.warning);
            vibrate = new long[]{0, 1000, 500, 500, 500};
            //postNotifications();
            myNotification();
        }
        else if(left < sidesThreshold && right > sidesThreshold && front < frontThreshold){
            if(mWarningState != warningState.frontAndLeft)
                mWarningCount = 0;
            mWarningState = warningState.frontAndLeft;
            mWarningText = "前方與左方危險, 注意";
            mWarningCount += 1;
            soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.warning_frontleft);
            vibrate = new long[]{0, 100, 500, 500, 500};
            //postNotifications();
            myNotification();
        }
        else if(left > sidesThreshold && right < sidesThreshold && front < frontThreshold){
            if(mWarningState != warningState.frontAndRight)
                mWarningCount = 0;
            mWarningState = warningState.frontAndRight;
            mWarningText = "前方與右方危險, 注意";
            mWarningCount += 1;
            soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.warning_frontright);
            vibrate = new long[]{0, 500, 500, 500, 500};
            //postNotifications();
            myNotification();
        }
        else{
            if(mWarningState != warningState.others)
                mWarningCount = 0;
            mWarningState = warningState.others;
            mWarningText = "注意";
            mWarningCount += 1;
            soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.warning);
            vibrate = new long[]{0, 1000, 500, 500, 500};
            //postNotifications();
            myNotification();
        }

    }
    /*
    private void postNotifications(){
        sendBroadcast(new Intent(NotificationIntentReceiver.ACTION_ENABLE_MESSAGES)
                .setClass(this, NotificationIntentReceiver.class));

        NotificationPreset preset = NotificationPresets.BASIC;
        CharSequence titlePreset = "危險!";
        CharSequence textPreset =mWarningText;
        PriorityPreset priorityPreset = PriorityPresets.MAX;
        ActionsPreset actionsPreset = ActionsPresets.NO_ACTIONS_PRESET;

        NotificationPreset.BuildOptions options = new NotificationPreset.BuildOptions(
                titlePreset,
                textPreset,
                priorityPreset,
                actionsPreset,
                true,
                false,
                false,
                true
        );

        Notification[] notifications = preset.buildNotifications(this, options);

        // Post new notifications
        for (int i = 0; i < notifications.length; i++) {
            NotificationManagerCompat.from(this).notify(i, notifications[i]);
        }
        // Cancel any that are beyond the current count.
        for (int i = notifications.length; i < postedNotificationCount; i++) {
            NotificationManagerCompat.from(this).cancel(i);
        }
        postedNotificationCount = notifications.length;
    }
    */
   public void myNotification(){
       int notificationId = 001;

       Intent openIntent = new Intent(this, MainActivity.class);
       openIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
       PendingIntent openPendingIntent = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT);

       Intent deleteIntent = new Intent(this, DeleteService.class);
       PendingIntent deletePendingIntent = PendingIntent.getService(this, 0, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT);


       NotificationCompat.WearableExtender wearableExtender =
               new NotificationCompat.WearableExtender()
                       .setHintHideIcon(true)
                       .setBackground(BitmapFactory.decodeResource(getResources(), R.drawable.warning));

       NotificationCompat.Builder notificationBuilder =
               new NotificationCompat.Builder(this)
                       .setSmallIcon(R.drawable.ic_launcher)
                       .setContentTitle("危險！")
                       .setContentText(mWarningText)
                       .setContentIntent(openPendingIntent)
                       .setDeleteIntent(deletePendingIntent)
                       .addAction(R.drawable.ic_full_reply, "Turn Off", deletePendingIntent)
                       .extend(wearableExtender)
                       .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                       .setSound(soundUri)
                       .setVibrate(vibrate);

       // Get an instance of the NotificationManager service
       NotificationManagerCompat notificationManager =
               NotificationManagerCompat.from(this);

       // Build the notification and issues it with notification manager.
       notificationManager.notify(notificationId, notificationBuilder.build());
   }

    public class MsgReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            mDeviceAddress = intent.getStringExtra("mDeviceAddress");
            connectionState = intent.getStringExtra("connectionState");
            Log.d(TAG, "mDeviceAddress"+mDeviceAddress);

            if (mBluetoothLeService.connect(mDeviceAddress)) {
                Log.d(TAG, "Connect request success");
                mConnectionState = theConnectionState.valueOf(connectionState);
                onConectionStateChange(mConnectionState);
                handler.postDelayed(mConnectingOverTimeRunnable, 10000);
            } else {
                Log.d(TAG, "Connect request fail");
                connectionState = "isToScan";
                transferIntent.putExtra("connectionState", connectionState);
                sendBroadcast(intent);
                mConnectionState = theConnectionState.valueOf(connectionState);
                onConectionStateChange(mConnectionState);
            }
        }
    }

    public class ThresholdReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            frontThreshold = intent.getIntExtra("frontThreshold", frontThreshold);
            sidesThreshold = intent.getIntExtra("sidesThreshold", sidesThreshold);
        }
    }

    public class DeleteReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            turnOff = intent.getBooleanExtra("turnOff", turnOff);
        }
    }
}
