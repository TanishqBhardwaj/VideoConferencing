package com.example.webrtcsample;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface JsonApiHolder {

    @GET("create_room")
    Call<Integer> createRoomId();

    @POST("get_room_details")
    Call<GetRoomDetailsResponse> getRoomDetails(@Body RoomIdModel roomIdModel);
}
