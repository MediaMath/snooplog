package com.mediamath.logging;

import ch.qos.logback.classic.LoggerContext;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by cresnick on 4/1/1
 *
 */
public class LogTest {

	private static Logger logger = LoggerFactory.getLogger(LogTest.class);

	public static void main(String[] args) throws InterruptedException {

		logger.trace("trace");
		logger.debug("debug");
		logger.info("info");
		logger.warn("warn");
		logger.error("error");

		logger.trace("hi");
		for (int i = 0; i < 100; i++) {
			logger.trace("message " + i);
		}
		logger.trace("bye");

		//Gracefully shut down LoggerFactory. Typically this would be unnecessary
		//as a long-lived process would be closed by SIGINT or other means.
		ILoggerFactory factory = LoggerFactory.getILoggerFactory();
		if(factory instanceof LoggerContext) {
			LoggerContext ctx = (LoggerContext)factory;
			ctx.stop();
		}
	}
}
