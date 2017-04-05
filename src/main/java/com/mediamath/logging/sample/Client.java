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
 *
 * Very simple client to read logged messages. Typically this code would reside in a command line client
 * or backend service (e.g. REST) for general access.
 */
public class Client {

    public static void main(String[] args) throws InterruptedException {

        final ZContext context = new ZContext(1);

        ZMQ.Socket sub = context.createSocket(ZMQ.SUB);
        sub.connect("tcp://localhost:5557");

        sub.subscribe("".getBytes());
        while(true){
            System.out.println(new String(sub.recv()));
        }

    }

}
