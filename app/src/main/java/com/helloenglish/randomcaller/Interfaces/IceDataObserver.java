package com.helloenglish.randomcaller.Interfaces;

import com.helloenglish.randomcaller.Models.SingalingModel;

import org.webrtc.IceCandidate;

public interface IceDataObserver {
    void onIceReceived(SingalingModel singalingModel);
}
