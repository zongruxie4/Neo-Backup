package com.machiav3lli.backup.plugins

import com.machiav3lli.backup.tracePlugin
import java.io.File

class ShellScriptPlugin(file: File) : TextPlugin(file) {

    init {
        tracePlugin { ("${this::class.simpleName} $name <- ${file.name}") } //TODO hg42
    }

    companion object {

        fun register() = Plugin.registerType("ShellScript", ShellScriptPlugin::class, listOf("sh"))

        fun findScript(name: String) =
            try {
                (Plugin.plugins.get(name) as ShellScriptPlugin).file
            } catch (e: Throwable) {
                null
            }
    }
}
