package com.machiav3lli.backup.plugins

import androidx.compose.runtime.Composable
import com.machiav3lli.backup.OABX
import com.machiav3lli.backup.tracePlugin
import timber.log.Timber
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.primaryConstructor

abstract class Plugin(val name: String, var file: File) {

    val className: String? get() = this::class.simpleName
    val typeName: String get() = className?.replace("Plugin", "") ?: "Unknown"

    @Composable
    abstract fun Editor()

    abstract fun save()
    abstract fun delete()

    companion object {

        // add new plugin classes here, necessary to have all classes initialized

        val pluginClasses = mutableListOf<KClass<out Plugin>>(
            SpecialFilesPlugin::class,
            SpecialKotlinScriptPlugin::class,
            RegexPlugin::class,
            ShellScriptPlugin::class,
        )

        var pluginTypes = mutableMapOf<String, KClass<out Plugin>>()
        var pluginExtensions = mutableMapOf<String, String>()
        var pluginExtension = mutableMapOf<String, String>()
        val default = "SpecialFiles"

        fun registerType(type: String, pluginClass: KClass<out Plugin>, extensions: List<String>) : Boolean {
            tracePlugin { ("register ${pluginClass.simpleName} type: $type, extensions: $extensions") }
            pluginTypes.put(type, pluginClass)
            pluginExtension.put(type, extensions.first())
            extensions.forEach {
                pluginExtensions.put(it, type)
            }
            return true
        }

        fun createFrom(file: File) =
            pluginExtensions.get(file.extension)?.let { type ->
                pluginTypes.get(type)?.primaryConstructor?.call(file)
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
            if (file.isDirectory()) {
                return loadPluginFromDir(file)
            } else {
                return createFrom(file)
            }
        }

        fun loadPluginsFromDir(dir: File) {
            dir.listFiles()?.forEach {
                loadPlugin(it)?.let { plugin ->
                    plugins.put(plugin.name, plugin)
                }
            }
        }

        fun scan() {

            pluginClasses.forEach { pluginClass ->
                pluginClass.companionObject?.declaredFunctions
                    ?.find { it.name == "register" }
                    ?.call(pluginClass.companionObjectInstance)
            }

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
