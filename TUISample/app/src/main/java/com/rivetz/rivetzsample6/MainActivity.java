/*******************************************************************************
 *
 * RIVETZ CORP. CONFIDENTIAL
 *__________________________
 *
 * Copyright (c) 2018 Rivetz Corp.
 * All Rights Reserved.
 *
 * All information and intellectual concepts contained herein is, and remains,
 * the property of Rivetz Corp and its suppliers, if any.  Dissemination of this
 * information or reproduction of this material, or any facsimile, is strictly
 * forbidden unless prior written permission is obtained from Rivetz Corp.
 ******************************************************************************/

package com.rivetz.rivetzsample6;

import java.util.List;
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
import com.rivetz.bridge.DevicePropertyIds;
import com.rivetz.bridge.RivetAndroid;
import com.rivetz.bridge.RivetWalletActivity;
import com.rivetz.rivetzsample6.R;

import static com.rivetz.api.RivetRules.REQUIRE_DUAL_ROOT;
import static com.rivetz.api.RivetRules.REQUIRE_TUI_CONFIRM;
import static com.rivetz.api.RivetRules.REQUIRE_TUI_PIN;

public class MainActivity extends RivetWalletActivity {
    private RivetCrypto crypto;
    private EncryptResult encryptedText;
    private static String drtSupported;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Starts the Rivet lifecycle with the Activity and sets the UI
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        makeUnclickable(findViewById(R.id.createkey));
        makeUnclickable(findViewById(R.id.encrypt));
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
                                onDevicePairing(false);
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

    /**
     * Called when getDeviceProperties completes
     *
     * @param result String result of the properties call
     * @param throwable if not null, the exception
     */
    private void getPropertyComplete(String result, Throwable throwable) {
        if (throwable == null) {
            drtSupported = result;
            if (result.equals("true"))
                runOnUiThread(() -> alert("Paired, DRT supported"));
            else
                runOnUiThread(() -> alert("Paired, DRT not supported"));
            makeClickable(findViewById(R.id.createkey));

        } else {
            runOnUiThread(() -> alert("Failed to get DRT properties"));
        }
    }

    public void onDevicePairing(boolean success){
        if (success) {
            // Check if DRT is supported
            RivetAndroid rivet = (RivetAndroid)crypto;
            rivet.getDeviceProperty(DevicePropertyIds.DRT_SUPPORTED).
                    whenComplete(this::getPropertyComplete);
        } else {
            alert("Pairing error!");
        }
        notLoading();
    }

    // Encrypts a password using the Rivet
    public void encrypt(View v) {
        EditText payload = findViewById(R.id.encryptText);
        crypto.encrypt("EncryptKey", payload.getText().toString().getBytes()).whenComplete(this::encryptComplete);
        makeUnclickable(findViewById(R.id.encryptText));
        loading();
    }

    public void encryptComplete(EncryptResult e , Throwable thrown){
        if(thrown == null){
            encryptedText = e;
            runOnUiThread(() ->alert("Your text has been encrypted to " + new String(e.getCipherText())));
            runOnUiThread(() -> notLoading());
        }
        else {
            alert(thrown.getMessage());
        }
    }

    // Gets the password corresponding to the place the user has entered and decrypts it
    public void decrypt(View v) {
        crypto.decrypt("EncryptKey", encryptedText).whenComplete(this::decryptComplete);
        makeUnclickable(findViewById(R.id.encryptText));
        loading();
    }

    // Asynchronous callback for when decrypting a password is complete
    public void decryptComplete(byte[] decrypted, Throwable thrown) {
        if (decrypted != null) {
            runOnUiThread(() -> alert("Your text has been decrypted: " + new String(decrypted)));
            runOnUiThread(() -> makeClickable(findViewById(R.id.encryptText)));
        }
        else {
            alert(thrown.getMessage());
        }
        notLoading();
    }

    // Check the key's existence asynchronously by generating a descriptor for it
    // before creating it
    public void checkKeyExistence(View v) {
        try {
            crypto.getKeyNamesOf(RivetKeyTypes.AES256_CGM).whenComplete(this::checkKeyExistenceComplete);
            loading();
        }
        catch (Exception e) {
            alert(e.getMessage());
        }
    }

    // Callback for when Key existence checking is complete
    public void checkKeyExistenceComplete(List<String> keyNames, Throwable thrown){
        if(thrown == null) {
            // Check if the key is in the list if key names
            if (keyNames.contains("EncryptKey")) {
                runOnUiThread(() -> {
                    alert("Key already exists");
                    makeUnclickable(findViewById(R.id.createkey));
                    notLoading();
                });
            }
            else {
                createKey();
            }
        }
        else {
            runOnUiThread(() -> alert(thrown.getMessage()));
        }
    }

    // Create a key asynchronously
    public void createKey() {
        try {
            // If DRT supported add Dual Root of Trust usage rule
            RivetRules rules[];
            if (drtSupported.equals("true")) {
                rules = new RivetRules[2];
                rules[0] = REQUIRE_DUAL_ROOT;
                rules[1] = REQUIRE_TUI_CONFIRM;
            } else {
                rules = new RivetRules[1];
                rules[0] = REQUIRE_TUI_CONFIRM;
            }
            // Create the key
            crypto.createKey("EncryptKey", RivetKeyTypes.AES256_CGM, rules).whenComplete(this::createKeyComplete);
        }
        catch (Exception e) {
            runOnUiThread(() -> alert(e.getMessage()));
        }
    }

    // Callback for when Key creation is complete
    public void createKeyComplete(Void v, Throwable thrown){
        if(thrown == null) {
            runOnUiThread(() -> {
                alert("Key successfully created");
                makeClickable(findViewById(R.id.encrypt));
                makeUnclickable(findViewById(R.id.createkey));

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
