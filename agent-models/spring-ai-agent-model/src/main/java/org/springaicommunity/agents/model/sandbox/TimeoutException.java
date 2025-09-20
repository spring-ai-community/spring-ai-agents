/*
 * Copyright 2024 Spring AI Community
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springaicommunity.agents.model.sandbox;

import java.time.Duration;

/**
 * Exception thrown when a command execution times out.
 */
public class TimeoutException extends Exception {

	private final Duration timeout;

	public TimeoutException(String message, Duration timeout) {
		super(message);
		this.timeout = timeout;
	}

	public TimeoutException(String message, Duration timeout, Throwable cause) {
		super(message, cause);
		this.timeout = timeout;
	}

	public Duration getTimeout() {
		return timeout;
	}

}