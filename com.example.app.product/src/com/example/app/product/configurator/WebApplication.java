package com.example.app.product.configurator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Scanner;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.app.product.ProductPlugin;

public class WebApplication implements IApplication {

	
	// Copied from IDEApplication
	private static final String METADATA_FOLDER = ".metadata"; //$NON-NLS-1$
	private static final String VERSION_FILENAME = "version.ini"; //$NON-NLS-1$
	private static final String WORKSPACE_VERSION_KEY = "org.eclipse.core.runtime"; //$NON-NLS-1$
	private static final String WORKSPACE_VERSION_VALUE = "2"; //$NON-NLS-1$
	
	private IApplicationContext applicationContext;

	private String[] args = new String[]{};

	private Location instanceLocation;

	/**
	 * A special return code that will be recognized by the PDE launcher and
	 * used to show an error dialog if the workspace is locked.
	 */
	private static final Integer EXIT_WORKSPACE_LOCKED = new Integer(15);

	@Override
	public Object start(IApplicationContext applicationContext) throws Exception {
		this.applicationContext = applicationContext;

		args = (String[]) applicationContext.getArguments().get(IApplicationContext.APPLICATION_ARGS);
		
		ensureBundleStarted("org.ops4j.pax.logging.pax-logging-api");
		ensureBundleStarted("org.ops4j.pax.logging.pax-logging-service");
		ensureBundleStarted("org.eclipse.equinox.http.registry");

		
		try {
			
			this.instanceLocation = ProductPlugin.getDefault().getInstanceLocation();
			
			if (checkInstanceLocation()){
				return IApplicationContext.EXIT_ASYNC_RESULT;
			}


		} finally {
			if (instanceLocation != null)
				instanceLocation.release();
		}

		return EXIT_OK;
		
	}

	@Override
	public void stop() {

		try {
			
			ResourcesPlugin.getWorkspace().save(true, new NullProgressMonitor());
			
			if (instanceLocation!=null && instanceLocation.isSet() && instanceLocation.isLocked())
				instanceLocation.release();
		} catch (Exception e) {
			getLogger().error("Error occurred while releasing the instance lock");
		}
		if (this.applicationContext != null)
			this.applicationContext.setResult(EXIT_OK, this);
	}

	/**
	 * Simplified copy of IDEAplication processing that does not offer to choose
	 * a workspace location.
	 */
	private boolean checkInstanceLocation() {

		// Eclipse has been run with -data @none or -data @noDefault options so
		// we don't need to validate the location
		if (this.instanceLocation == null) {
			getLogger().error("IDEs need a valid workspace. Restart without the @none option.");
			return false;
		}

		// -data "/valid/path", workspace already set
		if (this.instanceLocation.isSet()) {
			// make sure the meta data version is compatible (or the user
			// has
			// chosen to overwrite it).
			if (!checkValidWorkspace()) {
				return false;
			}

			// at this point its valid, so try to lock it and update the
			// metadata version information if successful
			try {
				if (instanceLocation.lock()) {
					writeWorkspaceVersion();
					return true;
				}

				// we failed to create the directory.
				// Two possibilities:
				// 1. directory is already in use
				// 2. directory could not be created
				File workspaceDirectory = new File(instanceLocation.getURL().getFile());
				if (workspaceDirectory.exists()) {
					getLogger().error(String.format("Could not launch the product because the associated workspace at ''{0}'' is currently in use by another Eclipse application.",workspaceDirectory.getAbsolutePath()));
				} else {
					getLogger().error(String.format("Could not launch the product because the specified workspace cannot be created.  The specified workspace directory is either invalid or read-only."));
				}
			} catch (IOException e) {
				getLogger().error("Internal Error!",e);
			}
		}
		return false;
	}

	
	/**
	 * Return true if the argument directory is ok to use as a workspace and
	 * false otherwise. A version check will be performed, and a confirmation
	 * box may be displayed on the argument shell if an older version is
	 * detected.
	 *
	 * @return true if the argument URL is ok to use as a workspace and false
	 *         otherwise.
	 */
	private boolean checkValidWorkspace() {
		// a null url is not a valid workspace
		URL url = instanceLocation.getURL();
		if (url == null) {
			return false;
		}

		String version = readWorkspaceVersion();

		// if the version could not be read, then there is not any existing
		// workspace data to trample, e.g., perhaps its a new directory that
		// is just starting to be used as a workspace
		if (version == null) {
			return true;
		}

		final int ide_version = Integer.parseInt(WORKSPACE_VERSION_VALUE);
		int workspace_version = Integer.parseInt(version);

		// equality test is required since any version difference (newer
		// or older) may result in data being trampled
		if (workspace_version == ide_version) {
			return true;
		}

		// At this point workspace has been detected to be from a version
		// other than the current ide version -- find out if the user wants
		// to use it anyhow.
		
		String message;
	
		if(ide_version > workspace_version)
			message = String.format("Workspace %s was written with an older version of the product and will be updated."
					+ " Updating the workspace can make it incompatible with older versions of the product.",url.getFile());
		else
			message = String.format("Workspace %s was written with a newer version of the product and can be incompatible"
					+ " with this version. If you continue, this can cause unexpected behavior or data loss.",url.getFile());
		
		System.out.println(message);
		
		Scanner sc = new Scanner(System.in);
		System.out.println("\n\nAre you sure you want to continue with this workspace? (Y/N)\n");
		char c = sc.next().charAt(0);
		//sc.close();
		return (c=='Y' || c=='y')? Boolean.TRUE: Boolean.FALSE;
	
	}
	
	/**
	 * Look at the argument URL for the workspace's version information. Return
	 * that version if found and null otherwise.
	 */
	private String readWorkspaceVersion() {
		
		File versionFile = getVersionFile(false);
		if (versionFile == null || !versionFile.exists()) {
			return null;
		}
		
		// Although the version file is not spec'ed to be a Java properties
		// file, it happens to follow the same format currently, so using
		// Properties to read it is convenient.
		Properties props = new Properties();

		try(FileInputStream is = new FileInputStream(versionFile);) {
			props.load(is);
			return props.getProperty(WORKSPACE_VERSION_KEY);
		} catch (IOException e) {
			getLogger().error("Error occurred while readiing the version file", e);
			return null;
		}
	}
	
	/**
	 * Write the version of the metadata into a known file overwriting any
	 * existing file contents. Writing the version file isn't really crucial, so
	 * the function is silent about failure
	 */
	private  void writeWorkspaceVersion() {
		
		if (instanceLocation == null || instanceLocation.isReadOnly()) {
			return;
		}
		
		File versionFile = getVersionFile(true);
		if (versionFile == null) {
			return;
		}

		String versionLine = WORKSPACE_VERSION_KEY + '=' + WORKSPACE_VERSION_VALUE;
		
		try(OutputStream os = new FileOutputStream(versionFile)) {
			os.write(versionLine.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			getLogger().error("Error occurred while writing the worskapce versiong file", e);
		}
	}


	/**
	 * The version file is stored in the metadata area of the workspace. This
	 * method returns an URL to the file or null if the directory or file does
	 * not exist (and the create parameter is false).
	 *
	 * @param create
	 *            If the directory and file does not exist this parameter
	 *            controls whether it will be created.
	 * @return An url to the file or null if the version file does not exist or
	 *         could not be created.
	 */
	private  File getVersionFile(boolean create) {
		
		URL workspaceUrl = instanceLocation.getURL();
		
		if (workspaceUrl == null) {
			return null;
		}

		try {
			// make sure the directory exists
			File metaDir = new File(workspaceUrl.getPath(), METADATA_FOLDER);
			if (!metaDir.exists() && (!create || !metaDir.mkdir())) {
				return null;
			}

			// make sure the file exists
			File versionFile = new File(metaDir, VERSION_FILENAME);
			if (!versionFile.exists() && (!create || !versionFile.createNewFile())) {
				return null;
			}

			return versionFile;
		} catch (IOException e) {
			
			return null;
		}
	}
	
	private Object checkInstanceLocation1() {
		Location instanceLoc = ProductPlugin.getDefault().getInstanceLocation();
		// -data must be specified
		Logger logger = getLogger();

		if (instanceLoc == null || !instanceLoc.isSet()) {
			logger.error("Instance location must be set"); //$NON-NLS-1$
			return EXIT_OK;
		}

		// at this point its valid, so try to lock it
		try {
			if (instanceLoc.lock()) {
				logger.info("Workspace location locked successfully: " + instanceLoc.getURL()); //$NON-NLS-1$
				return null;
			}
		} catch (IOException e) {
			logger.error("Workspace location could not be locked: " + instanceLoc.getURL()); //$NON-NLS-1$
		}

		// we failed to create the directory.
		// Two possibilities:
		// 1. directory is already in use
		// 2. directory could not be created
		File workspaceDirectory = new File(instanceLoc.getURL().getFile());
		if (workspaceDirectory.exists()) {
			logger.error("The workspace location is already in use by another server instance: " + workspaceDirectory); //$NON-NLS-1$
			return EXIT_WORKSPACE_LOCKED;
		}
		logger.error("Workspace location could not be created: " + workspaceDirectory); //$NON-NLS-1$
		return EXIT_OK;
	}

	private Logger getLogger() {
		return LoggerFactory.getLogger(WebApplication.class);
	}

	private void ensureBundleStarted(String symbolicName) throws BundleException {
		Bundle bundle = ProductPlugin.getDefault().getBundle(symbolicName);

		if (bundle == null)
			return;

		int bundleState = bundle.getState();
			
		
		if (bundleState == Bundle.INSTALLED 
				|| bundleState == Bundle.RESOLVED 
				|| bundle.getState() == Bundle.STARTING) {
			bundle.start(Bundle.START_TRANSIENT);
		}
	}

}
