/*
 * Copyright (c) 2019 Rivetz Corp.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of RIVETZ CORP. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.rivetz.TUISample;

import java.util.List;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.rivetz.api.EncryptResult;
import com.rivetz.api.RivetCrypto;
import com.rivetz.api.RivetKeyTypes;
import com.rivetz.api.RivetRules;
import com.rivetz.api.RivetRuntimeException;
import com.rivetz.api.SPID;
import com.rivetz.bridge.DevicePropertyIds;
import com.rivetz.bridge.RivetAndroid;
import com.rivetz.bridge.RivetApiActivity;
import com.rivetz.TUISample.R;

import static com.rivetz.api.RivetRules.REQUIRE_DUAL_ROOT;
import static com.rivetz.api.RivetRules.REQUIRE_TUI_CONFIRM;

public class MainActivity extends RivetApiActivity {
    private RivetCrypto crypto;
    private EncryptResult encryptedText;
    private static String drtSupported;


    @Override
    public void onCreate(@NonNull Bundle savedInstanceState) {

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
            crypto.getDeviceProperty(DevicePropertyIds.DRT_SUPPORTED.toString()).
                    whenComplete(this::getPropertyComplete);
        } else {
            alert("Pairing error!");
        }
        notLoading();
    }

    // Encrypts a password using the Rivet
    public void encrypt(@NonNull View v) {
        EditText payload = findViewById(R.id.encryptText);
        crypto.encrypt("EncryptKey", payload.getText().toString().getBytes()).whenComplete(this::encryptComplete);
        loading();
    }

    public void encryptComplete(@Nullable EncryptResult e ,@Nullable Throwable thrown){
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
    public void decrypt(@NonNull View v) {
        crypto.decrypt("EncryptKey", encryptedText).whenComplete(this::decryptComplete);
        makeUnclickable(findViewById(R.id.encryptText));
        loading();
    }

    // Asynchronous callback for when decrypting a password is complete
    public void decryptComplete(@Nullable byte[] decrypted,@Nullable Throwable thrown) {
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
    public void checkKeyExistence(@NonNull View v) {
        try {
            crypto.getKeyNamesOf(RivetKeyTypes.AES256_CGM).whenComplete(this::checkKeyExistenceComplete);
            loading();
        }
        catch (Exception e) {
            alert(e.getMessage());
        }
    }

    // Callback for when Key existence checking is complete
    public void checkKeyExistenceComplete(@Nullable List<String> keyNames,@Nullable Throwable thrown){
        if(thrown == null) {
            // Check if the key is in the list if key names
            if (keyNames.contains("EncryptKey")) {
                runOnUiThread(() -> {
                    alert("Key already exists");
                    makeUnclickable(findViewById(R.id.createkey));
                    makeClickable(findViewById(R.id.encrypt));
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
    public void createKeyComplete(Void v,@Nullable Throwable thrown){
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
    public void alert(@NonNull String text) {
        new AlertDialog.Builder(this)
                .setMessage(text)
                .create().show();
    }

    // Makes a button unclickable
    public void makeUnclickable(@NonNull Button button){
        button.setAlpha(.5f);
        button.setClickable(false);
    }

    // Makes a button clickable
    public void makeClickable(@NonNull Button button){
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
