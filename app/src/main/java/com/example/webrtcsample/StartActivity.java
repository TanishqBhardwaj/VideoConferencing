package com.example.webrtcsample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StartActivity extends AppCompatActivity {
    private Button buttonCreateRoom;
    private EditText editTextRoomId;
    private Button buttonJoinRoom;
    private JsonApiHolder jsonApiHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        jsonApiHolder = RetrofitInstance.getRetrofitInstance(StartActivity.this)
                .create(JsonApiHolder.class);

        buttonCreateRoom = findViewById(R.id.buttonCreateRoom);
        editTextRoomId = findViewById(R.id.editTextRoomId);
        buttonJoinRoom = findViewById(R.id.buttonJoinRoom);

        buttonCreateRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createRoomId();
            }
        });

        buttonJoinRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String roomId = editTextRoomId.getText().toString();
                if(roomId.isEmpty()) {
                    Toast.makeText(StartActivity.this, "Room Id can't be empty !",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                goIntoRoom(roomId);
            }
        });
    }

    private void createRoomId() {
        Call<Integer> call = jsonApiHolder.createRoomId();
        call.enqueue(new Callback<Integer>() {
            @Override
            public void onResponse(Call<Integer> call, Response<Integer> response) {
                String roomId = response.body().toString();
                Intent intent = new Intent(StartActivity.this, MainActivity.class);
                intent.putExtra(MainActivity.ROOM_ID, roomId);
                intent.putExtra(String.valueOf(MainActivity.createOffer), true);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Call<Integer> call, Throwable t) {
                Toast.makeText(StartActivity.this, "Error occurred !", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void goIntoRoom(final String roomId) {
        RoomIdModel roomIdModel = new RoomIdModel(roomId);
        Call<GetRoomDetailsResponse> call = jsonApiHolder.getRoomDetails(roomIdModel);
        call.enqueue(new Callback<GetRoomDetailsResponse>() {
            @Override
            public void onResponse(Call<GetRoomDetailsResponse> call, Response<GetRoomDetailsResponse> response) {
                if(response.body() == null) {
                    Toast.makeText(StartActivity.this, "Room does not exist !",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                Intent intent = new Intent(StartActivity.this, MainActivity.class);
                intent.putExtra(MainActivity.ROOM_ID, roomId);
                intent.putExtra(String.valueOf(MainActivity.createOffer), false);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Call<GetRoomDetailsResponse> call, Throwable t) {
                Toast.makeText(StartActivity.this, "Error occurred !", Toast.LENGTH_LONG).show();
            }
        });
    }
}
