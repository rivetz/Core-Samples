package com.rivetz.r_app2;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
import com.rivetz.api.EncryptResult;
import com.rivetz.api.RivetCrypto;
import com.rivetz.api.RivetKeyTypes;
import com.rivetz.api.RivetRules;
import com.rivetz.api.RivetRuntimeException;
import com.rivetz.api.SPID;
import com.rivetz.bridge.RivetWalletActivity;

public class MainActivity extends RivetWalletActivity {
    private RivetCrypto crypto;
    private EncryptResult encryptedText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Starts the Rivet lifecycle with the Activity and sets the UI
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        makeUnclickable(findViewById(R.id.encrypt));
        makeUnclickable(findViewById(R.id.decrypt));
        makeUnclickable(findViewById(R.id.createKey));
        loading();

        // Start the pairing process
        pairDevice(SPID.DEVELOPER_TOOLS_SPID).whenComplete((paired, ex) -> {
            runOnUiThread(() -> {
                notLoading();
            });

            if (paired != null) {

                if (paired.booleanValue()) {

                    try {
                        crypto = getRivetCrypto();
                        if(crypto != null) {
                            runOnUiThread(() -> {
                                onDevicePairing(true);
                            });
                        }
                        else {
                            runOnUiThread(() -> {
                                onDevicePairing(true);
                            });
                        }
                    }

                    catch (RivetRuntimeException e) {
                        runOnUiThread(() -> {
                            alert(e.getError().getMessage());
                        });
                    }

                } else  {
                    runOnUiThread(() -> {
                        onDevicePairing(false);
                    });
                }
            } else {
                // An exception occurred, the device is not paired
                runOnUiThread(() -> {
                    onDevicePairing(false);
                });
            }
        });
    }

    public void onDevicePairing(boolean success){
        if (success) {
            alert("Paired");
            makeClickable(findViewById(R.id.createKey));
        } else {
            alert("Pairing error!");
        }
        notLoading();
    }

    // Encrypts a password using the Rivet
    public void encrypt(View v) {
        EditText payload = findViewById(R.id.payload);
        crypto.encrypt("EncryptKey", payload.getText().toString().getBytes()).whenComplete(this::encryptComplete);
        makeUnclickable(findViewById(R.id.encrypt));
        loading();
    }

    public void encryptComplete(EncryptResult e , Throwable thrown){
        if(thrown == null){
            encryptedText = e;
            runOnUiThread(() ->alert("Your text has been encrypted to " + new String(e.getCipherText())));
            runOnUiThread(() -> makeClickable(findViewById(R.id.decrypt)));
            runOnUiThread(() -> notLoading());
        }
        else {
            alert(thrown.getMessage());
        }
    }

    // Gets the password corresponding to the place the user has entered and decrypts it
    public void decrypt(View v) {
        crypto.decrypt("EncryptKey", encryptedText).whenComplete(this::decryptComplete);
        makeUnclickable(findViewById(R.id.decrypt));
        loading();
    }

    // Asynchronous callback for when decrypting a password is complete
    public void decryptComplete(byte[] decrypted, Throwable thrown) {
        if (decrypted != null) {
            runOnUiThread(() -> alert("Your text has been decrypted: " + new String(decrypted)));
            runOnUiThread(() -> makeClickable(findViewById(R.id.encrypt)));
        }
        else {
            alert(thrown.getMessage());
        }
        notLoading();
    }

    // Create a key asynchronously
    public void createKey(View v) {
        try {
            //Ensure the key doesn't already exist
            crypto.deleteKey("EncryptKey").get();
            // Create the key
            crypto.createKey("EncryptKey", RivetKeyTypes.AES256_CGM, new RivetRules[0]).whenComplete(this::createKeyComplete);
            loading();
        }
        catch (Exception e) {
            alert(e.getMessage());
        }
    }

    // Callback for when Key creation is complete
    public void createKeyComplete(Void v, Throwable thrown){
        if(thrown == null) {
            runOnUiThread(() -> {
                alert("Key successfully created");
                makeClickable(findViewById(R.id.encrypt));
                makeUnclickable(findViewById(R.id.createKey));

            });
        }
        else {
            runOnUiThread(() -> alert(thrown.getMessage()));
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
