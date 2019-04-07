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
package com.rivetz.signingsample;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.rivetz.api.RivetHashTypes;
import com.rivetz.api.RivetKeyTypes;
import com.rivetz.api.RivetRules;
import com.rivetz.api.RivetRuntimeException;
import com.rivetz.api.SPID;
import com.rivetz.api.RivetCrypto;
import com.rivetz.api.Signature;
import com.rivetz.bridge.DevicePropertyIds;
import com.rivetz.bridge.RivetApiActivity;

import java.util.List;

import static com.rivetz.api.RivetRules.REQUIRE_DUAL_ROOT;


public class MainActivity extends RivetApiActivity {
    private RivetCrypto crypto;
    private Signature signature;
    private String message;
    private static String drtSupported;

    @Override
    public void onCreate(@NonNull Bundle savedInstanceState) {
        // Starts the Rivet lifecycle with the Activity and sets the UI
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        makeUnclickable(findViewById(R.id.checkAuthenticity));
        makeUnclickable(findViewById(R.id.sign));
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
    private void getPropertyComplete(@Nullable String result, @Nullable Throwable throwable) {
        if (throwable == null) {
            drtSupported = result;
            if (result.equals("true"))
                runOnUiThread(() -> alert("Paired, DRT supported"));
            else
                runOnUiThread(() -> alert("Paired, DRT not supported"));
            makeClickable(findViewById(R.id.createKey));

        } else {
            runOnUiThread(() -> alert("Failed to get DRT properties"));
        }
    }

    public void onDevicePairing(boolean success){
        if (success) {
            // Check if DRT is supported
            crypto.getDeviceProperty(DevicePropertyIds.DRT_SUPPORTED.toString()).
                    whenComplete(this::getPropertyComplete);
        } else {
            alert("Pairing error!");
        }
    }

    // Signs the real message asynchronously
    public void sign(@NonNull View v) {
        EditText real = findViewById(R.id.real);
        message = real.getText().toString();
        crypto.sign("SigningKey", message.getBytes(), RivetHashTypes.SHA256).whenComplete(this::signComplete);
        makeUnclickable(findViewById(R.id.sign));
        loading();
    }

    // Callback when the real message is done signing which checks if the signing was successful
    public void signComplete(@Nullable Signature s, @Nullable Throwable thrown) {
        if (thrown == null) {
            signature = s;
            runOnUiThread(() -> alert("Signing complete"));
        }
        else{
            alert("Error signing:" + thrown.getMessage());
        }
        notLoading();

    }

    // Verifies if the real message is authentic asynchronously using its signature
    public void checkAuthenticity(@NonNull View v){
        EditText real = findViewById(R.id.real);
        if(signature == null){
            alert("Both messages are fake!");
        }
        else {
            crypto.verify("SigningKey", message.getBytes(), RivetHashTypes.SHA256, signature).whenComplete(this::verifyComplete);
            loading();
        }
    }

    // Callback for when the verification is done which checks if the message is authentic or not and returns an alert accordingly
    public void verifyComplete(@Nullable Boolean validity,@Nullable Throwable thrown){
        if(thrown == null){
            if(validity){
                EditText real = findViewById(R.id.real);
                runOnUiThread(() ->alert("The message " + "'" + message + "'" + " is authentic, the other is fake!"));
            }
            if(!validity){
                alert("Both messages are fake!");
            }
        }
        else alert(thrown.getMessage());
        notLoading();
    }

    // Check the key's existence asynchronously by generating a descriptor for it
    // before creating it
    public void checkKeyExistence(@NonNull View v) {
        try {
            crypto.getKeyNamesOf(RivetKeyTypes.NISTP256).whenComplete(this::checkKeyExistenceComplete);
            loading();
        }
        catch (Exception e) {
            alert(e.getMessage());
        }
    }

    // Callback for when Key existence checking is complete
    public void checkKeyExistenceComplete(@Nullable List<String> keyNames,@Nullable Throwable thrown){
        if(thrown == null) {
            if (keyNames.contains("SigningKey")) {
                runOnUiThread(() ->{
                    alert("Key already exists");
                    makeClickable(findViewById(R.id.checkAuthenticity));
                    makeClickable(findViewById(R.id.sign));
                    makeUnclickable(findViewById(R.id.createKey));
                    notLoading();
                });
            }
            else {
                createKey();
            }

        }
        else{
            runOnUiThread(() -> alert(thrown.getMessage()));
        }
    }

    // Creates a Key asynchronously
    public void createKey() {
        try {
            // If DRT supported add Dual Root of Trust usage rule
            RivetRules rules[] = null;
            if (drtSupported.equals("true")) {
                rules = new RivetRules[1];
                rules[0] = REQUIRE_DUAL_ROOT;
            }
            // Create the key
            crypto.createKey("SigningKey", RivetKeyTypes.NISTP256, rules).whenComplete(this::createKeyComplete);
        }

        catch (Exception e){
            runOnUiThread(() -> alert(e.getMessage()));
        }
    }

    public void createKeyComplete(Void v,@Nullable Throwable thrown){
        if(thrown == null){
            runOnUiThread(() ->{
                alert("Key successfully created");
                makeClickable(findViewById(R.id.checkAuthenticity));
                makeClickable(findViewById(R.id.sign));
                makeUnclickable(findViewById(R.id.createKey));
            });
        }
        else {
            runOnUiThread(() -> alert(thrown.getMessage()));
        }
        runOnUiThread(() -> notLoading());
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
