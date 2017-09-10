package com.gb.canibuythat

class MonzoConstants {
    companion object {
        const val CLIENT_ID = "oauthclient_00009NwZL762SfIFWuVJE9"
        const val CLIENT_SECRET = "2WIqPL+7cdaH1Dgl6Gi776GUPPWlHPs2kLjvC7cB0389MCJvVOqZrystvq4nbeLf25p9Cxkks3WpYZXPQflI"

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
        const val MONZO_DISPATCH_API_BASE = "http://arlecchino.pythonanywhere.com"

        const val MONZO_DISPATCH_API_WEBHOOK = "http://arlecchino.pythonanywhere.com/webhook"
    }
}
