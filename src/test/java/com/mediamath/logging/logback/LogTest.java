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
