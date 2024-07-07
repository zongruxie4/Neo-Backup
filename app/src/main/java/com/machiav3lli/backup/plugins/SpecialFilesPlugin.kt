package com.machiav3lli.backup.plugins

import com.machiav3lli.backup.BuildConfig
import com.machiav3lli.backup.dbs.entity.SpecialInfo
import com.machiav3lli.backup.handler.ShellCommands
import java.io.File

class SpecialFilesPlugin : Plugin {

    val name : String
    val icon : Int = -1
    val files : List<String>

    constructor(file: File) {

        name = file.nameWithoutExtension

        var filesFile : File = file
        if (file.isDirectory()) {
            filesFile = file.resolve("files")
        }
        files = filesFile.readLines()

        register[name] = this
    }

    companion object {

        val register : PluginRegister<SpecialFilesPlugin> = mutableMapOf()

        fun specialInfos() : List<SpecialInfo> {

            ensureScanned()

            return register.map { entry->
                val plugin = entry.value
                val name = plugin.name
                val label = "$ " + name.replace(".", " ").replace("_", " ")
                val files = plugin.files.map { replaceVars(it) }
                SpecialInfo(
                    packageName = "special.$name",
                    label = label,
                    versionName = BuildConfig.VERSION_NAME,
                    versionCode = BuildConfig.VERSION_CODE,
                    specialFiles = files.toTypedArray(),
                    plugin.icon
                )
            }
        }

        fun replaceVars(text: String) : String {
            val userId = ShellCommands.currentProfile
            val replacements = mapOf(
                "userId"        to userId.toString(),
                "miscData"      to "/data/misc",
                "systemData"    to "/data/system",
                "userData"      to "/data/system/users/$userId",
                "systemCeData"  to "/data/system_ce/$userId",
                "vendorDeData"  to "/data/vendor_de/$userId",
            )
            var result = text
            replacements.forEach { replacement ->
                result = result.replace("<" + replacement.key + ">", replacement.value)
            }
            return result
        }
    }
}