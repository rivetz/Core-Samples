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

package com.rivetz.singleton_rivet;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
    public void onCreate(@NonNull Bundle savedInstanceState) {

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
    public void hash(@NonNull View v) {
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
    public void hashComplete(@Nullable byte[] result, @Nullable Throwable th) {
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
    public void alert(@NonNull String text) {
        new AlertDialog.Builder(this)
                .setMessage(text)
                .create().show();
    }

    // Makes a button unclickable
    @UiThread
    public void makeUnclickable(@NonNull Button button) {
        button.setAlpha(.5f);
        button.setClickable(false);
    }

    // Makes a button clickable
    @UiThread
    public void makeClickable(@NonNull Button button) {
        button.setAlpha(1f);
        button.setClickable(true);
    }
}
