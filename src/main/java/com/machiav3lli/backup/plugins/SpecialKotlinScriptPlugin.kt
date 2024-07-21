package com.machiav3lli.backup.plugins

import com.machiav3lli.backup.tracePlugin
import java.io.File

class SpecialKotlinScriptPlugin(file: File) : TextPlugin(file) {

    init {
        tracePlugin { ("${this.javaClass.simpleName} $name <- ${file.name} (not implemented)") } //TODO hg42
    }

    companion object {

        fun register() = PluginTypes.register("SpecialKotlinScript", SpecialKotlinScriptPlugin::class, listOf("special_kts"))
    }
}