package com.mediamath.logging.sample;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;

/**
 * Created by cresnick on 4/1/17.
 *
 * Java version of snoop proxy.
 *
 */
public class Proxy {
	public static void main (String[] args) {
		//  Prepare our context and sockets
		ZContext context = new ZContext(1);

		//  This is where the weather server sits
		ZMQ.Socket frontend =  context.createSocket(ZMQ.XSUB);
		frontend.bind("tcp://*:5556");

		//  This is our public endpoint for subscribers
		ZMQ.Socket backend  = context.createSocket(ZMQ.XPUB);
		backend.bind("tcp://*:5557");

		//  Run the proxy forever
		ZMQ.proxy (frontend, backend, null);

		//  Never reached, adding it anyway.
		context.close();
	}
}
