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

package com.rivetz.hashsample;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
import android.support.annotation.NonNull;

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
    public void onCreate(@NonNull Bundle savedInstanceState) {

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
    public void hash(@NonNull View v) {
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
    public void alert(@NonNull String text) {
        new AlertDialog.Builder(this)
                .setMessage(text)
                .create().show();
    }

    // Makes a button unclickable
    public void makeUnclickable(@NonNull Button button) {
        button.setAlpha(.5f);
        button.setClickable(false);
    }

    // Makes a button clickable
    public void makeClickable(@NonNull Button button) {
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
