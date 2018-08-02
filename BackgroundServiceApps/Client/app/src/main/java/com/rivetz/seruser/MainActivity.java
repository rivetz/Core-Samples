package com.rivetz.seruser;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;


public class MainActivity extends AppCompatActivity {

    boolean isBound = false;
    // Messenger object to send a message to the Bindable Service
    Messenger mMessenger;
    // Messenger object to receive a message from the Bindable Service
    Messenger incomingMessenger;
    // Numerical code for which mathematical operation to use
    int operation;

    /* Takes the incoming message and displays its first argument, which is the result of the calculation */
    private class IncomingHandler extends Handler{

        public void handleMessage(Message msg){
            alert(Integer.toString(msg.arg1));
        }

    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Binds to the Bindable Service
        Intent intent = new Intent();
        intent.setClassName("com.rivetz.bindableservice", "com.rivetz.bindableservice.RemoteService");
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }


    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            isBound = true;
            incomingMessenger = new Messenger(new IncomingHandler());
            // Creates the Messenger object
            mMessenger = new Messenger(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Unbind or process might have crashed
            mMessenger = null;
            isBound = false;
        }
    };

    public void setPlus(View v){
        this.operation = 1;
        alert("Addition selected");
    }

    public void setMinus(View v){
        this.operation = 2;
        alert("Subtraction selected");
    }

    public void setTimes(View v){
        this.operation = 3;
        alert("Multiplication selected");
    }

    public void setDivide(View v){
        this.operation = 4;
        alert("Division selected");
    }

    public void sendMessage(View v){
        if(isBound) {
            EditText first = findViewById(R.id.firstNumber);
            String firstNo = first.getText().toString();
            EditText second = findViewById(R.id.secondNumber);
            String secondNo = second.getText().toString();

            // Create a Message
            Message msg = Message.obtain(null, operation, 0, 0);
            // Sets itself as the messenger to reply to
            msg.replyTo = incomingMessenger;
            // Puts the numbers as Strings into a bundle and attaches it to the message to be sent
            Bundle bundle = new Bundle();
            bundle.putString("firstNo", firstNo);
            bundle.putString("secondNo", secondNo);
            msg.setData(bundle);

            // Send the Message to the Service
            try {
                mMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        else {
            alert("Service is not bound!");
        }

    }

    // Helper Function
    public void alert(String text) {
        new AlertDialog.Builder(this)
                .setMessage(text)
                .create().show();
    }
}
