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

package com.mediamath.logging.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.status.ErrorStatus;
import com.mediamath.logging.zmq.AsyncPublisher;

import java.io.InputStream;
import java.util.Scanner;



public class SnoopAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

	private String endpoint;

	private AsyncPublisher publisher;

	private Layout<ILoggingEvent> layout;

	public Layout<ILoggingEvent> getLayout() {
		return layout;
	}

	public void setLayout(Layout<ILoggingEvent> layout) {
		this.layout = layout;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public String getEndpoint() {
		return endpoint;
	}

	@Override
	public void start() {
		if (endpoint == null) {
			endpoint = findEndpoint();
			if (endpoint == null) {
				addStatus(new ErrorStatus(
						"Endpoint was neither set nor found in classpath core-site.xml.",this));
				return;
			} else {
				publisher = new AsyncPublisher(endpoint);
			}
		}
		super.start();
	}

	@Override
	public void stop() {
		publisher.close();
		super.stop();
	}

	protected void append(ILoggingEvent eventObject) {
		publisher.publish(format(eventObject).trim());
	}

	private String format(ILoggingEvent eventObject) {
		return layout == null ? eventObject.getFormattedMessage() : layout.doLayout(eventObject);
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

			int pos = coreSite.indexOf("snoop.proxy.endpoint");
			if (pos > 0) {
				pos = coreSite.indexOf("value>", pos) + 6;
				return coreSite.substring(pos, coreSite.indexOf("<", pos)).trim();
			}
		}
		return null;
	}
}
