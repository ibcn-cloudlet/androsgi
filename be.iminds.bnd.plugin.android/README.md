BND Android plugin
==================

This BND plugin adds a classes.dex file to the .jar to enable deployment on Android's Dalvik VM.

The plugin can be downloaded from http://aiolos.intec.ugent.be/repositories/androsgi/be.iminds.bnd.plugin.android/be.iminds.bnd.plugin.android.jar

Usage
-----

Add the plugin to the project's bnd.bnd for which you want to add a classes.dex file in the .jar
using the -pluginpath and -plugin attributes. For example:

	-pluginpath: [path-to-the-jar]/be.iminds.bnd.plugin.android.jar
	-plugin: be.iminds.bnd.plugin.android.AndroidPlugin

To configure the plugin for all projects in your workspace, you can also add the plugin to the 
cnf project by placing the plugin .jar in cnf/plugins/be.iminds.bnd.plugin.android/be.iminds.bnd.plugin.android.jar
and adding the following configuration in cnf/build.bnd

	-pluginpath:	${ext.repositories.-pluginpath}, cnf/plugins/be.iminds.bnd.plugin.android/be.iminds.bnd.plugin.android.jar
	-plugin: 		${ext.repositories.-plugin}, be.iminds.bnd.plugin.android.AndroidPlugin


Dx
--

The plugin uses the dx tool provided by the Android SDK. Make sure that you have added the path to dx to your PATH
environment variabele, or explicitly configure it in your .bnd file:

	-plugin: be.iminds.bnd.plugin.android.AndroidPlugin;dx=/opt/android-sdk-linux/platform-tools/dx

