package com.rivetz.rivetsample5;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import com.rivetz.api.KeyRecord;
import com.rivetz.api.RivetInterface;
import com.rivetz.api.SPID;
import com.rivetz.api.internal.Utilities;
import com.rivetz.bridge.Rivet;

import org.slf4j.helpers.Util;

import java.security.Key;
import java.util.Optional;

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
        if (requestCode == Rivet.INSTRUCT_PAIRDEVICE) {
            onDevicePairing(resultCode);
        }
    }

    // Creates a Key asynchronously
    public void createKey(View v) {
        try {
            loading();
            rivet.createKeyAsync(RivetInterface.KeyType.ECDSA_NISTP256, "MyKey", RivetInterface.UsageRule.PRIV_KEY_EXPORTABLE).whenComplete(this::createKeyComplete);
        }
        catch (Exception e){
            alert(e.getMessage());
        }
    }

    // Callback when the key is done creating
    public void createKeyComplete(KeyRecord key, Throwable thrown){
        if(thrown == null){
            runOnUiThread(() -> alert("Key successfully created"));
        }
        else {
            runOnUiThread(() -> alert(thrown.getMessage()));
        }
        runOnUiThread(() -> notLoading());
    }

    // Deletes a key asynchronously
    public void deleteKey(View v) {
        loading();
        try {
            rivet.deleteKeyAsync("MyKey").whenComplete(this::deleteKeyComplete);
        } catch (Exception e) {
            alert(e.getMessage());
        }
    }

    // Callback when the key is done deleting
    public void deleteKeyComplete(Void v, Throwable thrown){
        if(thrown == null){
            runOnUiThread(() ->alert("Key successfully deleted"));
        }
        else {
            runOnUiThread(() -> alert(thrown.getMessage()));
        }
        notLoading();
    }

    // Gets the KeyRecord for the Key asynchronously
    public void export(View v){
        rivet.getKeyAsync("MyKey").whenComplete(this::exportComplete);
        loading();
    }

    // Callback for when the export function is complete which returns the Public Key and Private Key
    public void exportComplete(Optional<KeyRecord> key, Throwable thrown){
        if(thrown == null){
            if(key.isPresent()) {
                runOnUiThread(() -> alert("Your public key is: " +Utilities.bytesToHex(key.get().getPublicKeyBytes())));
                runOnUiThread(() -> alert("Your private key is: " +Utilities.bytesToHex(key.get().privateKey)));
            }
            else runOnUiThread(() -> alert("Key not found"));
        }
        runOnUiThread(()->notLoading());
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
