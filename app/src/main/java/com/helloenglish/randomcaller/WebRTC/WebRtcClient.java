package com.helloenglish.randomcaller.WebRTC;

import android.content.Context;
import android.util.Log;

import com.helloenglish.randomcaller.Interfaces.AnswerSuccessObserver;
import com.helloenglish.randomcaller.Interfaces.OfferSuccessObserver;

import org.webrtc.AddIceObserver;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.BuiltinAudioDecoderFactoryFactory;
import org.webrtc.BuiltinAudioEncoderFactoryFactory;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.List;

public class WebRtcClient {
    private Context context; 
    public String TAG = "WebRtcClient";
    private PeerConnectionFactory peerConnectionFactory;
    private List<PeerConnection.IceServer> iceServers = new ArrayList<>();
    private MediaConstraints mediaConstraints = new MediaConstraints();
    private MediaConstraints audioConstraints = new MediaConstraints();
    private PeerConnection peerConnection;
    private AudioSource audioSource;
    private AudioTrack audioTrack;
    private String audioTrackId = "local_audio_track_id";

    public WebRtcClient(Context context, PeerConnection.Observer observer){
        this.context = context;
        initPeerConnectionFactory();
        this.peerConnectionFactory = createPeerConnectionFactory();
        iceServers.add(PeerConnection.IceServer.builder("turn:freestun.net:3479") // Free TURN server link
                .setUsername("free") // TURN Server Auth Creds
                .setPassword("free").createIceServer());
        peerConnection = createPeerConnection(observer);
        initMediaConstraints();
        audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
    }

    private void initMediaConstraints() {
        // Optional Audio Source Media Constraints
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googEchoCancellation", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googAutoGainControl", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googHighpassFilter", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googNoiseSuppression", "true"));
    }

    private void initPeerConnectionFactory(){
        PeerConnectionFactory.InitializationOptions options = PeerConnectionFactory.InitializationOptions
                .builder(context)
                .setFieldTrials("WebRTC-ExternalAndroidAudioDevice/Enabled/") // You can change to "WebRTC-H264HighProfile/Enabled" for high quality
                .setEnableInternalTracer(true)
                .createInitializationOptions();
        PeerConnectionFactory.initialize(options);
    }

    private PeerConnectionFactory createPeerConnectionFactory(){
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        options.disableEncryption = false;
        options.disableNetworkMonitor = false;
        return PeerConnectionFactory.builder()
                .setOptions(options)
                .setAudioDecoderFactoryFactory(new BuiltinAudioDecoderFactoryFactory())
                .setAudioEncoderFactoryFactory(new BuiltinAudioEncoderFactoryFactory())
                .createPeerConnectionFactory();
    }

    private PeerConnection createPeerConnection(PeerConnection.Observer observer){
        return peerConnectionFactory.createPeerConnection(iceServers,observer);
    }

    public void setStreams(){
        audioTrack = peerConnectionFactory.createAudioTrack(audioTrackId, audioSource);
        if(peerConnection!=null){
            peerConnection.addTrack(audioTrack);
        } else {
            Log.e(TAG, "PeerConnection is not initialized");
        }
    }

    public void addTrack(MediaStreamTrack mst){
        peerConnection.addTrack(mst);
    }

    public void createOffer(OfferSuccessObserver signalDataObserver) {
        try {
            peerConnection.createOffer(new MySdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {
                    super.onCreateSuccess(sessionDescription);
                    peerConnection.setLocalDescription(new MySdpObserver() {
                        @Override
                        public void onSetSuccess() {
                            super.onSetSuccess();
                            // Successfully set the local sdp
                            // Let's send the sdp to another user
                            signalDataObserver.onCreateOffer(sessionDescription);
                        }

                        @Override
                        public void onSetFailure(String s) {
                            super.onSetFailure(s);
                            Log.e(TAG, "on create offer set failure "+ s);
                        }
                    }, sessionDescription);
                }
            }, mediaConstraints);
        } catch (Exception e) {
            Log.e(TAG + " CreateOfferException", e.getMessage());
            e.printStackTrace();
        }
    }

    public void acceptOffer(AnswerSuccessObserver signalDataObserver) {
        try {
            peerConnection.createAnswer(new MySdpObserver(){
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {
                    super.onCreateSuccess(sessionDescription);
                    peerConnection.setLocalDescription(new MySdpObserver(){
                        @Override
                        public void onSetSuccess() {
                            super.onSetSuccess();
                            // Successfully set the local sdp
                            // Let's send the sdp to another user
                            signalDataObserver.onAnswerOffer(sessionDescription);
                        }
                    }, sessionDescription);
                }

                @Override
                public void onSetFailure(String s) {
                    super.onSetFailure(s);
                    Log.e(TAG, "on accept offer set failure "+ s);
                }
            }, mediaConstraints);
        } catch (Exception e){
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    public void setRemoteSdp(SessionDescription sessionDescription){
        peerConnection.setRemoteDescription(new MySdpObserver(){
            @Override
            public void onSetFailure(String s) {
                super.onSetFailure(s);
                Log.e(TAG, "remote sdp set failure: " + s);
            }
        }, sessionDescription);
    }
    public void addIceCandidate(IceCandidate iceCandidate) {
        peerConnection.addIceCandidate(iceCandidate, new AddIceObserver() {
            @Override
            public void onAddSuccess() {
                Log.d(TAG, "AddIceSuccess");
            }

            @Override
            public void onAddFailure(String s) {
                Log.e(TAG + " onAddFailure", s);
                peerConnection.addIceCandidate(iceCandidate);
            }
        });
    }


    public void closeConnection(){
        try {
            peerConnection.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    //UI Helper Methods
    public void setMuted(boolean shouldMute){
        //If shouldMute is TRUE, we disable the local audio track by setting FALSE
        audioTrack.setEnabled(!shouldMute);
    }
}
