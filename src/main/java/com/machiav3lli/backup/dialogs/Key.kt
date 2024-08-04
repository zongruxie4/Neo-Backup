package com.machiav3lli.backup.dialogs

sealed class DialogKey {
    open class Warning(
        val message: String,
        val action: () -> Unit,
    ) : DialogKey()

    open class Encryption : DialogKey()

    data class Error(val message: String) : DialogKey()
}