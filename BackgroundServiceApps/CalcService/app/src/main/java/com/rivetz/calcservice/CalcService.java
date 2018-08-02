package com.rivetz.calcservice;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class CalcService extends Service {


    /* Not only this code is neccessary for the service to work, but also a mention
    in the Android Manifest of the service */

    private String TAG = "CalcService";

    // Messenger object used by clients to send messages to IncomingHandler
    Messenger mMessenger = new Messenger(new IncomingHandler());
    Messenger replyTo;

    // Incoming messages Handler
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            replyTo = msg.replyTo;
            switch (msg.what) {

                // Different cases on what mathematical operation to perform
                case 1:
                    int sum = msg.arg1 + msg.arg2;
                    reply(sum);
                    break;

                case 2:
                    int difference = msg.arg1 - msg.arg2;
                    reply(difference);
                    break;

                case 3:
                    int product = msg.arg1 * msg.arg2;
                    reply(product);
                    break;

                case 4:
                    int quotient = msg.arg1 / msg.arg2;
                    reply(quotient);
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

    public CalcService() {
    }


    public void reply(int result){
        Message reply = Message.obtain(null,5,result,0);
        try {
            replyTo.send(reply);
        }
        catch (RemoteException e){
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate called");
    }


    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind done");
        // Returns the Messenger interface for sending messages to the service
        return mMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "unbinding done");
        return false;
    }

}