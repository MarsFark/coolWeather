package com.example.coolweather.util;


import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;


public class HttpUtil {
    //连接服务器，处理响应数据
    public static void sendOkHttpRequest(String address , Callback callback){

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(address).build();

        client.newCall(request).enqueue(callback);
    }
}
