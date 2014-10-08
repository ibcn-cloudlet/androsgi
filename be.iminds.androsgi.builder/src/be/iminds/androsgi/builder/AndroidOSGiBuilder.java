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
package be.iminds.androsgi.builder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;

public class AndroidOSGiBuilder extends IncrementalProjectBuilder {

	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) {
		if (kind == IncrementalProjectBuilder.FULL_BUILD) {
			build(monitor);
		} else {
			IResourceDelta delta = getDelta(getProject());
			if (delta == null) {
				build(monitor);
			} else {
				incrementalBuild(delta, monitor);
			}
		}
		return null;
	}

	protected void clean(IProgressMonitor monitor){
		File workspaceRoot = this.getProject().getWorkspace().getRoot().getLocation().toFile();
		File projectDir = this.getProject().getFullPath().toFile();

		// remove libs (TODO this will also remove manually added libs - therefore, libs should only be added using bnd.bnd)
		File libsDir = new File(workspaceRoot.getAbsolutePath()+File.separator
				+projectDir.getAbsolutePath()+File.separator
				+"libs");
		if(libsDir.exists()){
			for(File f : libsDir.listFiles()){
				// TODO which libs should be deleted?
				if(!f.getName().startsWith("android-support-")){ // ignore support libs
					f.delete();
				}
			}
		}
		
		// clean assets/bundles
		File bundlesDir = new File(workspaceRoot.getAbsolutePath()+File.separator
				+projectDir.getAbsolutePath()+File.separator
				+"assets"+File.separator
				+"bundles");
		if(bundlesDir.exists()){
			for(File f : bundlesDir.listFiles()){
				f.delete();
			}
		}
	
		
		// remove assets/bundles.bndrun
		String runFile = workspaceRoot.getAbsolutePath()+File.separator
				+projectDir.getAbsolutePath()+File.separator
				+"assets"+File.separator
				+"bundles.bndrun";
		File f = new File(runFile);
		if(f.exists()){
			f.delete();
		}
		
		// clean markers
		try {
			IMarker[] markers = this.getProject().findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
			for(IMarker marker : markers){
				if(marker.getAttribute("androsgi", false))
					marker.delete();
			}
		} catch(Exception e){}
	}
	
	private void incrementalBuild(IResourceDelta delta, final IProgressMonitor monitor) {
		try {
			delta.accept(new IResourceDeltaVisitor() {
				public boolean visit(IResourceDelta delta) {
					
					if(delta.getFullPath().toString().startsWith(delta.getResource().getProject().getFullPath()+File.separator+"assets")){
						return true;
					}
					
					if(delta.getResource().getFileExtension()!=null){
						String ext = delta.getResource().getFileExtension().toString();
						if(ext.equals("bnd")||ext.equals("bndrun")){
							clean(monitor);
							build(monitor);
						}
					}
					return true; // visit children too
				}
			});
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	private void build(IProgressMonitor monitor) {
		Workspace ws = null;
		Project project = null;
		Project runproject = null;
		try {
			File workspaceRoot = this.getProject().getWorkspace().getRoot().getLocation().toFile();
			ws = new Workspace(workspaceRoot);
		
			String projectName = this.getProject().getName();
			project = ws.getProject(projectName);
			
			File projectDir = this.getProject().getFullPath().toFile();
			File bndrun = this.getProject().getFile("osgi.bndrun").getLocation().toFile();
			
			runproject = new Project(ws, projectDir, bndrun);
			
			// copy all run bundles to assets/bundles
			for(Container c : runproject.getRunbundles()){
				String from = c.getFile().getAbsolutePath();
				String to = workspaceRoot.getAbsolutePath()
						+projectDir.getAbsolutePath()+File.separator
						+"assets"+File.separator
						+"bundles"+File.separator
						+c.getFile().getName();
				copy(from, to, true);
			}
			
			// copy api bundles that are required (on build path of bnd.bnd) to libs folder (without .dex now)
			for(Container c : project.getBuildpath()){
				if(!c.getFile().isDirectory() && !c.getBundleSymbolicName().startsWith("osgi.core")){ // ignore /bin path and osgi.core lib
					
					String from = c.getFile().getAbsolutePath();
					String to = workspaceRoot.getAbsolutePath()
							+projectDir.getAbsolutePath()+File.separator
							+"libs"+File.separator
							+c.getFile().getName();
					copy(from, to, false);
				}
			}
			
			// copy runconfig to assets/bundles.bndrun
			String runFile = workspaceRoot.getAbsolutePath()+File.separator
					+projectDir.getAbsolutePath()+File.separator
					+"assets"+File.separator
					+"bundles.bndrun";
			Properties runproperties = runproject.getProperties();
			String runsystempackages = runproperties.getProperty("-runsystempackages");
			// add android and api packages to the runsystempackages
			for(Container c : project.getBuildpath()){
				if(!c.getFile().isDirectory()){
					Manifest manifest = c.getManifest();
					Attributes attr = manifest.getMainAttributes();
					String exports = attr.getValue("Export-Package");
					exports = exports.replaceAll("uses:=\".*?\";", "");
					runsystempackages+=","+exports;
				}
			}
			runsystempackages+=","+ANDROID_FRAMEWORK_PACKAGES;
			runproperties.put("-runsystempackages", runsystempackages);
			
			
			// fetch osgi runtime implementation
			for(String runpath : runproject.getProjectLauncher().getRunpath()){
				String framework = runpath.substring(runpath.lastIndexOf("/")+1);
				if(!runpath.contains("launcher")){ // ignore bnd launcher jar
					String to = workspaceRoot.getAbsolutePath()
							+projectDir.getAbsolutePath()+File.separator
							+"libs"+File.separator
							+framework;
					copy(runpath, to, false);
					
					// store META-INF/services/org.osgi.framework.launch.FrameworkFactory in properties
					JarFile jar = new JarFile(to);
					ZipEntry entry = jar.getEntry("META-INF/services/org.osgi.framework.launch.FrameworkFactory");
					BufferedReader reader = new BufferedReader(new InputStreamReader(jar.getInputStream(entry)));
					String launchFactory = reader.readLine();
					runproperties.put("org.osgi.framework.launch.FrameworkFactory", launchFactory);
					jar.close();
				}
			}
			
			runproperties.store(new FileOutputStream(new File(runFile)), "bndrun configuration - generated by OSGi Android builder");
			
		} catch(Exception e){
			e.printStackTrace();
		} finally {
			try {
				this.getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
			} catch(Exception e){}
			
			ws.close();
			project.close();
			runproject.close();
		}
	}
	
	private void copy(String from, String to, boolean includeDex){
		System.out.println("COPY "+from+" to "+to);
		
		JarOutputStream out = null;
		JarFile jar = null;
		try {
			File dest = new File(to);
			dest.getParentFile().mkdirs();
			
			out = new JarOutputStream(new FileOutputStream(dest));
			jar = new JarFile(new File(from));
			
			boolean dexIncluded = false;
			
			Enumeration<JarEntry> entries = jar.entries();
			while(entries.hasMoreElements()){
				JarEntry entry = entries.nextElement();
				
				if(entry.getName().endsWith(".dex")){
					if(!includeDex){
						continue;
					} else {
						dexIncluded = true;
					}
				}
				
				out.putNextEntry(new JarEntry(entry.getName()));
				
				InputStream is = null;
				try {
					is = jar.getInputStream(entry);
					byte[] buffer = new byte[1024];
				    int length;
				    while ((length = is.read(buffer)) > 0) {
				    	out.write(buffer, 0, length);
				    }
				    out.flush();
				} catch(IOException e){
					throw e;
				} finally {
					is.close();
				}
			}

			if(includeDex && !dexIncluded){
				try {
					IMarker marker = this.getProject().createMarker(IMarker.PROBLEM);
					marker.setAttribute(IMarker.MESSAGE, "No classes.dex file included in "+to);
					marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
					marker.setAttribute("androsgi", true);
				} catch(Exception e){}
			}
		} catch(IOException e){
			try {
				IMarker marker = this.getProject().createMarker(IMarker.PROBLEM);
				marker.setAttribute(IMarker.MESSAGE, "Error copying file "+from+" to "+to);
				marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
				marker.setAttribute("androsgi", true);
			} catch(Exception ex){}
		} finally {
			try {
				out.flush();
				out.close();
				jar.close();
			} catch(IOException e){}
		}
	}
	
	
	private static final String ANDROID_FRAMEWORK_PACKAGES = (
            // ANDROID
            "android, " + 
            "android.app," + 
            "android.content," + 
            "android.content.res," +
            "android.content.pm," + 
            "android.database," + 
            "android.database.sqlite," + 
            "android.graphics, " + 
            "android.graphics.drawable, " + 
            "android.graphics.drawable.shapes, " + 
            "android.graphics.glutils, " + 
            "android.hardware, " + 
            "android.location, " + 
            "android.media, " + 
            "android.net, " + 
            "android.opengl, " + 
            "android.os, " + 
            "android.provider, " + 
            "android.sax, " + 
            "android.speech.recognition, " + 
            "android.telephony, " + 
            "android.telephony.gsm, " + 
            "android.text, " + 
            "android.text.method, " + 
            "android.text.style, " + 
            "android.text.util, " + 
            "android.util, " + 
            "android.view, " + 
            "android.view.animation, " + 
            "android.webkit, " + 
            "android.widget, " + 
            //MAPS
            "com.google.android.maps, " + 
            "com.google.android.xmppService, " + 
            // JAVAx
            "javax.crypto, " + 
            "javax.crypto.interfaces, " + 
            "javax.crypto.spec, " + 
            "javax.microedition.khronos.opengles, " + 
            "javax.microedition.khronos.egl, " + 
            "javax.net, " + 
            "javax.net.ssl, " + 
            "javax.security.auth, " + 
            "javax.security.auth.callback, " + 
            "javax.security.auth.login, " + 
            "javax.security.auth.x500, " + 
            "javax.security.cert, " + 
            "javax.sound.midi, " + 
            "javax.sound.midi.spi, " + 
            "javax.sound.sampled, " + 
            "javax.sound.sampled.spi, " + 
            "javax.sql, " + 
            "javax.xml.parsers"
			).intern(); 
}
