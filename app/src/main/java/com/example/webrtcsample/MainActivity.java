package com.example.webrtcsample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String VIDEO_TRACK_ID = "video1";
    private static final String AUDIO_TRACK_ID = "audio1";
    private static final String LOCAL_STREAM_ID = "stream1";
    private static final String SDP_MID = "sdpMid";
    private static final String SDP_M_LINE_INDEX = "sdpMLineIndex";
    private static final String SDP = "sdp";
    private static final String CREATEROOM = "create_room";
    private static final String ADD_ANSWER_SDP = "add_answer_sdp";

    public static final String ROOM_ID = "room_id";
    public static final boolean createOffer = false;

    private PeerConnectionFactory peerConnectionFactory;
    private VideoSource localVideoSource;
    private PeerConnection peerConnection;
    private MediaStream localMediaStream;
    private VideoRenderer otherPeerRenderer;
    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;
    final int ALL_PERMISSIONS_CODE = 1;
//    private PrefUtils prefUtils;
    private JsonApiHolder jsonApiHolder;
    private Socket socket;
    {
        try {
            socket = IO.socket(new RetrofitInstance().getBaseUrl());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private TextView textViewRoomId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        prefUtils = new PrefUtils(MainActivity.this);
        jsonApiHolder = RetrofitInstance.getRetrofitInstance(MainActivity.this)
                .create(JsonApiHolder.class);

        socket.connect();

        textViewRoomId = findViewById(R.id.textViewRoomId);
        textViewRoomId.setText("Room Id : " + getIntent().getStringExtra(ROOM_ID));

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO}, ALL_PERMISSIONS_CODE);
        } else {
            // all permissions already granted
            start();
            if(getIntent().getBooleanExtra(String.valueOf(createOffer), false)) {
                call();
            }
            else {
                answer();
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == ALL_PERMISSIONS_CODE
                && grantResults.length == 2
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            // all permissions granted
            start();
            if(getIntent().getBooleanExtra(String.valueOf(createOffer), false)) {
                call();
            }
            else {
                answer();
            }
        } else {
            finish();
        }
    }

    private void start() {
        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(true);

        PeerConnectionFactory.initializeAndroidGlobals(
                this,  // Context
                true,  // Audio Enabled
                true,  // Video Enabled
                true,  // Hardware Acceleration Enabled
                null); // Render EGL Context

        peerConnectionFactory = new PeerConnectionFactory();

        VideoCapturerAndroid vcFront = VideoCapturerAndroid.create(
                VideoCapturerAndroid.getNameOfFrontFacingDevice(), null);

        localVideoSource = peerConnectionFactory.createVideoSource(vcFront, new MediaConstraints());
        localVideoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, localVideoSource);
        localVideoTrack.setEnabled(true);

        AudioSource audioSource = peerConnectionFactory.createAudioSource(new MediaConstraints());
        localAudioTrack = peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
        localAudioTrack.setEnabled(true);

        localMediaStream = peerConnectionFactory.createLocalMediaStream(LOCAL_STREAM_ID);
        localMediaStream.addTrack(localVideoTrack);
        localMediaStream.addTrack(localAudioTrack);

        GLSurfaceView videoView = (GLSurfaceView) findViewById(R.id.gl_surface_view);

        VideoRendererGui.setView(videoView, null);
        try {
            otherPeerRenderer = VideoRendererGui.createGui(0, 0, 100, 100,
                    VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);
            VideoRenderer renderer = VideoRendererGui.createGui(50, 50, 50, 50,
                    VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);
            localVideoTrack.addRenderer(renderer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void call() {
        if (peerConnection != null)
            return;

        Toast.makeText(MainActivity.this, "ON CALL ", Toast.LENGTH_LONG).show();

        ArrayList<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));

        peerConnection = peerConnectionFactory.createPeerConnection(
                iceServers,
                new MediaConstraints(),
                peerConnectionObserver);

        peerConnection.addStream(localMediaStream);

        peerConnection.createOffer(sdpObserver, new MediaConstraints());


            socket.on("recieve_answer_sdp", new Emitter.Listener() {

                    @Override
                    public void call(Object... args) {
                        try {
                            String type = "";
                            String answerSdp = "";
                            for(Object object : args) {
                                JSONObject jsonObject = (JSONObject) object;
                                type = jsonObject.getString("type");
                                answerSdp = jsonObject.getString(SDP);
                            }

                            //storing answer sdp in remote description
                            peerConnection.setRemoteDescription(new SdpObserver() {
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
                            }, new SessionDescription(
                                    SessionDescription.Type.fromCanonicalForm(type.toLowerCase()), answerSdp));
                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

            })
            .on("recieve_callee_candidates", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    try {
                        for(Object object : args) {
                            JSONObject jsonObject = (JSONObject) object;
                            JSONArray jsonArray = jsonObject.getJSONArray("calleeCandidates");
                            List<IceCandidate> yourArray = new Gson().fromJson(jsonArray.toString(),
                                    new TypeToken<List<IceCandidate>>(){}.getType());

                            //storing ice candidates
                            for(IceCandidate iceCandidate : yourArray) {
                                peerConnection.addIceCandidate(new IceCandidate(iceCandidate.sdpMid,
                                        iceCandidate.sdpMLineIndex, iceCandidate.sdp));
                            }
                        }
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            });

    }

    private void answer() {

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("roomId", getIntent().getStringExtra(ROOM_ID));
            socket.emit("join_room", jsonObject);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        if (peerConnection != null)
            return;

        Toast.makeText(MainActivity.this, "ANSWER", Toast.LENGTH_LONG).show();

        ArrayList<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));

        peerConnection = peerConnectionFactory.createPeerConnection(
                iceServers,
                new MediaConstraints(),
                peerConnectionObserver);

        peerConnection.addStream(localMediaStream);

        goIntoRoom(getIntent().getStringExtra(ROOM_ID));

    }

    SdpObserver sdpObserver = new SdpObserver() {
        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
            peerConnection.setLocalDescription(sdpObserver, sessionDescription);
            try {
                JSONObject obj = new JSONObject();
                obj.put("type", sessionDescription.type.canonicalForm().toLowerCase());
                obj.put("sdp", sessionDescription.description);
                JSONObject jsonObject = new JSONObject();

                if (getIntent().getBooleanExtra(String.valueOf(createOffer), false)) {
                    jsonObject.put("offer", obj);
                    jsonObject.put("roomId", getIntent().getStringExtra(ROOM_ID));
                    socket.emit(CREATEROOM, jsonObject);

                    //saved offer locally
                    peerConnection.setLocalDescription(sdpObserver, sessionDescription);
                }
                else {
                    jsonObject.put("answer", obj);
                    jsonObject.put("roomId", getIntent().getStringExtra(ROOM_ID));
                    socket.emit(ADD_ANSWER_SDP, jsonObject);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
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
    };

    PeerConnection.Observer peerConnectionObserver = new PeerConnection.Observer() {
        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            Log.d("RTCAPP", "onSignalingChange:" + signalingState.toString());
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            Log.d("RTCAPP", "onIceConnectionChange:" + iceConnectionState.toString());
        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {

        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            try {
                JSONObject obj = new JSONObject();
                obj.put(SDP_MID, iceCandidate.sdpMid);
                obj.put(SDP_M_LINE_INDEX, iceCandidate.sdpMLineIndex);
                obj.put("candidate", iceCandidate.sdp);

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("roomId", getIntent().getStringExtra(ROOM_ID));
                jsonObject.put("candidate", obj);

                if (getIntent().getBooleanExtra(String.valueOf(createOffer), false)) {
                    socket.emit("add_caller_candidates", jsonObject);
                }
                else {
                    socket.emit("add_callee_candidates", jsonObject);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            mediaStream.videoTracks.getFirst().addRenderer(otherPeerRenderer);
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {

        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {

        }

        @Override
        public void onRenegotiationNeeded() {

        }
    };

    private void goIntoRoom(final String roomId) {
        RoomIdModel roomIdModel = new RoomIdModel(roomId);
        Call<GetRoomDetailsResponse> call = jsonApiHolder.getRoomDetails(roomIdModel);
        call.enqueue(new Callback<GetRoomDetailsResponse>() {
            @Override
            public void onResponse(Call<GetRoomDetailsResponse> call,
                                   Response<GetRoomDetailsResponse> response) {
                GetRoomDetailsResponse.OfferSdp offerSdp = response.body().getOfferSdp();
                peerConnection.setRemoteDescription(new SdpObserver() {
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
                }, new SessionDescription(
                        SessionDescription.Type.fromCanonicalForm(offerSdp.getType()), offerSdp.getSdp()));

                peerConnection.createAnswer(sdpObserver, new MediaConstraints());

                List<GetRoomDetailsResponse.Candidate> iceCandidates = response.body().getCallerCandidates();
                for(GetRoomDetailsResponse.Candidate candidate : iceCandidates) {
                    peerConnection.addIceCandidate(new IceCandidate(candidate.getSdpMid(),
                            candidate.getSdpMLineIndex(), candidate.getCandidate()));
                }
            }

            @Override
            public void onFailure(Call<GetRoomDetailsResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error occurred !", Toast.LENGTH_LONG).show();
            }
        });
    }
}
