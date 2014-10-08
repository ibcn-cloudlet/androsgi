/*
 * Copyright (c) 2014, Tim Verbelen
 * Internet Based Communication Networks and Services research group (IBCN),
 * Department of Information Technology (INTEC), Ghent University - iMinds.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    - Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 *    - Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    - Neither the name of Ghent University - iMinds, nor the names of its 
 *      contributors may be used to endorse or promote products derived from 
 *      this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */
package be.iminds.androsgi.util;

import java.io.File;
import java.util.Properties;

import org.osgi.framework.BundleContext;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.view.Surface;
import android.view.WindowManager;

public class OSGiHelper {

	public static OSGiRuntime bootOSGi(android.content.Context androidContext) {
		
		// initialize OSGi
		OSGiRuntime runtime = OSGiRuntime.getInstance();
		if(runtime.getBundleContext()==null){
			try {
				// Open properties from assets
				Properties properties = new Properties();
				AssetManager assetMgr = androidContext.getAssets();
				properties.load(assetMgr.open("bundles.bndrun"));

				// Clean up previous cache dir (required when developing...)
				File cache = androidContext.getDir("cache", 0);
				if(cache.exists()){
					delete(cache);
				}
				// Set cache dir property
				properties.put("cacheDir", cache.getAbsolutePath());
				
				// Add some Android related properties to -runproperties
				// e.g. default device orientation
				// TODO add other relevant device properties here?
				String runproperties = properties.getProperty("-runproperties");
				String orientationProperty = "android.device.orientation="+getDeviceDefaultOrientation(androidContext);
				if(runproperties==null){
					properties.put("-runproperties", orientationProperty);
				} else {
					properties.put("-runproperties", runproperties+","+orientationProperty);
				}
				
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(androidContext);
				
				// Init
				runtime.init(properties, assetMgr, prefs);
				
				// Register system services
				registerSystemServices(runtime.getBundleContext(), androidContext);
				
			} catch(Exception e){
				showErrorDialog(e, androidContext);
			}
		}
		return runtime;
	}
	
	private static int getDeviceDefaultOrientation(android.content.Context androidContext) {

	    WindowManager windowManager =  (WindowManager) androidContext.getSystemService(android.content.Context.WINDOW_SERVICE);

	    Configuration config = androidContext.getResources().getConfiguration();

	    int rotation = windowManager.getDefaultDisplay().getRotation();

	    if ( ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) &&
	            config.orientation == Configuration.ORIENTATION_LANDSCAPE)
	        || ((rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) &&    
	            config.orientation == Configuration.ORIENTATION_PORTRAIT)) {
	      return Configuration.ORIENTATION_LANDSCAPE;
	    }
	    else {
	      return Configuration.ORIENTATION_PORTRAIT;
	    }
	}
	
	private static void delete(File target) {
    	if (target.isDirectory()) {
    		for (File file : target.listFiles()) {
    			delete(file);
    		}
    	}
    	target.delete();
    }
	
	private static void showErrorDialog(Exception e, android.content.Context androidContext){
		// show a dialog
		AlertDialog alertDialog = new AlertDialog.Builder(
                androidContext).create();
		alertDialog.setTitle("Error starting application");
		alertDialog.setMessage("Failed to start the OSGi runtime \n"
				+e.getMessage());
	
		alertDialog.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				System.exit(-1);
			}
		});
		alertDialog.show();
	}
	
	// register some system services (TODO cover them all?)
	private static void registerSystemServices(BundleContext context, android.content.Context androidContext){
		context.registerService(ConnectivityManager.class.getName(), androidContext.getSystemService(Context.CONNECTIVITY_SERVICE), null);
		context.registerService(LocationManager.class.getName(), androidContext.getSystemService(Context.LOCATION_SERVICE), null);
		context.registerService(SensorManager.class.getName(), androidContext.getSystemService(Context.SENSOR_SERVICE), null);
		context.registerService(WifiManager.class.getName(), androidContext.getSystemService(Context.WIFI_SERVICE), null);
		
	
		context.registerService(AssetManager.class.getName(), androidContext.getAssets(), null);
		
		// also register android context as service, allows you to interact with android
		// as if from within the activity itself - TODO is this safe to do?
		context.registerService("android.content.Context", androidContext, null);
	}
}
