package com.machiav3lli.backup.plugins

import com.machiav3lli.backup.OABX
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

                    //TODO hg42 register handlers on extensions

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
