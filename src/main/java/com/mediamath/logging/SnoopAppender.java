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

package com.mediamath.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.status.ErrorStatus;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.InputStream;
import java.util.Scanner;


/**
 * Utility to support pub sub messaging across a cluster. PUB tcp socket connects to known endpoint. Because
 * zmq sockets are not threadsafe all messages are sent using threadLocal PUSH socket connecting to inproc
 * (zmq inter-thread) PULL socket forwarding to the PUB socket.
 *
 * Created by cresnick on 1/13/17.
 */
public class SnoopAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

	private String endpoint;
	private String inprocEndpoint = "inproc://message-pipe";
	private ZContext context;
	private static ThreadLocal<ZMQ.Socket> socketThreadLocal = new ThreadLocal<ZMQ.Socket>();

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
			}
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
		super.start();
	}

	@Override
	public void stop() {
		context.close();
		super.stop();
	}

	protected void append(ILoggingEvent eventObject) {
		ZMQ.Socket s = socketThreadLocal.get();
		if (s == null) {
			s = context.createSocket(ZMQ.PUSH);
			s.connect(inprocEndpoint);
			socketThreadLocal.set(s);
		}
		try {
			s.send(format(eventObject).trim());
		} catch (Throwable e) {
			//drop message
			socketThreadLocal.set(null);
		}
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
