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

package org.springaicommunity.agents.claude.sdk.exceptions;

/**
 * Raised when Claude Code is not found or not installed. Corresponds to CLINotFoundError
 * in Python SDK.
 */
public class CLINotFoundException extends CLIConnectionException {

	private final String cliPath;

	public CLINotFoundException(String message) {
		this(message, null);
	}

	public CLINotFoundException(String message, String cliPath) {
		super(cliPath != null ? message + ": " + cliPath : message);
		this.cliPath = cliPath;
	}

	public String getCliPath() {
		return cliPath;
	}

	public static CLINotFoundException defaultMessage() {
		return new CLINotFoundException("Claude Code not found");
	}

	public static CLINotFoundException withPath(String cliPath) {
		return new CLINotFoundException("Claude Code not found", cliPath);
	}

}