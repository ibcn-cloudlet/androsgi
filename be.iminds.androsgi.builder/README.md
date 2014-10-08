Androsgi: making Android and OSGi easy
======================================

This Eclipse plugin combines the Android ADT plugin with the BNDtools plugin for OSGi development
in order to enable easy development of Android applications using OSGi and OSGi bundles.

Installation
------------

The Androsgi plugin can be installed from the following update site:

http://aiolos.intec.ugent.be/plugin

Usage
-----

To create an Androsgi enabled project, first create a regular Android project using the Android ADT plugin.
To convert this project to enable OSGi, right click on the project, and select Configure > Convert to Androsgi Project.
This will add two new files to the project root: bnd.bnd and osgi.bndrun. 

The bnd.bnd file is used to define all libraries that are required on the build path. For example if you want
to call an OSGi service from within the Android code, the interface of this service has to be on the build path,
so the API bundle of the service has to be added here to the build path.

The osgi.bndrun file specifies the run configuration of the OSGi runtime. This specifies which OSGi framework
should be used, and which bundles should be started up within the Android application. 

To access the OSGi runtime from within the Android code, some helper classes are provided. To initialize and 
access the OSGi framework, use the OSGiActivity helper class. Instead of extending the Activity class when creating 
and Android activity, use the OSGiActivity class instead. This class will correctly initialize the OSGi runtime
based on the osgi.bndrun file provided. It also has a BundleContext context member which provides access to the
OSGi framework. Likewise, an OSGiService class is provided to be used to create an OSGi enabled Android Service.
If you don't want to extend one of those classes, you can also manually initialize the OSGi runtime using 
OSGiHelper.bootOSGi(android.content.Context context) method. 

To have access to the OSGi runtime outside of the Activity/Service classes, you can pass the BundleContext from
there, or use the OSGiRuntime class. The OSGiRuntime singleton class provides access to the OSGi BundleContext 
using OSGiRuntime.getRuntime().getBundleContext();

Dex bytecodes
-------------

Because Android uses the Dalvik VM, the bundles stated in osgi.bndrun have to contain .dex bytecodes. Therefore,
instead of .class files, Android expects a classes.dex file inside the bundle .jars. You can create the required
classes.dex file from existing jars using the dx tool which is provided with the Android SDK. To build your own 
OSGi bundles with Android support using BND, you can also use the be.iminds.bnd.plugin.android plugin, which is 
available at http://aiolos.intec.ugent.be/repositories/androsgi/be.iminds.bnd.plugin.android/be.iminds.bnd.plugin.android.jar

Caveats
-------

Android does not play well with multiple definitions of the same class. Therefore, make sure that (the same version of) 
a class is only exported once in the system, by cleanly separating API from implementation, and not including the  
same class in multiple bundles.
