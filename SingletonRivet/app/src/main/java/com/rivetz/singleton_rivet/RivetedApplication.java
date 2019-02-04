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

import android.app.Application;

import com.rivetz.api.RivetCrypto;
import com.rivetz.api.RivetErrors;
import com.rivetz.api.RivetRuntimeException;
import com.rivetz.bridge.RivetSupportAndroid;
import com.rivetz.bridge.RivetSupportAndroidImpl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class RivetedApplication extends Application {
    private RivetSupportAndroidImpl rivetSupport;
    private CompletableFuture<Boolean> pairResult = new CompletableFuture<>();
    private RivetRuntimeException failReason;

    @Override
    public void onCreate() {
        super.onCreate();

        // Construct the Android support object
        rivetSupport = new RivetSupportAndroidImpl();

        if (rivetSupport.isRivetInstalled(getApplicationContext())) {
            // Startup on a background thread


        }
        else {
            pairResult.completeExceptionally(new RivetRuntimeException(RivetErrors.NOT_INSTALLED));
        }
    }

    public CompletableFuture<Boolean> isPaired() {
        return pairResult;
    }

    public RivetCrypto getRivetCrypto() {

        try {
            return rivetSupport.getRivetCrypto();
        }
        catch (RivetRuntimeException ex) {
            failReason = ex;
        }

        return null;
    }

    public RivetRuntimeException getFailReason() {
        return failReason;
    }
}
