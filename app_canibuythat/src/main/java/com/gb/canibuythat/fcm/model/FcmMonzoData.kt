package com.gb.canibuythat.fcm.model

class FcmMonzoData(val data: FcmTransactionData?)

class FcmTransactionData(val merchant: FcmMerchant?)

class FcmMerchant(val category: String?)