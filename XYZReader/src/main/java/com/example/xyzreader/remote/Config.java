package com.example.xyzreader.remote;

import java.net.MalformedURLException;
import java.net.URL;

public class Config {

    private static URL BASE_URL;
    private static final String JSON_URL = "https://dl.dropboxusercontent.com/u/231329/xyzreader_data/data.json";

    public static URL getBaseUrl() throws MalformedURLException {
        URL url;
        if (BASE_URL == null) {
            url = new URL(JSON_URL);
            BASE_URL = url;
        }
        return BASE_URL;
    }
}
