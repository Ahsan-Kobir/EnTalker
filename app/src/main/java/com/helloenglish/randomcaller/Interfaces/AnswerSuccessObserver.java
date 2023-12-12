package com.helloenglish.randomcaller.Interfaces;

import org.webrtc.SessionDescription;

public interface AnswerSuccessObserver {
    void onAnswerOffer(SessionDescription answerSdp);
}
