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

package org.springaicommunity.agents.geminisdk.exceptions;

/**
 * Exception thrown when Gemini CLI process execution fails. Contains detailed information
 * about the process failure.
 */
public class ProcessExecutionException extends GeminiSDKException {

	private final int exitCode;

	private final String stdout;

	private final String stderr;

	public ProcessExecutionException(String message, int exitCode, String stdout, String stderr) {
		super(String.format("%s (Exit Code: %d)", message, exitCode));
		this.exitCode = exitCode;
		this.stdout = stdout;
		this.stderr = stderr;
	}

	public ProcessExecutionException(String message, int exitCode, String stdout, String stderr, Throwable cause) {
		super(String.format("%s (Exit Code: %d)", message, exitCode), cause);
		this.exitCode = exitCode;
		this.stdout = stdout;
		this.stderr = stderr;
	}

	public int getExitCode() {
		return exitCode;
	}

	public String getStdout() {
		return stdout;
	}

	public String getStderr() {
		return stderr;
	}

}