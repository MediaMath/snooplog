# Snoop "Loggy" Log

Snoop Log lets you tail your cluster without the need for log aggregation

![log message flow](./ClusterTail.png)

Snoop Log is a Logback or Log4j appender that fans in scoped messages to a proxied ZeroMQ PUB endpoint. The use case for Snoop Loggy Log is to tail all logs of a "live forever" application, like Flink or Spark Streaming, without the need for continuous log aggregation. Snoop "Loggy" Log is so-named because it uses logging semantics; it's loggy but not really a log, more a log publisher. 
 
Like any proper logging tool, Snoop Log is both minimal in dependencies and unobtrusive at runtime. It's fully asynchronous (note Log4j appender will need do be configured [async] (./src/test/resources/log4j.xml)), will drop messages if the Snoop Proxy is not reachable, and reconnect when it is. It uses native java JeroMQ, so will work with any JVM-based code without need for native zmq libraries.

## Install 
```
mvn clean install
```
## Usage
* Include logback-snoop and jeromq jars on your classpath
* Add entry in logback.xml for com.mediamath.logging.SnoopAppender as well as desired logger scopes and levels (see test resources).
* Start Proxy somwhere. 
* Connect to Proxy with Client. (Proxy and Client can be started or stopped anytime and components will reconnect).
* Run your cluster job.
* Tail Client for log.

## Example (test)

1. Run sample Proxy.
2. Run sample Client.
3. Run LogTest.java.

The above three steps can be run in any order. Running the Client before the Proxy demonstrates ZeroMQ's late connecting. Running LogTest without the Proxy or Client demonstrates appender no-op. 

### Golang tools

With ZeroMQ there are several language options for Proxy and Client. We use golang for our monitoring tools, and included are a golang proxy and client. To install these you will also need the zmq native library installed. There are [many ways to install](http://zeromq.org/intro:get-the-software), but if you have brew on a mac, it's simple:

`brew install zmq`

then

```
go get github.com/pebbe/zmq4
cd cmd/snoop-proxy
go build
cd ../snoop-client
go build
```

You can now repeat the above tests mixing and matching golang and java clients and switching between proxies.

## Real World

1. Run proxy on some stable instance. We use spot YARN clusters and run the included golang proxy on the Resource Manager. 
2. Consider fronting client with a REST call so cluster can be tailed anywhere.

## Notes
* If hadoop core-site.xml exists in classpath, SnoopAppender will check it for "snoop.proxy.endpoint" hostport (using a simple parse to avoid hadoop dependency).. Hostport can also be set in logback.xml. For other means of dynamic discovery, override `SnoopAppender.findEndpoint`.

* Logback appender has been thoroughly tested. Log4j should work as well but it should perhaps be considered beta.  
