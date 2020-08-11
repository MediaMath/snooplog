/**
* Copyright (C) 2017 MediaMath <http://www.mediamath.com>
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
* @author cresnick
*/

package com.mediamath.logging.log4j;

import com.mediamath.logging.zmq.AsyncPublisher;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.InputStream;
import java.io.Serializable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;


@Plugin(name = "SnoopAppender", category = "Core", elementType = "appender", printObject = true)
public class SnoopAppender extends AbstractAppender {
	private String endpoint;
	private AsyncPublisher publisher;
	
	public boolean requiresLayout() {
		return false;
	}
	
	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}
	public String getEndpoint() {
		return endpoint;
	}

	static {
		try {
			System.setProperty("hostName", InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	public void activateOptions() {
		if (endpoint == null) {
			endpoint = findEndpoint();
		}
		if (endpoint == null) {
			error("Endpoint was neither set nor found in classpath core-site.xml.");
			return;
		}
		publisher = new AsyncPublisher(endpoint);
	}
	
	synchronized public void close() {
		publisher.close();
	}


	private String format(LogEvent eventObject) {
		return getLayout() == null 
				? eventObject.getMessage().toString() 
				: toSerializable(eventObject).toString();
	}

	/**
	 * Called to provide endpoint if not configured in logback.xml. Default implementation parses a Hadoop
	 * core-site.xml. Hadoop dependency is avoided.
	 * @return endpoint in form of host:port
	 */
	protected String findEndpoint() {
		InputStream in = this.getClass().getClassLoader().getResourceAsStream("core-site.xml");
		if (in != null) {
			Scanner s = new Scanner(in).useDelimiter("\\A");
			String coreSite = s.hasNext() ? s.next() : "";

			int pos = coreSite.indexOf("snoop.endpoint");
			if (pos > 0) {
				pos = coreSite.indexOf("value>", pos) + 6;
				return coreSite.substring(pos, coreSite.indexOf("<", pos)).trim();
			}
		}
		return null;
	}

	public SnoopAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout, 
						 boolean ignoreExceptions, Property[] properties) {
		super(name, filter, layout, ignoreExceptions, properties);
	}

	@PluginFactory
	public static SnoopAppender createAppender(@PluginAttribute("name") String name,
											   @PluginAttribute("ignoreExceptions") boolean ignoreExceptions,
											   @PluginAttribute("endpoint") String endpoint,
											   @PluginElement("Layout") Layout layout,
											   @PluginElement("Filters") Filter filter) {
		if (layout == null) {
			layout = PatternLayout.createDefaultLayout();
		}

		SnoopAppender appender = new SnoopAppender(name, filter, layout, ignoreExceptions, null);
		appender.setEndpoint(endpoint);
		return appender;
	}

	@Override
	public void append(final LogEvent event) {
		if (publisher == null) {
			activateOptions();
		}
		publisher.publish(format(event).trim());
	}
}
