package com.machiav3lli.backup.plugins

import com.machiav3lli.backup.tracePlugin
import java.io.File

class RegexPlugin(file: File) : TextPlugin(file) {

    init {
        tracePlugin { ("${this.javaClass.simpleName} $name <- ${file.name}") } //TODO hg42
    }

    val regex get() = Regex("""(?x)(^(""" + text + """)$)""")

    companion object {

        fun register() = Plugin.registerType("Regex", RegexPlugin::class, listOf("regex"))

        fun findRegex(name: String) =
            try {
                (Plugin.plugins.get(name) as RegexPlugin).regex
            } catch (e: Throwable) {
                Regex("T-h-I-s--S-h-O-u-L-d--N-e-V-e-R--m-A-t-C-h")
            }
    }
}