package com.helloenglish.randomcaller.Activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.helloenglish.randomcaller.R;

import com.google.firebase.auth.FirebaseAuth;

public class Splash extends AppCompatActivity {
    FirebaseAuth fAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        fAuth = FirebaseAuth.getInstance();

        //Check if User is already logged in or not
        if(fAuth.getUid() != null){
            startActivity(new Intent(Splash.this, StartCallActivity.class));
            finish();
        } else {
            login();
        }



    }

    // We need user to sign in because when we will try to connect our
    // call with other users, we need a unique id for every user
    // By using Firebase Auth we get a UID to use it as unique user

    private void login(){
        // Sign in anonymously so that we don't need to input any user info
        fAuth.signInAnonymously().addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                startActivity(new Intent(Splash.this, StartCallActivity.class));
                finishAffinity();
            } else {
                Toast.makeText(this, task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}