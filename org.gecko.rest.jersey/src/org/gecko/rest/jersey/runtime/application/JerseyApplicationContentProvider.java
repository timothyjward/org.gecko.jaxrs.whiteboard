/**
 * Copyright (c) 2012 - 2018 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.gecko.rest.jersey.runtime.application;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gecko.rest.jersey.provider.application.AbstractJaxRsProvider;
import org.gecko.rest.jersey.provider.application.JaxRsApplicationContentProvider;
import org.gecko.rest.jersey.provider.application.JaxRsApplicationProvider;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceObjects;
import org.osgi.service.jaxrs.runtime.dto.DTOConstants;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

/**
 * A wrapper class for a JaxRs resources 
 * @author Mark Hoffmann
 * @param <T>
 * @param <T>
 * @since 09.10.2017
 */
public class JerseyApplicationContentProvider<T> extends AbstractJaxRsProvider<ServiceObjects<T>> implements JaxRsApplicationContentProvider {

	private static final Logger logger = Logger.getLogger("jersey.contentProvider");
	private Filter applicationFilter;
	private Class<? extends Object> clazz;

	public JerseyApplicationContentProvider(ServiceObjects<T> serviceObjects, Map<String, Object> properties) {
		super(serviceObjects, properties);
		serviceObjects = getProviderObject();
		if(serviceObjects != null) {
			T service = null;
			try {
				service = serviceObjects.getService();
			} catch(Exception e) {
				logger.warning("Error getting the service " + e.getMessage());
			}
			
			// if this is called while a service gets unregistered, the ServiceObjects returns null as a Service 
			if(service != null) {
				clazz = service.getClass();
				try {
					//For some reason we had some explainable situation when this has produced an 
					//IllegalArgumentException which should be impossible according to the javadoc and they way 
					//we retrieve the object. 
					serviceObjects.ungetService(service);
				} catch (Throwable e) {
					updateStatus(DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE);
				}
			}
			else {
				updateStatus(DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE);
			}
		}
		else {
			updateStatus(DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE);
		}
	}

	/**
	 * Returns <code>true</code>, if this resource is a singleton service
	 * @return <code>true</code>, if this resource is a singleton service
	 */
	public boolean isSingleton() {
		String scope = (String) getProviderProperties().get(Constants.SERVICE_SCOPE);
		if (!Constants.SCOPE_PROTOTYPE.equalsIgnoreCase(scope)) {
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.JaxRsRandEProvider#getObjectClass()
	 */
	@Override
	public Class<?> getObjectClass() {
		return clazz;
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.JaxRsRandEProvider#getProperties()
	 */
	@Override
	public Map<String, Object> getProperties() {
		return getProviderProperties();
	}

	@Override
	public boolean canHandleApplication(JaxRsApplicationProvider application) {
		if (applicationFilter != null) {
			try {
				boolean applicationMatch = applicationFilter.matches(application.getApplicationProperties());
				if (!applicationMatch) {
					logger.log(Level.FINE, "[" + getId() + "] The given application select filter does not match to this application " + application.getId() + " for this resource/extension: " + getId());
					return false;
				}
			} catch (Exception e) {
				logger.log(Level.WARNING, "The given application select filter causes an error: " + applicationFilter, e);
				return false;
			}
		} else {
			if (!application.isDefault()) {
//				Check if app is a potential default app
				if(".default".equals(application.getName()) || "/".equals(application.getPath()) || "/*".equals(application.getPath())) {
					logger.fine("Potential default app " + application.getName() + " can handle content");
					return true;
				}
				logger.log(Level.INFO, "[" + getId() + "] There is no application select filter defined, using default application");
				return false;
			} else {
				return canHandleDefaultApplication();
			}
		}
		return true;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsApplicationContentProvider#canHandleDefaultApplication()
	 */
	@Override
	public boolean canHandleDefaultApplication() {
		if (applicationFilter == null) {
			return true;
		} else {
			Map<String, Object> properties = Collections.singletonMap(JaxrsWhiteboardConstants.JAX_RS_NAME, ".default");
			return applicationFilter.matches(properties);
		}
	}
	
	
	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsApplicationContentProvider#canHandleDefaultApplication(org.gecko.rest.jersey.provider.application.JaxRsApplicationProvider)
	 */
	@Override
	public boolean canHandleDefaultApplication(JaxRsApplicationProvider application) {
		if(application.isDefault()) {
			return canHandleDefaultApplication();
		}
		if (applicationFilter == null) {
			return true;
		} else {
			return applicationFilter.matches(application.getProviderProperties());
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsApplicationContentProvider#canHandleApplications(java.util.Collection)
	 */
	@Override
	public boolean validateApplications(Collection<JaxRsApplicationProvider> applications) {
		if (applications == null) {
			return false;
		}
		long matched = applications.stream().filter((app)->canHandleApplication(app)).count();
		boolean canHandle = canHandleDefaultApplication() || matched > 0;
		if (!canHandle) {
			updateStatus(DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
		}
		return canHandle;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.AbstractJaxRsProvider#doValidateProperties(java.util.Map)
	 */
	protected void doValidateProperties(Map<String, Object> properties) {
		Object resourceProp = properties.get(getJaxRsResourceConstant());
		if (resourceProp == null || (resourceProp instanceof Boolean && !((Boolean) resourceProp)) || !Boolean.parseBoolean(resourceProp.toString())) {
			logger.log(Level.WARNING, "The resource to add is not declared with the resource property: " + getJaxRsResourceConstant());
			updateStatus(INVALID);
			return;
		}
		String filter = (String) properties.get(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT);
		if (filter != null) {
			try {
				applicationFilter = FrameworkUtil.createFilter(filter);
			} catch (InvalidSyntaxException e) {
				logger.log(Level.SEVERE, "The given application select filter is invalid: " + filter, e);
				updateStatus(DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
				return;
			}
		}
	}
	
	/**
	 * Returns the {@link JaxRSWhiteboardConstants} for this resource type 
	 * @return the {@link JaxRSWhiteboardConstants} for this resource type
	 */
	protected String getJaxRsResourceConstant() {
		return new String();
	}

}
