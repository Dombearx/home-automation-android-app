package com.example.homeautomationapp;

import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import androidx.core.content.ContextCompat;

import android.widget.EditText;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import android.content.pm.PackageManager;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.INTERNET;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import android.os.AsyncTask;
import java.io.File;
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button recordButton = findViewById(R.id.recordButton);
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
                mediaRecorder.setAudioEncodingBitRate(128000);
                mediaRecorder.setAudioChannels(1);
                mediaRecorder.setAudioSamplingRate(16000);
                mediaRecorder.setAudioEncodingBitRate(2);
                mediaRecorder.setOutputFile(getCacheDir().getAbsolutePath() + "/audio" + ext);
                mediaRecorder.prepare();
                mediaRecorder.start();
                isRecording = true;
                EditText editTextServerIP = findViewById(R.id.editTextServerIP);
                ipAddress = editTextServerIP.getText().toString();
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
//            playAudio();
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


    public class AudioFileUploader extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                String serverUrl = "http://" + ipAddress + "/";
                String filePath = getCacheDir().getAbsolutePath() + "/audio" + ext;

                // Create a Retrofit instance
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(serverUrl)
                        .addConverterFactory(GsonConverterFactory.create())
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
                Call<Void> call = apiService.uploadAudioFile(body);

                // Execute the call synchronously
                call.execute();

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }
    }

}