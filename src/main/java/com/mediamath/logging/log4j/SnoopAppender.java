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

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

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
	private String inprocEndpoint = "inproc://message-pipe";
	private ZContext context;
	private static ThreadLocal<ZMQ.Socket> socketThreadLocal = new ThreadLocal<ZMQ.Socket>();

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
		context  = new ZContext(1);
		context.setLinger(1000);
		String addr = "tcp://" + endpoint;
		final ZMQ.Socket remote = context.createSocket(ZMQ.PUB);
		remote.connect(addr);
		//give PUB-SUB a few milliseconds to establish.
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		final ZMQ.Socket local = context.createSocket(ZMQ.PULL);
		local.bind(inprocEndpoint);
		Thread t = new Thread(new Runnable() {
			public void run() {
				ZMQ.proxy(local, remote, null);
			}
		});
		t.start();
		//set threadlocal directly to PUB in case we are logging from this thread
		socketThreadLocal.set(remote);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if (context != null) {
					context.destroy();
				}
			}
		});
	}


	synchronized public void close() {
		context.close();
	}


	@Override
	protected synchronized void append(LoggingEvent event) {
		ZMQ.Socket s = socketThreadLocal.get();
		if (s == null) {
			if (context == null) {
				activateOptions();
			}
			s = context.createSocket(ZMQ.PUSH);
			s.connect(inprocEndpoint);
			socketThreadLocal.set(s);
		}
		try {
			s.send(format(event).trim());
		} catch (Throwable e) {
			//drop message
			socketThreadLocal.set(null);
		}
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
