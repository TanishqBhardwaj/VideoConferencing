package com.example.webrtcsample;

import android.content.Context;
import android.content.SharedPreferences;

import org.webrtc.IceCandidate;

import java.util.List;

public class PrefUtils {
    private static SharedPreferences sp;
    private SharedPreferences.Editor editor;
    private Context ctx;

//    private static final String ANSWER_SDP = "answer_sdp";

    public PrefUtils(Context context){
        ctx = context;
//        sp = ctx.getSharedPreferences(ANSWER_SDP , Context.MODE_PRIVATE);
        editor = sp.edit();
    }


}
