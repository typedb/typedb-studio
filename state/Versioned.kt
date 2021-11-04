package com.vaticle.typedb.studio.state

import java.util.UUID

interface Versioned<T> {
    val value: T
    val version: UUID
}
