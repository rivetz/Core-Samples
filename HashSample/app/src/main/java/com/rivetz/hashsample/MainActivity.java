package com.rivetz.hashsample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
import com.rivetz.api.SPID;
import com.rivetz.api.internal.RivetBase;
import com.rivetz.bridge.Rivet;
import com.rivetz.api.internal.Utilities;

public class MainActivity extends AppCompatActivity {
    Rivet rivet;

    // Creates and pairs a Rivet if necessary
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rivet = new Rivet(this, SPID.DEVELOPER_TOOLS_SPID);
        if (!rivet.isPaired()) {
            rivet.pairDevice(this);
        }
    }

    // Checks if the Rivet was successfully paired
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        notLoading();
        if (requestCode == Rivet.INSTRUCT_PAIRDEVICE) {
            onDevicePairing(resultCode);
        }
    }

    // Takes the given text and hashes it using SHA256
    public void hash(View v){
        EditText dataToBeHashed = findViewById(R.id.dataForHash);
            byte[] hashed = rivet.hash(RivetBase.HASH_SHA256,dataToBeHashed.getText().toString().getBytes());
            alert("This String has been hashed using SHA256: " +Utilities.bytesToHex(hashed));
    }

    public void onDevicePairing(int resultCode){
        if (resultCode == RESULT_CANCELED) {
            alert("Pairing error: " + String.valueOf(resultCode));
        }
        if (resultCode == RESULT_OK) {
            alert("Paired");
        }

        notLoading();
    }

    // Helper functions

    // Creates an alert with some text
    public void alert(String text) {
        new AlertDialog.Builder(this)
                .setMessage(text)
                .create().show();
    }

    // Makes a button unclickable
    public void makeUnclickable(Button button){
        button.setAlpha(.5f);
        button.setClickable(false);
    }

    // Makes a button clickable
    public void makeClickable(Button button){
        button.setAlpha(1f);
        button.setClickable(true);
    }

    // Shows a loading animation
    public void loading(){
        findViewById(R.id.loading).setVisibility(View.VISIBLE);
    }

    // Makes the loading animation invisible
    public void notLoading(){
        findViewById(R.id.loading).setVisibility(View.GONE);
    }
}
