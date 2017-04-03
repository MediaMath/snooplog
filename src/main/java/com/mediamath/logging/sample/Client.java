package com.mediamath.logging.sample;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;

/**
 * Created by cresnick on 3/31/17.
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
