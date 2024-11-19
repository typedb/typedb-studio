package com.typedb.studio.service.common.util

object ConnectionString {
    const val SCHEME = "typedb://"
    const val USERNAME_PASSWORD_SEPARATOR = ":"
    const val AUTH_ADDRESS_SEPARATOR = "@"
    const val PATH_SEPARATOR = "/"
    const val ADDRESSES_SEPARATOR = ","
    const val ADDRESS_TRANSLATION_SEPARATOR = ";"
    const val PARAM_STARTER = "?"
    const val PARAM_SEPARATOR = "&"
    const val PARAM_KEY_VALUE_SEPARATOR = "="
    const val TLS_ENABLED = "tlsEnabled"

    fun build(username: String, password: String, addresses: List<String>, tlsEnabled: Boolean): String = listOf(
        SCHEME,
        username, USERNAME_PASSWORD_SEPARATOR, password,
        AUTH_ADDRESS_SEPARATOR,
        addresses.joinToString(ADDRESSES_SEPARATOR),
        PATH_SEPARATOR, PARAM_STARTER, TLS_ENABLED, PARAM_KEY_VALUE_SEPARATOR, tlsEnabled.toString()
    ).joinToString("")

    fun buildTranslated(username: String, password: String, translatedAddresses: List<Pair<String, String>>, tlsEnabled: Boolean) = build (
        username, password, translatedAddresses.map { (a, b) -> "$a$ADDRESS_TRANSLATION_SEPARATOR$b" }, tlsEnabled
    )
}
