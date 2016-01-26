package com.example.helloworld;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class HelloBundleActivator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		HelloBundleActivator.context = bundleContext;
		try{
			doSomethingNaughty();
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	private void doSomethingNaughty() throws Exception {
		
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		
		System.err.println(workspace);
		IWorkspaceRoot root = workspace.getRoot();
		
		
		IProject[] projects = root.getProjects();
		
		for (IProject iProject : projects) {
			System.out.println(iProject.getName());
		}
		
		IProgressMonitor pm = new NullProgressMonitor();
		
		IProject project = root.getProject("Sample");
		
		
		
		if(!project.exists()) {
			IProjectDescription description = workspace.newProjectDescription("Sample");
			project.create(description, pm);
		}
		
		if(!project.isOpen())
			project.open(pm);
		
		IFile file = project.getFile("Sample.txt");
		
		if(!file.exists()){
			file.create(new ByteArrayInputStream("hello world!\n".getBytes()), true, pm);
		}
		
		
		try(BufferedReader br = new BufferedReader(new InputStreamReader(file.getContents()))){
			
			String line;
			
			while((line=br.readLine())!=null)
				System.out.println(line);
			
		}catch(Exception  e){
			throw e;
		}
		
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		HelloBundleActivator.context = null;
	}

}
