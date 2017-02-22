package com.saado.rotem.messageq;

import android.app.AlertDialog;
import android.content.Context;

import java.util.Random;

// This class add general functionality, to the application to prevent reuse of code.
public class AppHelper {

    private static Random randomColorGenerator = new Random();
    private static final int NUMBER_OF_COLOR = 4;

    // Generate color to the picture of the signed user
    public static int generateRandomColor(){
        return randomColorGenerator.nextInt(NUMBER_OF_COLOR);
    }


    // Function that builds alert dialog
    public static AlertDialog buildAlertDialog(String title, String message, boolean isCancelable, Context context){

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message)
                .setTitle(title);

        if(isCancelable){
            builder.setPositiveButton(android.R.string.ok, null);
        }else {
            builder.setCancelable(false);
        }
        return builder.create();
    }
}
