package com.github.mtatheonly.plugins.quasar

import org.gradle.api.Plugin
import org.gradle.api.Project

class QuasarInstrumentationPlugin implements Plugin<Project> {
    def static final PLUGIN_NAME = "com.github.mtatheonly.plugins.quasar"

    @Override
    void apply(Project project) {
        def extension = project.extensions.create("quasarInstrumentor", QuasarInstrumentationPluginExtension)

        project.tasks.create('quasarInstrumentation', QuasarInstrumentationTask) { qi ->
            qi.description 'Instrument classes with Quasar Instrumentor'
            qi.group 'Quasar'
        }
    }


}
