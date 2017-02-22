package com.saado.rotem.messageq;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Date;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener, GoogleApiClient.OnConnectionFailedListener {

    // Private members
    private static final String TAG = LoginActivity.class.getSimpleName();
    private static final int RC_SIGN_IN = 1;
    private EditText mLoginUserEmail, mLoginPassword;
    private TextView mCreateAccount, mLostPassword;
    private Button mLoginButton;
    private AlertDialog mDialog;
    private FirebaseAuth mAuth;
    private SignInButton mSignInButton;
    private GoogleApiClient mGoogleApiClient;
    private DatabaseReference mDatabaseReference;


    @Override
    // When a loginActivity page created
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Get view reference
        mLoginUserEmail = (EditText) findViewById(R.id.loginUserEmail);
        mLoginPassword = (EditText) findViewById(R.id.loginPassword);
        mCreateAccount = (TextView) findViewById(R.id.createAccount);
        mLostPassword = (TextView) findViewById(R.id.lostPassword);
        mLoginButton = (Button) findViewById(R.id.loginBtn);

        // Set the dimensions of the sign-in button.
        mSignInButton = (SignInButton) findViewById(R.id.googleSignInBtn);
        mSignInButton.setSize(SignInButton.SIZE_STANDARD);

        mSignInButton.setOnClickListener(this);
        mLoginButton.setOnClickListener(this);
        mCreateAccount.setOnClickListener(this);

        mAuth = FirebaseAuth.getInstance();
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // Configure google client
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            // When Login Button clicked
            case R.id.loginBtn:
                String userEmail = mLoginUserEmail.getText().toString();
                String userPassword = mLoginPassword.getText().toString();

                if (userEmail.equals("") || userPassword.equals("")) {

                    mDialog = AppHelper.buildAlertDialog(getString(R.string.login_error_title),
                            getString(R.string.login_error_message), true, this);
                    mDialog.show();

                } else {
                    executeEmailLogin(userEmail, userPassword);
                }
                break;
            // When createAccount Button clicked
            case R.id.createAccount:
                Intent intent = new Intent(this, RegisterActivity.class);
                startActivity(intent);
                break;
            // When Google sign in Button clicked
            case R.id.googleSignInBtn:
                executeGoogleSignIn();
                break;
        }

    }

    // Execute E-mail login process
    private void executeEmailLogin(String email, String password) {

        mDialog = AppHelper.buildAlertDialog(getString(R.string.login_title),
                getString(R.string.login_message), false, this);
        mDialog.show();
        // Authenticate with Firebase
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                mDialog.dismiss();
                checkLoginSuccess(task, false);

            }
        });
    }
    // Execute Google Sign in process
    private void executeGoogleSignIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }


    @Override
    // Gets the google sign in result
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            } else {
                // Google Sign In failed
                Toast.makeText(this, "Google Sign In failed", Toast.LENGTH_LONG).show();
            }

        }
    }
    // Authenticate with Firebase
    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {

        mDialog = AppHelper.buildAlertDialog(getString(R.string.login_title),
                getString(R.string.login_message), false, this);
        mDialog.show();
        // Get credentials and check if sucsess
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());
                        mDialog.dismiss();
                        checkLoginSuccess(task, true);
                    }
                });

    }

    // Check Google sign in login Success
    private void checkLoginSuccess(@NonNull Task<AuthResult> task, boolean isGoogleSignIn)
    {
        if (task.isSuccessful()) {
//            if (mAuth.getCurrentUser() != null) {
//                String userId = mAuth.getCurrentUser().getUid();
//            }
            // Add the user to the Firebase DB
            if(isGoogleSignIn)
            {
                String userId = mAuth.getCurrentUser().getUid();
                String displayName =  mAuth.getCurrentUser().getDisplayName();
                String userEmail =  mAuth.getCurrentUser().getEmail();
                Uri photoUrl = mAuth.getCurrentUser().getPhotoUrl();
                User user = new User(displayName, userEmail, "", new Date().getTime(), photoUrl.toString());
                mDatabaseReference.child("users").child(userId).setValue(user);
            }
            // Goto MainActivity
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        // Otherwise - show a dialog with Firebase error message
        } else {
            mDialog = AppHelper.buildAlertDialog(getString(R.string.login_title),
                    task.getException().getMessage(), true, LoginActivity.this);
            mDialog.show();
        }
    }

    @Override
    // Error when connection failed on Firebase
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        Toast.makeText(this, "You got an Error", Toast.LENGTH_LONG).show();
    }
}

