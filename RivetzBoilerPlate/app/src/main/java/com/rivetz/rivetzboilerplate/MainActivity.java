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

package com.rivetz.rivetzboilerplate;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;

import com.rivetz.api.RivetCrypto;
import com.rivetz.api.RivetRuntimeException;
import com.rivetz.api.SPID;
import com.rivetz.bridge.RivetWalletActivity;

import java.util.concurrent.ExecutionException;

public class MainActivity extends RivetWalletActivity {
    private RivetCrypto crypto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Starts the Rivet lifecycle with the Activity and sets the UI
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
                        runOnUiThread(() -> {
                            onDevicePairing(true);
                        });
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
        } else {
            alert("Pairing error!");
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
