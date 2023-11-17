package com.example.homeautomationapp;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {

    @Multipart
    @POST("receive_audio")
    Call<Void> uploadAudioFile(
            @Part MultipartBody.Part file
    );
}
