package com.helloenglish.randomcaller.Helpers;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.helloenglish.randomcaller.Interfaces.IceDataObserver;
import com.helloenglish.randomcaller.Interfaces.OfferListener;
import com.helloenglish.randomcaller.Interfaces.SuccessListener;
import com.helloenglish.randomcaller.Models.SignalType;
import com.helloenglish.randomcaller.Models.SingalingModel;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

public class FirebaseController {

    String TAG = "FirebaseController";
    private DatabaseReference offerDbRef;
    private DatabaseReference iceDbRef;
    private Gson gson;
    private String myUid;
    private String remoteUserId;
    private ValueEventListener iceDataListener;
    private ValueEventListener offerDataListener;

    public FirebaseController(FirebaseDatabase firebaseDatabase, String remoteUserId) {
        this.offerDbRef = firebaseDatabase.getReference("signaling").child("offer");
        this.iceDbRef = firebaseDatabase.getReference("signaling").child("ice");
        this.myUid = FirebaseAuth.getInstance().getUid();
        this.remoteUserId = remoteUserId;
        gson = new Gson();
    }

    public void sendIceCandidate(IceCandidate iceCandidate, String to, String callId, SuccessListener listener){
        //Send the ice candidate to other peer
        SingalingModel model = new SingalingModel(to, SignalType.ICE.name(), gson.toJson(iceCandidate), callId);
        iceDbRef.child(myUid)
                .setValue(model)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        listener.onSent();
                    } else {
                        listener.onFailed(task.getException().getLocalizedMessage());
                    }
                });
    }

    public void sendOfferData(SessionDescription sessionDescription, SignalType signalType, String to, String callId, SuccessListener listener){
        //Send the SDP data to other peer
        SingalingModel model = new SingalingModel(to, signalType.name(), gson.toJson(sessionDescription), callId);
        offerDbRef.child(myUid)
                .setValue(model)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        listener.onSent();
                    } else {
                        listener.onFailed(task.getException().getLocalizedMessage());
                    }
                });
    }

    public void listenForRemoteOfferData(OfferListener listener){
        offerDataListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    SingalingModel model = snapshot.getValue(SingalingModel.class);
                    if(model==null){
                        Log.e(TAG, "listenForAccept got null value model");
                    } else {
                        if(model.getDataType().equals(SignalType.ANSWER.name())){
                            SessionDescription sessionDescription = gson.fromJson(model.getData(), SessionDescription.class);
                            listener.onReceiveSignal(sessionDescription);
                        } else if(model.getDataType().equals(SignalType.OFFER.name())){
                            SessionDescription sessionDescription = gson.fromJson(model.getData(), SessionDescription.class);
                            listener.onReceiveSignal(sessionDescription);
                        } else {
                            Log.e(TAG, "Received other kind of data : " + model.getDataType());
                        }
                    }
                } else {
                    Log.e(TAG, "snapshot doesn't exist");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "onCancelled in offerDataListener " + error.getDetails());
            }
        };
        offerDbRef.child(remoteUserId)
                .addValueEventListener(offerDataListener);
    }

    public void stopSignaling(){
        // When we cut the call, we delete the signaling data from Realtime Database
        // and stop observing the remote user database reference

        if(offerDbRef!=null){
            if(offerDataListener!=null) {
                offerDbRef.child(remoteUserId).removeEventListener(offerDataListener);
            }
            offerDbRef.child(myUid)
                    .removeValue();
        }

        if(iceDbRef!=null){
            if(iceDataListener!=null) {
                iceDbRef.child(remoteUserId).removeEventListener(iceDataListener);
            }
            iceDbRef.child(myUid)
                    .removeValue();
        }
    }

    public void listenForIceData(IceDataObserver iceDataObserver){
        iceDataListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                SingalingModel model = snapshot.getValue(SingalingModel.class);
                if(model!=null && model.getDataType().equals(SignalType.ICE.name())){
                    // If the data is a ICE candidate, we set it to our WebRTC Client
                   iceDataObserver.onIceReceived(model);
                } else {
                    Log.e(TAG, "iceDataListener Model is null or data is not ICE");
                    Log.e("SIGNALDATA", gson.toJson(model));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "onCancelled listenForIceData" + error.getMessage());
            }
        };
        // We observe the remote peer Database Reference for any changes he made for signaling
        // and the other peer is observing this peer's Database Reference for getting the signaling data
        //
        // This way we use Firebase similar like WebSOCKET

        iceDbRef.child(remoteUserId)
                .addValueEventListener(iceDataListener);
    }

    public void stopOfferListening() {
        if(offerDbRef!=null){
            if(offerDataListener!=null) {
                offerDbRef.child(remoteUserId).removeEventListener(offerDataListener);
            }
            offerDbRef.child(myUid)
                    .removeValue();
        }
    }
}
