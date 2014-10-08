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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.AssetManager;

/*
 * Helper class for running OSGi on Android. This singleton
 * contains reference to the unique OSGi runtime embedded
 * in this project. One can access the OSGi runtime and the
 * available services using the BundleContext of the system
 * bundle
 */
public class OSGiRuntime {

	private static OSGiRuntime instance = null;
	
	private Framework framework = null;
	
	private OnSharedPreferenceChangeListener listener = null;
	
	public static OSGiRuntime getInstance(){
		if(instance==null){
			synchronized(OSGiRuntime.class){
				if(instance==null){
					instance = new OSGiRuntime();
				}
			}
		}
		return instance;
	}
	
	private OSGiRuntime(){
		
	}
	
	public synchronized void init(final Properties properties, final AssetManager assetManager, final SharedPreferences prefs) throws Exception {
		if(framework==null){
			initFramework(properties);
			
			// start bundles in separate thread
			new Thread(){
				public void run(){
					try {
						initBundles(properties, assetManager, prefs);
					} catch (Exception e) {
						e.printStackTrace();
					}	
				}
			}.start();
			
			listener = new OnSharedPreferenceChangeListener() {
				
				@Override
				public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
					if(prefs.getBoolean(key, true)){
						try {
							startBundle(key, assetManager);
						} catch(Exception e){
							e.printStackTrace();
						}
					} else {
						try {
							stopBundle(key);
						} catch(Exception e){
							e.printStackTrace();
						}
					}
					
				}
			};
			prefs.registerOnSharedPreferenceChangeListener(listener);
		}
	}
	
	private void initFramework(Properties properties) throws Exception{
		String factoryClass = properties.getProperty(FrameworkFactory.class.getName());
		if(factoryClass == null)
			throw new Exception("No FrameworkFactory available!");
		
		FrameworkFactory frameworkFactory = (FrameworkFactory) Class.forName(factoryClass).newInstance();
		
		Map<String, String> config = new HashMap<String, String>();
		String runproperties = properties.getProperty("-runproperties");
		if(runproperties!=null){
			StringTokenizer st = new StringTokenizer(runproperties, ",");
			while(st.hasMoreTokens()){
				String runproperty = st.nextToken();
				int equalsIndex = runproperty.indexOf('=');
				if(equalsIndex!=-1){
					String key = runproperty.substring(0, equalsIndex);
					String value = runproperty.substring(equalsIndex+1);
					config.put(key, value);
				}
			}
		}
		
		// point storage dir to internal storage
		config.put("org.osgi.framework.storage", (String)properties.getProperty("cacheDir"));
		// add framework exports
		config.put("org.osgi.framework.system.packages.extra", (String)properties.get("-runsystempackages"));
		framework = frameworkFactory.newFramework(config);
		framework.start();
	}
	
	private void initBundles(Properties properties, AssetManager assetMgr, SharedPreferences prefs) throws Exception {
		String bundles = properties.getProperty("-runbundles");
		
		StringTokenizer st = new StringTokenizer(bundles, ",;");
		while(st.hasMoreElements()){
			String bundle = st.nextToken();
			
			// for now ignore versions
			if(bundle.startsWith("version") || bundle.startsWith("uses") || bundle.endsWith("'"))
				continue;

			if(prefs.getBoolean(bundle, true)){
				startBundle(bundle, assetMgr);
			}
		}
	}
	
	public void startBundle(String bundle, AssetManager assetMgr) throws Exception {
		String[] assets = new String[0];
		try {
			assets = assetMgr.list("bundles");
		}catch(Exception e){
			// should not happen!
			throw new Exception("Not bundles asset directory available!");
		}
		
		String bundleLocation = null;
		for(String asset : assets){
			if(asset.startsWith(bundle)){
				bundleLocation = "bundles/"+asset;
				break;
			}
		}
		
		System.out.println("Starting bundle "+bundle+" from location "+bundleLocation);

		if(bundleLocation!=null){
			try {
				InputStream is = assetMgr.open(bundleLocation);
				org.osgi.framework.Bundle b = framework.getBundleContext().installBundle(bundle, is);
				b.start();
				
				System.out.println("Started bundle "+b.getSymbolicName());
			} catch(Exception e){
				e.printStackTrace();
				throw new Exception("Error starting bundle "+bundle, e);
			}
		} else {
			throw new Exception("Bundle "+bundle + " not found!");
		}
	}
	
	public void stopBundle(String bundle){
		System.out.println("Stopping bundle "+bundle);
		for(Bundle b : framework.getBundleContext().getBundles()){
			if(bundle.equals(b.getSymbolicName())){
				try {
					b.stop();
				} catch (BundleException e) {
				}
			}
		}
	}
	
	public synchronized void stop(){
		if(framework!=null){
			try {
				framework.waitForStop(10000);
			} catch(Exception e){
				e.printStackTrace();
			}
			framework = null;
		}
	}
	
	public BundleContext getBundleContext(){
		if(framework!=null)
			return framework.getBundleContext();
		return null;
	}
}
