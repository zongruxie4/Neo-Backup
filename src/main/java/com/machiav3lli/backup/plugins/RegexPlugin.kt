package com.machiav3lli.backup.plugins

import android.annotation.SuppressLint
import com.machiav3lli.backup.tracePlugin
import java.io.File

class RegexPlugin(file: File) : TextPlugin(file) {

    init {
        tracePlugin { ("${this.javaClass.simpleName} $name <- ${file.name}") } //TODO hg42
    }

    val regex get() = Regex(
        """(?x)(^("""
                + replaceVars(text)
                + """)$)"""
    )

    companion object : PluginCompanion {

        override fun klass() = RegexPlugin::class
        override fun register() = registerType(name(), Companion, listOf("regex"))
        override fun create(file: File) = RegexPlugin(file)

        fun findRegex(name: String) =
            try {
                (plugins.get(name) as RegexPlugin).regex
            } catch (e: Throwable) {
                Regex("T-h-I-s--S-h-O-u-L-d--N-e-V-e-R--m-A-t-C-h")
            }

        @SuppressLint("SdCardPath")
        fun replaceVars(text: String) : String {
            val replacements = mapOf(
                "ownPackage" to com.machiav3lli.backup.BuildConfig.APPLICATION_ID.replace(".", """\.""")
            )
            var result = text
            replacements.forEach { replacement ->
                result = result.replace("<" + replacement.key + ">", replacement.value)
            }
            return result
        }
    }
}