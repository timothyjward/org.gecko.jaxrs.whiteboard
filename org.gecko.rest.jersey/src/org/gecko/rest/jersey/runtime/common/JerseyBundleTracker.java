/**
 * Copyright (c) 2012 - 2022 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.gecko.rest.jersey.runtime.common;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gecko.rest.jersey.provider.JerseyConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.condition.Condition;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

import aQute.bnd.annotation.service.ServiceCapability;

/**
 * Checker that ensures that all Jersey bundles are started properly to make the Jersey whiteboard work
 * @author Mark Hoffmann
 * @since 11.10.2022
 */
@ServiceCapability(value = Condition.class)
public class JerseyBundleTracker implements BundleTrackerCustomizer<Boolean>{

	private static final Logger logger = Logger.getLogger("runtime.check");
	private final Map<String, Boolean> bsns =new HashMap<>(5);
	private ServiceRegistration<Condition> jerseyRuntimeCondition;
	private BundleContext context;
	private final BundleTracker<Boolean> tracker;

	/**
	 * Creates a new instance.
	 * @param context the {@link BundleContext}
	 */
	public JerseyBundleTracker(BundleContext context) {
		this(context, false);
	}

	/**
	 * Creates a new instance.
	 * @param context the {@link BundleContext}
	 * @param isClientOnly indicator that marks a client only mode 
	 */
	public JerseyBundleTracker(BundleContext context, boolean isClientOnly) {
		this.context = context;
		bsns.put("org.glassfish.hk2.osgi-resource-locator", Boolean.FALSE);
		bsns.put("org.glassfish.jersey.inject.jersey-hk2", Boolean.FALSE);
		bsns.put("org.glassfish.jersey.core.jersey-common", Boolean.FALSE);
		bsns.put("org.glassfish.jersey.core.jersey-client", Boolean.FALSE);
		if (isClientOnly) {
			bsns.put("org.glassfish.jersey.core.jersey-server", Boolean.FALSE);
		}
		startBundles();
		tracker = new BundleTracker<Boolean>(context, Bundle.ACTIVE, this);
	}
	
	/**
	 * Opens the tracker
	 */
	public void open() {
		tracker.open();
	}
	
	/**
	 * Close the tracker
	 */
	public void close() {
		tracker.close();
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.osgi.util.tracker.BundleTrackerCustomizer#addingBundle(org.osgi.framework.Bundle, org.osgi.framework.BundleEvent)
	 */
	@Override
	public Boolean addingBundle(Bundle bundle, BundleEvent event) {
		String bsn = bundle.getSymbolicName();
		if (bsns.containsKey(bsn)) {
			bsns.put(bsn, Boolean.TRUE);
			updateCondition();
			return true;
		}
		return false;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.util.tracker.BundleTrackerCustomizer#modifiedBundle(org.osgi.framework.Bundle, org.osgi.framework.BundleEvent, java.lang.Object)
	 */
	@Override
	public void modifiedBundle(Bundle bundle, BundleEvent event, Boolean object) {
		// No modification can happen
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.util.tracker.BundleTrackerCustomizer#removedBundle(org.osgi.framework.Bundle, org.osgi.framework.BundleEvent, java.lang.Object)
	 */
	@Override
	public void removedBundle(Bundle bundle, BundleEvent event, Boolean object) {
		String bsn = bundle.getSymbolicName();
		if (bsns.containsKey(bsn)) {
			bsns.put(bsn, Boolean.FALSE);
			logger.log(Level.WARNING, ()->"Removed Jersey bundle: " + bsn);
			updateCondition();
		}
	}
	
	/**
	 * Starts all defined bundles that are necessary to get Jersey running properly
	 */
	private void startBundles() {
		for (Bundle bundle : context.getBundles()) {
			String bsn = bundle.getSymbolicName();
			if (bsns.containsKey(bsn)) {
				try {
					bundle.start();
				} catch (BundleException e) {
					logger.log(Level.WARNING, e, ()->"Cannot start Jersey bundle: " + bsn);
				}
			}
		}
	}

	/**
	 * Updates the Jersey Runtime Condition. It will be removed, if not all needed bundles are started.
	 * It will be registered, if all needed bundles are started 
	 */
	private synchronized void updateCondition() {
		if (!bsns.containsValue(Boolean.FALSE)) {
			if (jerseyRuntimeCondition != null) {
				logger.info(()->"Jersey runtime condition is already registered! This should not happen! Doing nothing ...");
			}
			Dictionary<String, Object> properties = new Hashtable<String, Object>();
			properties.put(Condition.CONDITION_ID, JerseyConstants.JERSEY_RUNTIME);
			jerseyRuntimeCondition = context.registerService(Condition.class, Condition.INSTANCE, properties);
			logger.info(()->"Registered Jersey runtime condition");
		} else {
			if (jerseyRuntimeCondition != null) {
				jerseyRuntimeCondition.unregister();
				jerseyRuntimeCondition = null;
				logger.info(()->"Un-registered Jersey runtime condition");
			}
		}
	}

}
