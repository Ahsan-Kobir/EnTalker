package com.akapps.randomcaller.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.akapps.randomcaller.Models.User;
import com.akapps.randomcaller.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.FirebaseDatabase;

public class Splash extends AppCompatActivity {

    GoogleSignInClient mGoogleSignInClient;
    int ReqestCode = 77;
    FirebaseAuth fAuth;
    FirebaseDatabase fdb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        fAuth = FirebaseAuth.getInstance();
        fdb = FirebaseDatabase.getInstance();


        if(fAuth.getUid() != null){
            startActivity(new Intent(Splash.this, MainActivity.class));
            finishAffinity();
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);


        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findViewById(R.id.button).setEnabled(false);
                Intent in = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(in, ReqestCode);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==ReqestCode && resultCode == -1){
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            GoogleSignInAccount account = task.getResult();
            login(account.getIdToken());
        } else {
            findViewById(R.id.button).setEnabled(true);
        }
    }

    private void login(String idToken){
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        fAuth.signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()){
                    FirebaseUser fUser = fAuth.getCurrentUser();
                    Log.d("Name", fUser.getDisplayName());
                    User user = new User(fUser.getUid(), fUser.getDisplayName(), fUser.getPhotoUrl().toString(), "Las Vegas", 50000);
                    fdb.getReference()
                                    .child("profiles")
                                    .child(user.getUid())
                                    .setValue(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()){
                                        startActivity(new Intent(Splash.this, MainActivity.class));
                                        finishAffinity();
                                    } else {
                                        Toast.makeText(getApplicationContext(), task.getException().getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                    }
                                }
                            });

                } else {
                    task.getException().printStackTrace();
                }
            }
        });
    }
}