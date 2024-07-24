package com.machiav3lli.backup.plugins

import androidx.compose.runtime.Composable
import com.machiav3lli.backup.OABX
import com.machiav3lli.backup.tracePlugin
import timber.log.Timber
import java.io.File
import kotlin.reflect.KClass

interface PluginCompanion {

    fun klass() : KClass<out Plugin>
    fun name() : String = klass().simpleName?.removeSuffix("Plugin") ?: "Unknown"
    fun register() : Boolean
    fun create(file: File) : Plugin?
}

abstract class Plugin(val name: String, var file: File) {

    val className: String? get() = this::class.simpleName
    val typeName: String get() = className?.replace("Plugin", "") ?: "Unknown"

    @Composable
    abstract fun Editor()

    abstract fun save()
    abstract fun delete()

    companion object {

        // add new plugin classes here, necessary to have all classes initialized

        val pluginCompanions = mutableListOf<PluginCompanion>(
            SpecialFilesPlugin.Companion,
            RegexPlugin.Companion,
            ShellScriptPlugin.Companion,
        )

        var pluginTypes = mutableMapOf<String, PluginCompanion>()
        var pluginExtensions = mutableMapOf<String, String>()
        var pluginExtension = mutableMapOf<String, String>()
        const val DEFAULT_TYPE = "SpecialFiles"

        fun registerType(
            type: String,
            pluginCompanion: PluginCompanion,
            extensions: List<String>,
        ): Boolean {
            tracePlugin { ("register ${pluginCompanion.name()} type: $type, extensions: $extensions") }
            pluginTypes[type] = pluginCompanion
            pluginExtension[type] = extensions.first()
            extensions.forEach {
                pluginExtensions[it] = type
            }
            return true
        }

        fun createFrom(file: File) =
            pluginExtensions[file.extension]?.let { type ->
                pluginTypes[type]?.create(file)
            }

        var scanned = false

        var plugins = mutableMapOf<String, Plugin>()

        val builtinDir get() = OABX.context.filesDir?.resolve("plugin")
        val userDir get() = OABX.context.getExternalFilesDir(null)?.resolve("plugin")

        fun loadPluginFromDir(dir: File): Plugin? {
            Timber.w("not implemented: loadPluginFromDir ${dir.name}") //TODO hg42
            return null
        }

        fun loadPlugin(file: File): Plugin? {
            return if (file.isDirectory()) {
                loadPluginFromDir(file)
            } else {
                createFrom(file)
            }
        }

        fun loadPluginsFromDir(dir: File) {
            dir.listFiles()?.forEach {
                loadPlugin(it)?.let { plugin ->
                    plugins[plugin.name] = plugin
                }
            }
        }

        fun scan() {

            pluginCompanions.forEach { it.register() }

            synchronized(Plugin) {
                scanned = false
                plugins.clear()
                loadPluginsFromDir(OABX.assets.directory.resolve("plugin"))
                OABX.context.getExternalFilesDir(null)?.let {
                    loadPluginsFromDir(it.resolve("plugin"))
                }
                scanned = true
            }
        }

        fun ensureScanned() {
            synchronized(Plugin) {
                if (!scanned) {
                    scan()
                }
            }
        }

    }
}
