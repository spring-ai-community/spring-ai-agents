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

/**
 * Raised when unable to decode JSON from CLI output. Corresponds to CLIJSONDecodeError in
 * Python SDK.
 */
public class CLIJSONDecodeException extends ClaudeSDKException {

	private final String line;

	private final Exception originalError;

	public CLIJSONDecodeException(String line, Exception originalError) {
		super(buildMessage(line));
		this.line = line;
		this.originalError = originalError;
	}

	public String getLine() {
		return line;
	}

	public Exception getOriginalError() {
		return originalError;
	}

	private static String buildMessage(String line) {
		String truncated = line.length() > 100 ? line.substring(0, 100) + "..." : line;
		return "Failed to decode JSON: " + truncated;
	}

	/**
	 * Creates a new CLIJSONDecodeException with the full context.
	 */
	public static CLIJSONDecodeException create(String line, Exception originalError) {
		return new CLIJSONDecodeException(line, originalError);
	}

}