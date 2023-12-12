package com.helloenglish.randomcaller.Models;

public class SingalingModel {
    private String to;
    private String dataType;
    private String data;
    private String callId;

    public SingalingModel() {
    }

    public SingalingModel(String to, String dataType, String data, String callId) {
        this.to = to;
        this.dataType = dataType;
        this.data = data;
        this.callId = callId;
    }


    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }
}
