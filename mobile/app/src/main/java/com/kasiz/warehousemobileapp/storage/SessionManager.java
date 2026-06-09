package com.kasiz.warehousemobileapp.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.kasiz.warehousemobileapp.model.MeProfile;

public final class SessionManager {

    private static final String PREF_NAME = "warehouse_mobile_session";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_IDENTIFIER = "identifier";
    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_PROFILE_JSON = "profile_json";
    public static final String DEFAULT_BASE_URL = "http://10.0.2.2:4000/api/";

    private static final Gson GSON = new Gson();

    private static SessionManager instance;

    private final SharedPreferences preferences;

    private SessionManager(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    public void saveLogin(String identifier) {
        preferences.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_IDENTIFIER, identifier)
                .apply();
    }

    public boolean isLoggedIn() {
        return preferences.getBoolean(KEY_LOGGED_IN, false);
    }

    public String getIdentifier() {
        return preferences.getString(KEY_IDENTIFIER, "");
    }

    public void saveProfile(MeProfile profile) {
        if (profile == null) return;
        preferences.edit().putString(KEY_PROFILE_JSON, GSON.toJson(profile)).apply();
    }

    public MeProfile getCachedProfile() {
        String json = preferences.getString(KEY_PROFILE_JSON, "");
        if (json == null || json.trim().isEmpty()) return null;
        try {
            return GSON.fromJson(json, MeProfile.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    public String getFullName() {
        MeProfile profile = getCachedProfile();
        if (profile != null && profile.fullName != null && !profile.fullName.trim().isEmpty()) {
            return profile.fullName.trim();
        }
        return getIdentifier();
    }

    public int getPositionLevel() {
        MeProfile profile = getCachedProfile();
        return profile == null ? 0 : profile.positionLevel;
    }

    public boolean hasPermission(String resourceType, String permissionName) {
        MeProfile profile = getCachedProfile();
        if (profile == null || profile.permissions == null) return false;
        String wantedResource = resourceType == null ? "" : resourceType.trim().toUpperCase();
        String wantedPermission = permissionName == null ? "" : permissionName.trim().toUpperCase();
        for (MeProfile.PermissionItem permission : profile.permissions) {
            if (permission == null) continue;
            String actualResource = permission.resourceType == null ? "" : permission.resourceType.trim().toUpperCase();
            String actualPermission = permission.permissionName == null ? "" : permission.permissionName.trim().toUpperCase();
            if (wantedResource.equals(actualResource) && wantedPermission.equals(actualPermission)) {
                return true;
            }
        }
        return false;
    }

    public boolean isFieldWorker() {
        return "congNhan".equals(getRoleKey());
    }

    public String getRoleKey() {
        MeProfile profile = getCachedProfile();
        if (profile == null) return "congNhan";
        int level = profile.positionLevel;
        if (level >= 5) return "bGD";
        if (level >= 4) return "admin";
        if (level >= 3) {
            int pid = profile.positionId;
            if (pid == 6 || pid == 8) return "truongPhong";
            if (pid == 7 || pid == 9) return "headPtkT";
            return "truongCa";
        }
        if (level >= 2) return "kyThuat";
        return "congNhan";
    }

    public boolean canUpdateAssets() {
        if (!isFieldWorker()) return false;
        MeProfile profile = getCachedProfile();
        if (profile == null) return false;
        if (profile.positionLevel >= 2) return true;
        return hasPermission("ASSET", "UPDATE");
    }

    public boolean canCreateAssets() {
        MeProfile profile = getCachedProfile();
        if (profile == null) return false;
        if (profile.positionLevel >= 2) return true;
        return hasPermission("ASSET", "CREATE");
    }

    public boolean canDeleteAssets() {
        MeProfile profile = getCachedProfile();
        if (profile == null) return false;
        if (profile.positionLevel >= 3) return true;
        return hasPermission("ASSET", "DELETE");
    }

    public String getBaseUrl() {
        String value = preferences.getString(KEY_BASE_URL, DEFAULT_BASE_URL);
        if (value == null || value.trim().isEmpty()) {
            return DEFAULT_BASE_URL;
        }
        String normalized = value.trim();
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    public void saveBaseUrl(String baseUrl) {
        String normalized = baseUrl == null ? DEFAULT_BASE_URL : baseUrl.trim();
        if (normalized.isEmpty()) {
            normalized = DEFAULT_BASE_URL;
        }
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        preferences.edit().putString(KEY_BASE_URL, normalized).apply();
    }

    public void clear() {
        String baseUrl = getBaseUrl();
        preferences.edit().clear().putString(KEY_BASE_URL, baseUrl).apply();
    }
}