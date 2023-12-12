package com.helloenglish.randomcaller.Activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.helloenglish.randomcaller.Models.Constants;
import com.helloenglish.randomcaller.R;
import com.helloenglish.randomcaller.Helpers.FindCallerHelper;
import com.helloenglish.randomcaller.Helpers.SpHelper;

public class StartCallActivity extends AppCompatActivity {

    private String[] permissions = new String[] {Manifest.permission.RECORD_AUDIO};
    private int permReqCode = 111;
    FindCallerHelper findCallerHelper;
    LinearLayout findingLayout, callActionLayout;
    Button startCallForEnglish, startCallForCasual, reConnect, stopButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finding);

        findRefByIDs();

        startCallForEnglish.setOnClickListener((view)->{
           handleCallButton(Constants.ENGLISH_PRACTICE);
        });

        startCallForCasual.setOnClickListener(v-> {
            handleCallButton(Constants.CASUAL_CALL);
        });

        reConnect.setOnClickListener( v -> {
            handleReconnectButton();
        });

        stopButton.setOnClickListener(v -> {
            deleteCallRequest();
            showCallButton();
        });
    }

    private void findRefByIDs() {
        findingLayout = findViewById(R.id.findingLayout);
        startCallForEnglish = findViewById(R.id.startCallButtonEnglish);
        startCallForCasual = findViewById(R.id.startCallButtonCasual);
        reConnect = findViewById(R.id.startCallButtonReconnect);
        stopButton = findViewById(R.id.stopSearchButton);
        callActionLayout = findViewById(R.id.callActionLayout);
    }

    private void handleCallButton(String callType){
        if(isPermissionGranted()){
            startFindingPartner(callType);
        }  else {
            askPermission();
        }
    }

    private void handleReconnectButton(){
            String lastCallerId = SpHelper.getPrefs(this).getLastCaller();
            if(lastCallerId != null){
                startFindingPartner(Constants.RECONNECT_LAST_USER);
            } else {
                Toast.makeText(this, "No recent call found", Toast.LENGTH_SHORT).show();
            }
    }

    private void startFindingPartner(String callType) {
        findCallerHelper = new FindCallerHelper(this, callType);
        if(callType.equals(Constants.RECONNECT_LAST_USER)){
            findCallerHelper.reConnectLastCaller();
        } else {
            findCallerHelper.findRandomCaller();
        }
        hideCallButtons();
    }
    private void hideCallButtons() {
        callActionLayout.setVisibility(View.GONE);
        findingLayout.setVisibility(View.VISIBLE);
    }

    private void showCallButton(){
        callActionLayout.setVisibility(View.VISIBLE);
        findingLayout.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        deleteCallRequest();
    }

    private void deleteCallRequest(){
        findCallerHelper.cancelFinding();
    }

    private void askPermission(){
        ActivityCompat.requestPermissions(this, permissions, permReqCode);
    }

    private boolean isPermissionGranted(){
        for(String perm : permissions){
            if(ActivityCompat.checkSelfPermission(this, perm)  != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

}