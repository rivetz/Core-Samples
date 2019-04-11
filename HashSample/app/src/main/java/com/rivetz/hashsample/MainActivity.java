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

import com.rivetz.api.RivetErrors;
import com.rivetz.api.RivetHashTypes;
import com.rivetz.api.RivetCrypto;
import com.rivetz.api.RivetRuntimeException;
import com.rivetz.api.SPID;
import com.rivetz.api.internal.Utilities;
import com.rivetz.bridge.DevicePropertyIds;
import com.rivetz.bridge.RivetApiActivity;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

/**
 * An example of extending the {@code RivetApiActivity}
 *
 * This is the standard boilerplate code all other apps incorporate
 *
 */

public class MainActivity extends RivetApiActivity {

    // The startup process sets the state of these variables. When startupComplete() is
    // called, it can evaluate each of these and set the UI accordingly.

    private RivetCrypto crypto = null;  /** The instance of the crypto interface, or null on error */
    private static boolean pairSuccess = false; /** true if the Rivet is paired */
    private static boolean drtSupported = false; /** true if Dual Root of Trust is supported */

    @Override
    public void onCreate(@NonNull Bundle savedInstanceState) {

        // This allows the Rivet to acquire the resources it will need when
        // pairing with the SPID
        super.onCreate(savedInstanceState);

        // Standard Android startup
        setContentView(R.layout.activity_main);

        // Disable all of the UI elements that require the Rivet
        setUiDisabled();

        // If the Rivetz app is not installed, when you call pairDevice(), the user
        // will be sent to the PlayStore to download it
        if (!isRivetInstalled()) {
            alertFromUiThread("Please install the Rivetz app");
        }

        // Now there will be a delay running the startup in the background. Entertain the user...
        findViewById(R.id.loading).setVisibility(View.VISIBLE);

        // Put all of the high latency, asynchronous work on a background thread
        new Thread(this::doStartup).start();
    }

    /**
     * Perform all of the startup actions needed on a background thread. This allows us
     * to block on all async calls, making the flow easier to follow. The downside is
     * handling exceptions. If the error handling for any failure is the same, a single
     * try/catch can wrap the entire startup.
     *
     * This method will set the state of the variables described below:
     *
     * {@code pairingSuccess} is true if the user accepted pairing.
     * {@code crypto} is an instance of the Rivet crypto interface, or null on error
     * {@code drtSupported} is true if Dual Root of Trust is enabled
     */
    private void doStartup() {
        Exception reason = null;

        // Pair with the SPID, block until it completes
        try {
            pairSuccess = pairDevice(SPID.DEVELOPER_TOOLS_SPID).get();
        }
        catch (ExecutionException ex) {
            reason = ex;
        }
        catch (InterruptedException ex) {
            reason = ex;
        }

        if (reason != null) {

            // If the error is that the Rivet isn't installed, the user was sent to the PlayStore,
            // sending this app to the background. Exit completely so the startup will find the
            // new Rivet.
            if (reason instanceof ExecutionException) {
                if (reason.getCause() instanceof RivetRuntimeException) {
                    if (((RivetRuntimeException)reason.getCause()).getError() == RivetErrors.NOT_INSTALLED) {
                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(1);
                    }
                }
            }

            // The user doesn't really want to see the raw pairing error, but
            // useful as a development sample.
            alertFromBgThread(reason.getMessage());
        } else if (!pairSuccess) {
            alertFromBgThread("User declined");
        } else {

            // The Rivet is paired, get an instance of the crypto interface
            // NOTE: This method could throw a RivetRuntimeException(), but only
            // for not being paired, so it doesn't need a try/catch
            crypto = getRivetCrypto();

            try {
                // Check if DRT is supported, block until it completes
                drtSupported = crypto.getDeviceProperty(DevicePropertyIds.DRT_SUPPORTED.toString()).get().equals("true");
            }
            catch (ExecutionException ex) {
                reason = ex;
            }
            catch (InterruptedException ex) {
                reason = ex;
            }

            if (reason != null) {
                // Give the reason to the user - as stated above, for development only.
                alertFromBgThread(reason.getMessage());
            }
        }


        // Now update the UI with the results
        runOnUiThread(this::startupComplete);
    }

    /**
     * Called on the UI thread when all background operations are complete
     */
    private void startupComplete() {

        // Turn off the user entertainment, startup is done
        findViewById(R.id.loading).setVisibility(View.GONE);

        // Fatal case for this sample, exit.
        if (!pairSuccess || crypto == null) {
            finish();
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        } else {

            if (drtSupported) {
                alertFromBgThread("DRT supported");
            } else {
                alertFromBgThread("DRT not supported");
            }

            setUI();
        }
    }


    /**
     * Hash the given data using SHA256 and display it to the user
     *
     * @param v The Android View.
     */
    public void hash(@NonNull View v) {
        // Disable the UI for a while
        setUiDisabled();
        EditText dataToBeHashed = findViewById(R.id.dataForHash);

        crypto.hash(RivetHashTypes.SHA256, dataToBeHashed.getText().toString().getBytes(StandardCharsets.UTF_8))
                .whenComplete((result, ex) -> {
                    if (result != null) {
                        alertFromBgThread("This String has been hashed using SHA256: " + Utilities.bytesToHex(result));
                    } else {
                     alertFromBgThread("Hash failed");
                    }
                });
        // Re-enable the UI
        setUI();
    }

    /**
     * Disable all Rivet related controls while pairing
     *
     */

    private void setUiDisabled() {
        makeUnclickable(findViewById(R.id.hash));
    }


    private void setUI(){
        makeClickable(findViewById(R.id.hash));
    }

    // Helper functions used in all sample apps

    /**
     * Generate a UI alert from a background thread
     *
     * Rivet callbacks are always on a background thread, so create an Alert
     * on the UI thread.
     *
     * @param text the message to be shown to the user
     */
    private void alertFromBgThread(@NonNull String text) {
        runOnUiThread(()->{
            alertFromUiThread(text);
        });
    }

    /**
     * Generate a UI alert
     *
     * @param text the message to be shown to the user
     */
    private void alertFromUiThread(@NonNull String text) {
        new AlertDialog.Builder(this)
                .setMessage(text)
                .create().show();
    }


    /**
     * Make a button unclickable
     *
     * @param button the button to be made clickable
     */

    private void makeUnclickable(@NonNull Button button){
        button.setAlpha(.5f);
        button.setClickable(false);
    }


    /**
     * Make a button clickable
     *
     * @param button the button to be made clickable
     */

    private void makeClickable(@NonNull Button button){
        button.setAlpha(1f);
        button.setClickable(true);
    }

}
