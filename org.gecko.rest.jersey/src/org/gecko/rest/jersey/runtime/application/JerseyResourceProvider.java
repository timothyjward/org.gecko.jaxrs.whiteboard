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

import java.util.Map;

import org.gecko.rest.jersey.dto.DTOConverter;
import org.gecko.rest.jersey.provider.application.JaxRsResourceProvider;
import org.osgi.framework.ServiceObjects;
import org.osgi.service.jaxrs.runtime.dto.BaseDTO;
import org.osgi.service.jaxrs.runtime.dto.DTOConstants;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

/**
 * A wrapper class for a JaxRs resources 
 * @author Mark Hoffmann
 * @param <T>
 * @since 09.10.2017
 */
public class JerseyResourceProvider<T extends Object> extends JerseyApplicationContentProvider<T> implements JaxRsResourceProvider {

	public JerseyResourceProvider(ServiceObjects<T> serviceObjects, Map<String, Object> properties) {
		super(serviceObjects, properties);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.JaxRsResourceProvider#isResource()
	 */
	@Override
	public boolean isResource() {
		return getProviderStatus() != INVALID;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.JaxRsResourceProvider#getResourceDTO()
	 */
	@Override
	public BaseDTO getResourceDTO() {
		int status = getProviderStatus();
		if (status == NO_FAILURE) {
			return DTOConverter.toResourceDTO(this);
		} else {
			return DTOConverter.toFailedResourceDTO(this, status == INVALID ? DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE : status);
		}
	}
	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		return new JerseyResourceProvider<T>(getProviderObject(), getProviderProperties());
	}
	
	/**
	 * Returns the {@link JaxRSWhiteboardConstants} for this resource type 
	 * @return the {@link JaxRSWhiteboardConstants} for this resource type
	 */
	protected String getJaxRsResourceConstant() {
		return JaxrsWhiteboardConstants.JAX_RS_RESOURCE;
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.AbstractJaxRsProvider#updateStatus(int)
	 */
	@Override
	public void updateStatus(int newStatus) {
		super.updateStatus(newStatus);
	}

}
