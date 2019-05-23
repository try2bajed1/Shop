package com.example.shop.rest;

import android.support.annotation.NonNull;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

class ApiService {

    private static final long TIMEOUT_SEC = 60L;
    private static final long TIMEOUT_READ_SEC = 60L;
    private static final long TIMEOUT_WRITE_SEC = 5 * 60L;
    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    private static String baseUrl = "https://url.ru/";

    private static ApiService ourInstance = new ApiService();

    @NonNull
    public static ApiService getInstance() {
        return ourInstance;
    }

    @NonNull
    public API service;

    public ApiService() {
        service = configRetrofit().create(API.class);
    }

    public void setBaseUrl(String _baseUrl) {
        baseUrl = _baseUrl;
        service = configRetrofit().create(API.class);
    }

    public String getBaseUrl()
    {
        return baseUrl;
    }

    @NonNull
    private Retrofit configRetrofit() {
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addCallAdapterFactory(RxErrorHandlingAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(GSON))
                .client(configHttpClient())
                .build();
    }


    @NonNull
    private OkHttpClient configHttpClient() {
        if(BuildConfig.LOG_HTTP){
            return new OkHttpClient.Builder()
                    .connectTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
                    .readTimeout(TIMEOUT_READ_SEC, TimeUnit.SECONDS)
                    .writeTimeout(TIMEOUT_WRITE_SEC, TimeUnit.SECONDS)
                    .addInterceptor(configLoggingInterceptor())
                    .build();
        }
        //в релиз версии выключаем интерсептор
        return new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_READ_SEC, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_WRITE_SEC, TimeUnit.SECONDS)
                .build();
    }

    @NonNull
    private HttpLoggingInterceptor configLoggingInterceptor() {
        final HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        return loggingInterceptor;
    }
}
