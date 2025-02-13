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
package org.gecko.rest.jersey.provider;

/**
 * Interface for constants used in Jersey
 * @author Mark Hoffmann
 * @since 13.07.2017
 */
public interface JerseyConstants {
	
	public static final String JERSEY_SCHEMA = "jersey.schema";
	public static final String JERSEY_HOST = "jersey.host";
	public static final String JERSEY_PORT = "jersey.port";
	public static final String JERSEY_CONTEXT_PATH = "jersey.context.path";
	public static final String JERSEY_WHITEBOARD_NAME = "jersey.jaxrs.whiteboard.name";
	public static final String JERSEY_STRICT_MODE = "jersey.jaxrs.whiteboard.strict";
	public static final Object JERSEY_DISABLE_SESSION = "jersey.disable.sessions";
	
	public static final Integer WHITEBOARD_DEFAULT_PORT = Integer.valueOf(8181);
	public static final String WHITEBOARD_DEFAULT_CONTEXT_PATH = "/rest";
	public static final String WHITEBOARD_DEFAULT_HOST = "localhost";
	public static final String WHITEBOARD_DEFAULT_SCHEMA = "http";
	public static final String WHITEBOARD_DEFAULT_NAME = "Jersey REST";

}
