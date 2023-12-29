package com.example.homeautomationapp;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {

    @Multipart
    @POST("receive_audio")
    Call<ApiResponse> uploadAudioFile(
            @Part MultipartBody.Part file
    );

    @FormUrlEncoded
    @POST("receive_command")
    Call<ApiResponse> sendMessage(@Field("human_order") String human_order);

}
