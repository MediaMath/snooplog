package com.mediamath.logging.zmq;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;


/**
 * Utility to support pub sub messaging across a cluster. PUB tcp socket connects to known endpoint. Because
 * zmq sockets are not threadsafe all messages are sent using threadLocal PUSH socket connecting to inproc
 * (zmq inter-thread) PULL socket forwarding to the PUB socket.
 *
 * Created by cresnick on 1/13/17.
 */
public class AsyncPublisher {

    private String inprocEndpoint = "inproc://message-pipe";
    private ZContext context;
    private static ThreadLocal<ZMQ.Socket> socketThreadLocal = new ThreadLocal<ZMQ.Socket>();

    public AsyncPublisher(String endpoint) {

        if (endpoint == null) {
            throw new ExceptionInInitializerError("endpoint is missing or null.");
        }
        context  = new ZContext(1);
        context.setLinger(1000);
        String addr = "tcp://" + endpoint;
        final ZMQ.Socket remote = context.createSocket(ZMQ.PUB);
        remote.connect(addr);
        //give PUB-SUB a few milliseconds to establish (mostly for local testing).
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
        //set threadlocal directly to PUB in case we are single-threaded
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

    public void publish(String message) {

        ZMQ.Socket s = socketThreadLocal.get();
        if (s == null) {
            s = context.createSocket(ZMQ.PUSH);
            s.connect(inprocEndpoint);
            socketThreadLocal.set(s);
        }
        try {
            s.send(message);
        } catch (Throwable e) {
            //drop message
            socketThreadLocal.set(null);
        }
    }

    public void close() {
        context.close();
    }


}
