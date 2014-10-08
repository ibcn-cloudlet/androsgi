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
package be.iminds.bnd.plugin.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.StringTokenizer;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.AnalyzerPlugin;
import aQute.bnd.service.Plugin;
import aQute.service.reporter.Reporter;

/**
 * This BND AnalyzerPlugin will generate a .dex file from all .class files included
 * in the bundle using the Android dx tool (set location with dx property or have 
 * it somewhere on the PATH), and include the .dex in the .jar.
 * 
 * This allows the bundle to run on an OSGi runtime running on the Dalvik VM
 * 
 * @author tverbele
 *
 */
public class AndroidPlugin implements Plugin, AnalyzerPlugin {
	
	private String dx = null;
	
	@Override
	public boolean analyzeJar(Analyzer analyzer) throws Exception {
		try {
			// create tmp directory
			String TMP_PATH = analyzer.getBase().getAbsolutePath()+File.separator
					+"generated"+File.separator+"android"+File.separator
					+analyzer.getBsn()+File.separator;
			
			File f = new File(TMP_PATH);
			f.mkdirs();
			
			// get all .class files
			Jar jar = analyzer.getJar();
			if(jar==null)
				return false;
			
			Map<String, Resource> resources = jar.getResources();
			
			boolean containsClasses = false;
			for(String name : resources.keySet()){
				if(name.endsWith(".class")){
					containsClasses = true;
					InputStream is = null;
					FileOutputStream fos = null;
					try {
						is = resources.get(name).openInputStream();
						File dir = new File(TMP_PATH+name.substring(0,name.lastIndexOf(File.separator)));
						dir.mkdirs();
						fos = new FileOutputStream(new File(TMP_PATH+name));
						byte[] buffer = new byte[1024];
				        int length;
				        while ((length = is.read(buffer)) > 0) {
				            fos.write(buffer, 0, length);
				        }
					} finally {
				        is.close();
				        fos.close();
					}
				}
			}
			if(!containsClasses)
				return false;
			
			// create classes.dex using dx tool
			
			if(dx==null)
				findDX();
			if (dx==null)
				throw new Exception("dx not found, make sure dx location is configured and the Android SDK is correctly set up");
			
			Process p = Runtime.getRuntime().exec(dx+" --dex --output="+TMP_PATH+"classes.dex "+TMP_PATH);
			p.waitFor();
			// TODO parse dx output to spot errors
			
			// add classes.dex to the .jar 
			File dexFile = new File(TMP_PATH+"classes.dex");
			if(!dexFile.exists()){
				throw new Exception("Failed to create classes.dex, make sure compiler level is 1.6");
			}
			
			analyzer.addIncluded(dexFile);
			jar.putResource("classes.dex", new DexResource(dexFile));
		} catch(Exception e){
			analyzer.warning("Error embedding classes.dex file: " + e.getLocalizedMessage());
			return false;
		}
		
		return true;
	}

	
	private class DexResource implements Resource {

		final File dex;

		public DexResource(File dexFile){
			dex = dexFile;
		}
		
		@Override
		public void write(OutputStream out) throws Exception {
			InputStream is = null;
			try {
				is = new FileInputStream(dex);
				byte[] buffer = new byte[1024];
		        int length;
		        while ((length = is.read(buffer)) > 0) {
		            out.write(buffer, 0, length);
		        }
			} finally {
		        is.close();
			}
		}
		
		@Override
		public long size() throws Exception {
			return dex.length();
		}
		
		@Override
		public void setExtra(String extra) {
			
		}
		
		@Override
		public InputStream openInputStream() throws Exception {
			return new FileInputStream(dex);
		}
		
		@Override
		public long lastModified() {
			return dex.lastModified();
		}
		
		@Override
		public String getExtra() {
			return null;
		}
		
	}

	@Override
	public void setProperties(Map<String, String> map) {
		if(map.containsKey("dx"))
			dx = map.get("dx");
	}

	@Override
	public void setReporter(Reporter processor) {
		
	}
	
	private void findDX() throws Exception {
		try {
			if (Runtime.getRuntime().exec("dx --version").waitFor() == 0) { //same as exit code
				dx = "dx";
				return;
			}
		} catch (Exception ex) {}
			
		try {
			// TODO: is this still needed?
			String path = System.getenv("PATH");
			StringTokenizer st = new StringTokenizer(path,":");
			String dir = null;
			while(st.hasMoreElements()){
				dir = st.nextToken();
				File f = new File(dir+File.separator+"dx");
				if(f.exists()){
					dx = dir+File.separator+"dx";
					return;
				}
			}
		} catch (Exception ex) {}
	}
}
