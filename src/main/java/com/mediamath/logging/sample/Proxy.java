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
