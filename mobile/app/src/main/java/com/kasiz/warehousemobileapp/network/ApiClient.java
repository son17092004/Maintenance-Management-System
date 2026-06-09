package com.kasiz.warehousemobileapp.network;

import android.content.Context;

import com.kasiz.warehousemobileapp.BuildConfig;
import com.kasiz.warehousemobileapp.storage.SessionManager;

import java.io.IOException;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {

    private static ApiService service;
    private static String currentBaseUrl;
    private static PersistentCookieJar cookieJar;

    static {
        currentBaseUrl = BuildConfig.API_BASE_URL;
    }

    private ApiClient() {
    }

    public static synchronized ApiService getService(Context context) {
        String baseUrl = SessionManager.getInstance(context).getBaseUrl();
        if (cookieJar == null) {
            cookieJar = new PersistentCookieJar(context);
        }
        if (service == null || !baseUrl.equals(currentBaseUrl)) {
            currentBaseUrl = baseUrl;

            HttpLoggingInterceptor logger = new HttpLoggingInterceptor();
            logger.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .cookieJar(cookieJar)
                    .addInterceptor(logger)
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request original = chain.request();
                            List<Cookie> cookies = cookieJar.loadForRequest(original.url());
                            if (!cookies.isEmpty()) {
                                StringBuilder sb = new StringBuilder();
                                for (int i = 0; i < cookies.size(); i++) {
                                    if (i > 0) {
                                        sb.append("; ");
                                    }
                                    sb.append(cookies.get(i).name()).append("=").append(cookies.get(i).value());
                                }
                                Request newRequest = original.newBuilder()
                                        .header("Cookie", sb.toString())
                                        .build();
                                return chain.proceed(newRequest);
                            }
                            return chain.proceed(original);
                        }
                    })
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            service = retrofit.create(ApiService.class);
        }
        return service;
    }

    public static void clearCookies() {
        if (cookieJar != null) {
            cookieJar.clear();
        }
    }

    public static synchronized void reset() {
        service = null;
        currentBaseUrl = null;
    }
}