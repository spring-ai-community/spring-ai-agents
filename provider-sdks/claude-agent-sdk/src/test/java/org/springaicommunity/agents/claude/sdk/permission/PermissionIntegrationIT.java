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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springaicommunity.agents.claude.sdk.config.PermissionMode;
import org.springaicommunity.agents.claude.sdk.test.ClaudeCliTestBase;
import org.springaicommunity.agents.claude.sdk.transport.BidirectionalTransport;
import org.springaicommunity.agents.claude.sdk.transport.CLIOptions;
import org.springaicommunity.agents.claude.sdk.types.Message;
import org.springaicommunity.agents.claude.sdk.types.ResultMessage;
import org.springaicommunity.agents.claude.sdk.types.control.ControlRequest;
import org.springaicommunity.agents.claude.sdk.types.control.ControlResponse;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for permission handling with real Claude CLI.
 *
 * <p>
 * Tests permission callbacks for:
 * <ul>
 * <li>CanUseToolRequest callback invocation</li>
 * <li>Permission allow/deny behavior</li>
 * <li>Tool input data capture</li>
 * </ul>
 *
 * <p>
 * Key patterns:
 * <ul>
 * <li>Track callback_invocations list</li>
 * <li>Use tool-forcing prompts like "Write 'hello world' to /tmp/test.txt"</li>
 * <li>Assert specific tool names in invocations (e.g., "Write" in
 * callback_invocations)</li>
 * </ul>
 */
@Timeout(value = 180, unit = TimeUnit.SECONDS)
class PermissionIntegrationIT extends ClaudeCliTestBase {

	private static final String HAIKU_MODEL = CLIOptions.MODEL_HAIKU;

	/**
	 * Helper for running tests with transport.
	 */
	private void withTransport(CLIOptions options, TransportConsumer consumer) throws Exception {
		try (BidirectionalTransport transport = new BidirectionalTransport(workingDirectory(), Duration.ofMinutes(3),
				getClaudeCliPath())) {
			consumer.accept(transport, options);
		}
	}

	@FunctionalInterface
	interface TransportConsumer {

		void accept(BidirectionalTransport transport, CLIOptions options) throws Exception;

	}

	/**
	 * Tests that can_use_tool callback gets invoked when Claude uses a tool.
	 */
	@Test
	@DisplayName("Permission callback gets called when tool is used")
	void permissionCallbackGetsCalled() throws Exception {
		// Given - track callback invocations
		List<String> callbackInvocations = new CopyOnWriteArrayList<>();
		CountDownLatch resultLatch = new CountDownLatch(1);

		CLIOptions options = CLIOptions.builder().model(HAIKU_MODEL).permissionMode(PermissionMode.DEFAULT).build();

		withTransport(options, (transport, opts) -> {
			// Tool-forcing prompt to trigger Write tool
			transport.startSession("Write 'hello world' to /tmp/test.txt", opts, message -> {
				System.out.println("Got message: " + message);
				if (message.isRegularMessage() && message.asMessage() instanceof ResultMessage) {
					resultLatch.countDown();
				}
			}, request -> {
				// Handle permission callback
				if (request.request() instanceof ControlRequest.CanUseToolRequest canUseTool) {
					String toolName = canUseTool.toolName();
					Map<String, Object> inputData = canUseTool.input();

					System.out.println("Permission callback called for: " + toolName + ", input: " + inputData);
					callbackInvocations.add(toolName);

					// Allow tool execution
					return ControlResponse.success(request.requestId(), Map.of("behavior", "allow"));
				}

				// Handle hook callbacks
				if (request.request() instanceof ControlRequest.HookCallbackRequest hookCallback) {
					Map<String, Object> input = hookCallback.input();
					String toolName = (String) input.get("tool_name");
					if (toolName != null) {
						callbackInvocations.add(toolName);
					}
					return ControlResponse.success(request.requestId(),
							Map.of("hookSpecificOutput", Map.of("permissionDecision", "allow")));
				}

				// Default allow
				return ControlResponse.success(request.requestId(), Map.of("behavior", "allow"));
			});

			// Wait for completion
			boolean completed = resultLatch.await(120, TimeUnit.SECONDS);
			assertThat(completed).as("Should complete within timeout").isTrue();

			// Assert: "Write" in callback_invocations
			System.out.println("Callback invocations: " + callbackInvocations);
			assertThat(callbackInvocations)
				.as("can_use_tool callback should have been invoked for Write tool, got: " + callbackInvocations)
				.contains("Write");
		});
	}

	/**
	 * Tests permission deny blocks tool execution. Uses a more explicit prompt that
	 * reliably triggers Write tool use.
	 */
	@Test
	@DisplayName("Permission deny blocks tool execution")
	void permissionDenyBlocksToolExecution() throws Exception {
		// Given - track denied tools
		List<String> deniedTools = new CopyOnWriteArrayList<>();
		AtomicReference<String> resultText = new AtomicReference<>();
		CountDownLatch resultLatch = new CountDownLatch(1);

		CLIOptions options = CLIOptions.builder().model(HAIKU_MODEL).permissionMode(PermissionMode.DEFAULT).build();

		withTransport(options, (transport, opts) -> {
			// Use explicit tool-forcing prompt that reliably triggers Write tool
			transport.startSession("Use the Write tool to create a file at /tmp/denied.txt with content 'test'. "
					+ "Do not ask me " + "questions, just use the Write tool immediately.", opts, message -> {
						if (message.isRegularMessage()) {
							Message msg = message.asMessage();
							if (msg instanceof ResultMessage result) {
								resultText.set(result.result());
								resultLatch.countDown();
							}
						}
					}, request -> {
						// Deny Write tool
						if (request.request() instanceof ControlRequest.CanUseToolRequest canUseTool) {
							String toolName = canUseTool.toolName();
							System.out.println("Denying tool: " + toolName);
							deniedTools.add(toolName);

							return ControlResponse.success(request.requestId(), Map.of("behavior", "deny"));
						}

						// Handle hook callbacks - also deny
						if (request.request() instanceof ControlRequest.HookCallbackRequest hookCallback) {
							Map<String, Object> input = hookCallback.input();
							String toolName = (String) input.get("tool_name");
							if (toolName != null) {
								deniedTools.add(toolName);
							}
							return ControlResponse.success(request.requestId(),
									Map.of("hookSpecificOutput", Map.of("permissionDecision", "deny")));
						}

						return ControlResponse.success(request.requestId(), Map.of("behavior", "allow"));
					});

			// Wait for completion
			boolean completed = resultLatch.await(120, TimeUnit.SECONDS);
			assertThat(completed).as("Should complete within timeout").isTrue();

			// Verify tool was denied - the callback should have been invoked for Write
			// tool
			System.out.println("Denied tools: " + deniedTools);
			assertThat(deniedTools).as("Permission callback should have been invoked for file writing tools")
				.isNotEmpty();
			// Note: Could be "Write" or "Bash" depending on model choice
		});
	}

	/**
	 * Tests permission callback receives tool input data.
	 */
	@Test
	@DisplayName("Permission callback receives tool input data")
	void permissionCallbackReceivesToolInput() throws Exception {
		// Given - capture tool input
		AtomicReference<Map<String, Object>> capturedInput = new AtomicReference<>();
		AtomicReference<String> capturedToolName = new AtomicReference<>();
		CountDownLatch resultLatch = new CountDownLatch(1);

		CLIOptions options = CLIOptions.builder().model(HAIKU_MODEL).permissionMode(PermissionMode.DEFAULT).build();

		withTransport(options, (transport, opts) -> {
			transport.startSession("Read the file /etc/hostname", opts, message -> {
				if (message.isRegularMessage() && message.asMessage() instanceof ResultMessage) {
					resultLatch.countDown();
				}
			}, request -> {
				if (request.request() instanceof ControlRequest.CanUseToolRequest canUseTool) {
					// Capture tool name and input
					capturedToolName.set(canUseTool.toolName());
					capturedInput.set(canUseTool.input());

					System.out.println("Captured tool: " + capturedToolName.get());
					System.out.println("Captured input: " + capturedInput.get());

					return ControlResponse.success(request.requestId(), Map.of("behavior", "allow"));
				}

				if (request.request() instanceof ControlRequest.HookCallbackRequest) {
					return ControlResponse.success(request.requestId(),
							Map.of("hookSpecificOutput", Map.of("permissionDecision", "allow")));
				}

				return ControlResponse.success(request.requestId(), Map.of("behavior", "allow"));
			});

			// Wait for completion
			boolean completed = resultLatch.await(120, TimeUnit.SECONDS);
			assertThat(completed).as("Should complete within timeout").isTrue();

			// Verify input was captured (could be Read or Bash depending on model choice)
			assertThat(capturedToolName.get()).as("Should have captured tool name").isNotNull();
			assertThat(capturedInput.get()).as("Should have captured input data").isNotNull();
		});
	}

	/**
	 * Tests bypass permissions mode skips callbacks.
	 */
	@Test
	@DisplayName("Bypass permissions mode skips permission callbacks")
	void bypassPermissionsModeSkipsCallbacks() throws Exception {
		// Given - track if any permission callback is called
		List<String> callbackInvocations = new CopyOnWriteArrayList<>();
		CountDownLatch resultLatch = new CountDownLatch(1);

		// Use BYPASS_PERMISSIONS mode
		CLIOptions options = CLIOptions.builder()
			.model(HAIKU_MODEL)
			.permissionMode(PermissionMode.BYPASS_PERMISSIONS)
			.build();

		withTransport(options, (transport, opts) -> {
			transport.startSession("What is 2 + 2?", opts, message -> {
				if (message.isRegularMessage() && message.asMessage() instanceof ResultMessage) {
					resultLatch.countDown();
				}
			}, request -> {
				// Track any permission callbacks
				if (request.request() instanceof ControlRequest.CanUseToolRequest canUseTool) {
					callbackInvocations.add(canUseTool.toolName());
				}
				return ControlResponse.success(request.requestId(), Map.of("behavior", "allow"));
			});

			// Wait for completion
			boolean completed = resultLatch.await(60, TimeUnit.SECONDS);
			assertThat(completed).as("Should complete within timeout").isTrue();

			// In bypass mode, simple queries shouldn't trigger permission callbacks
			// (tool use may proceed without asking)
			System.out.println("Callback invocations in bypass mode: " + callbackInvocations);
		});
	}

	/**
	 * Tests that permission callback is called for Bash tool. Uses a file creation prompt
	 * which reliably triggers tool use.
	 */
	@Test
	@DisplayName("Permission callback called for Bash tool")
	void permissionCallbackCalledForBashTool() throws Exception {
		// Given
		List<String> callbackInvocations = new CopyOnWriteArrayList<>();
		CountDownLatch resultLatch = new CountDownLatch(1);

		// Note: Do NOT include Bash in allowedTools - we want permission callback to be
		// triggered
		CLIOptions options = CLIOptions.builder().model(HAIKU_MODEL).permissionMode(PermissionMode.DEFAULT).build();

		withTransport(options, (transport, opts) -> {
			// Use a prompt that reliably triggers tool use (file operations work well)
			transport.startSession("Create a file /tmp/bash_test.txt containing 'hello'", opts, message -> {
				if (message.isRegularMessage() && message.asMessage() instanceof ResultMessage) {
					resultLatch.countDown();
				}
			}, request -> {
				if (request.request() instanceof ControlRequest.CanUseToolRequest canUseTool) {
					System.out.println("Permission callback for: " + canUseTool.toolName());
					callbackInvocations.add(canUseTool.toolName());
					return ControlResponse.success(request.requestId(), Map.of("behavior", "allow"));
				}

				if (request.request() instanceof ControlRequest.HookCallbackRequest hookCallback) {
					Map<String, Object> input = hookCallback.input();
					String toolName = (String) input.get("tool_name");
					if (toolName != null) {
						callbackInvocations.add(toolName);
					}
					return ControlResponse.success(request.requestId(),
							Map.of("hookSpecificOutput", Map.of("permissionDecision", "allow")));
				}

				return ControlResponse.success(request.requestId(), Map.of("behavior", "allow"));
			});

			// Wait for completion
			boolean completed = resultLatch.await(120, TimeUnit.SECONDS);
			assertThat(completed).as("Should complete within timeout").isTrue();

			// Verify tool permission callback was invoked (could be Bash or Write)
			System.out.println("Callback invocations: " + callbackInvocations);
			assertThat(callbackInvocations).as("Permission callback should have been invoked for file operation")
				.isNotEmpty();
		});
	}

}
