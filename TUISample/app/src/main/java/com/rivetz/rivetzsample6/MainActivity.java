package com.rivetz.rivetzsample6;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.rivetz.api.KeyRecord;
import com.rivetz.api.RivetInterface;
import com.rivetz.api.SPID;
import com.rivetz.bridge.Rivet;
import com.rivetz.api.internal.Utilities;

import java.util.Optional;


public class MainActivity extends AppCompatActivity {
    Rivet rivet;
    String keyName;
    RivetInterface.UsageRule usageRule;

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

    public void onDevicePairing(int resultCode){
        if (resultCode == RESULT_CANCELED) {
            alert("Pairing error: " + String.valueOf(resultCode));
        }
        if (resultCode == RESULT_OK) {
            alert("Paired");
            if(!rivet.getCapabilities().hasTUI()){
                alert("Unfortunately this app will not work on your phone because it does not support TUI");
            }

        }

        notLoading();
    }

    // Creates/Sets the TUI Pin Key
    public void createKeyConfirm(View v){
        keyName = "ConfirmKey";
        usageRule = RivetInterface.UsageRule.REQUIRE_TUI_CONFIRM;
        loading();
        rivet.getKeyAsync(keyName).whenComplete(this::verifyExistenceComplete);
    }

    // Creates/Sets the TUI Pin Key
    public void createKeyPin(View v) {
        keyName = "PinKey";
        usageRule = RivetInterface.UsageRule.REQUIRE_TUI_PIN;
        loading();
        rivet.getKeyAsync(keyName).whenComplete(this::verifyExistenceComplete);
    }

    // Callback for when verification of the keys existence is complete
    public void verifyExistenceComplete(Optional<KeyRecord> key, Throwable thown){
        if(key.isPresent()){
            runOnUiThread(() -> notLoading());
        }
        // If the requested Key does not exist yet, it is created here
        else {
            try {
                rivet.createKeyAsync(RivetInterface.KeyType.ECDSA_NISTP256, keyName, usageRule).whenComplete(this::createKeyComplete);
            }
            catch (Exception e){
                runOnUiThread(() -> alert(e.getMessage()));
            }
        }
    }

    // Callback when the key is done creating
    public void createKeyComplete(KeyRecord key, Throwable thrown){
        if(thrown != null){
            runOnUiThread(() -> alert(thrown.getMessage()));
        }
        runOnUiThread(() -> notLoading());
    }

    //Encrypts the text in the box asynchronously
    public void encrypt(View v){
        loading();
        EditText textToEncrypt = findViewById(R.id.encryptText);
        rivet.encryptAsync(keyName,textToEncrypt.getText().toString().getBytes())
                .whenComplete(this::encryptComplete);

    }
    // Callback for when encryption is complete
    public void encryptComplete(byte[] encrypted, Throwable thrown){
        if(thrown == null){
            runOnUiThread(() -> alert("Encryption complete: " + Utilities.bytesToHex(encrypted)));
        }
        else {
            runOnUiThread(() -> alert(thrown.getMessage()));
        }
        runOnUiThread(() -> notLoading());
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
