/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.service.common.util

import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder
import java.nio.charset.Charset

object ConnectionUri {
    private const val SCHEME_CLOUD = "typedb-cloud"
    private const val SCHEME_CORE = "typedb-core"
    private const val ADDRESSES_SEPARATOR = ","
    private const val ADDRESS_TRANSLATION_SEPARATOR = ";"
    private const val TLS_ENABLED = "tlsEnabled"
    val PLACEHOLDER_URI = buildCloud(Label.USERNAME.lowercase(), Label.PASSWORD.lowercase(), listOf(Label.ADDRESS.lowercase()), true)

    fun buildCore(address: String) = "$SCHEME_CORE://$address"

    fun buildCloud(username: String, password: String, addresses: List<String>, tlsEnabled: Boolean)=
        "$SCHEME_CLOUD://$username:$password@${addresses.joinToString(ADDRESSES_SEPARATOR)}/?$TLS_ENABLED=${tlsEnabled}"

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

        if (uri.scheme == SCHEME_CLOUD) {
            val (username, password, addresses) = uri.authority?.let {
                val (auth, address) = it.split("@", limit = 2)
                    .let { Pair(it[0], it.getOrNull(1)) }
                val (username, password) = auth.split(":", limit = 2)
                    .let { Pair(it[0], it.getOrNull(1)) }
                val addresses = address?.split(ADDRESSES_SEPARATOR) ?: emptyList()
                Triple(username, password, addresses)
            } ?: Triple(null, null, emptyList())

            val queryParams = uri.query?.split("&")?.associate {
                it.split("=", limit = 2).let { Pair(it[0], it.getOrNull(1)) }
            }

            val decodedPassword = password?.let { URLDecoder.decode(it, Charset.defaultCharset()) }
            val tlsEnabled = queryParams?.get(TLS_ENABLED)?.toBoolean()
            if (addresses.getOrNull(0)?.contains(ADDRESS_TRANSLATION_SEPARATOR) == true) {
                return ParsedCloudTranslatedConnectionUri(
                    username = username,
                    password = decodedPassword,
                    addresses = addresses.map {
                        it.split(ADDRESS_TRANSLATION_SEPARATOR, limit = 2).let { Pair(it[0], it.getOrNull(1)) }
                    }.filter { it.second != null }.map { it.first to it.second!! },
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
        } else if (uri.scheme == SCHEME_CORE && !uri.authority.isNullOrBlank()) {
            return ParsedCoreConnectionUri(uri.authority)
        } else {
            return null
        }
    }

}
