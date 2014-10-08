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

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Properties;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public class AndrosgiConvert implements IObjectActionDelegate {

    private ISelection selection;

    public void run(IAction action) {
        if (selection instanceof IStructuredSelection) {
            for (Iterator< ? > it = ((IStructuredSelection) selection).iterator(); it.hasNext();) {
                Object element = it.next();
                IProject project = null;
                if (element instanceof IProject) {
                    project = (IProject) element;
                } else if (element instanceof IAdaptable) {
                    project = (IProject) ((IAdaptable) element).getAdapter(IProject.class);
                }
                if (project != null) {
                    convert(project);
                }
            }
        }
    }
  
    public void selectionChanged(IAction action, ISelection selection) {
        this.selection = selection;
    }

    public void setActivePart(IAction action, IWorkbenchPart targetPart) {}

    private static void convert(IProject project) {
        try {
        	// Add the bnd nature
            IProjectDescription description = project.getDescription();
            String[] natures = description.getNatureIds();
            
            String[] newNatures = new String[natures.length + 1];
            System.arraycopy(natures, 0, newNatures, 0, natures.length);
            newNatures[natures.length] = "bndtools.core.bndnature";
            description.setNatureIds(newNatures);
            project.setDescription(description, null);
            
            // Add bnd.bnd file
            IFile bnd = project.getFile("bnd.bnd");
            File bndFile = bnd.getLocation().toFile();
            Properties bndProperties = new Properties();
            bndProperties.put("-buildpath", "osgi.core,be.iminds.androsgi.util");
            bndProperties.store(new PrintWriter(bndFile), "bnd.bnd - specify the project build path here");
            
            // Add osgi.bndrun file
            IFile run = project.getFile("osgi.bndrun");
            File runFile = run.getLocation().toFile();
            Properties runProperties = new Properties();
            runProperties.put("-runfw", "org.eclipse.concierge");
            runProperties.put("-runee", "OSGi/Minimum-1.2");
            runProperties.put("-runbundles", "");
            runProperties.put("-runsystempackages", "");
            
            runProperties.store(new PrintWriter(runFile), "osgi.bndrun - specify the osgi run configuration");
            
            // Add repository
            File workspaceRoot = project.getWorkspace().getRoot().getLocation().toFile();
			Properties repoProperties = new Properties();
			File repoFile = new File(workspaceRoot.getAbsolutePath()+File.separator+"cnf"+File.separator+"ext"+File.separator+"repositories.bnd");
			repoProperties.load(new FileInputStream(repoFile));
			
            String repositories = repoProperties.getProperty("-plugin");
            if(!repositories.contains("Androsgi")){
            	repositories += ",aQute.bnd.deployer.repository.FixedIndexedRepo; name=Androsgi; locations=http://users.ugent.be/~tverbele/repositories/androsgi/index.xml";
            }
            if(!repositories.contains("Concierge")){
            	repositories += ",aQute.bnd.deployer.repository.FixedIndexedRepo; name=Concierge; locations=http://users.ugent.be/~tverbele/repositories/concierge/index.xml";
            }          
            
            repoProperties.put("-plugin", repositories);
            repoProperties.store(new PrintWriter(repoFile), null);
            
            // Add builder
            ICommand[] buildCommands = description.getBuildSpec();
            ICommand[] newCommands = new ICommand[buildCommands.length + 1];
            System.arraycopy(buildCommands, 0, newCommands, 1, buildCommands.length);

            ICommand androsgiBuild = description.newCommand();
            androsgiBuild.setBuilderName("be.iminds.androsgi.builder.AndroidOSGiBuilder");
            newCommands[0] = androsgiBuild;
            description.setBuildSpec(newCommands);
            project.setDescription(description, null);
            
            // add .gitignore to assets dir
            IFile assetsIgnore = project.getFile("assets/.gitignore");
            File assetsIgnoreFile = assetsIgnore.getLocation().toFile();
            PrintWriter writer = new PrintWriter(assetsIgnoreFile);
            writer.println("/bundles/");
            writer.println("bundles.bndrun");
            writer.close();
            
            // add .gitignore to root (by default ignore bin and lib)
            IFile projectIgnore = project.getFile(".gitignore");
            File projectIgnoreFile = projectIgnore.getLocation().toFile();
            writer = new PrintWriter(projectIgnoreFile);
            writer.println("/bin/");
            writer.println("/generated/");
            writer.println("/gen/");
            writer.println("/libs/");
            writer.close();
            
            // Clean and build
            project.build(IncrementalProjectBuilder.CLEAN_BUILD, null);
            
        } catch (Exception e) {
        	e.printStackTrace();
        	
        }
    }



}
