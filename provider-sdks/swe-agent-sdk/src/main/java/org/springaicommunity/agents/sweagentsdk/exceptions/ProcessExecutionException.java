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

package org.springaicommunity.agents.sweagentsdk.exceptions;

/**
 * Exception thrown when a process execution fails.
 *
 * <p>
 * This exception captures detailed information about process failures, including exit
 * codes, stderr output, and command details.
 * </p>
 */
public class ProcessExecutionException extends SweSDKException {

	private final int exitCode;

	private final String command;

	private final String stderr;

	/**
	 * Creates a new ProcessExecutionException.
	 * @param message the detail message
	 * @param exitCode the process exit code
	 * @param command the command that was executed
	 * @param stderr the stderr output from the process
	 */
	public ProcessExecutionException(String message, int exitCode, String command, String stderr) {
		super(formatMessage(message, exitCode, command, stderr));
		this.exitCode = exitCode;
		this.command = command;
		this.stderr = stderr;
	}

	/**
	 * Creates a new ProcessExecutionException with a cause.
	 * @param message the detail message
	 * @param exitCode the process exit code
	 * @param command the command that was executed
	 * @param stderr the stderr output from the process
	 * @param cause the underlying cause
	 */
	public ProcessExecutionException(String message, int exitCode, String command, String stderr, Throwable cause) {
		super(formatMessage(message, exitCode, command, stderr), cause);
		this.exitCode = exitCode;
		this.command = command;
		this.stderr = stderr;
	}

	/**
	 * Gets the process exit code.
	 * @return the exit code
	 */
	public int getExitCode() {
		return exitCode;
	}

	/**
	 * Gets the command that was executed.
	 * @return the command string
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * Gets the stderr output from the process.
	 * @return the stderr output
	 */
	public String getStderr() {
		return stderr;
	}

	/**
	 * Formats the exception message with process details.
	 */
	private static String formatMessage(String message, int exitCode, String command, String stderr) {
		StringBuilder sb = new StringBuilder();
		sb.append(message);
		sb.append("\nCommand: ").append(command);
		sb.append("\nExit Code: ").append(exitCode);
		if (stderr != null && !stderr.trim().isEmpty()) {
			sb.append("\nStderr: ").append(stderr.trim());
		}
		return sb.toString();
	}

}