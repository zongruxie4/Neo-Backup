package com.machiav3lli.backup.plugins

import com.machiav3lli.backup.BuildConfig
import com.machiav3lli.backup.OABX
import com.machiav3lli.backup.dbs.entity.SpecialInfo
import com.machiav3lli.backup.handler.ShellCommands
import timber.log.Timber
import java.io.File

typealias PluginRegister<P> = MutableMap<String, P>

abstract class Plugin {

    companion object {

        var scanned = false

        fun loadPluginFromDir(dir: File) {
            Timber.w("not implemented: loadPluginFromDir ${dir.name}") //TODO hg42
        }

        fun loadPlugin(file: File) {
            if (file.isDirectory()) {
                loadPluginFromDir(file)
            } else {
                when (file.extension) {

                    "specialfiles" -> {
                        SpecialFilesPlugin(file)
                    }

                    else           -> {
                        ScriptPlugin(file)
                    }
                }
            }
        }

        fun loadPluginsFromDir(dir: File) {
            dir.listFiles()?.forEach {
                loadPlugin(it)
            }
        }

        fun scan() {
            synchronized(Plugin) {
                loadPluginsFromDir(OABX.assets.directory.resolve("plugin"))
                OABX.context.getExternalFilesDir(null)?.let {
                    loadPluginsFromDir(it.resolve("plugin"))
                }
                scanned = true
            }
        }

        fun ensureScanned() {
            if (!scanned) {
                scan()
            }
        }
    }
}

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

            val userId = ShellCommands.currentUser
            val replacements = mapOf(
                "userId"        to userId.toString(),
                "miscData"      to "/data/misc",
                "systemData"    to "/data/system",
                "userData"      to "/data/system/users/$userId",
                "systemCeData"  to "/data/system_ce/$userId",
                "vendorDeData"  to "/data/vendor_de/$userId",
            )
            return register.map { entry->
                val plugin = entry.value
                val name = plugin.name
                val label = "$ " + name.replace(".", " ").replace("_", " ")
                val files = plugin.files.map {
                    var result = it
                    replacements.forEach { replacement ->
                        result = result.replace("<" + replacement.key + ">", replacement.value)
                    }
                    result
                }
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
    }
}

class ScriptPlugin(val file: File) : Plugin() {
    fun constructor() {
        val name = file.nameWithoutExtension
        Timber.w("not implemented: ScriptPlugin ${file.name} -> $name") //TODO hg42
    }
}
