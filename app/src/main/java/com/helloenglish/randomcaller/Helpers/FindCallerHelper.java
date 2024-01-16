package com.helloenglish.randomcaller.Helpers;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.helloenglish.randomcaller.Interfaces.CallerFindListener;
import com.helloenglish.randomcaller.Models.CallReqModel;
import com.helloenglish.randomcaller.Models.Constants;

import java.util.UUID;

import javax.inject.Singleton;

public class FindCallerHelper {
    private Activity activity;
    private DatabaseReference dbRef;
    private ValueEventListener callReceiveListener;
    private String lastCallerId;
    private final String myUid;
    private boolean shouldWait = false;
    private CallReqModel currentCall;

    private CallerFindListener callerFindListener;

    public FindCallerHelper(Activity activity, String callType, CallerFindListener callerFindListener) {
        this.activity = activity;
        this.callerFindListener = callerFindListener;
        dbRef = FirebaseDatabase.getInstance().getReference("callRequests").child(callType);
        myUid = FirebaseAuth.getInstance().getUid();
    }

    public void findRandomCaller() {
        //Check if there is any user trying to call or not
        dbRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData currentData) {
                CallReqModel callReqModel = null;
                currentCall = null;
                if (currentData.getChildrenCount() > 0) {
                    //Converting previous call request to CallReqModel Class
                    for (MutableData snapshot : currentData.getChildren()) {
                        if (snapshot.child("status").getValue(String.class).equals(Constants.WAITING)) {
                            // We got a callRequest who is waiting
                            callReqModel = snapshot.getValue(CallReqModel.class);
                            break;
                        }
                    }
                }
                //If it's null, then no caller is waiting
                if (callReqModel == null) {
                    Log.d("FindRandomCaller", "callReqModel is null, starting new call");
                    currentData.child(myUid)
                            .setValue(new CallReqModel(myUid, lastCallerId, Constants.WAITING, getRandomId()));
                    shouldWait = true;
                } else { //Already has a caller, let's connect to him
                    if (callReqModel.getCallerId().equals(myUid)) { // Check if the call request is from my side or not
                        currentData.child(myUid)
                                .setValue(new CallReqModel(myUid, lastCallerId, Constants.WAITING, getRandomId()));
                        shouldWait = true;
                    } else {
                        connectCall(currentData, callReqModel);
                        currentCall = callReqModel;
                        shouldWait = false;
                    }
                }
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                if(committed){
                    if (shouldWait){
                        listenForChanges();
                    } else {
                        startCallActivity(currentCall.getCallerId(), currentCall.getCallId(), false);
                    }
                } else {
                    Log.e("FindCallHelper", error.getDetails());
                    Toast.makeText(activity, error.getMessage(), Toast.LENGTH_SHORT).show();
                    cancelAndClose();
                }
            }
        });
    }

    private void connectCall(MutableData mutableData, CallReqModel callReqModel) {
        if (lastCallerId == null) {
            mutableData.child(callReqModel.getCallerId())
                    .child("receiverId")
                    .setValue(myUid);
            mutableData.child(callReqModel.getCallerId())
                    .child("status")
                    .setValue(Constants.IN_CALL);
        } else {
            mutableData.child(callReqModel.getCallerId())
                    .child("status")
                    .setValue(Constants.RECONNECT_LAST_USER);
        }
    }

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
        dbRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData currentData) {
                CallReqModel callReqModel = null;
                currentCall = null;
                if (currentData.child(lastCallerId).getValue()!=null) {
                    // Converting previous call request to CallReqModel class and
                    // checking if he is looking for me or not
                    if (currentData.child(lastCallerId).child("receiverId").getValue(String.class).equals(myUid)) {
                        callReqModel = currentData.child(lastCallerId).getValue(CallReqModel.class);
                    }
                }
                //If it's null, then other peer didn't make a request, so we create and wait for him
                if (callReqModel == null) { //Let's start a new call request
                    currentData.child(myUid)
                            .setValue(new CallReqModel(myUid, lastCallerId, Constants.WAITING, getRandomId()));
                    shouldWait = true;
                } else { //Already has a caller, let's connect to him
                    connectCall(currentData, callReqModel);
                    currentCall = callReqModel;
                    shouldWait = false;
                }
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                if(committed){
                    if (shouldWait){
                        listenForChanges();
                    } else {
                        startCallActivity(currentCall.getCallerId(), currentCall.getCallId(), false);
                    }
                } else {
                    Log.e("FindCallHelper", error.getDetails());
                    Toast.makeText(activity, error.getMessage(), Toast.LENGTH_SHORT).show();
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
            public void onDataChange(DataSnapshot snapshot) {
                if(snapshot.getValue()!=null){
                    if (lastCallerId == null) { // if lastCallerId == null, then we know that user is not trying to reconnect
                        if (snapshot.child("receiverId").getValue(String.class) != null) { //If we get a receiver, then we forward to next activity to start a call
                            startCallActivity(snapshot.child("receiverId").getValue(String.class),
                                    snapshot.child("callId").getValue(String.class),
                                    true);
                            dbRef.child(myUid).removeEventListener(this);
                        }
                    } else {
                        if (snapshot.child("status").getValue(String.class).equals(Constants.RECONNECT_LAST_USER)) { //If we get a receiver, then we forward to next activity to start a call
                            startCallActivity(snapshot.child("receiverId").getValue(String.class),
                                    snapshot.child("callId").getValue(String.class),
                                    true);
                            dbRef.child(myUid).removeEventListener(this);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("FindCallerHelper", error.getDetails());
            }
        };
        dbRef.child(myUid)
                .addValueEventListener(callReceiveListener);
    }


    private void startCallActivity(String receiverId, String callId, boolean shouldCreateOffer) {
        SpHelper.getPrefs(activity).setLastCaller(receiverId);
        callerFindListener.onFound(receiverId, callId, shouldCreateOffer);
        lastCallerId = null;
        cancelFinding();
    }

    private void cancelAndClose() {
        cancelFinding();
        activity.finish();
    }
}
