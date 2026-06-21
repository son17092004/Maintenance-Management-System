package com.kasiz.warehousemobileapp.network;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public final class PersistentCookieJar implements CookieJar {

    private static final String PREF_NAME = "warehouse_mobile_cookies";
    private static final String KEY_COOKIES = "cookies";

    private static final Type MAP_TYPE = new TypeToken<Map<String, StoredCookie>>() { } .getType();

    private final SharedPreferences preferences;
    private final Gson gson = new Gson();

    public PersistentCookieJar(Context context) {
        this.preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // luu cookie khi server gui ve
    @Override
    public synchronized void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        Map<String, StoredCookie> stored = readAll();
        long now = System.currentTimeMillis();
        for (Cookie cookie : cookies) {
            String key = key(cookie);
            if (cookie.expiresAt() < now) {
                stored.remove(key);
            } else {
                stored.put(key, StoredCookie.from(cookie));
            }
        }
        writeAll(stored);
    }

    //
    @Override
    public synchronized List<Cookie> loadForRequest(HttpUrl url) {
        Map<String, StoredCookie> stored = readAll();
        long now = System.currentTimeMillis();
        List<Cookie> result = new ArrayList<>();
        boolean changed = false;
        //duyet toan bo cookie
        for (Map.Entry<String, StoredCookie> entry : stored.entrySet()) {
            StoredCookie data = entry.getValue();
            if (data == null) continue;
            Cookie cookie = data.toCookie(url); //chuyen storedCookie thanh cookie cua okhttp
            if (cookie.expiresAt() < now) {
                changed = true;
                continue;
            }
            result.add(cookie);
        }

        if (changed) {
            Map<String, StoredCookie> refreshed = new HashMap<>();
            for (Map.Entry<String, StoredCookie> entry : stored.entrySet()) {
                StoredCookie data = entry.getValue();
                if (data != null && data.expiresAt >= now) {
                    refreshed.put(entry.getKey(), data);
                }
            }
            writeAll(refreshed);
        }

        return result;
    }

    public synchronized void clear() {
        preferences.edit().remove(KEY_COOKIES).apply();
    }


    //doc cookie tu shared preferences
    private Map<String, StoredCookie> readAll() {
        String json = preferences.getString(KEY_COOKIES, "{}");
        Map<String, StoredCookie> stored = gson.fromJson(json, MAP_TYPE);
        return stored == null ? new HashMap<>() : stored;
    }

    //ghi de cookie vao shared preferences
    private void writeAll(Map<String, StoredCookie> cookies) {
        preferences.edit().putString(KEY_COOKIES, gson.toJson(cookies, MAP_TYPE)).apply();
    }

    //sinh khoa duy nhat, tranh trung ten cookie
    private String key(Cookie cookie) {
        return cookie.name() + "|" + cookie.domain() + "|" + cookie.path();
    }

    //dto de luu xuong JSON
    private static final class StoredCookie {
        String name;
        String value;
        String domain;
        String path;
        long expiresAt;
        boolean secure;
        boolean httpOnly;
        boolean hostOnly;

        //chuyen cookie thanh storedCookie
        static StoredCookie from(Cookie cookie) {
            StoredCookie stored = new StoredCookie();
            stored.name = cookie.name();
            stored.value = cookie.value();
            stored.domain = cookie.domain();
            stored.path = cookie.path();
            stored.expiresAt = cookie.expiresAt();
            stored.secure = cookie.secure();
            stored.httpOnly = cookie.httpOnly();
            stored.hostOnly = cookie.hostOnly();
            return stored;
        }

        //chuyen storedCookie thanh cookie cua okhttp
        Cookie toCookie(HttpUrl url) {
            Cookie.Builder builder = new Cookie.Builder()
                    .name(name)
                    .value(value)
                    .path(path)
                    .expiresAt(expiresAt);
            if (httpOnly) {
                builder.httpOnly();
            }
            // Force cookie domain to match current request host to ensure it passes OkHttp checks
            builder.hostOnlyDomain(url.host());
            return builder.build();
        }
    }
}