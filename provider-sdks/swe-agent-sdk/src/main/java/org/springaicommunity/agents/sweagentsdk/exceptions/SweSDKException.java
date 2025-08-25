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
 * Base exception for all SWE Agent SDK operations.
 *
 * <p>
 * This is the parent class for all exceptions thrown by the SWE Agent SDK. It provides a
 * consistent exception hierarchy for error handling.
 * </p>
 */
public class SweSDKException extends RuntimeException {

	/**
	 * Creates a new SweSDKException with the specified message.
	 * @param message the detail message
	 */
	public SweSDKException(String message) {
		super(message);
	}

	/**
	 * Creates a new SweSDKException with the specified message and cause.
	 * @param message the detail message
	 * @param cause the underlying cause
	 */
	public SweSDKException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Creates a new SweSDKException with the specified cause.
	 * @param cause the underlying cause
	 */
	public SweSDKException(Throwable cause) {
		super(cause);
	}

}