package com.example.xyzreader.remote;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

import java.io.IOException;
import java.net.URL;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RemoteEndpointUtil {
    private static final String TAG = "RemoteEndpointUtil";

    private RemoteEndpointUtil() {
    }

    @Nullable
    public static JSONArray fetchJsonArray() throws IOException, JSONException {
        String itemsJson;
        itemsJson = fetchPlainText(Config.getBaseUrl());

        // Parse JSON
        JSONTokener tokener = new JSONTokener(itemsJson);
        Object val = tokener.nextValue();
        if (!(val instanceof JSONArray)) {
            throw new JSONException("Expected JSONArray");
        }
        return (JSONArray) val;
    }

    @NonNull
    static String fetchPlainText(URL url) throws IOException {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }
}
