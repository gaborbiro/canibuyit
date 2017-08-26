package com.gb.canibuythat

class MonzoConstants {
    companion object {
        const val CLIENT_ID = "oauthclient_00009NdpMmS85laW9PUzlh"
        const val CLIENT_SECRET = "olQNjJkkXQmi3l9ocGC4yd+uhPXN3K8tfTut71XqYlLP9K9bapIIM87H7Pob2nHQDjoL3ohYxAv2EpFaG29V"

        const val ACCOUNT_ID = "acc_00009GgHgzFZ5EffRUGtxR"

        // Auth

        const val MONZO_OAUTH_URL = "https://auth.getmondo.co.uk"
        const val MONZO_OAUTH_PARAM_AUTHORIZATION_CODE = "code"

        // Callbacks

        const val MONZO_AUTH_SCHEME = "https"
        const val MONZO_AUTH_AUTHORITY = "canibuythat.com"
        const val MONZO_AUTH_PATH_BASE = "auth"
        const val MONZO_AUTH_PATH_CALLBACK = "callback"

        const val MONZO_URI_AUTH_BASE = "$MONZO_AUTH_SCHEME://$MONZO_AUTH_AUTHORITY/$MONZO_AUTH_PATH_BASE"

        const val MONZO_URI_AUTH_CALLBACK = MONZO_URI_AUTH_BASE + "/" + MONZO_AUTH_PATH_CALLBACK

        // Api

        const val MONZO_API_BASE = "https://api.monzo.com"
    }
}
