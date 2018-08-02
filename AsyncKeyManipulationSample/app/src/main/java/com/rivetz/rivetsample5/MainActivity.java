package com.rivetz.rivetsample5;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.rivetz.api.KeyRecord;
import com.rivetz.api.RivetInterface;
import com.rivetz.api.SPID;
import com.rivetz.api.internal.Utilities;
import com.rivetz.bridge.Rivet;

import org.slf4j.helpers.Util;

import java.security.Key;
import java.util.Optional;

public class MainActivity extends AppCompatActivity {
    Rivet rivet;
    String message;
    byte[] signed;

    // Creates and pairs a Rivet if necessary
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rivet = new Rivet(this, SPID.DEVELOPER_TOOLS_SPID);

        if (!rivet.isPaired()) {
            rivet.pairDevice(this);
        }
    }

    // Checks if the Rivet was successfully paired
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Rivet.INSTRUCT_PAIRDEVICE) {
            onDevicePairing(resultCode);
        }
    }

    public void onDevicePairing(int resultCode) {
        if (resultCode == RESULT_CANCELED) {
            alert("Pairing error: " + String.valueOf(resultCode));
        }
        if (resultCode == RESULT_OK) {
            alert("Paired");
            rivet.getKeyAsync("MyKey").whenComplete(this::checkKeyComplete);
            loading();
        }
    }

    // Verifies if a key already exists and sets the buttons accordingly
    public void checkKeyComplete(Optional<KeyRecord> key, Throwable thrown) {
        if (thrown == null) {
            if (!key.isPresent()) {
                runOnUiThread(() -> makeUnclickable(findViewById(R.id.delete)));
                runOnUiThread(() -> makeUnclickable(findViewById(R.id.export)));
                runOnUiThread(() -> makeUnclickable(findViewById(R.id.sign)));
                runOnUiThread(() -> makeUnclickable(findViewById(R.id.Import)));
                runOnUiThread(() -> makeUnclickable(findViewById(R.id.verify)));
            } else {
                runOnUiThread(() -> makeUnclickable(findViewById(R.id.createKey)));
                runOnUiThread(() -> makeUnclickable(findViewById(R.id.Import)));
                runOnUiThread(() -> makeUnclickable(findViewById(R.id.verify)));
            }
        } else {
            runOnUiThread(() -> alert(thrown.getMessage()));
        }
        runOnUiThread(() -> notLoading());
    }

    // Creates an eliptic curve Key with an exportable private key asynchronously
    public void createKey(View v) {
        try {
            loading();
            rivet.createKeyAsync(RivetInterface.KeyType.ECDSA_NISTP256, "MyKey", RivetInterface.UsageRule.PRIV_KEY_EXPORTABLE).whenComplete(this::createKeyComplete);
        } catch (Exception e) {
            alert(e.getMessage());
        }
    }

    // Callback when the key is done creating
    public void createKeyComplete(KeyRecord key, Throwable thrown) {
        if (thrown == null) {
            runOnUiThread(() -> alert("Key successfully created"));
            runOnUiThread(() -> makeClickable(findViewById(R.id.delete)));
            runOnUiThread(() -> makeClickable(findViewById(R.id.sign)));
            runOnUiThread(() -> makeClickable(findViewById(R.id.export)));
            runOnUiThread(() -> makeUnclickable(findViewById(R.id.Import)));
            runOnUiThread(() -> makeUnclickable(findViewById(R.id.createKey)));
        } else {
            runOnUiThread(() -> alert(thrown.getMessage()));
        }
        runOnUiThread(() -> notLoading());
    }

    // Deletes a key asynchronously
    public void deleteKey(View v) {
        loading();
        try {
            rivet.deleteKeyAsync("MyKey").whenComplete(this::deleteKeyComplete);
        } catch (Exception e) {
            alert(e.getMessage());
        }
    }

    // Callback when the key is done deleting
    public void deleteKeyComplete(Void v, Throwable thrown) {
        if (thrown == null) {
            runOnUiThread(() -> alert("Key successfully deleted"));
            runOnUiThread(() -> makeUnclickable(findViewById(R.id.export)));
            runOnUiThread(() -> makeUnclickable(findViewById(R.id.sign)));
            runOnUiThread(() -> makeClickable(findViewById(R.id.createKey)));
            runOnUiThread(() -> makeClickable(findViewById(R.id.Import)));
            runOnUiThread(() -> makeUnclickable(findViewById(R.id.verify)));
            runOnUiThread(() -> makeUnclickable(findViewById(R.id.delete)));
        } else {
            runOnUiThread(() -> alert(thrown.getMessage()));
        }
        notLoading();
    }

    // Signs a given String asynchronously to later verify with the exported key
    public void sign(View v) {
        loading();
        EditText msg = findViewById(R.id.signed);
        message = msg.getText().toString();
        rivet.signAsync("MyKey", message.getBytes()).whenComplete(this::signComplete);
    }

    // Callback for when signing is done
    public void signComplete(byte[] signedText, Throwable thrown) {
        if (thrown == null) {
            signed = signedText;
            EditText output = findViewById(R.id.signed);
            runOnUiThread(() -> output.setText(Utilities.bytesToHex(signedText)));
        } else {
            runOnUiThread(() -> alert(thrown.getMessage()));
        }
        runOnUiThread(() -> notLoading());
    }

    // Gets the KeyRecord for the Key asynchronously
    public void export(View v) {
        rivet.getKeyAsync("MyKey").whenComplete(this::exportComplete);
        loading();
    }

    // Callback for when the export function is complete which returns the Public Key and Private Key
    public void exportComplete(Optional<KeyRecord> key, Throwable thrown) {
        if (thrown == null) {
            //Checks if a key was found, as this is not always the case with Optional<>
            if (key.isPresent()) {
                EditText output = findViewById(R.id.publicKey);
                runOnUiThread(() -> output.setText(Utilities.bytesToHex(key.get().getPublicKeyBytes())));
                runOnUiThread(() -> makeClickable(findViewById(R.id.Import)));
            } else runOnUiThread(() -> alert("Key not found"));
        }
        runOnUiThread(() -> notLoading());
    }

    // Imports the Key asynchronously after it was exported and deleted in order to verify the signature
    public void Import(View v) {
        EditText publicKey = findViewById(R.id.publicKey);
        try {
            rivet.addKeyAsync(RivetInterface.KeyType.ECDSA_NISTP256, "MyKey",
                    Utilities.hexToBytes(publicKey.getText().toString()), new byte[0], RivetInterface.UsageRule.PRIV_KEY_EXPORTABLE).whenComplete(this::importComplete);

        } catch (Exception e) {
            alert(e.getMessage());
        }
    }

    // Callback for when importing the Key is complete
    public void importComplete(KeyRecord key, Throwable thrown) {
        if (thrown == null) {
            runOnUiThread(() -> alert("Key successfully imported"));
            runOnUiThread(() -> makeClickable(findViewById(R.id.verify)));
            runOnUiThread(() -> makeUnclickable(findViewById(R.id.createKey)));
            runOnUiThread(() -> makeUnclickable(findViewById(R.id.Import)));
        } else {
            runOnUiThread(() -> alert(thrown.getMessage()));
        }
    }

    // Verifies the signature asynchronously
    public void verify(View v) {
        rivet.verifyAsync("MyKey", signed, message.getBytes()).whenComplete(this::verifyComplete);
    }

    // Callback for when verifying the signature is complete
    public void verifyComplete(Boolean isReal,Throwable thrown){
        if(thrown == null){
            if(isReal){
                runOnUiThread(() -> alert("This message is real"));
            }
            else {
                runOnUiThread(() -> alert("This message is fake"));
            }
        }
        else {
            runOnUiThread(() -> alert(thrown.getMessage()));
        }
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
