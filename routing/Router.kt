package com.vaticle.typedb.studio.routing

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class Router(initialRoute: Route) {

    var currentRoute: Route by mutableStateOf(initialRoute)
        private set

    fun navigateTo(route: Route) {
        currentRoute = route
    }
}
