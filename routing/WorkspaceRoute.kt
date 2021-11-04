package com.vaticle.typedb.studio.routing

import com.vaticle.typedb.studio.data.DB

class WorkspaceRoute(val loginForm: LoginFormSubmission): Route {
    val db: DB = loginForm.db
}
