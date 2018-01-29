package com.github.mtatheonly.plugins.quasar

import co.paralleluniverse.fibers.instrument.Log
import co.paralleluniverse.fibers.instrument.LogLevel
import co.paralleluniverse.fibers.instrument.QuasarInstrumentor
import groovy.io.FileType
import org.gradle.api.DefaultTask
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.TaskAction

class QuasarInstrumentationTask extends DefaultTask {
    Logger logger = Logging.getLogger(this.class)

    Logger getLog() {
        logger
    }

    @TaskAction
    void runTask() {
        def runtimeClasspath = project.sourceSets.test.runtimeClasspath
        def extension = project.extensions.quasarInstrumentor
        def urls = new URL[runtimeClasspath.files.size()]
        def i = 0
        URLClassLoader cl

        runtimeClasspath.files.each { file ->
            urls[i++] = file.toURI().toURL()
        }

        cl = new URLClassLoader(urls, getClass().getClassLoader())
        def instrumentor = new QuasarInstrumentor(true)
        instrumentor.setCheck(extension.instrumentorCheck)
        instrumentor.setVerbose(extension.instrumentorVerbose)
        instrumentor.setDebug(extension.instrumentorDebug)
        instrumentor.setAllowBlocking(extension.instrumentorAllowBlocking)
        instrumentor.setAllowMonitors(extension.instrumentorAllowMonitors)
        instrumentor.setLog(new Log() {
            @Override
            void log(LogLevel level, String msg, Object... args) {
                final String message = String.format(msg, args)
                switch (level) {
                    case LogLevel.DEBUG:
                        getLog().debug(message)
                        break
                    case LogLevel.INFO:
                        getLog().info(message)
                        break
                    case LogLevel.WARNING:
                        getLog().warn(message)
                        break
                    default:
                        getLog().error(message)
                        break
                }
            }

            @Override
            void error(String msg, Throwable ex) {
                getLog().error(msg,ex)
            }
        })

        Map<String, File> classes = new HashMap<>()
        project.buildDir.eachFileRecurse(FileType.FILES) { file ->
            if(file.name.endsWith(".class")) {
                String checkedClass = instrumentor.getMethodDatabase(cl).checkClass(file)
                if(checkedClass != null) {
                    classes.put(checkedClass, file)
                }
            }
        }
        instrumentClasses(instrumentor, cl, classes)
    }

    protected void logDebug(String fmt, Object... args) {
        getLog().debug(String.format(fmt, args))
    }

    protected void logInfo(String fmt, Object... args) {
        getLog().info(String.format(fmt, args))
    }

    protected void logWarn(String fmt, Object... args) {
        getLog().warn(String.format(fmt, args))
    }

    protected void logError(String s, Exception e) {
        getLog().error(s, e)
    }

    private void instrumentClasses(QuasarInstrumentor instrumentor, ClassLoader cl, Map<String, File> classes) {
        logInfo("Instrumenting %d classes...", classes.size())
        for (Map.Entry<String, File> entry : classes.entrySet())
            instrumentClass(instrumentor, cl, entry.getKey(), entry.getValue())
    }

    private void instrumentClass(QuasarInstrumentor instrumentor, ClassLoader cl, String name, File file) {
        if (!instrumentor.shouldInstrument(name))
            return
        try {
            file.withInputStream { fis ->
                String className = name.replace('.', '/')
                byte[] newClass = instrumentor.instrumentClass(cl, className, fis)
                file.withOutputStream { fos ->
                    fos.write(newClass)
                }
            }
        } catch (IOException ex) {
            logError("Error instrumenting file {}", file)
            throw new PluginApplicationException(QuasarInstrumentationPlugin.PLUGIN_NAME, ex)
        }
    }
}
