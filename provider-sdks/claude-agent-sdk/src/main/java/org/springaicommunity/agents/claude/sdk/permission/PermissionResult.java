/*
 * Copyright 2025 Spring AI Community
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

package org.springaicommunity.agents.claude.sdk.permission;

import java.util.Map;

/**
 * Result of a tool permission check. The SDK's permission callback returns this to
 * indicate whether a tool should be allowed to execute and optionally provides modified
 * input.
 *
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>{@code
 * // Allow with modified input
 * PermissionResult.allow(Map.of("file_path", "/safe/path.txt", "safe_mode", true));
 *
 * // Allow without modification
 * PermissionResult.allow();
 *
 * // Deny with reason
 * PermissionResult.deny("Access to /etc/passwd is not allowed");
 * }</pre>
 */
public sealed interface PermissionResult permits PermissionResult.Allow, PermissionResult.Deny {

	/**
	 * Creates an allow result without input modification.
	 * @return allow result
	 */
	static Allow allow() {
		return new Allow(null);
	}

	/**
	 * Creates an allow result with modified input.
	 * @param updatedInput the modified input to use instead of original
	 * @return allow result with updated input
	 */
	static Allow allow(Map<String, Object> updatedInput) {
		return new Allow(updatedInput);
	}

	/**
	 * Creates a deny result without a message.
	 * @return deny result
	 */
	static Deny deny() {
		return new Deny(null);
	}

	/**
	 * Creates a deny result with an explanation message.
	 * @param message the reason for denial
	 * @return deny result with message
	 */
	static Deny deny(String message) {
		return new Deny(message);
	}

	/**
	 * Returns true if this result allows the tool to execute.
	 */
	boolean isAllowed();

	/**
	 * Allow result - permits tool execution with optional input modification.
	 *
	 * @param updatedInput optional modified input to use instead of original, or null to
	 * use original input
	 */
	record Allow(Map<String, Object> updatedInput) implements PermissionResult {
		@Override
		public boolean isAllowed() {
			return true;
		}

		/**
		 * Returns true if this result includes modified input.
		 */
		public boolean hasUpdatedInput() {
			return updatedInput != null && !updatedInput.isEmpty();
		}
	}

	/**
	 * Deny result - blocks tool execution.
	 *
	 * @param message optional explanation for why the tool was denied
	 */
	record Deny(String message) implements PermissionResult {
		@Override
		public boolean isAllowed() {
			return false;
		}

		/**
		 * Returns true if this result includes a denial message.
		 */
		public boolean hasMessage() {
			return message != null && !message.isEmpty();
		}
	}

}
