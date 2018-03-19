/**
 * Copyright (c) 2012 - 2017 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.gecko.rest.jersey.provider.application;

import java.util.List;
import java.util.Map;

import org.osgi.framework.Filter;

/**
 * Base interface that provides basic provider information
 * 
 * @author Mark Hoffmann
 * @since 11.10.2017
 */
public interface JaxRsProvider extends Cloneable {
	
	/**
	 * Returns the primary identifier for the provider, which is usually the service id
	 * @return the application id
	 */
	public String getId();
	
	/**
	 * Returns the application name which targets to the property osgi.jaxrs.name
	 * @return the application name
	 */
	public String getName();
	
	/**
	 * Returns the service id, component id or <code>null</code>
	 * @return the service id, component id or <code>null</code>
	 */
	public Long getServiceId();
	
	/**
	 * Returns the service rank
	 * @return the service rank
	 */
	public Integer getServiceRank();
	
	/**
	 * Returns the providers properties, which are usually the service properties or an empty map
	 * @return the providers properties, which are usually the service properties or an empty map
	 */
	public Map<String, Object> getProviderProperties();
	
	/**
	 * Returns the provider object
	 * @return the provider object
	 */
	public <T> T getProviderObject();
	
	/**
	 * Returns <code>true</code>, if this application can handle the given properties.
	 * If the application contains a whiteboard target select, than the properties are checked against
	 * the select filter and returns the result.
	 * If the application has no whiteboard select filter, the method returns <code>true</code>
	 * @param runtimeProperties the properties of the whiteboard runtime
	 * @return <code>true</code>, if the application can be handled by a whiteboard runtime with the given properties
	 */
	public boolean canHandleWhiteboard(Map<String, Object> runtimeProperties);
	
	/**
	 * Returns <code>true</code>, if the is provider requires one or more extensions
	 * @return <code>true</code>, if the is provider requires one or more extensions
	 */
	public boolean requiresExtensions();
	
	/**
	 * Returns a {@link List} of {@link Filter} or an empty list
	 * @return a {@link List} of {@link Filter} or an empty list
	 */
	public List<Filter> getExtensionFilters();
	
	/**
	 * Returns <code>true</code>, if this provider is failed provider, which will result in a failed DTO
	 * @return <code>true</code>, if this provider is failed provider, which will result in a failed DTO
	 */
	public boolean isFailed();

}
