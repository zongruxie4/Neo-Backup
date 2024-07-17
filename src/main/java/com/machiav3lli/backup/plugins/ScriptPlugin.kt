package com.machiav3lli.backup.plugins

import timber.log.Timber
import java.io.File

class ScriptPlugin(val file: File) : Plugin() {
    fun constructor() {
        val name = file.nameWithoutExtension
        Timber.w("not implemented: ScriptPlugin ${file.name} -> $name") //TODO hg42
    }
}