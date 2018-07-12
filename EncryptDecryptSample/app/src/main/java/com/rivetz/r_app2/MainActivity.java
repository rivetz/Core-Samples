package com.rivetz.r_app2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;

import com.rivetz.api.KeyRecord;
import com.rivetz.api.SPID;
import com.rivetz.api.internal.Utilities;
import com.rivetz.bridge.Rivet;
import com.rivetz.api.RivetInterface;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Optional;

public class MainActivity extends AppCompatActivity {
    Rivet rivet;
    HashMap<String, byte[]> mapOfPasswords;

    // Creates and pairs a Rivet if necessary
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapOfPasswords = new HashMap<>();
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

    // Encrypts a password using the Rivet and saves it to a hashmap where the place is the key
    public void encryptAndSave(View v) {
        EditText pass = findViewById(R.id.password);
        EditText place = findViewById(R.id.place);
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        byte[] encrypted = rivet.encrypt("EncryptKey", bytes, pass.getText().toString().getBytes());
        mapOfPasswords.put(place.getText().toString(), encrypted);
        if (mapOfPasswords.containsValue(encrypted)) {
            alert("Successfully encrypted and saved the password for " + place.getText() + ". It has been encrypted to " + Utilities.bytesToHex(encrypted));
        }
        makeUnclickable(findViewById(R.id.encryptAndSave));
        makeClickable(findViewById(R.id.getPassword));
    }

    // Asynchronous callback for when decrypting a password is complete
    public void decryptComplete(byte[] decrypted, Throwable thrown) {
        if (decrypted != null) {
            String password = new String(decrypted);
            runOnUiThread(() -> alert("Your password is: " + password));
        }
        else {
            alert(thrown.getMessage());
        }
        notLoading();
    }

    // Gets the password corresponding to the place the user has entered and decrypts it
    public void getPassword(View v) {
        EditText place = findViewById(R.id.place);
        byte[] encryptedPassword = mapOfPasswords.get(place.getText().toString());
        rivet.decryptAsync("EncryptKey", encryptedPassword).whenComplete(this::decryptComplete);
        loading();
    }
    //make clickable after key creation the other 2 buttons
    public void createKey(View v) {
        try {
            rivet.createKeyAsync(RivetInterface.KeyType.AES_256_GCM, "EncryptKey", RivetInterface.UsageRule.REQUIRE_TUI_PIN).whenComplete(this::createKeyComplete);
            loading();
        }
        catch (Exception e) {
            alert(e.getMessage());
        }
    }

    // Callback for when Key creation is complete
    public void createKeyComplete(KeyRecord key, Throwable thrown){
        if(thrown == null){
            runOnUiThread(() ->alert("Key successfully created"));
            runOnUiThread(() -> makeClickable(findViewById(R.id.encryptAndSave)));
            runOnUiThread(() -> makeClickable(findViewById(R.id.getPassword)));
            runOnUiThread(() -> makeUnclickable(findViewById(R.id.createkey)));
        }
        else {
            runOnUiThread(() -> alert(thrown.getMessage()));
        }
        runOnUiThread(() -> notLoading());
    }

    public void onDevicePairing(int resultCode){
        if (resultCode == RESULT_CANCELED) {
            alert("Pairing error: " + String.valueOf(resultCode));
        }
        if (resultCode == RESULT_OK) {
            alert("Paired");
            rivet.getKeyAsync("EncryptKey").whenComplete(this::checkKeyComplete);
            loading();
        }
    }

    // Callback for when checking if a key exists is done
    public void checkKeyComplete(Optional<KeyRecord> key, Throwable thrown){
        if(thrown == null){
            if(!key.isPresent()){
                runOnUiThread(() -> makeUnclickable(findViewById(R.id.encryptAndSave)));
                runOnUiThread(() -> makeUnclickable(findViewById(R.id.getPassword)));
            }
            else {
                runOnUiThread(() -> makeUnclickable(findViewById(R.id.createkey)));
                runOnUiThread(() -> makeUnclickable(findViewById(R.id.getPassword)));
            }
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
