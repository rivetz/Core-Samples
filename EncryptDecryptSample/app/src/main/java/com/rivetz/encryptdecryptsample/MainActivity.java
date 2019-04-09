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

package com.rivetz.encryptdecryptsample;

import java.util.List;
import java.util.concurrent.ExecutionException;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.rivetz.api.EncryptResult;
import com.rivetz.api.RivetCrypto;
import com.rivetz.api.RivetErrors;
import com.rivetz.api.RivetKeyTypes;
import com.rivetz.api.RivetRules;
import com.rivetz.api.RivetRuntimeException;
import com.rivetz.api.SPID;
import com.rivetz.bridge.DevicePropertyIds;
import com.rivetz.bridge.RivetApiActivity;
import com.rivetz.encryptdecryptsample.R;
import static com.rivetz.api.RivetRules.REQUIRE_DUAL_ROOT;

/**
 * An example of extending the {@code RivetApiActivity}
 *
 * This example illustrates a single activity that uses a riveted key for encryption and decryption. By
 * extending the {@code RivetApiActivity}, the Rivet instance will be managed properly within the
 * lifecycle of your Activity.
 *
 */

public class MainActivity extends RivetApiActivity {
    // For some simple use cases, a "hard-coded" key name will be all an activity needs
    private final String KEY_NAME = "EncryptKey";

    // The startup process sets the state of these variables. When startupComplete() is
    // called, it can evaluate each of these and set the UI accordingly.

    private RivetCrypto crypto = null;  /** The instance of the crypto interface, or null on error */
    private static boolean pairSuccess = false; /** true if the Rivet is paired */
    private static boolean drtSupported = false; /** true if Dual Root of Trust is supported */
    private static boolean hasKey = false; /** true if the activity key exists */
    private EncryptResult encryptedText = null; /** The encrypted text that will be generated in this sample */

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
     * {@code hasKey} is set true if the key name defined in the activity exists
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

        // If we have the crypto instance, check for the key
        if (crypto != null) {
            try {
                List<String> keyNames = crypto.getKeyNamesOf(RivetKeyTypes.AES256_CGM).get();
                if (keyNames.contains(KEY_NAME)) {
                    hasKey = true;
                }
            }
            catch (ExecutionException ex) {
                reason = ex;
            }
            catch (InterruptedException ex) {
                reason = ex;
            }

            // Ignore the reason, just say the key doesn't exist. That allows
            // create key to be used, and that can show a reasonable TA error
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

            sethasKeyUI();
        }
    }

    /**
     *  Create a Key
     *
     * @param v the Android View
     */
    public void createKey(@NonNull View v) {
        // Disable all the UI for a bit
        setUiDisabled();

        // If DRT supported add Dual Root of Trust usage rule
        RivetRules rules[] = null;

        if (drtSupported) {
            rules = new RivetRules[]{REQUIRE_DUAL_ROOT};
        }

        // This operation completes and calls a method on the class. This helps break
        // up the code if handling the result requires more than a few lines to process
        crypto.createKey(KEY_NAME, RivetKeyTypes.AES256_CGM, rules).whenComplete(this::createKeyComplete);
    }

    /**
     * Handle completion of creating a key
     *
     * @param v nothing is returned.
     * @param thrown null for success, or the error exception.
     */
    private void createKeyComplete(Void v, @Nullable Throwable thrown){
        if (thrown == null){
            // No exception means the key has been created
            hasKey = true;
        }
        else {
            alertFromBgThread(thrown.getMessage());
        }

        // Update the UI state
        runOnUiThread(() ->{
            sethasKeyUI();
        });
    }


    /**
     * Encrypt a payload of data
     *
     * @param v The Android View.
     */

    public void encrypt(@NonNull View v) {
        // Disable all the UI for a bit
        setUiDisabled();

        // Find the text to encrypt and encrypt it
        EditText payload = findViewById(R.id.payload);
        crypto.encrypt("EncryptKey", payload.getText().toString().getBytes()).whenComplete(this::encryptComplete);

        // Disable the encryption button
        makeUnclickable(findViewById(R.id.encrypt));
    }

    /**
     * Handle completion of encryption
     *
     * @param e the result of the encryption
     * @param thrown null for success, or the error exception.
     */
    private void encryptComplete(@Nullable EncryptResult e, @Nullable Throwable thrown){
        if(thrown == null){
            // Save the result of the encryption
            encryptedText = e;
            // Notify the user
            alertFromBgThread("Your text has been encrypted to " + new String(e.getCipherText()));
            //Allow decryption
            runOnUiThread(() -> makeClickable(findViewById(R.id.decrypt)));
        }
        else {
            alertFromBgThread(thrown.getMessage());
        }
    }

    /**
     * Decrypt the data previously encrypted
     *
     * @param v The Android View.
     */
    public void decrypt(@NonNull View v) {
        // Disable all the UI for a bit
        setUiDisabled();

        crypto.decrypt("EncryptKey", encryptedText).whenComplete(this::decryptComplete);

        // Disable the decryption button
        makeUnclickable(findViewById(R.id.decrypt));
    }

    /**
     * Handle completion of decryption
     *
     * @param decrypted the result of the decryption (in this case the text the user originally
     *                  entered)
     * @param thrown null for success, or the error exception.
     */
    private void decryptComplete(@Nullable byte[] decrypted, @Nullable Throwable thrown) {
        if (decrypted != null) {
           alertFromBgThread("Your text has been decrypted: " + new String(decrypted));
            runOnUiThread(() -> sethasKeyUI());
        }
        else {
            alertFromBgThread(thrown.getMessage());
        }
    }

    /**
     * Disable all Rivet related controls while pairing
     *
     */

    private void setUiDisabled() {
        makeUnclickable(findViewById(R.id.createKey));
        makeUnclickable(findViewById(R.id.decrypt));
        makeUnclickable(findViewById(R.id.encrypt));
    }


    private void sethasKeyUI(){
        if(hasKey){
            makeClickable(findViewById(R.id.encrypt));
            makeUnclickable(findViewById(R.id.createKey));
        }
        else {
            makeClickable(findViewById(R.id.createKey));
        }
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
