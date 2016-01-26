package com.example.app.product;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The activator class controls the plug-in life cycle
 */
public class ProductPlugin extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.example.app.product"; //$NON-NLS-1$

	// The shared instance
	private static ProductPlugin plugin;

	private BundleContext context;

	private ServiceTracker<Location, Location> instanceLocationTracker;
	
	/**
	 * The constructor
	 */
	public ProductPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		this.context = context;
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		this.context = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static ProductPlugin getDefault() {
		return plugin;
	}
	
	public BundleContext getContext(){
		return this.context;
	}
	

	public Location getInstanceLocation() {
		if (instanceLocationTracker == null) {
			Filter filter;
			try {
				 filter = this.context.createFilter(Location.INSTANCE_FILTER);
			} catch (InvalidSyntaxException e) {
				e.printStackTrace();
				return null;
			}
			 instanceLocationTracker = new ServiceTracker<Location, Location>(this.context, filter, null);
			instanceLocationTracker.open();
		}
		return instanceLocationTracker.getService();
	}

	public Bundle getBundle(String symbolicName) {
		return this.context.getBundle(symbolicName);
	}
	
}
