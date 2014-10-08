Androsgi Tools and Utilities
============================

Androsgi is an umbrella project for various tools and utilities to facilitate embedding OSGi in Android projects. In order to build and run OSGi on top of Android
we have created an [Eclipse plugin](be.iminds.androsgi.builder/README.md) that facilitates starting an OSGi runtime within your Android application. In order install
OSGi bundles on your OSGi runtime in Android you need to make sure that your bundles contains the classes in the dex format. Use the 
[BND Android plugin](be.iminds.bnd.plugin.android/README.md) to build your application bundles with a .dex file included.

The androsgi projects are:

- Androsgi Builder

This is an Eclipse plugin that combines the Android Ecipse plugin and BNDTools to provide an easy development model for embedding an OSGi container inside your Android project. More information can be found [here](be.iminds.androsgi.builder/README.md).

- Logger

The logger project provides a simple implementation of the OSGi LogService which forwards all logging to the Android logging system.

- Util

The util project contains various utilities for facilitating embedding OSGi in an Android application and is used by the Androsgi Builder. For example, it provides the OSGiActivity class, which provides a simple Android Activity with an embedded OSGi runtime.

- BND Android plugin

This BND plugin will compile your OSGi bundle code into a classes.dex file inside the .jar that can be used by the Dalvik VM on Android. To configure and use this BND plugin, you need to have a correct Android SDK setup as explained [here](be.iminds.bnd.plugin.android/README.md).
