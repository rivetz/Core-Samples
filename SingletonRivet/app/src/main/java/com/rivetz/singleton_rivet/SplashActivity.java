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

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class SplashActivity extends AppCompatActivity {
    private final String FIRST_RUN_KEY = "firstRun";
    private RivetedApplication ourApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);
        findViewById(R.id.status_message).setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        // Get the settings to determine if it's the first run
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        // Can't exist on the first run
        if (settings.getBoolean(FIRST_RUN_KEY, true)) {

            // On the first launch of the app, ensure there is network connectivity
            ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

            // No network, explain and exit
            if(cm == null || cm.getActiveNetworkInfo() == null) {
                final AppCompatActivity activity = this;

                AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                        .setMessage(R.string.network_err_msg)
                        .setTitle(R.string.network_err_title)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                            // When the user says Ok, go to the network settings and exit the app
                            public void onClick(DialogInterface dialog, int id) {
                                Intent intent = new Intent(activity, MainActivity.class);
                                startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                                finish();
                            }
                        });

                builder.create().show();
            }
        }
        else {
            // Entertain the user while running the high-latency startup tasks
            findViewById(R.id.loading).setVisibility(View.VISIBLE);

            ourApp = (RivetedApplication)getApplication();
            ourApp.isPaired().whenComplete(this::handleResult);
        }


    /*
        // TODO: Set false after pairing
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putBoolean(FIRST_RUN_KEY, false);
        prefEditor.apply();
    */






    }

    private void handleResult(Boolean result, Throwable ex) {

        if (ex == null) {
            if (result) {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
            }
            else {
                // User rejected
            }
        }
    }

    private void handleResult(Boolean result) {
        if (result) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
/*
            new AlertDialog.Builder(this).setMessage(R.string.splash_activity_error)
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            System.exit(1);
                        }
                    })
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            System.exit(1);
                        }
                    })
                    .create().show();
*/
        }
    }
}
