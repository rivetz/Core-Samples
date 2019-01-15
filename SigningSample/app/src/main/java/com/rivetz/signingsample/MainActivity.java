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

package com.rivetz.signingsample;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;

import com.rivetz.api.RivetHashTypes;
import com.rivetz.api.RivetKeyTypes;
import com.rivetz.api.RivetRules;
import com.rivetz.api.RivetRuntimeException;
import com.rivetz.api.SPID;
import com.rivetz.api.RivetCrypto;
import com.rivetz.api.Signature;
import com.rivetz.bridge.DevicePropertyIds;
import com.rivetz.bridge.RivetAndroid;
import com.rivetz.bridge.RivetWalletActivity;
import com.rivetz.api.RivetKeyDescriptor;

import java.util.List;

import static com.rivetz.api.RivetRules.REQUIRE_DUAL_ROOT;


public class MainActivity extends RivetWalletActivity {
    private RivetCrypto crypto;
    private Signature signature;
    private String message;
    private static String drtSupported;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
    private void getPropertyComplete(String result, Throwable throwable) {
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
            RivetAndroid rivet = (RivetAndroid)crypto;
            rivet.getDeviceProperty(DevicePropertyIds.DRT_SUPPORTED).
                    whenComplete(this::getPropertyComplete);
        } else {
            alert("Pairing error!");
        }
    }

    // Signs the real message asynchronously
    public void sign(View v) {
        EditText real = findViewById(R.id.real);
        message = real.getText().toString();
        crypto.sign("SigningKey", message.getBytes(), RivetHashTypes.SHA256).whenComplete(this::signComplete);
        makeUnclickable(findViewById(R.id.sign));
        loading();
    }

    // Callback when the real message is done signing which checks if the signing was successful
    public void signComplete(Signature s, Throwable thrown) {
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
    public void checkAuthenticity(View v){
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
    public void verifyComplete(Boolean validity, Throwable thrown){
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
    public void checkKeyExistence(View v) {
        try {
            crypto.getKeyNamesOf(RivetKeyTypes.NISTP256).whenComplete(this::checkKeyExistenceComplete);
            loading();
        }
        catch (Exception e) {
            alert(e.getMessage());
        }
    }

    // Callback for when Key existence checking is complete
    public void checkKeyExistenceComplete(List<String> keyNames, Throwable thrown){
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
            RivetRules rules[];
            if (drtSupported.equals("true")) {
                rules = new RivetRules[1];
                rules[0] = REQUIRE_DUAL_ROOT;
            } else {
                rules = new RivetRules[0];
            }
            crypto.createKey("SigningKey", RivetKeyTypes.NISTP256, rules).whenComplete(this::createKeyComplete);
        }

        catch (Exception e){
            runOnUiThread(() -> alert(e.getMessage()));
        }
    }

    public void createKeyComplete(Void v, Throwable thrown){
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
