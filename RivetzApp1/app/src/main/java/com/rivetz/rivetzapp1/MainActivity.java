package com.rivetz.rivetzapp1;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
import com.rivetz.api.SPID;
import com.rivetz.bridge.Rivet;
import com.rivetz.api.RivetInterface;

public class MainActivity extends AppCompatActivity {
    Rivet rivet;
    byte[] realmessage;

    //creates and pairs a Rivet if necessary
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rivet = new Rivet(this, SPID.DEVELOPER_TOOLS_SPID);

        if (rivet != null) {
            if (!rivet.isPaired()) {
                rivet.pairDevice(this);
            }
        }

    }

    //checks if the Rivet was successfully paired
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Rivet.INSTRUCT_PAIRDEVICE) {

            if (resultCode == RESULT_CANCELED) {
                alert("Pairing error: " + String.valueOf(resultCode));
            }
            if (resultCode == RESULT_OK) {
                alert("Paired");
            }
        }
        notLoading();
        if (rivet.getKey("SignKey") == null){
            makeUnclickable(findViewById(R.id.sign));
            makeUnclickable(findViewById(R.id.checkAuthenticity));
        }

        else{
            makeUnclickable(findViewById(R.id.createKey));

        }

    }


    //signs the real message asynchronously
    public void sign(View v) {
        EditText real = findViewById(R.id.real);
        rivet.signAsync("SignKey",real.getText().toString().getBytes()).whenComplete(this::signComplete);
        makeUnclickable(findViewById(R.id.sign));
        loading();
    }



    //callback when the real message is done signing which checks if the signing was successful
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




    //verifies if the real message is authentic asynchronously using its signature
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



    //callback for when the verification is done which checks if the message is authentic or not and returns an alert accordingly
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



    //creates a Key
    public void createKey(View v) {

        try {
            rivet.createKeySync(RivetInterface.KeyType.ECDSA_NISTP256, "SignKey", RivetInterface.UsageRule.REQUIRE_TUI_CONFIRM);
        }

        catch (Exception e){
            alert(e.getMessage());
        }
    }

    //Helper functions


    //creates an alert with some text
    public void alert(String text) {
        new AlertDialog.Builder(this)
                .setMessage(text)
                .create().show();
    }

    //makes a button unclickable
    public void makeUnclickable(Button button){
        button.setAlpha(.5f);
        button.setClickable(false);
    }

    //makes a button clickable
    public void makeClickable(Button button){
        button.setAlpha(1f);
        button.setClickable(true);
    }

    //shows a loading animation
    public void loading(){
        findViewById(R.id.loading).setVisibility(View.VISIBLE);
    }

    //makes the loading animation invisible
    public void notLoading(){
        findViewById(R.id.loading).setVisibility(View.GONE);
    }


    //converts a byte array to hexadecimals
    public static String bytesToHex(byte[] bytes) {
        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];

        for ( int j = 0; j < bytes.length; j++ ) {

            int v = bytes[j] & 0xFF;

            hexChars[j * 2] = hexArray[v >>> 4];

            hexChars[j * 2 + 1] = hexArray[v & 0x0F];

        }


        return new String(hexChars);

    }


}