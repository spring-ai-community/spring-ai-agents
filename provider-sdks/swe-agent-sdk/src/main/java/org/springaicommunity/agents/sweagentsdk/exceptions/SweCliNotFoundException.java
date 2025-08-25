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
 * Exception thrown when the mini-swe-agent CLI cannot be found or is not functional.
 *
 * <p>
 * This exception provides detailed information about CLI availability issues and includes
 * helpful installation instructions.
 * </p>
 *
 * <p>
 * <strong>Common causes:</strong>
 * </p>
 * <ul>
 * <li>mini-swe-agent CLI is not installed</li>
 * <li>CLI is not in PATH or specified location</li>
 * <li>CLI is installed but not executable</li>
 * <li>CLI version is incompatible</li>
 * </ul>
 */
public class SweCliNotFoundException extends SweSDKException {

	/**
	 * Creates a new SweCliNotFoundException with installation instructions.
	 * @param message the detail message describing the issue
	 */
	public SweCliNotFoundException(String message) {
		super(enhanceMessage(message));
	}

	/**
	 * Creates a new SweCliNotFoundException with cause.
	 * @param message the detail message describing the issue
	 * @param cause the underlying cause
	 */
	public SweCliNotFoundException(String message, Throwable cause) {
		super(enhanceMessage(message), cause);
	}

	/**
	 * Enhances the error message with installation instructions.
	 */
	private static String enhanceMessage(String message) {
		return message + "\n\n" + "To install mini-swe-agent CLI:\n"
				+ "1. Install via pip: pip install mini-swe-agent\n"
				+ "2. Or install via pipx: pipx install mini-swe-agent\n" + "3. Verify installation: mini --version\n"
				+ "4. Set OPENAI_API_KEY environment variable\n\n"
				+ "For more information, visit: https://github.com/SWE-agent/mini-swe-agent";
	}

}