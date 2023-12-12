package com.helloenglish.randomcaller.Interfaces;

import org.webrtc.SessionDescription;

public interface OfferSuccessObserver {
    void onCreateOffer(SessionDescription offerSdp);
}
