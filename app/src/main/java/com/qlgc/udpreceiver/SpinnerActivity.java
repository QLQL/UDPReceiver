package com.qlgc.udpreceiver;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Qingju on 21/02/2017.
 */
public class SpinnerActivity extends Activity implements AdapterView.OnItemSelectedListener {
    //public final static String KEY_TO_STRING = "QLQLQLQL";


    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
        String currentFs = parent.getItemAtPosition(pos).toString();
        //Toast.makeText(parent.getContext(), "The sampling rate is: " + currentFs, Toast.LENGTH_SHORT).show();

//        double S0 = Double.parseDouble(currentFs);
        // float f = Float.valueOf("> 12.4N-m/kg.".replaceAll(currentFs, ""));
        Pattern p = Pattern.compile("[0-9]*\\.?[0-9]+");
        Matcher m = p.matcher(currentFs);
        if (m.find()) {
            float f = Float.parseFloat(m.group());
            MainActivity.sampleRate = (int)(f*1000);
            Toast.makeText(parent.getContext(), "The sampling rate is: " + Integer.toString(MainActivity.sampleRate), Toast.LENGTH_SHORT).show();
            //            //Sender Side:
            //            Intent myIntent = new Intent(this,MainActivity.class);
            //            myIntent.putExtra("QLQLQLQL", sampleRate);
            //            startActivity(myIntent);
        }
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }
}