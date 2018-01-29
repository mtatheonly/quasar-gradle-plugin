# quasar-gradle-plugin
Plugin for Quasar instrumentation with gradle. Based on https://github.com/vy/quasar-maven-plugin

This plugins adds a task 'quasarInstrumentation', which does the instrumentation.


As a sugestion, you can add it as the finalizer task of compile
EG: compileJava.finalizedBy(quasarInstrumentation)


