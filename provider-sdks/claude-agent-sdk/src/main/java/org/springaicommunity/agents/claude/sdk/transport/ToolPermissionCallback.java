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

package org.springaicommunity.agents.claude.sdk.transport;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Callback interface for dynamic tool permission decisions. This is invoked when Claude
 * wants to use a tool and needs permission validation.
 *
 * <p>
 * The callback receives the tool name, input parameters, and any permission suggestions
 * from the CLI, and returns a decision to allow, deny, or modify the tool use.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 * {@code
 * // Allow all tools
 * ToolPermissionCallback callback = ToolPermissionCallback.allowAll();
 *
 * // Read-only mode - only allow specific tools
 * ToolPermissionCallback readOnly = ToolPermissionCallback.allowList(
 *     Set.of("Read", "Glob", "Grep", "WebFetch"));
 *
 * // Custom logic
 * ToolPermissionCallback custom = (tool, input, ctx) -> {
 *     if ("Bash".equals(tool) && input.get("command").toString().contains("rm")) {
 *         return CompletableFuture.completedFuture(
 *             ToolPermissionResult.deny("Destructive commands not allowed"));
 *     }
 *     return CompletableFuture.completedFuture(ToolPermissionResult.allow());
 * };
 * }
 * </pre>
 *
 * @see ToolPermissionResult
 * @see ToolPermissionContext
 */
@FunctionalInterface
public interface ToolPermissionCallback {

	/**
	 * Evaluate whether a tool can be used with the given input.
	 * @param toolName the name of the tool being invoked (e.g., "Bash", "Read", "Write")
	 * @param input the tool input parameters as a map
	 * @param context additional context including permission suggestions
	 * @return a future completing with the permission result
	 */
	CompletableFuture<ToolPermissionResult> canUseTool(String toolName, Map<String, Object> input,
			ToolPermissionContext context);

	/**
	 * Creates a callback that allows all tool uses.
	 * @return a callback that always allows
	 */
	static ToolPermissionCallback allowAll() {
		return (tool, input, ctx) -> CompletableFuture.completedFuture(ToolPermissionResult.allow());
	}

	/**
	 * Creates a callback that only allows tools in the specified set.
	 * @param allowedTools set of allowed tool names
	 * @return a callback that allows only listed tools
	 */
	static ToolPermissionCallback allowList(Set<String> allowedTools) {
		return (tool, input, ctx) -> {
			if (allowedTools.contains(tool)) {
				return CompletableFuture.completedFuture(ToolPermissionResult.allow());
			}
			return CompletableFuture.completedFuture(ToolPermissionResult.deny("Tool not in allowed list: " + tool));
		};
	}

	/**
	 * Creates a callback that denies tools in the specified set.
	 * @param deniedTools set of denied tool names
	 * @return a callback that denies listed tools
	 */
	static ToolPermissionCallback denyList(Set<String> deniedTools) {
		return (tool, input, ctx) -> {
			if (deniedTools.contains(tool)) {
				return CompletableFuture.completedFuture(ToolPermissionResult.deny("Tool is denied: " + tool));
			}
			return CompletableFuture.completedFuture(ToolPermissionResult.allow());
		};
	}

	/**
	 * Context provided to the permission callback with additional information.
	 *
	 * @param permissionSuggestions suggestions from the CLI for permission rules
	 * @param blockedPath path that was blocked (for file access tools)
	 */
	record ToolPermissionContext(List<Map<String, Object>> permissionSuggestions, String blockedPath) {
		public static ToolPermissionContext empty() {
			return new ToolPermissionContext(List.of(), null);
		}

		public static ToolPermissionContext of(List<Map<String, Object>> suggestions, String blockedPath) {
			return new ToolPermissionContext(suggestions != null ? suggestions : List.of(), blockedPath);
		}
	}

	/**
	 * Result of a tool permission check.
	 */
	sealed interface ToolPermissionResult permits ToolPermissionResult.Allow, ToolPermissionResult.Deny {

		/**
		 * Permission granted, optionally with modified input.
		 */
		record Allow(Map<String, Object> updatedInput) implements ToolPermissionResult {
			public Allow() {
				this(null);
			}
		}

		/**
		 * Permission denied with reason.
		 */
		record Deny(String reason, boolean interrupt) implements ToolPermissionResult {
			public Deny(String reason) {
				this(reason, false);
			}
		}

		/**
		 * Create an allow result.
		 */
		static ToolPermissionResult allow() {
			return new Allow();
		}

		/**
		 * Create an allow result with modified input.
		 */
		static ToolPermissionResult allowWithModifiedInput(Map<String, Object> updatedInput) {
			return new Allow(updatedInput);
		}

		/**
		 * Create a deny result with reason.
		 */
		static ToolPermissionResult deny(String reason) {
			return new Deny(reason);
		}

		/**
		 * Create a deny result that also interrupts the session.
		 */
		static ToolPermissionResult denyAndInterrupt(String reason) {
			return new Deny(reason, true);
		}

	}

}
