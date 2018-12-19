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

package com.rivetz.rivetsample5;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;

import com.rivetz.api.RivetCrypto;
import com.rivetz.api.RivetKeyDescriptor;
import com.rivetz.api.RivetKeyTypes;
import com.rivetz.api.RivetRules;
import com.rivetz.api.RivetRuntimeException;
import com.rivetz.api.SPID;
import com.rivetz.bridge.RivetWalletActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends RivetWalletActivity {
    private RivetCrypto crypto;

    // Creates and pairs a Rivet if necessary
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Starts the Rivet lifecycle with the Activity and sets the UI
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        makeUnclickable(findViewById(R.id.createKey));
        makeUnclickable(findViewById(R.id.describe));
        makeUnclickable(findViewById(R.id.delete));
        makeUnclickable(findViewById(R.id.getKeyNames));
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
                        if(crypto != null) {
                            runOnUiThread(() -> {
                                onDevicePairing(true);
                            });
                        }
                        else {
                            runOnUiThread(() -> {
                                onDevicePairing(false);
                            });
                        }
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
            makeClickable(findViewById(R.id.createKey));
        } else {
            alert("Pairing error!");
        }
    }

    // Check the key's existence asynchronously by generating a descriptor for it
    // before creating it
    public void checkKeyExistence(View v) {
        try {
            crypto.getKeyDescriptor("MyKey").whenComplete(this::checkKeyExistenceComplete);
            loading();
        }
        catch (Exception e) {
            alert(e.getMessage());
        }
    }

    // Callback for when Key existence checking is complete
    public void checkKeyExistenceComplete(RivetKeyDescriptor descriptor, Throwable thrown){
        if(thrown == null) {
            if (descriptor != null) {
                runOnUiThread(() ->{
                    alert("Key already exists");
                    makeClickable(findViewById(R.id.getKeyNames));
                    makeClickable(findViewById(R.id.delete));
                    makeClickable(findViewById(R.id.describe));
                    notLoading();
                });
            }
            else {
                createKey();
            }

        }
        else{
            runOnUiThread(() -> alert(thrown.getMessage()));
        }
    }

    // Creates a Key asynchronously
    public void createKey() {
        try {
            crypto.createKey("MyKey", RivetKeyTypes.NISTP256, new RivetRules[0]).whenComplete(this::createKeyComplete);
        }

        catch (Exception e){
            runOnUiThread(() -> alert(e.getMessage()));
        }
    }

    public void createKeyComplete(Void v, Throwable thrown){
        if(thrown == null){
            runOnUiThread(() ->{
                alert("Key successfully created");
                makeClickable(findViewById(R.id.getKeyNames));
                makeClickable(findViewById(R.id.delete));
                makeClickable(findViewById(R.id.describe));
            });
        }
        else {
            runOnUiThread(() -> alert(thrown.getMessage()));
        }
        notLoading();
    }

    // Deletes a key asynchronously
    public void deleteKey(View v) {
        loading();
        try {
            crypto.deleteKey("MyKey").whenComplete(this::deleteKeyComplete);
        } catch (Exception e) {
            alert(e.getMessage());
        }
    }

    // Callback when the key is done deleting
    public void deleteKeyComplete(Boolean b, Throwable thrown) {
        if (thrown == null) {
            runOnUiThread(() ->{
                alert("Key successfully deleted");
                makeUnclickable(findViewById(R.id.delete));
            });
        } else {
            runOnUiThread(() -> alert(thrown.getMessage()));
        }
        notLoading();
    }

    // Gets the Keydescriptor for the Key asynchronously
    public void describe(View v) {
        crypto.getKeyDescriptor("MyKey").whenComplete(this::describeComplete);
        loading();
    }

    // Callback for when the backup function is complete which returns a byte array
    // of the now encrypted keys that were meant to be backed up
    public void describeComplete(RivetKeyDescriptor descriptor, Throwable thrown) {
        if (thrown == null) {
            runOnUiThread(() ->{
                alert("The key " + descriptor.getName() + " is of the type " + descriptor.getKeyType());
            });
        }
        else{
                runOnUiThread(() -> alert(thrown.getMessage()));
        }

        notLoading();
    }

    // Restores the Key asynchronously after it was exported and deleted
    public void getKeyNames(View v) {
        try {
            crypto.getKeyNamesOf(RivetKeyTypes.NISTP256).whenComplete(this::getKeyNamesComplete);
            loading();
        } catch (Exception e) {
            alert(e.getMessage());
        }
    }

    // Callback for when restoring the Key is complete
    public void getKeyNamesComplete(List<String > keys, Throwable thrown) {
        if (thrown == null) {
            runOnUiThread(() -> {
                alert("The Keys of the NISTP256 type saved on this device are");
                for(int i=0; i<keys.size(); i++) {
                    alert(keys.get(i));
                }
            });
        } else {
            runOnUiThread(() -> alert(thrown.getMessage()));
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
