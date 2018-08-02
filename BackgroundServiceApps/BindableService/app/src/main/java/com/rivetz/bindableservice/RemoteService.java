package com.rivetz.bindableservice;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class RemoteService extends Service {

    /* Not only this code is neccessary for the service to work, but also a mention
    in the Android Manifest of the service */

    private String TAG = "RemoteService";

    //Indicates if the service is bound to the Calculator Service
    boolean isBound;

    // Messenger object to send messages to IncomingHandler
    Messenger icomingMessenger = new Messenger(new IncomingHandler());
    // Messenger object to forward the converted numbers to the calculator
    Messenger fwrdMessenger;
    // Messenger object to forward the results of the calculator to the Client
    Messenger clientMessenger;

    // Incoming message Handler
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                /* Forwards the numbers & mathematical operation to the Calculator for case 1,2,3,4 and
                saves the clientMessenger for later (when it will need to forward the Calculators reply to the client)*/
                case 1:
                    clientMessenger = msg.replyTo;
                    fwrdMessage(msg,1);
                    break;
                case 2:
                    clientMessenger = msg.replyTo;
                    fwrdMessage(msg,2);
                    break;

                case 3:
                    clientMessenger = msg.replyTo;
                    fwrdMessage(msg,3);
                    break;

                case 4:
                    clientMessenger = msg.replyTo;
                    fwrdMessage(msg,4);
                    break;

                case 5:
                    // Case 5 means a reply from the calculator, so it sends the result to the Client

                    Message forward = Message.obtain(null,1,msg.arg1,0);
                    try {
                        clientMessenger.send(forward);
                    }
                    catch (RemoteException e){
                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

    // Function for forwarding messages to the Calculator
    public void fwrdMessage(Message msg,int Case){
        // Gets the String data out of the bundle attached to the message and parses it to integers
        Bundle bundle = msg.getData();
        String firstNo = (String) bundle.get("firstNo");
        int first = Integer.parseInt(firstNo);
        String secondNo = (String) bundle.get("secondNo");
        int second = Integer.parseInt(secondNo);
        // Creates a new message and attaches the numbers and case
        Message forward = Message.obtain(null,Case,first,second);
        // Sets itself as the messenger to reply to
        forward.replyTo = icomingMessenger;
        // Trys to send the message and catches error
        try {
            fwrdMessenger.send(forward);
        }
        catch (RemoteException e){
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Constructor
    public RemoteService() {
    }

    @Override
    public void onCreate() {
        // Binds to the CalcService upon creation
        Intent intent = new Intent();
        intent.setClassName("com.rivetz.calcservice", "com.rivetz.calcservice.CalcService");
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "onCreate called");
    }


    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            isBound = true;
            // Creates the Messenger object
            fwrdMessenger = new Messenger(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Unbind or process might have crashed
            fwrdMessenger = null;
            isBound = false;
        }
    };


    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind done");
        // Returns the Messenger interface for sending messages to the service
        return icomingMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "unbinding done");
        return false;
    }

}