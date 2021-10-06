package com.vaticle.typedb.studio.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaticle.typedb.studio.data.DB
import com.vaticle.typedb.studio.data.DBClient

class LoginScreenState(serverAddress: String = "127.0.0.1:1729", dbName: String = ""): ScreenState() {

    var serverAddress by mutableStateOf(serverAddress)
    var dbClient by mutableStateOf(DBClient(serverAddress))
    var dbName by mutableStateOf(dbName)
    var db: DB? by mutableStateOf(if (serverAddress.isNotBlank() && dbName.isNotBlank()) DB(dbClient, dbName) else null)
}
