package com.rivetz.signingsample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
import com.rivetz.api.KeyRecord;
import com.rivetz.api.SPID;
import com.rivetz.bridge.Rivet;
import com.rivetz.api.RivetInterface;
import java.util.Optional;

public class MainActivity extends AppCompatActivity {
    Rivet rivet;
    byte[] realmessage;

    //creates and pairs a Rivet if necessary
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

    // Signs the real message asynchronously
    public void sign(View v) {
        EditText real = findViewById(R.id.real);
        rivet.signAsync("SignKey",real.getText().toString().getBytes()).whenComplete(this::signComplete);
        makeUnclickable(findViewById(R.id.sign));
        loading();
    }

    // Callback when the real message is done signing which checks if the signing was successful
    public void signComplete(byte[] signature, Throwable thrown) {
        if (signature != null) {
            runOnUiThread(() -> realmessage = signature);
            runOnUiThread(() -> alert("Signing complete"));
        }
        else{
            alert("Error signing:" + thrown.getMessage());
        }
        notLoading();

    }

    // Verifies if the real message is authentic asynchronously using its signature
    public void checkAuthenticity(View v){
        EditText real = findViewById(R.id.real);
        if(realmessage == null){
            alert("Both messages are fake!");
        }
        else {
            rivet.verifyAsync("SignKey",realmessage,real.getText().toString().getBytes()).whenComplete(this::verifyComplete);
            loading();
        }
    }

    // Callback for when the verification is done which checks if the message is authentic or not and returns an alert accordingly
    public void verifyComplete(Boolean validity, Throwable thrown){
        if(validity != null){
            if(validity){
                EditText real = findViewById(R.id.real);
                runOnUiThread(() ->alert("The message " + "'" + real.getText().toString() + "'" + " is authentic, the other is fake!"));
            }
            if(!validity){
                alert("Both messages are fake!");
            }
        }
        else alert(thrown.getMessage());
        notLoading();
    }

    // Creates a Key asynchronously
    public void createKey(View v) {
        try {
            rivet.createKeyAsync(RivetInterface.KeyType.ECDSA_NISTP256, "SignKey", RivetInterface.UsageRule.REQUIRE_TUI_CONFIRM).whenComplete(this::createKeyComplete);
            loading();
        }

        catch (Exception e){
            alert(e.getMessage());
        }
    }

    public void createKeyComplete(KeyRecord key, Throwable thrown){
        if(thrown == null){
            runOnUiThread(() ->alert("Key successfully created"));
            runOnUiThread(() -> makeClickable(findViewById(R.id.sign)));
            runOnUiThread(() -> makeClickable(findViewById(R.id.checkAuthenticity)));
            runOnUiThread(() -> makeUnclickable(findViewById(R.id.createKey)));
        }
        else {
            runOnUiThread(() -> alert(thrown.getMessage()));
        }
        runOnUiThread(() -> notLoading());
    }

    public void onDevicePairing(int resultCode){
        if (resultCode == RESULT_CANCELED) {
            alert("Pairing error: " + String.valueOf(resultCode));
        }
        if (resultCode == RESULT_OK) {
            alert("Paired");
            rivet.getKeyAsync("SignKey").whenComplete(this::checkKeyComplete);
            loading();
        }
    }

    public void checkKeyComplete(Optional<KeyRecord> key, Throwable thrown){
        if(thrown == null){
            if(!key.isPresent()){
                runOnUiThread(() -> makeUnclickable(findViewById(R.id.sign)));
                runOnUiThread(() -> makeUnclickable(findViewById(R.id.checkAuthenticity)));
            }
            else {
                runOnUiThread(() -> makeUnclickable(findViewById(R.id.createKey)));
            }
        }
        else {
            runOnUiThread(() -> alert(thrown.getMessage()));
        }
        runOnUiThread(() -> notLoading());
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
