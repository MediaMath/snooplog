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
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;


/**
 * Log4j 1.x version of Logback appender. Unlike logback, log4j does not suuport first-class async appenders, so
 * this should be configured as wrapped in a {@link org.apache.log4j.AsyncAppender} (see test example).
 *
 * Created by cresnick on 1/13/17.
 */
public class SnoopAppender extends AppenderSkeleton {


	private String endpoint;
	private AsyncPublisher publisher;

	private Layout layout;


	@Override
	public Layout getLayout() {
		return layout;
	}

	public boolean requiresLayout() {
		return false;
	}

	@Override
	public void setLayout(Layout layout) {
		this.layout = layout;
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

	@Override
	public void activateOptions() {

		if (endpoint == null) {
			endpoint = findEndpoint();
		}
		if (endpoint == null) {
			errorHandler.error(
					"Endpoint was neither set nor found in classpath core-site.xml.");
			return;
		}
		publisher = new AsyncPublisher(endpoint);

	}


	synchronized public void close() {
		publisher.close();
	}


	@Override
	protected synchronized void append(LoggingEvent event) {
			if (publisher == null) {
				activateOptions();
			}
			publisher.publish(format(event).trim());
	}


	private String format(LoggingEvent eventObject) {
		return layout == null ? eventObject.getMessage().toString() : layout.format(eventObject);
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
}
