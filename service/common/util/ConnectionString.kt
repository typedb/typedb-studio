package com.typedb.studio.service.common.util

import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder
import java.nio.charset.Charset

object ConnectionString {
    const val SCHEME_CLOUD = "typedb-cloud"
    const val SCHEME_CORE = "typedb-core"
    private const val SCHEME_SUFFIX = "://"
    const val USERNAME_PASSWORD_SEPARATOR = ":"
    const val AUTH_ADDRESS_SEPARATOR = "@"
    private const val PATH_SEPARATOR = "/"
    const val ADDRESSES_SEPARATOR = ","
    const val ADDRESS_TRANSLATION_SEPARATOR = ";"
    private const val PARAM_PREFIX = "?"
    const val PARAM_SEPARATOR = "&"
    const val PARAM_KEY_VALUE_SEPARATOR = "="
    const val TLS_ENABLED = "tlsEnabled"

    fun buildCore(address: String) = listOf(SCHEME_CORE, SCHEME_SUFFIX, address).joinToString("")

    fun buildCloud(username: String, password: String, addresses: List<String>, tlsEnabled: Boolean): String = listOf(
        SCHEME_CLOUD, SCHEME_SUFFIX,
        username, USERNAME_PASSWORD_SEPARATOR, password,
        AUTH_ADDRESS_SEPARATOR,
        addresses.joinToString(ADDRESSES_SEPARATOR),
        PATH_SEPARATOR, PARAM_PREFIX, TLS_ENABLED, PARAM_KEY_VALUE_SEPARATOR, tlsEnabled.toString()
    ).joinToString("")

    fun buildCloudTranslated(username: String, password: String, translatedAddresses: List<Pair<String, String>>, tlsEnabled: Boolean) = buildCloud (
        username, password, translatedAddresses.map { (a, b) -> "$a$ADDRESS_TRANSLATION_SEPARATOR$b" }, tlsEnabled
    )

    sealed interface ParsedConnectionString

    data class ParsedCoreConnectionString(val address: String): ParsedConnectionString

    sealed interface ParsedCloudConnectionString: ParsedConnectionString {
        val username: String
        val password: String
        val tlsEnabled: Boolean?
    }

    data class ParsedCloudUntranslatedConnectionString(
        override val username: String, override val password: String, val addresses: List<String>, override val tlsEnabled: Boolean?
    ): ParsedCloudConnectionString

    data class ParsedCloudTranslatedConnectionString(
        override val username: String, override val password: String, val addresses: List<Pair<String, String>>, override val tlsEnabled: Boolean?
    ): ParsedCloudConnectionString

    fun parse(connectionString: String): ParsedConnectionString? {
        val uri = try { URI(connectionString) } catch (_: URISyntaxException) { return null }

        val (auth, address) = uri.authority.split(AUTH_ADDRESS_SEPARATOR, limit = 2)
        val (username, password) = auth.split(USERNAME_PASSWORD_SEPARATOR, limit = 2)
        val addresses = address.split(ADDRESSES_SEPARATOR)
        val queryParams = uri.query?.split(PARAM_SEPARATOR)?.associate {
            val k = it.split(PARAM_KEY_VALUE_SEPARATOR, limit = 2)
            k[0] to k[1]
        }

        if (uri.scheme == SCHEME_CLOUD) {
            val decodedPassword = URLDecoder.decode(password, Charset.defaultCharset())
            val tlsEnabled = queryParams?.get(TLS_ENABLED)?.toBoolean()
            if (addresses.getOrNull(0)?.contains(ADDRESS_TRANSLATION_SEPARATOR) == true) {
                return ParsedCloudTranslatedConnectionString(
                    username = username,
                    password = decodedPassword,
                    addresses = addresses.map {
                        val splitAddresses = it.split(ADDRESS_TRANSLATION_SEPARATOR, limit = 2)
                        splitAddresses[0] to splitAddresses[1]
                    },
                    tlsEnabled = tlsEnabled
                )
            } else {
                return ParsedCloudUntranslatedConnectionString(
                    username = username,
                    password = decodedPassword,
                    addresses = addresses,
                    tlsEnabled = tlsEnabled
                )
            }
        } else if (uri.scheme == SCHEME_CORE) {
            return ParsedCoreConnectionString(addresses[0])
        } else {
            return null
        }
    }
}
