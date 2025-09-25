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

package org.springaicommunity.agents.core;

/**
 * Result of agent execution.
 *
 * @param success true if agent executed successfully
 * @param message result message or error description
 * @author Mark Pollack
 * @since 1.1.0
 */
public record Result(boolean success, String message) {

	/**
	 * Create a successful result with message.
	 * @param message success message
	 * @return successful result
	 */
	public static Result ok(String message) {
		return new Result(true, message);
	}

	/**
	 * Create a failed result with error message.
	 * @param message error message
	 * @return failed result
	 */
	public static Result fail(String message) {
		return new Result(false, message);
	}

}