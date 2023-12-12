package com.helloenglish.randomcaller.Helpers;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.helloenglish.randomcaller.Models.CallReqModel;
import com.helloenglish.randomcaller.Models.Constants;
import com.helloenglish.randomcaller.Activity.CallActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.util.UUID;

import javax.inject.Singleton;

@Singleton
public class FindCallerHelper {
    private Activity activity;
    private DatabaseReference dbRef;
    private ValueEventListener callReceiveListener;
    private String lastCallerId;
    private String myUid;

    public FindCallerHelper(Activity activity, String callType) {
        this.activity = activity;
        dbRef = FirebaseDatabase.getInstance().getReference("callRequests").child(callType);
        myUid = FirebaseAuth.getInstance().getUid();
    }

    public void findRandomCaller() {

        //Check if there is any user trying to call or not
        dbRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                CallReqModel callReqModel = null;
                if (currentData.getChildrenCount() > 0) {
                    //Converting previous call request to CallReqModel Class
                    for (DataSnapshot snapshot : currentData.getChildren()) {
                        if (snapshot.child("status").getValue(String.class).equals(Constants.WAITING)) {
                            callReqModel = snapshot.getValue(CallReqModel.class);
                            break;
                        }
                    }
                }
                //If it's null, then no caller is active
                if (callReqModel == null) {
                    Log.d("FindRandomCaller", "callReqModel is null, starting new call");
                    startNewCall();
                } else { //Already has a caller, let's connect to him
                    if (callReqModel.getCallerId().equals(myUid)) { // Check if the call request is from my side or not
                        startNewCall();
                    } else {
                        connectCall(callReqModel);
                    }
                }
            }
        });
    }

    ;

    public void cancelFinding() {
        //We delete our call request from the database
        if(dbRef!=null){
            dbRef.child(myUid).removeValue();
            if (callReceiveListener != null) {
                dbRef.removeEventListener(callReceiveListener);
            }
        }
    }

    public void reConnectLastCaller() {
        lastCallerId = SpHelper.getPrefs(activity).getLastCaller();
        checkForRemoteReconnect();
    }

    private void checkForRemoteReconnect() {
        // Let's check if the remote user is already placed a request for reconnect
        dbRef.child(lastCallerId).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                CallReqModel callReqModel = null;
                if (currentData.getValue()!=null) {
                    // Converting previous call request to CallReqModel class after
                    // checking if he is looking for me or not
                    if (currentData.child("receiverId").getValue(String.class).equals(myUid)) {
                        callReqModel = currentData.getValue(CallReqModel.class);
                    }
                }
                //If it's null, then he other peer didn't make a request, so we create and wait for him
                if (callReqModel == null) { //Let's start a new call request
                    Log.d("FindRandomCaller", "callReqModel is null, starting new call");
                    startNewCall();
                } else { //Already has a caller, let's connect to him
                    connectCall(callReqModel);
                }
            }
        });
    }

    public void startNewCall() {
        dbRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                // Setting our ID as a new callRequest child
                // If we are reconnecting, then last caller id will not be null
                // Otherwise it will be null
                currentData.child(myUid)
                        .setValue(new CallReqModel(myUid, lastCallerId, Constants.WAITING, getRandomId()));
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (committed) {
                    Log.d("FindRandom", "New call request sent and waiting");
                    listenForChanges(); // Waiting for other user to connect
                } else {
                    cancelAndClose();
                }
            }
        });
    }

    private String getRandomId() {
        return UUID.randomUUID().toString();
    }

    private void listenForChanges() {
        callReceiveListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.getValue()!=null){
                    if (lastCallerId == null) { // if lastCallerId == null, then we know that user is not trying to reconnect
                        if (snapshot.child("receiverId").getValue(String.class) != null) { //If we get a receiver, then we forward to next activity to start a call
                            startCallActivity(snapshot.child("receiverId").getValue(String.class),
                                    snapshot.child("callId").getValue(String.class),
                                    true);
                            dbRef.child(myUid).removeEventListener(this);
                            activity.finish();
                        }
                    } else {
                        if (snapshot.child("status").getValue(String.class).equals(Constants.RECONNECT_LAST_USER)) { //If we get a receiver, then we forward to next activity to start a call
                            startCallActivity(snapshot.child("receiverId").getValue(String.class),
                                    snapshot.child("callId").getValue(String.class),
                                    true);
                            dbRef.child(myUid).removeEventListener(this);
                            activity.finish();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        dbRef.child(myUid)
                .addValueEventListener(callReceiveListener);
    }

    public void connectCall(CallReqModel callReqModel) {
        if (callReqModel != null) {
            Log.d("CallReqModel", new Gson().toJson(callReqModel));
            if (lastCallerId == null) {
                dbRef.child(callReqModel.getCallerId())
                        .child("receiverId")
                        .setValue(myUid)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                startCallActivity(callReqModel.getCallerId(), callReqModel.getCallId(), false);
                                activity.finish();
                            } else {
                                Toast.makeText(activity, task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                cancelAndClose();
                            }
                        });
            } else {
                dbRef.child(callReqModel.getCallerId())
                        .child("status")
                        .setValue(Constants.RECONNECT_LAST_USER)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                startCallActivity(callReqModel.getCallerId(), callReqModel.getCallId(), false);
                                activity.finish();
                            } else {
                                Toast.makeText(activity, task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                cancelAndClose();
                            }
                        });
            }
        }
    }

    private void startCallActivity(String receiverId, String callId, boolean shouldCreateOffer) {
        // Let's save the remote user id for later
        SpHelper.getPrefs(activity).setLastCaller(receiverId);

        // Starting the call activity with remote user's id
        Intent in = new Intent(activity, CallActivity.class);
        in.putExtra(Constants.REMOTE_CALLER_ID, receiverId);
        in.putExtra(Constants.SHOULD_CREATE_OFFER, shouldCreateOffer);
        in.putExtra(Constants.CALL_ID, callId);
        activity.startActivity(in);
    }

    private void cancelAndClose() {
        cancelFinding();
        activity.finish();
    }
}
