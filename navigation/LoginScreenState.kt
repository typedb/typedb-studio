package com.vaticle.typedb.studio.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaticle.typedb.studio.db.DB
import com.vaticle.typedb.studio.db.DBServer

class LoginScreenState: ScreenState() {

    var serverAddress by mutableStateOf("127.0.0.1:1729")
    var dbServer: DBServer? by mutableStateOf(DBServer(serverAddress))
    var dbName by mutableStateOf("")
    var db: DB? by mutableStateOf(null)
}
