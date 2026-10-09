package com.payledger.app.notification

object ProviderPackages {
    /** Known package names for supported wallets (may include alternate packages). */
    val PACKAGE_TO_PROVIDER: Map<String, String> = mapOf(
        "com.techlogix.mobilinkcustomer" to "jazzcash",
        "com.jazzcash.customer" to "jazzcash",
        "pk.com.telenor.phoenix" to "easypaisa",
        "com.telenor.easypaisa.android" to "easypaisa",
        "com.easypaisa.customer" to "easypaisa",
        "com.techlogix.mobilinkbank" to "jazzcash",
        "com.sadapay.customer" to "sadapay",
        "com.sadapay.app" to "sadapay",
        "com.nayapay.app" to "nayapay",
        "com.nayapay.customer" to "nayapay",
    )

    fun providerFor(packageName: String): String? = PACKAGE_TO_PROVIDER[packageName]
}
