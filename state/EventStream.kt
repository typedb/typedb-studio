package com.vaticle.typedb.studio.state

import java.util.UUID

class TrackedValue<T>(initialValue: T) : Versioned<T> {
    override val value: T = initialValue
    override val version: UUID = UUID.randomUUID()
}
