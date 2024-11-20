package com.typedb.studio.service.common.util

import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder
import java.nio.charset.Charset

object ConnectionUri {
    private const val SCHEME_CLOUD = "typedb-cloud"
    private const val SCHEME_CORE = "typedb-core"
    private const val SCHEME_SUFFIX = "://"
    private const val USERNAME_PASSWORD_SEPARATOR = ":"
    private const val AUTH_ADDRESS_SEPARATOR = "@"
    private const val PATH_SEPARATOR = "/"
    private const val ADDRESSES_SEPARATOR = ","
    private const val ADDRESS_TRANSLATION_SEPARATOR = ";"
    private const val PARAM_PREFIX = "?"
    private const val PARAM_SEPARATOR = "&"
    private const val PARAM_KEY_VALUE_SEPARATOR = "="
    private const val TLS_ENABLED = "tlsEnabled"
    val PLACEHOLDER_URI = buildCloud(Label.USERNAME.lowercase(), Label.PASSWORD.lowercase(), listOf(Label.ADDRESS.lowercase()), true)

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

    sealed interface ParsedConnectionUri

    data class ParsedCoreConnectionUri(val address: String): ParsedConnectionUri

    sealed interface ParsedCloudConnectionUri: ParsedConnectionUri {
        val username: String?
        val password: String?
        val tlsEnabled: Boolean?
    }

    data class ParsedCloudUntranslatedConnectionUri(
        override val username: String?, override val password: String?, val addresses: List<String>, override val tlsEnabled: Boolean?
    ): ParsedCloudConnectionUri

    data class ParsedCloudTranslatedConnectionUri(
        override val username: String?, override val password: String?, val addresses: List<Pair<String, String>>, override val tlsEnabled: Boolean?
    ): ParsedCloudConnectionUri

    fun parse(connectionUri: String): ParsedConnectionUri? {
        val uri = try { URI(connectionUri) } catch (_: URISyntaxException) { return null }

        val (username, password, addresses) = uri.authority?.let {
            val (auth, address) = it.split(AUTH_ADDRESS_SEPARATOR, limit = 2).let {
                if (it.size == 2) Pair(it[0], it[1])
                else Pair(it[0], null)
            }
            val (username, password) = auth.split(USERNAME_PASSWORD_SEPARATOR, limit = 2).let {
                if (it.size == 2) Pair(it[0], it[1])
                else Pair(it[0], null)
            }
            val addresses = address?.split(ADDRESSES_SEPARATOR) ?: emptyList()
            Triple(username, password, addresses)
        } ?: Triple(null, null, emptyList())
        val queryParams = uri.query?.split(PARAM_SEPARATOR)
            ?.map { it.split(PARAM_KEY_VALUE_SEPARATOR, limit = 2) }
            ?.filter { it.size == 2 }
            ?.associate { it[0] to it[1] }

        if (uri.scheme == SCHEME_CLOUD) {
            val decodedPassword = password?.let { URLDecoder.decode(it, Charset.defaultCharset()) }
            val tlsEnabled = queryParams?.get(TLS_ENABLED)?.toBoolean()
            if (addresses.getOrNull(0)?.contains(ADDRESS_TRANSLATION_SEPARATOR) == true) {
                return ParsedCloudTranslatedConnectionUri(
                    username = username,
                    password = decodedPassword,
                    addresses = addresses.map {
                        val splitAddresses = it.split(ADDRESS_TRANSLATION_SEPARATOR, limit = 2)
                        splitAddresses[0] to splitAddresses[1]
                    },
                    tlsEnabled = tlsEnabled
                )
            } else {
                return ParsedCloudUntranslatedConnectionUri(
                    username = username,
                    password = decodedPassword,
                    addresses = addresses,
                    tlsEnabled = tlsEnabled
                )
            }
        } else if (uri.scheme == SCHEME_CORE) {
            return ParsedCoreConnectionUri(addresses[0])
        } else {
            return null
        }
    }
}
