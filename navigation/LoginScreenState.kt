package com.vaticle.typedb.studio.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.vaticle.typedb.studio.data.DB
import com.vaticle.typedb.studio.data.DBClient
import com.vaticle.typedb.studio.navigation.ServerSoftware.*

class LoginScreenState(serverSoftware: ServerSoftware = CORE, serverAddress: String = "127.0.0.1:1729",
                       username: String = "", password: String = "", rootCAPath: String = "", dbFieldText: String = "",
                       dbClient: DBClient? = null, db: DB? = null, allDBNames: List<String> = listOf()): ScreenState() {

    var serverSoftware: ServerSoftware by mutableStateOf(serverSoftware)
    var serverAddress: String by mutableStateOf(serverAddress)
    var username by mutableStateOf(username)
    var password by mutableStateOf(password)
    var rootCAPath: String by mutableStateOf(rootCAPath)
    var dbFieldText by mutableStateOf(dbFieldText)
    var dbClient: DBClient? by mutableStateOf(dbClient)
    val allDBNames: SnapshotStateList<String> = mutableStateListOf<String>().let { it += allDBNames; return@let it }
    var db: DB? by mutableStateOf(db)
    var databaseSelected: Boolean by mutableStateOf(dbFieldText.isNotBlank())
}

enum class ServerSoftware {
    CORE,
    CLUSTER
}
