package com.example.homeautomationapp;

import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.core.content.ContextCompat;

import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.pm.PackageManager;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.INTERNET;

import okhttp3.OkHttpClient;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    public static final int REQUEST_AUDIO_PERMISSION_CODE = 1;

    private String ipAddress = "";

    private String ext = ".wav";

    private MediaPlayer mPlayer;

    private EditText messageEditText;
    private Button sendButton;

    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ipAddress = "192.168.0.133:8000";

        ImageButton recordButton = findViewById(R.id.recordButton);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isRecording) {
                    startRecording();
                } else {
                    stopRecording();
                }
            }
        });

        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);

        recyclerView = findViewById(R.id.chatRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));


        List<Message> messages = new ArrayList<>();
        messageAdapter = new MessageAdapter(messages);
        messageAdapter.setRecyclerView(recyclerView);
        recyclerView.setAdapter(messageAdapter);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

    }

    public boolean CheckPermissions() {
        // this method is used to check permission
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), INTERNET);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }

    private void RequestPermissions() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, INTERNET}, REQUEST_AUDIO_PERMISSION_CODE);
    }

    private void startRecording() {
        if (CheckPermissions()) {
            try {
                mediaRecorder = new MediaRecorder(getApplicationContext());
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                mediaRecorder.setOutputFile(getCacheDir().getAbsolutePath() + "/audio" + ext);
                mediaRecorder.prepare();
                mediaRecorder.start();
                isRecording = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            RequestPermissions();
        }

    }

    private void stopRecording() {
        if (isRecording) {
            mediaRecorder.stop();
            mediaRecorder.release();
            isRecording = false;
            upload();
        }
    }


    public void playAudio() {
        mPlayer = new MediaPlayer();
        try {
            // below method is used to set the
            // data source which will be our file name
            mPlayer.setDataSource(getCacheDir().getAbsolutePath() + "/audio" + ext);

            // below method will prepare our media player
            mPlayer.prepare();

            // below method will start our media player.
            mPlayer.start();
            Toast.makeText(getApplicationContext(), "Recording Started Playing", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void upload() {
        Toast.makeText(getApplicationContext(), "Preparing to send audio", Toast.LENGTH_SHORT).show();
        new AudioFileUploader().execute();
    }

    private void sendMessage() {
        String userMessage = messageEditText.getText().toString().trim();
        // log the message to Logcat
        Log.d("MainActivity", "Sending message: " + userMessage);
        // clear the EditText
        messageEditText.setText("");


        String serverUrl = "http://" + ipAddress + "/";

        if (!userMessage.isEmpty()) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(serverUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            ApiService apiClient = retrofit.create(ApiService.class);

            Call<ApiResponse> call = apiClient.sendMessage(userMessage);
            Message newMessage = new Message(userMessage, true);
            messageAdapter.addMessage(newMessage);

            call.enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                    // Handle successful response
                    if (response.isSuccessful()) {
                        Log.d("MainActivity", "Message successfully sent!");
                        ApiResponse apiResponse = response.body();
                        String message = apiResponse.getResponse();
                        Log.d("MainActivity", "Message from server: " + message);
                        Message newMessage = new Message(message, false);
                        messageAdapter.addMessage(newMessage);
                    } else {
                        Log.e("MainActivity", "Unable to send message");
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {
                    // Handle failure
                    Log.e("MainActivity", "Error sending message", t);
                }
            });
        }
    }


    public class AudioFileUploader extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                String serverUrl = "http://" + ipAddress + "/";
                String filePath = getCacheDir().getAbsolutePath() + "/audio" + ext;

                int readTimeout = 180;
                int connectTimeout = 30;

                // Create a Retrofit instance
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(serverUrl)
                        .addConverterFactory(GsonConverterFactory.create())
                        .client(new OkHttpClient.Builder()
                                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                                .readTimeout(readTimeout, TimeUnit.SECONDS)
                                .build())
                        .build();

                // Create the service
                ApiService apiService = retrofit.create(ApiService.class);

                // Create a file object from the file path
                File file = new File(filePath);

                // Create a request body with file and its type
                RequestBody requestFile =
                        RequestBody.create(MediaType.parse("multipart/form-data"), file);

                // Create MultipartBody.Part using file request body and file name
                MultipartBody.Part body =
                        MultipartBody.Part.createFormData("audioFile", file.getName(), requestFile);

                // Call the uploadAudioFile method in the ApiService interface
                Call<ApiResponse> call = apiService.uploadAudioFile(body);

                // Execute the call synchronously
                call.enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                        // Handle successful response
                        if (response.isSuccessful()) {
                            Log.d("MainActivity", "Message successfully sent!");
                            ApiResponse apiResponse = response.body();
                            String message = apiResponse.getResponse();
                            String human_order = apiResponse.getHumanOrder();
                            Log.d("MainActivity", "Message from server: " + message);
                            Log.d("MainActivity", "Human order from server: " + human_order);
                            Message userMessage = new Message(human_order, true);
                            messageAdapter.addMessage(userMessage);
                            Message newMessage = new Message(message, false);
                            messageAdapter.addMessage(newMessage);

                        } else {
                            Log.e("MainActivity", "Unable to send message");
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse> call, Throwable t) {
                        // Handle failure
                        Log.e("MainActivity", "Error sending message", t);
                    }
                });


            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }
    }

}