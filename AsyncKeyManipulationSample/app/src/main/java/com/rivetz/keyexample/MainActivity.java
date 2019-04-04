package com.rivetz.keyexample;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;

import com.rivetz.api.RivetCrypto;
import com.rivetz.api.RivetErrors;
import com.rivetz.api.RivetKeyDescriptor;
import com.rivetz.api.RivetKeyTypes;
import com.rivetz.api.RivetRules;
import com.rivetz.api.RivetRuntimeException;
import com.rivetz.api.SPID;
import com.rivetz.bridge.DevicePropertyIds;
import com.rivetz.bridge.RivetApiActivity;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.rivetz.api.RivetRules.REQUIRE_DUAL_ROOT;


/**
 * An example of extending the {@code RivetApiActivity}
 *
 * This example illustrates a single activity that uses a Rivet for some cryptography functions. By
 * extending the {@code RivetApiActivity}, the Rivet instance will be managed properly within the
 * lifecycle of your Activity.
 *
 */
public class MainActivity extends RivetApiActivity {
    // For some simple use cases, a "hard-coded" key name will be all an activity needs
    private final String KEY_NAME = "MyKey";

    // The startup process sets the state of these variables. When startupComplete() is
    // called, it can evaluate each of these and set the UI accordingly.

    private RivetCrypto crypto = null;  /** The instance of the crypto interface, or null on error */
    private static boolean pairSuccess = false; /** true if the Rivet is paired */
    private static boolean drtSupported = false; /** true if Dual Root of Trust is supported */
    private static boolean hasKey = false; /** true if the activity key exists */


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
                List<String> keyNames = crypto.getKeyNamesOf(RivetKeyTypes.NISTP256).get();
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

            setUiHasKeyState();
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
        crypto.createKey(KEY_NAME, RivetKeyTypes.NISTP256, rules).whenComplete(this::createKeyComplete);
    }

    /**
     * Handle completion of creating a key
     *
     * @param v nothing is returned.
     * @param thrown null for success, or the error exception.
     */
    private void createKeyComplete(Void v, Throwable thrown){
        if (thrown == null){
            // No exception means the key has been created
            hasKey = true;
        }
        else {
            alertFromBgThread(thrown.getMessage());
        }

        // Update the UI state
        runOnUiThread(() ->{
            setUiHasKeyState();
        });
    }

    /**
     * Delete a key
     *
     * @param v the Android View
     */
    public void deleteKey(@NonNull View v) {
        // Disable all the UI for a bit
        setUiDisabled();

        // This operation completes with an inline lambda, with only a few
        // lines of code needed.
        crypto.deleteKey(KEY_NAME).whenComplete((wasDeleted, ex) -> {

            if (ex != null) {
                // Should show the user something more friendly, ok only for development
                alertFromBgThread(ex.getMessage());
            }
            else {
                // The key has been deleted.
                hasKey = false;
            }

            // Update the UI state
            runOnUiThread(() ->{
                setUiHasKeyState();
            });
        });
    }

    /**
     * Get the key information
     *
     * @param v the Android View
     */
    public void describe(@NonNull View v) {
        // Disable all the UI for a bit
        setUiDisabled();

        // Use a method for completion
        crypto.getKeyDescriptor(KEY_NAME).whenComplete(this::describeComplete);
    }

    /**
     * Handle the describe key completion
     *
     * @param descriptor the information about the key, or null for error
     * @param thrown null, or an exception on error
     */
    private void describeComplete(RivetKeyDescriptor descriptor, Throwable thrown) {
        if (thrown == null) {
            alertFromBgThread("The key " + descriptor.getName() + " is of the type " + descriptor.getKeyType());
        }
        else{
            alertFromBgThread(thrown.getMessage());
        }

        // Update the UI state
        runOnUiThread(() ->{
            setUiHasKeyState();
        });
    }

    // Restores the Key asynchronously after it was exported and deleted
    public void getKeyNames(@NonNull View v) {
        // Disable all the UI for a bit
        setUiDisabled();

        crypto.getKeyNamesOf(RivetKeyTypes.NISTP256).whenComplete(this::getKeyNamesComplete);
    }

    // Callback for when restoring the Key is complete
    private void getKeyNamesComplete(List<String > keys, Throwable thrown) {
        if (thrown == null) {

            for(int i=0; i<keys.size(); i++) {
                alertFromBgThread(keys.get(i));
            }

        } else {
            alertFromBgThread(thrown.getMessage());
        }

        // Update the UI state
        runOnUiThread(() ->{
            setUiHasKeyState();
        });
    }

    /**
     * Generate a UI alert from a background thread
     *
     * Rivet callbacks are always on a background thread, so create an Alert
     * on the UI thread.
     *
     * @param text the message to be shown to the user
     */
    private void alertFromBgThread(String text) {
        runOnUiThread(()->{
            alertFromUiThread(text);
        });
    }

    /**
     * Generate a UI alert
     *
     * @param text the message to be shown to the user
     */
    private void alertFromUiThread(String text) {
        new AlertDialog.Builder(this)
                .setMessage(text)
                .create().show();
    }

    /**
     * Disable all Rivet related controls while pairing
     *
     */
    private void setUiDisabled() {
        makeUnclickable(findViewById(R.id.createKey));
        makeUnclickable(findViewById(R.id.describe));
        makeUnclickable(findViewById(R.id.delete));
        makeUnclickable(findViewById(R.id.getKeyNames));
    }

    /**
     * Set UI state that depends on hasKey
     *
     * For this sample, the button states are binary based on the key
     * existence. If the key doesn't exist, allow the user to create it.
     * If the key does exist, allow the user to delete, and query.
     */
    private void setUiHasKeyState() {
        // Ready to set the UI state now
        if (hasKey) {
            makeClickable(findViewById(R.id.getKeyNames));
            makeClickable(findViewById(R.id.delete));
            makeClickable(findViewById(R.id.describe));
        } else {
            makeClickable(findViewById(R.id.createKey));
        }

    }

    private void makeUnclickable(Button button){
        button.setAlpha(.5f);
        button.setClickable(false);
    }

    private void makeClickable(Button button){
        button.setAlpha(1f);
        button.setClickable(true);
    }
}
