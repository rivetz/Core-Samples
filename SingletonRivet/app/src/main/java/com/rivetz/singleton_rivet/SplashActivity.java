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
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.rivetz.api.RivetRuntimeException;


/**
 * Splash screen displayed during startup
 *
 * This activity handles the rivet startup, notifying the user if the Rivetz app isn't
 * installed, or networking isn't available. If everything is Ok, it starts the pairing
 * process. When paired, it starts the main activity.
 */
public class SplashActivity extends AppCompatActivity {
    private final String TAG = SplashActivity.class.getSimpleName();

    private final String FIRST_RUN_KEY = "firstRun";
    private RivetedApplication ourApp;

    /**
     * Override onCreate to start pairing with the Rivet
     *
     * @param savedInstanceState the save instance state.
     */
    @UiThread
    @Override
    protected void onCreate(@NonNull Bundle savedInstanceState) {
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

                // When the user says Ok, go to the network settings and exit the app
                AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                        .setMessage(R.string.network_err_msg)
                        .setTitle(R.string.network_err_title)
                        .setPositiveButton(R.string.ok, (dialog, id) -> {

                            startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                            finish();

                            android.os.Process.killProcess(android.os.Process.myPid());
                            System.exit(1);
                        });

                builder.create().show();
            }
            else {
                waitPaired();
            }
        }
        else {
            waitPaired();
        }
    }

    /**
     * Pair the app with the Rivet
     */
    @UiThread
    private void waitPaired() {
        // Entertain the user while running the high-latency startup tasks
        findViewById(R.id.loading).setVisibility(View.VISIBLE);

        ourApp = (RivetedApplication)getApplication();
        ourApp.isPaired().whenComplete(this::handleResult);
    }

    /**
     * Handle the pairing result
     *
     * @param result true if user acccepted, false if rejected, or null on error
     * @param ex null for success, or an exception on error
     */
    @WorkerThread
    @SuppressWarnings("unused")
    private void handleResult(Boolean result, Throwable ex) {

        if (ex == null) {
            // Update the first run setting
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor prefEditor = settings.edit();
            prefEditor.putBoolean(FIRST_RUN_KEY, false);
            prefEditor.apply();

            // Release any Rivet resources
            ourApp.onExit();

            // Now start the main activity
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
        else {
            // Re-throw the exception and process it according to type
            try {
                throw ex;
            }
            catch (RivetRuntimeException rrEx) {
                notifyPairExceptions(rrEx);
            }
            catch (Throwable impossible) {
                Log.e(TAG, "Unexpected exception: " + impossible.getMessage());
            }
        }
    }

    /**
     * Handle Rivet exceptions
     *
     * @param reason the exception containing a {@code RivetErrors}
     */
    @WorkerThread
    private void notifyPairExceptions(RivetRuntimeException reason) {
        final Activity activity = this;

        // Log all of them, even user_cancel seems useful to be able to track
        Log.e(TAG, reason.getMessage());

        // Release any Rivet resources
        ourApp.onExit();

        // Start building the alert to notify the user that Pairing failed
        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle("Not Paired");

        switch (reason.getError()) {

            case NOT_INSTALLED:
                // This app is structured to check during the startup process
                // When the user says Ok, go to the PlayStore page to install, then exit
                // the app. Note: this case returns early to avoid the default behavior.
                //
                // The app could be structured to check for the app first, inform the user, call
                // pair to allow RivetzJ to start the activity, then exit the app.
                //
                builder.setMessage(R.string.not_installed);
                builder.setPositiveButton(R.string.ok, (dialog, id) -> {
                    // Starts the activity, then we can exit
                    ourApp.sendToPlayStore();

                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(1);
                });

                runOnUiThread(() -> builder.create().show());
                return;

            case REMOTE_EXCEPTION:
                builder.setMessage(R.string.remote_error);
                break;

            case USER_CANCELED:
                builder.setMessage(R.string.user_cancel);
                break;

            case INTERRUPTED_EXCEPTION:
                builder.setMessage(R.string.interrupted);
                break;

            default:
                builder.setMessage(R.string.runtime_error);
                break;
        }

        // All of these errors are fatal, exit the app when the user says Ok
        builder.setPositiveButton(R.string.ok, (dialog, id) -> {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
        });

        runOnUiThread(() -> builder.create().show());
    }
}
