package com.machiav3lli.backup.plugins

import android.annotation.SuppressLint
import com.machiav3lli.backup.dbs.entity.SpecialInfo
import com.machiav3lli.backup.handler.ShellCommands
import com.machiav3lli.backup.tracePlugin
import java.io.File

class SpecialFilesPlugin(file: File) : TextPlugin(file) {

    val files : List<String> get() = text
        .split("\n")
        .map { it.trim() }
        .filterNot {
            it.isEmpty() || it.startsWith("#")
        }
        .map { replaceVars(it) }

    init {
        tracePlugin { (listOf("${this.javaClass.simpleName} $name <- ${file.name}") + files).joinToString("\n  ") }
    }

    companion object {

        fun register() = Plugin.registerType("SpecialFiles", SpecialFilesPlugin::class, listOf("special_files"))

        fun specialInfos() : List<SpecialInfo> {

            ensureScanned()

            return plugins.filter { it.value is SpecialFilesPlugin }.map {
                val plugin = it.value as SpecialFilesPlugin
                val name = plugin.name
                val label = "$ " + name.replace(".", " ").replace("_", " ")
                val files = plugin.files
                SpecialInfo(
                    packageName = "special.$name",
                    label = label,
                    versionName = "",
                    versionCode = 0,
                    specialFiles = files.toTypedArray(),
                    -1
                )
            }
        }

        @SuppressLint("SdCardPath")
        fun replaceVars(text: String) : String {
            val userId = ShellCommands.currentProfile
            val replacements = mapOf(
                "userId"        to userId.toString(),
                "miscData"      to "/data/misc",
                "systemData"    to "/data/system",
                "userData"      to "/data/system/users/$userId",
                "systemCeData"  to "/data/system_ce/$userId",
                "vendorDeData"  to "/data/vendor_de/$userId",
                "userData"      to "/data/user/$userId",
                "userDeData"    to "/data/user_de/$userId",
                "extData"       to "/storage/emulated/$userId/Android/data",
                "extMedia"      to "/storage/emulated/$userId/Android/media",
                "extObb"        to "/storage/emulated/$userId/Android/obb",
            )
            var result = text
            replacements.forEach { replacement ->
                result = result.replace("<" + replacement.key + ">", replacement.value)
            }
            return result
        }
    }
}