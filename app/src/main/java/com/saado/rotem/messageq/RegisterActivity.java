package com.saado.rotem.messageq;

import android.app.AlertDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Date;

public class RegisterActivity extends AppCompatActivity {

    // Private members
    private static final String TAG = RegisterActivity.class.getSimpleName();
    private EditText mDisplayName, mRegisterUserEmail, mRegisterPassword;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabaseReference;
    private AlertDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Get view reference
        mDisplayName = (EditText) findViewById(R.id.registerDisplayName);
        mRegisterUserEmail = (EditText) findViewById(R.id.registerUserEmail);
        mRegisterPassword = (EditText) findViewById(R.id.registerPassword);


        mAuth = FirebaseAuth.getInstance();
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
    }
    // When the user clicks on the register button
    public void register(View view) {

        // Get the fields value
        String displayName = mDisplayName.getText().toString();
        String userEmail = mRegisterUserEmail.getText().toString();
        String userPassword = mRegisterPassword.getText().toString();

        // Check if the fields are empty
        if(displayName.equals("") || userEmail.equals("") || userPassword.equals("")){

            mDialog = AppHelper.buildAlertDialog(getString(R.string.register_error_title),
                    getString(R.string.register_error_message), true, this);
            mDialog.show();

        }
        // if the firebase accepts the values
        else if(isIncorrectValues(userEmail, userPassword)) {

            mDialog = AppHelper.buildAlertDialog(getString(R.string.register_error_title),
                    getString(R.string.register_error_incorrect_values), true, this);
            mDialog.show();
        }else {

            executeSignUp(userEmail, userPassword);
        }

    }
    // Validation on the input
    private boolean isIncorrectValues(String userEmail, String userPassword) {

        return userPassword.length() < 6 || !android.util.Patterns.EMAIL_ADDRESS.matcher(userEmail).matches();
    }
    // Executes the SignUp
    private void executeSignUp(String userEmail, String userPassword) {

        mDialog = AppHelper.buildAlertDialog(getString(R.string.register_title),
                getString(R.string.register_message), false, this);
        mDialog.show();

        // Set the values to firebase
        mAuth.createUserWithEmailAndPassword(userEmail, userPassword).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                mDialog.dismiss();

                if(task.isSuccessful()){
                    // If task success add a user to firebase db and start the MainActivity
                    addNewUser(task.getResult().getUser().getUid());

                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                }else {
                    // If the task not succeeded show an error
                    mDialog = AppHelper.buildAlertDialog(getString(R.string.register_title),
                            task.getException().getMessage(), true, RegisterActivity.this);
                    mDialog.show();
                }
            }
        });
    }
    // Add a new user to Firebase DB
    private void addNewUser(String userId) {

        String displayName = mDisplayName.getText().toString();
        String userEmail = mRegisterUserEmail.getText().toString();
        User user = new User(displayName, userEmail, "", new Date().getTime(), User.NO_URI);
        mDatabaseReference.child("users").child(userId).setValue(user);
    }

    // When the user clicks on the cancel button
    public void cancel(View view) {
        finish();
    }
}
