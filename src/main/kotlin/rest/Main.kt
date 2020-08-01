package rest

import arc.ApplicationListener
import arc.Core
import arc.util.Log
import mindustry.Vars.mods
import mindustry.plugin.Plugin

class Main : Plugin() {
    companion object {
        var pluginVersion: String? = null
    }

    override fun init() {
        for (mod in mods.list()){
            if (mod.meta.name == "Essentials"){
                pluginVersion = mod.meta.version
                val service = Network()
                service.start()
                Core.app.addListener(object : ApplicationListener {
                    override fun dispose() {
                        service.interrupt()
                    }
                })
            }
        }
        if(pluginVersion == null){
            Log.warn("Essential-REST_API plugin need Essentials plugin!")
        }
    }
}