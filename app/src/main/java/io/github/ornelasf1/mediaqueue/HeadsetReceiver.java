package io.github.ornelasf1.mediaqueue;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Defsin on 6/28/2017.
 */

public class HeadsetReceiver extends BroadcastReceiver {

    public interface HeadsetInterface{
        void checkHeadsetPluggedIn(boolean state);
    }

    private HeadsetInterface mHeadset = null;

    void registerListener(HeadsetInterface headset){
        mHeadset = headset;
    }

    void isHeadsetPluggedIn(boolean state){
        mHeadset.checkHeadsetPluggedIn(state);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        if(intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)){
            int state = intent.getIntExtra("state", -1);
            switch (state) {
                case 0:
                    Log.d("Broadcast Receiver", "Headset unplugged");
                    isHeadsetPluggedIn(false);
                    break;
                case 1:
                    Log.d("Broadcast Receiver", "Headset plugged");
                    isHeadsetPluggedIn(true);
                    break;
            }
        } else if(intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)) {
            Log.d("Broadcast Receiver", "Bluetooth Headset is about to be unplugged");
        }
        else if(intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)){
            Log.d("Broadcast Receiver", "Bluetooth Headset unplugged");
            isHeadsetPluggedIn(false);
        }
    }



}
