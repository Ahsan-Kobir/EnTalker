package com.akapps.randomcaller.Activity;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.DialogCompat;

import com.akapps.randomcaller.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;


public class CallActivity extends AppCompatActivity {

    ImageView endButton, muteButton, speakerButton, profilePic;
    TextView timeText;

    boolean loudSpeaker, isMuted = false;
    private Timer timer;
    private TimerTask timerTask;

    int counter;

    FirebaseDatabase fdb;

    //Agora Things
    private String appId = "5aa30c9cd5334eecad90b818f196d470";
    private int roomId;
    private String token = "";
    private RtcEngine mRtcEngine;
    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            super.onJoinChannelSuccess(channel, uid, elapsed);
            mRtcEngine.setEnableSpeakerphone(true);
            loudSpeaker = true;
            startTimeCounter();
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            Toast.makeText(getApplicationContext(), "Call Ended", Toast.LENGTH_SHORT).show();
            leaveAndDestroy();
            super.onUserOffline(uid, reason);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        endButton = findViewById(R.id.endButton);
        muteButton = findViewById(R.id.muteButton);
        speakerButton = findViewById(R.id.speakerButton);
        timeText = findViewById(R.id.timeCounter);
        profilePic = findViewById(R.id.pic);

        fdb = FirebaseDatabase.getInstance();
        fdb.getReference()
                .child("profiles")
                .child(getIntent().getStringExtra("remoteUser"))
                .child("photo")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String imgUrl = snapshot.getValue(String.class);
                        new ImageLoadTask(imgUrl, profilePic).execute();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        roomId = getIntent().getIntExtra("roomId", 0);
        initializeAndJoinChannel();



        endButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                leaveAndDestroy();
                finish();
            }
        });

        muteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isMuted){
                    mRtcEngine.muteLocalAudioStream(false);
                    isMuted = false;
                    muteButton.setImageResource(R.drawable.btn_unmute_normal);
                } else {
                    mRtcEngine.muteLocalAudioStream(true);
                    isMuted = true;
                    muteButton.setImageResource(R.drawable.btn_mute_normal);
                }

            }
        });

        speakerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(loudSpeaker){
                    mRtcEngine.setEnableSpeakerphone(false);
                    loudSpeaker = false;
                    speakerButton.setImageResource(R.drawable.btn_speaker_ear3);
                } else {
                    mRtcEngine.setEnableSpeakerphone(true);
                    loudSpeaker = true;
                    speakerButton.setImageResource(R.drawable.btn_speaker_loud3);
                }

            }
        });
    }

    private void initializeAndJoinChannel() {
        try {
            mRtcEngine = RtcEngine.create(getBaseContext(), appId, mRtcEventHandler);
        } catch (Exception e) {
            finish();
            throw new RuntimeException("Check the error");
        }
        mRtcEngine.joinChannel(token, String.valueOf(roomId), "", 0);

    }

    private void startTimeCounter() {
        counter = 0;
        timerTask = new TimerTask() {
            @Override
            public void run() {
                Log.e("TimerTask", String.valueOf(counter));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        int minutes = counter / 60;
                        int seconds = counter % 60;
                        timeText.setText(String.format("%02d:%02d", minutes, seconds));
                    }
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
//        AlertDialog.Builder builder1 = new AlertDialog.Builder(CallActivity.this);
//        builder1.setTitle("");
//        builder1.setMessage("You will be disconnected from the call");
//        builder1.setCancelable(true);
//
//        builder1.setPositiveButton(
//                "End Call",
//                new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                        dialog.cancel();
//                        leaveAndDestroy();
//                        finish();
//                    }
//                });
//
//        builder1.setNegativeButton(
//                "Stay Connected",
//                new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                        dialog.cancel();
//                    }
//                });
//
//        AlertDialog alert11 = builder1.create();
//        alert11.show();
        new AlertDialog.Builder(this)
                .setTitle("Are you sure?")
                .setMessage("You will be disconnected from the call")

                // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton("End Call", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        leaveAndDestroy();
                        finish();
                    }
                })
                .setNegativeButton("Stay", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();

    }

    private void leaveAndDestroy(){
        mRtcEngine.leaveChannel();
        mRtcEngine.destroy();
        timer.cancel();
    }

    public class ImageLoadTask extends AsyncTask<Void, Void, Bitmap> {

        private String url;
        private ImageView imageView;

        public ImageLoadTask(String url, ImageView imageView) {
            this.url = url;
            this.imageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            try {
                URL urlConnection = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) urlConnection
                        .openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap myBitmap = BitmapFactory.decodeStream(input);
                return myBitmap;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);
            imageView.setImageBitmap(result);
        }

    }
}