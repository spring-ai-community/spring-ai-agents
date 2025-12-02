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

package org.springaicommunity.agents.claude.sdk.hooks;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springaicommunity.agents.claude.sdk.config.PermissionMode;
import org.springaicommunity.agents.claude.sdk.test.ClaudeCliTestBase;
import org.springaicommunity.agents.claude.sdk.transport.BidirectionalTransport;
import org.springaicommunity.agents.claude.sdk.transport.CLIOptions;
import org.springaicommunity.agents.claude.sdk.types.ResultMessage;
import org.springaicommunity.agents.claude.sdk.types.control.ControlRequest;
import org.springaicommunity.agents.claude.sdk.types.control.ControlResponse;
import org.springaicommunity.agents.claude.sdk.types.control.HookOutput;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for hook execution with real Claude CLI.
 *
 * <p>
 * Tests hook callbacks for:
 * <ul>
 * <li>PreToolUse hooks with permissionDecision and reason fields</li>
 * <li>PostToolUse hooks with continue=false and stopReason</li>
 * <li>hookSpecificOutput for additional context</li>
 * </ul>
 *
 * <p>
 * Key patterns:
 * <ul>
 * <li>Uses DEFAULT permission mode to receive hook callbacks</li>
 * <li>Uses tool-forcing prompts like "Run: echo 'hello'"</li>
 * <li>Asserts specific tools are invoked (e.g., "Bash" in hook_invocations)</li>
 * </ul>
 */
@Timeout(value = 180, unit = TimeUnit.SECONDS)
class HookIntegrationIT extends ClaudeCliTestBase {

	private static final String HAIKU_MODEL = CLIOptions.MODEL_HAIKU;

	/**
	 * Tests that PreToolUse hooks with permissionDecision and reason fields work
	 * end-to-end.
	 *
	 * <p>
	 * TODO: Enable when hook config passing via CLIOptions is implemented (P1 feature).
	 * Currently, hooks need to be registered in CLIOptions and passed to CLI via
	 * --hook-config flag for the CLI to send hook callbacks.
	 */
	@Test
	@Disabled("Requires hook config passing via CLIOptions - P1 feature")
	@DisplayName("PreToolUse hook with permissionDecision and reason")
	void hookWithPermissionDecisionAndReason() throws Exception {
		// Given - track hook invocations
		List<String> hookInvocations = new CopyOnWriteArrayList<>();
		CountDownLatch resultLatch = new CountDownLatch(1);

		// Use DEFAULT mode to receive hook callbacks
		CLIOptions options = CLIOptions.builder()
			.model(HAIKU_MODEL)
			.permissionMode(PermissionMode.DEFAULT)
			.allowedTools(List.of("Bash", "Read"))
			.build();

		try (BidirectionalTransport transport = new BidirectionalTransport(workingDirectory(), Duration.ofMinutes(3),
				getClaudeCliPath())) {

			// Ask Claude to run a bash command
			transport.startSession("Run this bash command: echo 'hello'", options, message -> {
				if (message.isRegularMessage() && message.asMessage() instanceof ResultMessage) {
					resultLatch.countDown();
				}
			}, request -> {
				// Handle PreToolUse hook callbacks
				if (request.request() instanceof ControlRequest.HookCallbackRequest hookCallback) {
					Map<String, Object> input = hookCallback.input();
					String toolName = (String) input.get("tool_name");

					if (toolName != null) {
						System.out.println("Hook called for tool: " + toolName);
						hookInvocations.add(toolName);
					}

					// Return permissionDecision based on tool
					if ("Bash".equals(toolName)) {
						// Block Bash for this test
						return ControlResponse.success(request.requestId(),
								Map.of("reason", "Bash commands are blocked in this test for safety", "systemMessage",
										"Command blocked by hook", "hookSpecificOutput",
										Map.of("hookEventName", "PreToolUse", "permissionDecision", "deny",
												"permissionDecisionReason", "Security policy: Bash blocked")));
					}

					// Allow other tools
					return ControlResponse.success(request.requestId(),
							Map.of("reason", "Tool approved by security review", "hookSpecificOutput",
									Map.of("hookEventName", "PreToolUse", "permissionDecision", "allow",
											"permissionDecisionReason", "Tool passed security checks")));
				}

				// Handle CanUseToolRequest (permission callback)
				if (request.request() instanceof ControlRequest.CanUseToolRequest canUseTool) {
					String toolName = canUseTool.toolName();
					System.out.println("Permission requested for tool: " + toolName);
					hookInvocations.add(toolName);

					// Allow all tools to proceed
					return ControlResponse.success(request.requestId(), Map.of("behavior", "allow"));
				}

				// Default allow for other requests
				return ControlResponse.success(request.requestId(), Map.of("behavior", "allow"));
			});

			// Wait for completion
			boolean completed = resultLatch.await(120, TimeUnit.SECONDS);
			assertThat(completed).as("Should complete within timeout").isTrue();

			// Assert: "Bash" in hook_invocations
			System.out.println("Hook invocations: " + hookInvocations);
			assertThat(hookInvocations).as("Hook should have been invoked for Bash tool, got: " + hookInvocations)
				.contains("Bash");
		}
	}

	/**
	 * Tests that PostToolUse hooks with continue=false and stopReason work end-to-end.
	 *
	 * <p>
	 * TODO: Enable when hook config passing via CLIOptions is implemented (P1 feature).
	 */
	@Test
	@Disabled("Requires hook config passing via CLIOptions - P1 feature")
	@DisplayName("PostToolUse hook with continue=false and stopReason")
	void postToolUseHookWithContinueAndStopReason() throws Exception {
		// Given - track hook invocations
		List<String> hookInvocations = new CopyOnWriteArrayList<>();
		CountDownLatch resultLatch = new CountDownLatch(1);

		CLIOptions options = CLIOptions.builder()
			.model(HAIKU_MODEL)
			.permissionMode(PermissionMode.DEFAULT)
			.allowedTools(List.of("Bash"))
			.build();

		try (BidirectionalTransport transport = new BidirectionalTransport(workingDirectory(), Duration.ofMinutes(3),
				getClaudeCliPath())) {

			// Ask Claude to run a bash command
			transport.startSession("Run: echo 'test message'", options, message -> {
				if (message.isRegularMessage() && message.asMessage() instanceof ResultMessage) {
					resultLatch.countDown();
				}
			}, request -> {
				// Handle PostToolUse hook callbacks
				if (request.request() instanceof ControlRequest.HookCallbackRequest hookCallback) {
					Map<String, Object> input = hookCallback.input();
					String hookEventName = (String) input.get("hook_event_name");
					String toolName = (String) input.get("tool_name");

					if ("PostToolUse".equals(hookEventName) && toolName != null) {
						System.out.println("PostToolUse hook called for: " + toolName);
						hookInvocations.add(toolName);

						// Return continue=false with stopReason
						return ControlResponse.success(request.requestId(),
								Map.of("continue_", false, "stopReason", "Execution halted by test hook for validation",
										"reason", "Testing continue and stopReason fields", "systemMessage",
										"Test hook stopped execution"));
					}

					// For PreToolUse, allow execution
					if ("PreToolUse".equals(hookEventName)) {
						return ControlResponse.success(request.requestId(),
								Map.of("hookSpecificOutput", Map.of("permissionDecision", "allow")));
					}
				}

				// Handle CanUseToolRequest
				if (request.request() instanceof ControlRequest.CanUseToolRequest canUseTool) {
					hookInvocations.add(canUseTool.toolName());
					return ControlResponse.success(request.requestId(), Map.of("behavior", "allow"));
				}

				// Default allow
				return ControlResponse.success(request.requestId(), Map.of("behavior", "allow"));
			});

			// Wait for completion
			boolean completed = resultLatch.await(120, TimeUnit.SECONDS);
			assertThat(completed).as("Should complete within timeout").isTrue();

			// Assert: "Bash" in hook_invocations
			System.out.println("Hook invocations: " + hookInvocations);
			assertThat(hookInvocations).as("PostToolUse hook should have been invoked, got: " + hookInvocations)
				.contains("Bash");
		}
	}

	/**
	 * Tests that hooks with hookSpecificOutput provide additional context end-to-end.
	 *
	 * <p>
	 * TODO: Enable when hook config passing via CLIOptions is implemented (P1 feature).
	 */
	@Test
	@Disabled("Requires hook config passing via CLIOptions - P1 feature")
	@DisplayName("Hook with hookSpecificOutput additional context")
	void hookWithAdditionalContext() throws Exception {
		// Given - track hook invocations
		List<String> hookInvocations = new CopyOnWriteArrayList<>();
		CountDownLatch resultLatch = new CountDownLatch(1);

		CLIOptions options = CLIOptions.builder()
			.model(HAIKU_MODEL)
			.permissionMode(PermissionMode.DEFAULT)
			.allowedTools(List.of("Bash"))
			.build();

		try (BidirectionalTransport transport = new BidirectionalTransport(workingDirectory(), Duration.ofMinutes(3),
				getClaudeCliPath())) {

			// Ask Claude to run a bash command
			transport.startSession("Run: echo 'testing hooks'", options, message -> {
				if (message.isRegularMessage() && message.asMessage() instanceof ResultMessage) {
					resultLatch.countDown();
				}
			}, request -> {
				// Handle hook callbacks
				if (request.request() instanceof ControlRequest.HookCallbackRequest hookCallback) {
					Map<String, Object> input = hookCallback.input();
					String hookEventName = (String) input.get("hook_event_name");

					if ("PostToolUse".equals(hookEventName)) {
						System.out.println("PostToolUse hook providing additional context");
						hookInvocations.add("context_added");

						// Return hookSpecificOutput with additional context
						return ControlResponse.success(request.requestId(),
								Map.of("systemMessage", "Additional context provided by hook", "reason",
										"Hook providing monitoring feedback", "suppressOutput", false,
										"hookSpecificOutput",
										Map.of("hookEventName", "PostToolUse", "additionalContext",
												"The command executed successfully with hook monitoring")));
					}

					// For PreToolUse, allow
					return ControlResponse.success(request.requestId(),
							Map.of("hookSpecificOutput", Map.of("permissionDecision", "allow")));
				}

				// Handle CanUseToolRequest
				if (request.request() instanceof ControlRequest.CanUseToolRequest) {
					return ControlResponse.success(request.requestId(), Map.of("behavior", "allow"));
				}

				// Default allow
				return ControlResponse.success(request.requestId(), Map.of("behavior", "allow"));
			});

			// Wait for completion
			boolean completed = resultLatch.await(120, TimeUnit.SECONDS);
			assertThat(completed).as("Should complete within timeout").isTrue();

			// Assert: "context_added" in hook_invocations
			System.out.println("Hook invocations: " + hookInvocations);
			assertThat(hookInvocations).as("Hook with hookSpecificOutput should have been invoked")
				.contains("context_added");
		}
	}

	/**
	 * Tests hook config generation matches expected structure.
	 */
	@Test
	@DisplayName("Hook config generation matches CLI expectations")
	void hookConfigGenerationMatchesCLIExpectations() {
		// Given - create a hook registry with registrations
		HookRegistry registry = new HookRegistry();
		registry.registerPreToolUse("Bash", input -> HookOutput.allow());
		registry.registerPostToolUse(null, input -> HookOutput.allow());

		// Build the hook config
		Map<String, List<ControlRequest.HookMatcherConfig>> hookConfig = registry.buildHookConfig();

		// Then - verify config structure
		assertThat(hookConfig).containsKey("PreToolUse");
		assertThat(hookConfig).containsKey("PostToolUse");

		// PreToolUse should have Bash pattern
		List<ControlRequest.HookMatcherConfig> preToolUseHooks = hookConfig.get("PreToolUse");
		assertThat(preToolUseHooks).hasSize(1);
		assertThat(preToolUseHooks.get(0).matcher()).isEqualTo("Bash");

		// PostToolUse should match all (null pattern converted to .*)
		List<ControlRequest.HookMatcherConfig> postToolUseHooks = hookConfig.get("PostToolUse");
		assertThat(postToolUseHooks).hasSize(1);
		assertThat(postToolUseHooks.get(0).matcher()).isEqualTo(".*");
	}

}
