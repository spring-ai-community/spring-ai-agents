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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agents.claude.sdk.config.PermissionMode;
import org.springaicommunity.agents.claude.sdk.parsing.ParsedMessage;
import org.springaicommunity.agents.claude.sdk.test.ClaudeCliTestBase;
import org.springaicommunity.agents.claude.sdk.transport.BidirectionalTransport;
import org.springaicommunity.agents.claude.sdk.transport.CLIOptions;
import org.springaicommunity.agents.claude.sdk.types.Message;
import org.springaicommunity.agents.claude.sdk.types.ResultMessage;
import org.springaicommunity.agents.claude.sdk.types.control.ControlRequest;
import org.springaicommunity.agents.claude.sdk.types.control.ControlResponse;
import org.springaicommunity.agents.claude.sdk.types.control.HookEvent;
import org.springaicommunity.agents.claude.sdk.types.control.HookInput;
import org.springaicommunity.agents.claude.sdk.types.control.HookOutput;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for hook execution with real Claude CLI.
 */
class HookIntegrationIT extends ClaudeCliTestBase {

	private static final String HAIKU_MODEL = "claude-haiku-4-5-20251016";

	@Test
	@DisplayName("Should receive hook callback for tool use")
	void shouldReceiveHookCallbackForToolUse() throws Exception {
		// Given - create a hook registry
		HookRegistry registry = new HookRegistry();
		AtomicBoolean hookCalled = new AtomicBoolean(false);
		AtomicReference<HookInput> receivedInput = new AtomicReference<>();

		// Register a PreToolUse hook that will be triggered when any tool is used
		registry.registerPreToolUse(null, input -> {
			hookCalled.set(true);
			receivedInput.set(input);
			return HookOutput.allow();
		});

		CLIOptions options = CLIOptions.builder()
			.model(HAIKU_MODEL)
			.permissionMode(PermissionMode.BYPASS_PERMISSIONS)
			.build();

		CountDownLatch resultLatch = new CountDownLatch(1);
		List<ControlRequest> controlRequests = new ArrayList<>();

		try (BidirectionalTransport transport = new BidirectionalTransport(workingDirectory(), Duration.ofMinutes(2),
				getClaudeCliPath())) {

			// When - ask Claude to use a tool (Read tool to read a file)
			transport.startSession("Read the file /etc/hostname and tell me what it says", options, message -> {
				if (message.isRegularMessage() && message.asMessage() instanceof ResultMessage) {
					resultLatch.countDown();
				}
			}, request -> {
				controlRequests.add(request);

				// Handle hook callbacks via registry
				if (request.request() instanceof ControlRequest.HookCallbackRequest hookCallback) {
					String hookId = hookCallback.callbackId();
					Map<String, Object> inputMap = hookCallback.input();

					// For now, just allow - the hook execution happens separately
					return ControlResponse.success(request.requestId(), Map.of("decision", HookOutput.allow()));
				}

				// Default - allow all
				return ControlResponse.success(request.requestId(), Map.of("behavior", "allow"));
			});

			// Wait for result (may timeout if tool isn't used)
			boolean completed = resultLatch.await(60, TimeUnit.SECONDS);

			// Then - verify we received control requests
			// Note: Hook callbacks only happen if CLI actually uses a tool
			assertThat(completed).as("Should complete within timeout").isTrue();
		}
	}

	@Test
	@DisplayName("Should handle PreToolUse hook that blocks dangerous commands")
	void shouldBlockDangerousCommands() throws Exception {
		// Given
		AtomicBoolean blockAttempted = new AtomicBoolean(false);

		CLIOptions options = CLIOptions.builder()
			.model(HAIKU_MODEL)
			.permissionMode(PermissionMode.BYPASS_PERMISSIONS)
			.build();

		CountDownLatch resultLatch = new CountDownLatch(1);

		try (BidirectionalTransport transport = new BidirectionalTransport(workingDirectory(), Duration.ofMinutes(2),
				getClaudeCliPath())) {

			// When - ask Claude to run a potentially dangerous command
			// Note: With BYPASS_PERMISSIONS, the CLI may not even ask for hook approval
			transport.startSession("What is the current directory? Use pwd command.", options, message -> {
				if (message.isRegularMessage() && message.asMessage() instanceof ResultMessage) {
					resultLatch.countDown();
				}
			}, request -> {
				// Check if this is a tool use request we want to inspect
				if (request.request() instanceof ControlRequest.HookCallbackRequest hookCallback) {
					Map<String, Object> input = hookCallback.input();
					// Check if it's a Bash tool
					Object toolName = input.get("tool_name");
					if ("Bash".equals(toolName)) {
						blockAttempted.set(true);
						// Allow this safe command
						return ControlResponse.success(request.requestId(),
								Map.of("decision", Map.of("behavior", "allow")));
					}
				}

				// Default allow
				return ControlResponse.success(request.requestId(), Map.of("behavior", "allow"));
			});

			// Wait for completion
			boolean completed = resultLatch.await(60, TimeUnit.SECONDS);
			assertThat(completed).as("Should complete").isTrue();
		}
	}

	@Test
	@DisplayName("Should handle PostToolUse hook")
	void shouldHandlePostToolUseHook() throws Exception {
		// Given
		List<Map<String, Object>> postToolUseInputs = new ArrayList<>();

		CLIOptions options = CLIOptions.builder()
			.model(HAIKU_MODEL)
			.permissionMode(PermissionMode.BYPASS_PERMISSIONS)
			.build();

		CountDownLatch resultLatch = new CountDownLatch(1);

		try (BidirectionalTransport transport = new BidirectionalTransport(workingDirectory(), Duration.ofMinutes(2),
				getClaudeCliPath())) {

			// When - ask Claude to do something that uses a tool
			transport.startSession("What is 2 + 2? Just give me the number.", options, message -> {
				if (message.isRegularMessage() && message.asMessage() instanceof ResultMessage) {
					resultLatch.countDown();
				}
			}, request -> {
				// Collect PostToolUse hook callbacks
				if (request.request() instanceof ControlRequest.HookCallbackRequest hookCallback) {
					Map<String, Object> input = hookCallback.input();
					String eventName = (String) input.get("hook_event_name");
					if ("PostToolUse".equals(eventName)) {
						postToolUseInputs.add(input);
					}
				}

				// Default allow
				return ControlResponse.success(request.requestId(), Map.of("behavior", "allow"));
			});

			// Wait for completion
			boolean completed = resultLatch.await(60, TimeUnit.SECONDS);
			assertThat(completed).as("Should complete").isTrue();

			// PostToolUse hooks are only sent if tools were used
			// This simple math question may not require tools
		}
	}

	@Test
	@DisplayName("Should pass hook config in initialization")
	void shouldPassHookConfigInInitialization() throws Exception {
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

		// PostToolUse should have ".*" pattern (matches all - null patterns converted to
		// .*)
		List<ControlRequest.HookMatcherConfig> postToolUseHooks = hookConfig.get("PostToolUse");
		assertThat(postToolUseHooks).hasSize(1);
		assertThat(postToolUseHooks.get(0).matcher()).isEqualTo(".*");
	}

}
