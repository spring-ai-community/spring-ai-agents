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

package org.springaicommunity.agents.claudecode.sdk.exceptions;

import java.time.Duration;

/**
 * Exception for timeout-related errors.
 */
public class TimeoutException extends ClaudeSDKException {

	private final Duration timeout;

	private final String operationType;

	public TimeoutException(String message, Duration timeout) {
		this(message, timeout, null);
	}

	public TimeoutException(String message, Duration timeout, String operationType) {
		super(buildMessage(message, timeout, operationType));
		this.timeout = timeout;
		this.operationType = operationType;
	}

	public Duration getTimeout() {
		return timeout;
	}

	public String getOperationType() {
		return operationType;
	}

	private static String buildMessage(String message, Duration timeout, String operationType) {
		StringBuilder builder = new StringBuilder(message);

		if (operationType != null) {
			builder.append(" (operation: ").append(operationType).append(")");
		}

		if (timeout != null) {
			builder.append(" after ").append(timeout.toMillis()).append("ms");
		}

		return builder.toString();
	}

	public static TimeoutException queryTimeout(Duration timeout) {
		return new TimeoutException("Query timeout exceeded", timeout, "query");
	}

	public static TimeoutException toolTimeout(Duration timeout) {
		return new TimeoutException("Tool execution timeout exceeded", timeout, "tool_execution");
	}

	public static TimeoutException streamingTimeout(Duration timeout) {
		return new TimeoutException("Streaming timeout exceeded", timeout, "streaming");
	}

}