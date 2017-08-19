package com.gb.canibuythat;

public class MonzoConstants {

    public static final String USER_ID = "user_00009GOVUin6LydhadHxYn";
    public static final String CLIENT_ID = "oauthclient_00009NdpMmS85laW9PUzlh";
    public static final String CLIENT_SECRET = "olQNjJkkXQmi3l9ocGC4yd+uhPXN3K8tfTut71XqYlLP9K9bapIIM87H7Pob2nHQDjoL3ohYxAv2EpFaG29V";

    // Auth

    public static final String MONZO_OAUTH_URL = "https://auth.getmondo.co.uk";
    public static final String MONZO_OAUTH_PARAM_AUTHORIZATION_CODE = "code";

    // Callbacks

    public static final String MONZO_AUTH_SCHEME = "https";
    public static final String MONZO_AUTH_AUTHORITY = "canibuythat.com";
    public static final String MONZO_AUTH_PATH_BASE = "auth";
    public static final String MONZO_AUTH_PATH_CALLBACK = "callback";

    public static final String MONZO_URI_AUTH_BASE = MONZO_AUTH_SCHEME + "://" + MONZO_AUTH_AUTHORITY + "/" + MONZO_AUTH_PATH_BASE;

    public static final String MONZO_URI_AUTH_CALLBACK = MONZO_URI_AUTH_BASE + "/" + MONZO_AUTH_PATH_CALLBACK;

    // Api

    public static final String MONZO_API_BASE = "https://api.monzo.com";
}
