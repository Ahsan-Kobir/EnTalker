package com.helloenglish.randomcaller.Models;

public class CallReqModel {
    private String callerId;
    private String receiverId;
    private String status;

    private String callId;

    public CallReqModel() {
    }

    public CallReqModel(String callerId, String receiverId, String status, String callId) {
        this.callerId = callerId;
        this.receiverId = receiverId;
        this.status = status;
        this.callId = callId;
    }

    public String getCallerId() {
        return callerId;
    }

    public void setCallerId(String callerId) {
        this.callerId = callerId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }
}
