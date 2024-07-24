package com.machiav3lli.backup.plugins

import com.machiav3lli.backup.tracePlugin
import java.io.File

class ShellScriptPlugin(file: File) : TextPlugin(file) {

    init {
        tracePlugin { ("${this::class.simpleName} $name <- ${file.name}") } //TODO hg42
    }

    companion object : PluginCompanion {

        override fun klass() = ShellScriptPlugin::class
        override fun register() = registerType(name(), Companion, listOf("sh"))
        override fun create(file: File) = ShellScriptPlugin(file)

        fun findScript(name: String) =
            try {
                (Plugin.plugins.get(name) as ShellScriptPlugin).file
            } catch (e: Throwable) {
                null
            }
    }
}
