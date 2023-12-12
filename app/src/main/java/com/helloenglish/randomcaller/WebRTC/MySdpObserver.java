package com.helloenglish.randomcaller.WebRTC;

import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

// Creating custom class so that we don't need to
// override all the methods in main code

public class MySdpObserver implements SdpObserver {
    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
    }

    @Override
    public void onSetSuccess() {
    }

    @Override
    public void onCreateFailure(String s) {
    }

    @Override
    public void onSetFailure(String s) {
    }
}
