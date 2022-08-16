package com.vaticle.typedb.studio.state.preference

class Preference constructor(val description: String, val placeholder: String, var value: String, val validInput: (String) -> Boolean) {
    fun validInput(): Boolean {
        return validInput(value)
    }
}