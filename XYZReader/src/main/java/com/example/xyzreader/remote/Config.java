package com.example.xyzreader.remote;

import java.net.MalformedURLException;
import java.net.URL;

public class Config {
//    public static final URL BASE_URL;
    private static URL BASE_URL;
    private static final String JSON_URL = "https://dl.dropboxusercontent.com/u/231329/xyzreader_data/data.json";

//    static {
//        URL url = null;
//        try {
//            url = new URL("https://dl.dropboxusercontent.com/u/231329/xyzreader_data/data.json" );
//        } catch (MalformedURLException ignored) {
//            // FIXME: 19/04/2016 handle exception
//            // TODO: throw a real error
//        }
//
//        BASE_URL = url;
//    }

    public static URL getBaseUrl() throws MalformedURLException {
//        if (true) throw new MalformedURLException("BR");
        URL url;
        if (BASE_URL == null) {
            url = new URL(JSON_URL);
            BASE_URL = url;
        }
        return BASE_URL;
    }
}
