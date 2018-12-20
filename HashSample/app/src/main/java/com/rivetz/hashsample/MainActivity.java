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
package com.rivetz.hashsample;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;

import com.rivetz.api.RivetHashTypes;
import com.rivetz.api.RivetCrypto;
import com.rivetz.api.SPID;
import com.rivetz.api.internal.Utilities;
import com.rivetz.bridge.RivetWalletActivity;

import java.nio.charset.StandardCharsets;

public class MainActivity extends RivetWalletActivity {
    private RivetCrypto crypto;
    private Button hashButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Starts the Rivet lifecycle with the Activity
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        hashButton = (Button) findViewById(R.id.hash);

        loading();

        // Start the pairing process, then enable UI if successful
        pairDevice(SPID.DEVELOPER_TOOLS_SPID).whenComplete((paired, ex) -> {

            runOnUiThread(() -> {
                notLoading();
            });

            if (paired != null) {

                if (paired.booleanValue()) {
                    crypto = getRivetCrypto();
                    if (crypto != null) {
                        runOnUiThread(() -> {
                            onDevicePairing(true);
                        });
                    } else {
                        runOnUiThread(() -> {
                            onDevicePairing(false);
                        });
                    }
                } else {
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

    public void onDevicePairing(boolean success) {
        if (success) {
            alert("Paired");
            makeClickable(hashButton);
        } else {
            alert("Pairing error!");
            makeUnclickable(hashButton);
        }
    }

    // Takes the given text and hashes it using SHA256
    public void hash(View v) {
        EditText dataToBeHashed = findViewById(R.id.dataForHash);

        crypto.hash(RivetHashTypes.SHA256, dataToBeHashed.getText().toString().getBytes(StandardCharsets.UTF_8))
                .whenComplete((result, ex) -> {
                    if (result != null) {
                        runOnUiThread(() -> {
                            alert("This String has been hashed using SHA256: " + Utilities.bytesToHex(result));
                        });

                    } else {
                        runOnUiThread(() -> {
                            alert("Hash failed: " + ex.getMessage());
                        });
                    }
                });
    }

    // Helper functions

    // Creates an alert with some text
    public void alert(String text) {
        new AlertDialog.Builder(this)
                .setMessage(text)
                .create().show();
    }

    // Makes a button unclickable
    public void makeUnclickable(Button button) {
        button.setAlpha(.5f);
        button.setClickable(false);
    }

    // Makes a button clickable
    public void makeClickable(Button button) {
        button.setAlpha(1f);
        button.setClickable(true);
    }

    // Shows a loading animation
    public void loading() {
        findViewById(R.id.loading).setVisibility(View.VISIBLE);
    }

    // Makes the loading animation invisible
    public void notLoading() {
        findViewById(R.id.loading).setVisibility(View.GONE);
    }
}
