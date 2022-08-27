package com.akapps.randomcaller.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import android.view.View;

import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.akapps.randomcaller.Models.User;
import com.akapps.randomcaller.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class MainActivity extends AppCompatActivity {
    LottieAnimationView startButton;
    FirebaseDatabase fdb;
    FirebaseAuth fauth;
    TextView coinTv;
    int coin = 0;
    private String[] permissions = new String[] {Manifest.permission.RECORD_AUDIO};
    private int permReqCodee = 11;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fauth = FirebaseAuth.getInstance();
        fdb = FirebaseDatabase.getInstance();
        coinTv = (TextView) findViewById(R.id.coinTextView);
        startButton = findViewById(R.id.callAnimation);

        FirebaseUser firebaseUser = fauth.getCurrentUser();
        fdb.getReference().child("profiles").child(fauth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        coin = user.getCoin();
                        coinTv.setText(String.valueOf(coin));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isPermGranted()){
                    if(coin >= 5){
                        fdb.getReference().child("profiles").child(fauth.getUid()).child("coin").setValue(coin - 5);
                        startActivity(new Intent(MainActivity.this, FindingActivity.class));
                    } else {
                        Toast.makeText(getApplicationContext(), "Insufficient Balance", Toast.LENGTH_LONG).show();
                    }
                } else{
                    askPermission();
                }
            }
        });

    }

    void askPermission(){
        ActivityCompat.requestPermissions(this, permissions, permReqCodee);
    }

    private boolean isPermGranted(){
        for(String perm : permissions){
            if(ActivityCompat.checkSelfPermission(this, perm)  != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }
}