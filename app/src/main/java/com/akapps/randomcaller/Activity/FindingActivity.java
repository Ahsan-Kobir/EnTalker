package com.akapps.randomcaller.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.akapps.randomcaller.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Random;

public class FindingActivity extends AppCompatActivity {

    FirebaseAuth fAuth;
    FirebaseDatabase fdb;
    int roomId;
    private Boolean gonnaJoin = false;
    private Boolean roomCreated = false;
    String username;

    boolean gotIt = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finding);



        fAuth = FirebaseAuth.getInstance();
        fdb = FirebaseDatabase.getInstance();
        username = fAuth.getUid();
        fdb.getReference().child("online")
                .orderByChild("status")
                .equalTo(0).limitToFirst(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.getChildrenCount() > 0){
                            //Ready to join existing room
                            if(gotIt)
                                return;
                            gotIt =  true;
                            for(DataSnapshot childSnap : snapshot.getChildren()){
                                fdb.getReference()
                                        .child("online")
                                        .child(childSnap.getKey()) ///getting uid of  remote user
                                        .child("incoming")
                                        .setValue(username); //setting own uid
                                fdb.getReference()
                                        .child("online")
                                        .child(childSnap.getKey())
                                        .child("status")
                                        .setValue(1);
                                Intent intent = new Intent(FindingActivity.this, CallActivity.class);
                                //intent.putExtra("username", username);
                                intent.putExtra("remoteUser", childSnap.child("createdBy").getValue(String.class));
                                //intent.putExtra("createdBy", childSnap.child("createdBy").getValue(String.class));
                                //intent.putExtra("isAvailable", childSnap.child("isAvailable").getValue(Boolean.class));
                                intent.putExtra("roomId", childSnap.child("roomId").getValue(Integer.class));
                                fdb.getReference()
                                        .child("online")
                                        .child(username).removeEventListener(this);
                                startActivity(intent);
                                finish();

                            }
                        } else {
                            //No rooms avail... Create a new one
                            Random random = new Random();
                            roomId = random.nextInt(999999999);


                            HashMap<String, Object> map = new HashMap<>();
                            map.put("incoming", username);
                            map.put("createdBy", username);
                            map.put("isAvailable", true);
                            map.put("status", 0);
                            map.put("roomId", roomId);
                            fdb.getReference()
                                    .child("online")
                                    .child(username)
                                    .setValue(map).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {

                                            roomCreated = true;

                                            fdb.getReference()
                                                    .child("online")
                                                    .child(username)
                                                    .addValueEventListener(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                            if(snapshot.child("status").exists()){
                                                                if(snapshot.child("status").getValue(Integer.class) == 1){

                                                                    if (gotIt){
                                                                        return;
                                                                    }
                                                                    gotIt = true;

                                                                    Intent intent = new Intent(FindingActivity.this, CallActivity.class);
                                                                    //intent.putExtra("username", username);
                                                                    intent.putExtra("remoteUser", snapshot.child("incoming").getValue(String.class));
                                                                    //intent.putExtra("createdBy", snapshot.child("createdBy").getValue(String.class));
                                                                    //intent.putExtra("isAvailable", snapshot.child("isAvailable").getValue(Boolean.class));
                                                                    intent.putExtra("roomId", roomId);
                                                                    gonnaJoin = true;
                                                                    fdb.getReference()
                                                                            .child("online")
                                                                            .child(username).removeEventListener(this);
                                                                    deleteOnline();

                                                                    startActivity(intent);
                                                                    finish();
                                                                }
                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError error) {

                                                        }
                                                    });
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    @Override
    protected void onPause() {
        Log.d("AKits", "onPause callled");
        super.onPause();

        if(roomCreated && !gonnaJoin){
            //delete the waiting room
            fdb.getReference()
                    .child("online")
                    .child(username).setValue(null);
            finish();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    private void deleteOnline(){
        fdb.getReference()
                .child("online")
                .child(username).setValue(null);
    }

}