package com.helloenglish.randomcaller.Interfaces;

import org.webrtc.SessionDescription;

public interface OfferListener {
    void onReceiveSignal(SessionDescription sessionDescription);
}
