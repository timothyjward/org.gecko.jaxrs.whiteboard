/**
 * Copyright (c) 2012 - 2022 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 *     Stefan Bishof - API and implementation
 *     Tim Ward - implementation
 */
package org.gecko.rest.jersey.runtime.servlet;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.gecko.rest.jersey.binder.PromiseResponseHandlerBinder;
import org.gecko.rest.jersey.helper.DestroyListener;
import org.gecko.rest.jersey.runtime.common.ResourceConfigWrapper;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.servlet.ServletProperties;

/**
 * As Wrapper for the {@link ServletContainer} that locks the Servlet while its configuration is reloaded.
 * Furthermore it takes care that a reload is done, if a new configuration comes available while it is initialized
 * @author Juergen Albert
 * @since 1.0
 */
public class WhiteboardServletContainer extends ServletContainer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6509888299005723799L;

	private ResourceConfig initialConfig = null;;

	private AtomicBoolean initialized = new AtomicBoolean();
	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	private DestroyListener destroyListener;

	private ResourceConfigWrapper wrapper;
	
	private final ServiceLocator locator;

	public WhiteboardServletContainer(ResourceConfigWrapper configWrapper, DestroyListener destroyListener) {
		this(configWrapper.config, destroyListener);
		this.wrapper = configWrapper;
	}

	public WhiteboardServletContainer(ResourceConfig config, DestroyListener destroyListener) {
		initialConfig = config;
		this.destroyListener = destroyListener;
		
		// Ensure that promise types can be returned by resource methods
		locator = ServiceLocatorFactory.getInstance()
			.create("GeckoJerseyWhiteboard-" + System.identityHashCode(this));
		ServiceLocatorUtilities.bind(locator, new PromiseResponseHandlerBinder());
	}

	/* (non-Javadoc)
	 * @see org.glassfish.jersey.servlet.ServletContainer#init()
	 */
	@Override
	public void init() throws ServletException {
		
		lock.writeLock().lock();
		
		getServletContext().setAttribute(ServletProperties.SERVICE_LOCATOR, locator);
		
		try {
			super.init();
			// we have to wait until the injection manager is available on first start
			Future<?> future = Executors.newSingleThreadExecutor().submit(()->{
				ApplicationHandler handler = getApplicationHandler();
				while(handler.getInjectionManager() == null) {
					try {
						Thread.sleep(10l);
					} catch (InterruptedException e) {
					}
				}
			});
			future.get();
			initialized.set(true);
			if (initialConfig != null) {
				this.reload(initialConfig);
				wrapper.setInjectionManager(getApplicationHandler().getInjectionManager());
				initialConfig = null;
			}
		} catch (Exception e) {
			if (e instanceof ServletException) {
				throw (ServletException)e;
			} else {
				throw new ServletException(e);
			}
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.glassfish.jersey.servlet.ServletContainer#reload(org.glassfish.jersey.server.ResourceConfig)
	 */
	@Override
	public void reload(ResourceConfig configuration) {
		lock.writeLock().lock();
		try {
			if (initialized.get()) {
				super.reload(configuration);
			} else {
				initialConfig = configuration;
			}
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.glassfish.jersey.servlet.ServletContainer#service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		lock.readLock().lock();
		try {
			super.service(request, response);
		} finally {
			lock.readLock().unlock();
		}
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.glassfish.jersey.servlet.ServletContainer#destroy()
	 */
	@Override
	public void destroy() {
		super.destroy();
		if(destroyListener != null) {
			destroyListener.servletContainerDestroyed(this);
		}
		locator.shutdown();
	}

	/**
	 * @param config2
	 */
	public void reloadWrapper(ResourceConfigWrapper wrapper) {
		initialConfig = wrapper.config;
		this.wrapper = wrapper;
		if (!initialized.get()) {
			return;
		}
		reload(initialConfig);
		if(getApplicationHandler() != null) {
			wrapper.setInjectionManager(getApplicationHandler().getInjectionManager());
		}
	}
}
