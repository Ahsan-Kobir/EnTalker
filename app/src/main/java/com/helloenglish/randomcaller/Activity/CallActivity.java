package com.helloenglish.randomcaller.Activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.helloenglish.randomcaller.Helpers.FindCallerHelper;
import com.helloenglish.randomcaller.Helpers.FirebaseController;
import com.helloenglish.randomcaller.Interfaces.CallerFindListener;
import com.helloenglish.randomcaller.Interfaces.IceDataObserver;
import com.helloenglish.randomcaller.Interfaces.SuccessListener;
import com.helloenglish.randomcaller.Models.Constants;
import com.helloenglish.randomcaller.Models.SignalType;
import com.helloenglish.randomcaller.Models.SingalingModel;
import com.helloenglish.randomcaller.R;
import com.helloenglish.randomcaller.WebRTC.AudioManagerHelpers.RTCAudioManager;
import com.helloenglish.randomcaller.WebRTC.MyPeerConnectionObserver;
import com.helloenglish.randomcaller.WebRTC.WebRtcClient;
import com.google.firebase.database.FirebaseDatabase;

import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.RtpTransceiver;

import java.util.Timer;
import java.util.TimerTask;


public class CallActivity extends AppCompatActivity {

    String TAG = "CallActivity";
    private ImageView endButton, muteButton, speakerButton;
    private TextView timeText, idText;

    private boolean loudSpeaker, isMuted = false;
    private Timer timer;
    private TimerTask timerTask;

    private int counter;

    private WebRtcClient webRtcClient;
    private RTCAudioManager rtcAudioManager;
    private String remoteUserId, myUid;
    String callId;
    private FirebaseController firebaseController;

    private FindCallerHelper findCallerHelper;
    private ConstraintLayout callViews, searchingViews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        initViewById();

        String callType = getIntent().getStringExtra("callType");

        startFindingPartner(callType);

//        // Check if the activity received a valid call id
//        if (callId.isEmpty()) {
//            Log.e(TAG, "empty call id or receiver id");
//            finishAffinity();
//            return;
//        }

    }

    private void connectCall(boolean shouldOffer){
        Log.d(TAG, "NEW CALL ID: " + callId);

        searchingViews.setVisibility(View.GONE);
        callViews.setVisibility(View.VISIBLE);

        initialize();

        firebaseController = new FirebaseController(FirebaseDatabase.getInstance(), remoteUserId, callId);

        // One peer needs to create offer and other needs to accept that
        // So we call createOffer() method with the shouldOffer boolean
        // that we received from StartCallActivity/FindCallHelper
        initOffer(shouldOffer);

        setUiHandlers();

        // Setting default speaker mode to loudSpeaker mode initially
        loudSpeaker = true;
        rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE);
    }
    private void initViewById(){
        endButton = findViewById(R.id.endButton);
        muteButton = findViewById(R.id.muteButton);
        speakerButton = findViewById(R.id.speakerButton);
        timeText = findViewById(R.id.timeCounter);
        idText = findViewById(R.id.idText);

        idText.setText(FirebaseAuth.getInstance().getUid());

//        remoteUserId = getIntent().getStringExtra(Constants.REMOTE_CALLER_ID);
//        callId = getIntent().getStringExtra(Constants.CALL_ID);
        myUid = FirebaseAuth.getInstance().getUid();

        callViews = findViewById(R.id.callViews);
        searchingViews = findViewById(R.id.searchingViews);

    }
    private void startFindingPartner(String callType) {
        findCallerHelper = new FindCallerHelper(this, callType, new CallerFindListener() {
            @Override
            public void onFound(String receiverId, String currentCallId, boolean shouldCreateOffer) {
                remoteUserId = receiverId;
                callId = currentCallId;
                connectCall(shouldCreateOffer);
            }
        });
        if(callType.equals(Constants.RECONNECT_LAST_USER)){
            findCallerHelper.reConnectLastCaller();
        } else {
            findCallerHelper.findRandomCaller();
        }
        searchingViews.setVisibility(View.VISIBLE);
        callViews.setVisibility(View.GONE);
    }

    private void initOffer(boolean shouldOffer){
        if(shouldOffer){
            webRtcClient.createOffer(offerSdp -> firebaseController.sendOfferData(offerSdp, SignalType.OFFER, remoteUserId, callId, new SuccessListener() {
                @Override
                public void onSent() {
                    firebaseController.listenForRemoteOfferData(sessionDescription -> {
                        webRtcClient.setRemoteSdp(sessionDescription);
                        firebaseController.stopOfferListening();
                        startExchangeIce();
                    });
                }

                @Override
                public void onFailed(String s) {
                    Log.e(TAG, "send offer failed for: " +s);
                }
            }));
        } else {
            firebaseController.listenForRemoteOfferData(sessionDescription -> {
                webRtcClient.setRemoteSdp(sessionDescription);
                webRtcClient.acceptOffer(answerSdp -> firebaseController.sendOfferData(answerSdp, SignalType.ANSWER, remoteUserId, callId, new SuccessListener() {
                    @Override
                    public void onSent() {
                        firebaseController.stopOfferListening();
                        startExchangeIce();
                    }

                    @Override
                    public void onFailed(String s) {
                        Log.e(TAG, "send answer failed for: " +s);
                    }
                }));
            });
        }
    }

    private void startExchangeIce(){
        firebaseController.listenForIceData(signalingModel -> {
            if(signalingModel.getCallId().equals(callId) && signalingModel.getTo().equals(myUid)){
                IceCandidate iceCandidate = new Gson().fromJson(signalingModel.getData(), IceCandidate.class);
                webRtcClient.addIceCandidate(iceCandidate);
            } else {
                Log.e(TAG, "ICE Found but FOR OTHER CALL or USER");
            }

        });
    }

    private void setUiHandlers(){

        endButton.setOnClickListener(view -> {
            leaveAndDestroy();
            finish();
        });

        muteButton.setOnClickListener(view -> {
            if (isMuted) {
                webRtcClient.setMuted(false);
                muteButton.setImageResource(R.drawable.btn_unmute_normal);
            } else {
                webRtcClient.setMuted(true);
                muteButton.setImageResource(R.drawable.btn_mute_normal);
            }
            // Toggle the boolean variable isMuted
            isMuted = !isMuted;

        });

        speakerButton.setOnClickListener(view -> {
            if (loudSpeaker) {
                setSpeakerPhone(RTCAudioManager.AudioDevice.EARPIECE);
                speakerButton.setImageResource(R.drawable.btn_speaker_ear3);
            } else {
                setSpeakerPhone(RTCAudioManager.AudioDevice.SPEAKER_PHONE);
                speakerButton.setImageResource(R.drawable.btn_speaker_loud3);
            }
            // Toggle the boolean variable loudSpeaker
            loudSpeaker = !loudSpeaker;
        });
    }

    private void setSpeakerPhone(RTCAudioManager.AudioDevice audioDevice) {
        rtcAudioManager.setDefaultAudioDevice(audioDevice);
        rtcAudioManager.selectAudioDevice(audioDevice);
    }

    private void initialize() {
        // Initializing WebRTC Api
        webRtcClient = new WebRtcClient(
                this, new MyPeerConnectionObserver() {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                // Whenever our WebRTC Client generate and ICE Candidate, we set it in our local
                // WebRTC Connection and send it to remote peer

                if (iceCandidate != null) {
                    webRtcClient.addIceCandidate(iceCandidate);
                    firebaseController.sendIceCandidate(iceCandidate, remoteUserId, callId, new SuccessListener() {
                        @Override
                        public void onSent() {

                        }

                        @Override
                        public void onFailed(String s) {
                            Log.e(TAG, "ice candidate send failed for: " + s);
                        }
                    });
                } else {
                    Log.e("NullIceCandidate", "NullIceCandidate");
                }
            }

            @Override
            public void onConnectionChange(PeerConnection.PeerConnectionState newState) {
                super.onConnectionChange(newState);
                Log.d("ConnectChangedState", newState.toString());
                if (newState == PeerConnection.PeerConnectionState.CONNECTED) {
                    runOnUiThread(() -> {
                        // Cause WebRTC does these task in background thread,
                        // we need to use runOnUiTread() method to execute ui related code
                        Toast.makeText(CallActivity.this, "Connected", Toast.LENGTH_LONG).show();
                    });
                    startTimeCounter();
                } else if (newState == PeerConnection.PeerConnectionState.DISCONNECTED) {
                    Log.d("Closing", "User disconnected");
                    runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), "Call Ended", Toast.LENGTH_SHORT).show();
                        leaveAndDestroy();
                    });
                    finish();
                } else if (newState == PeerConnection.PeerConnectionState.FAILED) {
                    runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), "Connection Failed", Toast.LENGTH_SHORT).show();
                        leaveAndDestroy();
                    });
                    finish();
                }
            }

            @Override
            public void onTrack(RtpTransceiver transceiver) {
                super.onTrack(transceiver);
                // When connection is established, we receive remote peer audio track, wer add it to our WebRTC Client
                // and it plays the audio track
                Log.d("OnTrack", "Received remote track");
                webRtcClient.addTrack(transceiver.getReceiver().track());
            }
        });
        // We set our local audio into WebRTC Client
        webRtcClient.setStreams();
        rtcAudioManager = RTCAudioManager.create(this);
        rtcAudioManager.start((selectedAudioDevice, availableAudioDevices) -> {

        });
    }

    private void startTimeCounter() {
        counter = 0;
        timerTask = new TimerTask() {
            @Override
            public void run() {
                int minutes = counter / 60;
                int seconds = counter % 60;
                runOnUiThread(() -> {
                    timeText.setText(String.format("%02d:%02d", minutes, seconds));
                });
                counter++;
            }
        };

        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 0, 1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        leaveAndDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        new AlertDialog.Builder(this)
                .setTitle("Are you sure?")
                .setIcon(R.drawable.btn_endcall_normal)
                .setMessage("You will be disconnected from the call")
                .setPositiveButton("End Call", (dialog, which) -> {
                    leaveAndDestroy();
                    finish();
                })
                .setNegativeButton("Stay", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();

    }

    private void leaveAndDestroy() {
        // STop everything and exit from the activity
        if(firebaseController!=null){
            firebaseController.stopSignaling();
        }
        rtcAudioManager.stop();
        rtcAudioManager.destroyWakelock();
        try {
            timer.cancel();
        } catch (Exception e) {
            Log.e("TIME EXCEPTION", e.getMessage());
        }
        webRtcClient.closeConnection();
    }
}