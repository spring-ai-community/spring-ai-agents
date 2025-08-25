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
 * Raised when the CLI process fails. Corresponds to ProcessError in Python SDK.
 */
public class ProcessExecutionException extends ClaudeSDKException {

	private final Integer exitCode;

	private final String stderr;

	public ProcessExecutionException(String message) {
		this(message, null, null);
	}

	public ProcessExecutionException(String message, Integer exitCode, String stderr) {
		super(buildMessage(message, exitCode, stderr));
		this.exitCode = exitCode;
		this.stderr = stderr;
	}

	public ProcessExecutionException(String message, Throwable cause) {
		super(message, cause);
		this.exitCode = null;
		this.stderr = null;
	}

	public Integer getExitCode() {
		return exitCode;
	}

	public String getStderr() {
		return stderr;
	}

	private static String buildMessage(String message, Integer exitCode, String stderr) {
		StringBuilder builder = new StringBuilder(message);

		if (exitCode != null) {
			builder.append(" (exit code: ").append(exitCode).append(")");
		}

		if (stderr != null && !stderr.trim().isEmpty()) {
			builder.append("\nError output: ").append(stderr);
		}

		return builder.toString();
	}

	public static ProcessExecutionException withExitCode(String message, int exitCode) {
		return new ProcessExecutionException(message, exitCode, null);
	}

	public static ProcessExecutionException withStderr(String message, String stderr) {
		return new ProcessExecutionException(message, null, stderr);
	}

}