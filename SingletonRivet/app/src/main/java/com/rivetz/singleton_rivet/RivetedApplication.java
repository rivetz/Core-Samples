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

import android.support.annotation.AnyThread;
import android.support.annotation.MainThread;
import android.support.annotation.WorkerThread;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.rivetz.api.RivetCrypto;
import com.rivetz.api.RivetErrors;
import com.rivetz.api.RivetRuntimeException;
import com.rivetz.api.SPID;
import com.rivetz.bridge.DevicePropertyIds;
import com.rivetz.bridge.RivetSupportAndroidImpl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


/**
 * The Rivet application base class
 *
 * This example class extends the Android application class to construct and pair a Rivet with
 * the application. The Rivet uses the application context to bind with the Rivetz Core Service
 * in order to access the Trusted Execution Environment (TEE).
 *
 * In this example, pairing with the Rivet starts with onCreate() to make the Rivet available
 * as soon as possible within the app. Your application may want to start pairing at a later time,
 * or as the result of some user interaction. In that case, you would create a startup method that
 * calls the doStartup() on a background thread, as is done in the onCreate() here.
 */
public class RivetedApplication extends MultiDexApplication {
    private final String TAG = RivetedApplication.class.getSimpleName();

    private RivetSupportAndroidImpl rivetSupport;
    private CompletableFuture<Boolean> pairResult = new CompletableFuture<>();
    private boolean pairSuccess = false;
    private boolean drtSupported = false;
    private RivetRuntimeException failReason = null;
    private RivetCrypto crypto = null;


    @Override
    @MainThread
    public void onCreate() {
        super.onCreate();

        // Construct the Android support object
        rivetSupport = new RivetSupportAndroidImpl();

        // Don't allow RivetzJ to redirect the user to the PlayStore automatically, show
        // the error in an alert first. Use the SplashActivity to notify the user and then
        // call the redirect method.
        rivetSupport.setAutoRedirect(false);

        // Do all of the startup on a background thread. Expose the result with
        // the completeable future, which can trigger an exit from a splash screen.
        new Thread(this::doStartup).start();
    }

    /**
     * Send the user to the PlayStore to install the Rivet app.
     */
    @AnyThread
    public void sendToPlayStore() {
        rivetSupport.redirectToPS(getApplicationContext());
    }

    /**
     * The pairing result
     *
     * @return true for paired, false if user cancelled, or an exception on error.
     */
    @AnyThread
    public CompletableFuture<Boolean> isPaired() {
        return pairResult;
    }

    /**
     * Get a Rivet instance with the crypto interface
     *
     * @return a crypto rivet, or null if not paired
     */
    @AnyThread
    public RivetCrypto getRivetCrypto() {

        try {
            return rivetSupport.getRivetCrypto();
        }
        catch (RivetRuntimeException ex) {
            failReason = ex;
        }

        return null;
    }

    /**
     * Get the reason pairing failed
     *
     * @return the exception thrown during pairing, or null if pairing was successful.
     */
    @AnyThread
    public RivetRuntimeException getFailReason() {
        return failReason;
    }

    /**
     * Is Dual Root of Trust supported
     *
     * @return true if supported, false otherwise
     */
    @AnyThread
    public boolean isDrtSupported() {
        return drtSupported;
    }

    /**
     * This needs to be called when exiting the app to clean up the service connection
     */
    public void onExit() {
        rivetSupport.unpairDevice();
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
    @WorkerThread
    private void doStartup() {
        Log.i(TAG, "App startup, call pairDevice()");

        // Pair with the SPID, block until it completes. All exceptions are mapped into one of the
        // RivetErrors, so decoding is easier than dealing with different exception types.
        try {
            pairSuccess = rivetSupport.pairDevice(getApplicationContext(), SPID.DEVELOPER_TOOLS_SPID).get();
        }
        catch (ExecutionException eEx) {
            // The system may throw a runtime exception or a bug may cause an
            // excpetion other than one generated in the Rivet. Re-throw to handle
            // both cases
            try { throw eEx.getCause(); }
            catch (RivetRuntimeException rrEx) {
                failReason = rrEx;
            }
            catch (Throwable th) {
                failReason = new RivetRuntimeException(RivetErrors.UNEXPECTED_EXCEPTION);
            }
        }
        catch (RivetRuntimeException rrEx) {
            failReason = rrEx;
        }
        catch (InterruptedException intEx) {
            failReason = new RivetRuntimeException(RivetErrors.INTERRUPTED_EXCEPTION);
        }

        // Terminate early on error, with failReason holding the  first cause
        if (failReason != null) {
            Log.i(TAG, "Failed to pair: " + failReason.getMessage());

            pairResult.completeExceptionally(failReason);
            return;
        }

        // Terminate early on error, with failReason holding the  first cause,
        // which is because of the user in this case, not an actual error
        if (!pairSuccess) {
            failReason = new RivetRuntimeException(RivetErrors.USER_CANCELED);
            pairResult.completeExceptionally(failReason);
            return;
        }

        // The Rivet is paired, get an instance of the crypto interface
        //
        // NOTE: This method could throw a RivetRuntimeException(), but only
        // for not being paired, so it doesn't need a try/catch
        crypto = getRivetCrypto();

        try {
            // Check if DRT is supported, block until it completes
            drtSupported = crypto.getDeviceProperty(DevicePropertyIds.DRT_SUPPORTED.toString()).get().equals("true");
        }
        catch (RivetRuntimeException rrEx) {
            failReason = rrEx;
        }
        catch (Throwable ex) {
            // Anything that hasn't been mapped into a Rivet exception is a runtime excption such
            // as out of memory.
            failReason = new RivetRuntimeException(RivetErrors.UNEXPECTED_EXCEPTION);
        }

        // Terminate early on error, getting DRT support can't fail with the Rivet is working properly
        if (failReason != null) {
            pairResult.completeExceptionally(failReason);
            return;
        }

        // Pairing and startup complete
        pairResult.complete(true);
    }
}
