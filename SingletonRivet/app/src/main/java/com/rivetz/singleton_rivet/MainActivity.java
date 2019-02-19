/*******************************************************************************
 *
 * RIVETZ CORP. CONFIDENTIAL
 *__________________________
 *
 * Copyright (c) 2019 Rivetz Corp.
 * All Rights Reserved.
 *
 * All information and intellectual concepts contained herein is, and remains,
 * the property of Rivetz Corp and its suppliers, if any.  Dissemination of this
 * information or reproduction of this material, or any facsimile, is strictly
 * forbidden unless prior written permission is obtained from Rivetz Corp.
 ******************************************************************************/
package com.rivetz.singleton_rivet;

import android.os.Bundle;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;

import com.rivetz.api.RivetHashTypes;
import com.rivetz.api.RivetCrypto;
import com.rivetz.api.internal.Utilities;

import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {
    private final String TAG = MainActivity.class.getSimpleName();

    private RivetedApplication ourApp = null;
    private RivetCrypto crypto = null;
    private Button hashButton;

    @UiThread
    @Override
    public void onCreate(Bundle savedInstanceState) {

        // Starts the Rivet lifecycle with the Activity
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        hashButton = (Button) findViewById(R.id.hash);
        makeUnclickable(hashButton);

        // Get a reference to our app class to access the Rivet features
        ourApp = (RivetedApplication)getApplication();

        // The startup is complete, so this is safe in a UI thread
        crypto = ourApp.getRivetCrypto();

        // Make sure the Rivet is available before allow it to be used in the UI
        if (crypto != null) {
            makeClickable(hashButton);
        }
    }

    // Takes the given text and hashes it using SHA256
    @UiThread
    public void hash(View v) {
        EditText dataToBeHashed = findViewById(R.id.dataForHash);

        // Disable the UI while processing the async request
        makeUnclickable(hashButton);

        crypto.hash(RivetHashTypes.SHA256, dataToBeHashed.getText().toString().getBytes(StandardCharsets.UTF_8))
                .whenComplete(this::hashComplete);
    }

    /**
     * Update the UI on hash complete
     *
     * @param result the hash result, or null on error.
     * @param th null for success, or the error exception
     */
    @WorkerThread
    public void hashComplete(byte[] result, Throwable th) {
         if (result != null) {
             // Process the result while still on the background thread
             String hashStr = Utilities.bytesToHex(result);

            runOnUiThread(() -> {
                // The request is complete, allow another go at it
                makeClickable(hashButton);
                alert("This String has been hashed using SHA256: " + hashStr);
            });

        } else {
            runOnUiThread(() -> {
                // The request is complete, allow another go at it
                makeClickable(hashButton);
                alert("Hash failed: " + th.getMessage());
            });
        }
    }

    // Helper functions

    // Creates an alert with some text
    @UiThread
    public void alert(String text) {
        new AlertDialog.Builder(this)
                .setMessage(text)
                .create().show();
    }

    // Makes a button unclickable
    @UiThread
    public void makeUnclickable(Button button) {
        button.setAlpha(.5f);
        button.setClickable(false);
    }

    // Makes a button clickable
    @UiThread
    public void makeClickable(Button button) {
        button.setAlpha(1f);
        button.setClickable(true);
    }
}
