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

package org.springaicommunity.agents.claude.sdk.types.control;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for control message serialization and deserialization. These tests
 * verify that the Java types correctly map to the JSON protocol used by Claude CLI.
 *
 * @author Spring AI Community
 */
class ControlMessageSerializationTest {

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	@Nested
	@DisplayName("ControlRequest Tests")
	class ControlRequestTests {

		@Test
		@DisplayName("Should serialize InitializeRequest with hooks")
		void serializeInitializeRequest() throws JsonProcessingException {
			// Given
			var hookConfig = new ControlRequest.HookMatcherConfig("Bash|Write", List.of("hook_0", "hook_1"), 60);
			var initRequest = new ControlRequest.InitializeRequest(Map.of("PreToolUse", List.of(hookConfig)));
			var controlRequest = new ControlRequest("control_request", "req_1_abc123", initRequest);

			// When
			String json = objectMapper.writeValueAsString(controlRequest);

			// Then
			assertThat(json).contains("\"type\":\"control_request\"");
			assertThat(json).contains("\"request_id\":\"req_1_abc123\"");
			assertThat(json).contains("\"subtype\":\"initialize\"");
			assertThat(json).contains("\"matcher\":\"Bash|Write\"");
			assertThat(json).contains("\"hookCallbackIds\":[\"hook_0\",\"hook_1\"]");
		}

		@Test
		@DisplayName("Should deserialize HookCallbackRequest from CLI")
		void deserializeHookCallbackRequest() throws JsonProcessingException {
			// Given - exact format from CLI protocol
			String json = """
					{
					  "type": "control_request",
					  "request_id": "req_3_ghi789",
					  "request": {
					    "subtype": "hook_callback",
					    "callback_id": "hook_0",
					    "input": {
					      "hook_event_name": "PreToolUse",
					      "tool_name": "Bash",
					      "tool_input": {"command": "echo 'hello'"},
					      "session_id": "sess_xyz",
					      "transcript_path": "/tmp/t.md",
					      "cwd": "/home/user"
					    },
					    "tool_use_id": "tool_123"
					  }
					}
					""";

			// When
			ControlRequest request = objectMapper.readValue(json, ControlRequest.class);

			// Then
			assertThat(request.type()).isEqualTo("control_request");
			assertThat(request.requestId()).isEqualTo("req_3_ghi789");
			assertThat(request.request()).isInstanceOf(ControlRequest.HookCallbackRequest.class);

			var hookCallback = (ControlRequest.HookCallbackRequest) request.request();
			assertThat(hookCallback.callbackId()).isEqualTo("hook_0");
			assertThat(hookCallback.toolUseId()).isEqualTo("tool_123");
			assertThat(hookCallback.input()).containsEntry("tool_name", "Bash");
		}

		@Test
		@DisplayName("Should deserialize CanUseToolRequest from CLI")
		void deserializeCanUseToolRequest() throws JsonProcessingException {
			// Given - exact format from CLI protocol
			String json = """
					{
					  "type": "control_request",
					  "request_id": "req_2_def456",
					  "request": {
					    "subtype": "can_use_tool",
					    "tool_name": "Write",
					    "input": {
					      "path": "/home/user/file.txt",
					      "file_text": "Hello world"
					    },
					    "permission_suggestions": [
					      {
					        "type": "addRules",
					        "rules": [{"toolName": "Write", "ruleContent": "*.txt"}],
					        "behavior": "allow",
					        "destination": "session"
					      }
					    ],
					    "blocked_path": null
					  }
					}
					""";

			// When
			ControlRequest request = objectMapper.readValue(json, ControlRequest.class);

			// Then
			assertThat(request.request()).isInstanceOf(ControlRequest.CanUseToolRequest.class);

			var canUseTool = (ControlRequest.CanUseToolRequest) request.request();
			assertThat(canUseTool.toolName()).isEqualTo("Write");
			assertThat(canUseTool.input()).containsEntry("path", "/home/user/file.txt");
			assertThat(canUseTool.permissionSuggestions()).hasSize(1);
			assertThat(canUseTool.blockedPath()).isNull();
		}

		@Test
		@DisplayName("Should serialize InterruptRequest")
		void serializeInterruptRequest() throws JsonProcessingException {
			// Given
			var interruptRequest = new ControlRequest.InterruptRequest();
			var controlRequest = new ControlRequest("control_request", "req_4_xyz", interruptRequest);

			// When
			String json = objectMapper.writeValueAsString(controlRequest);

			// Then
			assertThat(json).contains("\"subtype\":\"interrupt\"");
		}

		@Test
		@DisplayName("Should serialize SetPermissionModeRequest")
		void serializeSetPermissionModeRequest() throws JsonProcessingException {
			// Given
			var request = new ControlRequest.SetPermissionModeRequest("acceptEdits");
			var controlRequest = new ControlRequest("control_request", "req_5_abc", request);

			// When
			String json = objectMapper.writeValueAsString(controlRequest);

			// Then
			assertThat(json).contains("\"subtype\":\"set_permission_mode\"");
			assertThat(json).contains("\"mode\":\"acceptEdits\"");
		}

		@Test
		@DisplayName("Should serialize McpMessageRequest")
		void serializeMcpMessageRequest() throws JsonProcessingException {
			// Given
			var request = new ControlRequest.McpMessageRequest("my_mcp_server",
					Map.of("jsonrpc", "2.0", "id", 1, "method", "tools/list"));
			var controlRequest = new ControlRequest("control_request", "req_7_mcp", request);

			// When
			String json = objectMapper.writeValueAsString(controlRequest);

			// Then
			assertThat(json).contains("\"subtype\":\"mcp_message\"");
			assertThat(json).contains("\"server_name\":\"my_mcp_server\"");
		}

	}

	@Nested
	@DisplayName("ControlResponse Tests")
	class ControlResponseTests {

		@Test
		@DisplayName("Should serialize success response")
		void serializeSuccessResponse() throws JsonProcessingException {
			// Given
			var response = ControlResponse.success("req_1_abc", Map.of("status", "ok"));

			// When
			String json = objectMapper.writeValueAsString(response);

			// Then
			assertThat(json).contains("\"type\":\"control_response\"");
			assertThat(json).contains("\"subtype\":\"success\"");
			assertThat(json).contains("\"request_id\":\"req_1_abc\"");
			assertThat(json).contains("\"status\":\"ok\"");
		}

		@Test
		@DisplayName("Should serialize error response")
		void serializeErrorResponse() throws JsonProcessingException {
			// Given
			var response = ControlResponse.error("req_2_def", "Something went wrong");

			// When
			String json = objectMapper.writeValueAsString(response);

			// Then
			assertThat(json).contains("\"type\":\"control_response\"");
			assertThat(json).contains("\"subtype\":\"error\"");
			assertThat(json).contains("\"request_id\":\"req_2_def\"");
			assertThat(json).contains("\"error\":\"Something went wrong\"");
		}

		@Test
		@DisplayName("Should deserialize success response")
		void deserializeSuccessResponse() throws JsonProcessingException {
			// Given
			String json = """
					{
					  "type": "control_response",
					  "response": {
					    "subtype": "success",
					    "request_id": "req_1_abc",
					    "response": {"supportedCommands": ["hook_callback", "can_use_tool"]}
					  }
					}
					""";

			// When
			ControlResponse response = objectMapper.readValue(json, ControlResponse.class);

			// Then
			assertThat(response.type()).isEqualTo("control_response");
			assertThat(response.response()).isInstanceOf(ControlResponse.SuccessPayload.class);

			var payload = (ControlResponse.SuccessPayload) response.response();
			assertThat(payload.requestId()).isEqualTo("req_1_abc");
		}

	}

	@Nested
	@DisplayName("HookEvent Tests")
	class HookEventTests {

		@Test
		@DisplayName("Should have correct protocol names")
		void protocolNames() {
			assertThat(HookEvent.PRE_TOOL_USE.getProtocolName()).isEqualTo("PreToolUse");
			assertThat(HookEvent.POST_TOOL_USE.getProtocolName()).isEqualTo("PostToolUse");
			assertThat(HookEvent.USER_PROMPT_SUBMIT.getProtocolName()).isEqualTo("UserPromptSubmit");
			assertThat(HookEvent.STOP.getProtocolName()).isEqualTo("Stop");
			assertThat(HookEvent.SUBAGENT_STOP.getProtocolName()).isEqualTo("SubagentStop");
			assertThat(HookEvent.PRE_COMPACT.getProtocolName()).isEqualTo("PreCompact");
		}

		@Test
		@DisplayName("Should parse from protocol name")
		void parseFromProtocolName() {
			assertThat(HookEvent.fromProtocolName("PreToolUse")).isEqualTo(HookEvent.PRE_TOOL_USE);
			assertThat(HookEvent.fromProtocolName("PostToolUse")).isEqualTo(HookEvent.POST_TOOL_USE);
		}

	}

	@Nested
	@DisplayName("HookInput Tests")
	class HookInputTests {

		@Test
		@DisplayName("Should deserialize PreToolUseInput")
		void deserializePreToolUseInput() throws JsonProcessingException {
			// Given
			String json = """
					{
					  "hook_event_name": "PreToolUse",
					  "session_id": "sess_abc",
					  "transcript_path": "/tmp/transcript.md",
					  "cwd": "/home/user",
					  "permission_mode": "default",
					  "tool_name": "Bash",
					  "tool_input": {"command": "ls -la"}
					}
					""";

			// When
			HookInput input = objectMapper.readValue(json, HookInput.class);

			// Then
			assertThat(input).isInstanceOf(HookInput.PreToolUseInput.class);

			var preToolUse = (HookInput.PreToolUseInput) input;
			assertThat(preToolUse.hookEventName()).isEqualTo("PreToolUse");
			assertThat(preToolUse.sessionId()).isEqualTo("sess_abc");
			assertThat(preToolUse.toolName()).isEqualTo("Bash");
			assertThat(preToolUse.toolInput()).containsEntry("command", "ls -la");
			assertThat(preToolUse.permissionMode()).contains("default");
		}

		@Test
		@DisplayName("Should deserialize PostToolUseInput")
		void deserializePostToolUseInput() throws JsonProcessingException {
			// Given
			String json = """
					{
					  "hook_event_name": "PostToolUse",
					  "session_id": "sess_abc",
					  "transcript_path": "/tmp/transcript.md",
					  "cwd": "/home/user",
					  "tool_name": "Bash",
					  "tool_input": {"command": "ls -la"},
					  "tool_response": {"output": "file1.txt\\nfile2.txt"}
					}
					""";

			// When
			HookInput input = objectMapper.readValue(json, HookInput.class);

			// Then
			assertThat(input).isInstanceOf(HookInput.PostToolUseInput.class);

			var postToolUse = (HookInput.PostToolUseInput) input;
			assertThat(postToolUse.toolName()).isEqualTo("Bash");
			assertThat(postToolUse.toolResponse()).isNotNull();
		}

		@Test
		@DisplayName("Should deserialize UserPromptSubmitInput")
		void deserializeUserPromptSubmitInput() throws JsonProcessingException {
			// Given
			String json = """
					{
					  "hook_event_name": "UserPromptSubmit",
					  "session_id": "sess_abc",
					  "transcript_path": "/tmp/transcript.md",
					  "cwd": "/home/user",
					  "prompt": "Hello, Claude!"
					}
					""";

			// When
			HookInput input = objectMapper.readValue(json, HookInput.class);

			// Then
			assertThat(input).isInstanceOf(HookInput.UserPromptSubmitInput.class);

			var userPrompt = (HookInput.UserPromptSubmitInput) input;
			assertThat(userPrompt.prompt()).isEqualTo("Hello, Claude!");
		}

		@Test
		@DisplayName("Should deserialize PreCompactInput")
		void deserializePreCompactInput() throws JsonProcessingException {
			// Given
			String json = """
					{
					  "hook_event_name": "PreCompact",
					  "session_id": "sess_abc",
					  "transcript_path": "/tmp/transcript.md",
					  "cwd": "/home/user",
					  "trigger": "auto",
					  "custom_instructions": "Focus on code"
					}
					""";

			// When
			HookInput input = objectMapper.readValue(json, HookInput.class);

			// Then
			assertThat(input).isInstanceOf(HookInput.PreCompactInput.class);

			var preCompact = (HookInput.PreCompactInput) input;
			assertThat(preCompact.trigger()).isEqualTo("auto");
			assertThat(preCompact.isAutoTrigger()).isTrue();
			assertThat(preCompact.isManualTrigger()).isFalse();
		}

		@Test
		@DisplayName("PreToolUseInput should extract typed arguments")
		void preToolUseInputTypedArguments() {
			// Given
			var input = new HookInput.PreToolUseInput("PreToolUse", "sess_1", "/tmp/t.md", "/home", null, "Write",
					"tool_123", Map.of("file_path", "/home/test.txt", "content", "hello"));

			// When/Then
			assertThat(input.getArgument("file_path", String.class)).contains("/home/test.txt");
			assertThat(input.getArgument("missing", String.class)).isEmpty();
		}

	}

	@Nested
	@DisplayName("HookOutput Tests")
	class HookOutputTests {

		@Test
		@DisplayName("Should serialize continue execution output")
		void serializeContinueOutput() throws JsonProcessingException {
			// Given
			var output = HookOutput.allow();

			// When
			String json = objectMapper.writeValueAsString(output);

			// Then
			assertThat(json).contains("\"continue\":true");
		}

		@Test
		@DisplayName("Should serialize block output")
		void serializeBlockOutput() throws JsonProcessingException {
			// Given
			var output = HookOutput.block("Not allowed");

			// When
			String json = objectMapper.writeValueAsString(output);

			// Then
			assertThat(json).contains("\"continue\":false");
			assertThat(json).contains("\"decision\":\"block\"");
			assertThat(json).contains("\"reason\":\"Not allowed\"");
		}

		@Test
		@DisplayName("Should serialize output with PreToolUse specific data")
		void serializePreToolUseSpecificOutput() throws JsonProcessingException {
			// Given
			var output = HookOutput.builder()
				.continueExecution(true)
				.hookSpecificOutput(HookOutput.HookSpecificOutput.preToolUseAllow("Approved"))
				.build();

			// When
			String json = objectMapper.writeValueAsString(output);

			// Then
			assertThat(json).contains("\"hookEventName\":\"PreToolUse\"");
			assertThat(json).contains("\"permissionDecision\":\"allow\"");
			assertThat(json).contains("\"permissionDecisionReason\":\"Approved\"");
		}

		@Test
		@DisplayName("Should serialize output with modified input")
		void serializeModifiedInput() throws JsonProcessingException {
			// Given
			var output = HookOutput.builder()
				.continueExecution(true)
				.hookSpecificOutput(
						HookOutput.HookSpecificOutput.preToolUseModify(Map.of("safe_mode", true, "timeout", 30)))
				.build();

			// When
			String json = objectMapper.writeValueAsString(output);

			// Then
			assertThat(json).contains("\"updatedInput\"");
			assertThat(json).contains("\"safe_mode\":true");
		}

		@Test
		@DisplayName("Should not include null fields")
		void nullFieldsExcluded() throws JsonProcessingException {
			// Given
			var output = HookOutput.builder().continueExecution(true).build();

			// When
			String json = objectMapper.writeValueAsString(output);

			// Then
			assertThat(json).doesNotContain("stopReason");
			assertThat(json).doesNotContain("systemMessage");
			assertThat(json).doesNotContain("hookSpecificOutput");
		}

		@Test
		@DisplayName("Round-trip serialization should preserve data")
		void roundTripSerialization() throws JsonProcessingException {
			// Given
			var original = HookOutput.builder()
				.continueExecution(true)
				.reason("Test reason")
				.hookSpecificOutput(HookOutput.HookSpecificOutput.preToolUseDeny("Blocked"))
				.build();

			// When
			String json = objectMapper.writeValueAsString(original);
			HookOutput deserialized = objectMapper.readValue(json, HookOutput.class);

			// Then
			assertThat(deserialized.continueExecution()).isEqualTo(original.continueExecution());
			assertThat(deserialized.reason()).isEqualTo(original.reason());
			assertThat(deserialized.hookSpecificOutput().permissionDecision()).isEqualTo("deny");
		}

		@Test
		@DisplayName("Should create async output without timeout")
		void asyncOutputWithoutTimeout() throws JsonProcessingException {
			// Given
			var output = HookOutput.async();

			// When
			String json = objectMapper.writeValueAsString(output);

			// Then
			assertThat(json).contains("\"async\":true");
			assertThat(json).contains("\"continue\":true");
			assertThat(json).doesNotContain("asyncTimeout");
		}

		@Test
		@DisplayName("Should create async output with timeout")
		void asyncOutputWithTimeout() throws JsonProcessingException {
			// Given
			var output = HookOutput.async(5000);

			// When
			String json = objectMapper.writeValueAsString(output);

			// Then
			assertThat(json).contains("\"async\":true");
			assertThat(json).contains("\"asyncTimeout\":5000");
			assertThat(json).contains("\"continue\":true");
		}

		@Test
		@DisplayName("Should serialize async output matching Python SDK format")
		void asyncOutputMatchesPythonFormat() throws JsonProcessingException {
			// Given - Python SDK uses async_ to avoid keyword, serializes as "async"
			var output = HookOutput.async(3000);

			// When
			String json = objectMapper.writeValueAsString(output);

			// Then - should use "async" in JSON, not "asyncExecution"
			assertThat(json).contains("\"async\":true");
			assertThat(json).doesNotContain("asyncExecution");
		}

	}

	@Nested
	@DisplayName("Protocol Compatibility Tests")
	class ProtocolCompatibilityTests {

		@Test
		@DisplayName("Full hook callback flow should match CLI protocol format")
		void fullHookCallbackFlow() throws JsonProcessingException {
			// Given - request from CLI
			String requestJson = """
					{
					  "type": "control_request",
					  "request_id": "req_3_ghi789",
					  "request": {
					    "subtype": "hook_callback",
					    "callback_id": "hook_0",
					    "input": {
					      "hook_event_name": "PreToolUse",
					      "tool_name": "Bash",
					      "tool_input": {"command": "rm -rf /"},
					      "session_id": "sess_xyz",
					      "transcript_path": "/tmp/t.md",
					      "cwd": "/home/user"
					    },
					    "tool_use_id": "tool_123"
					  }
					}
					""";

			// When - deserialize request
			ControlRequest request = objectMapper.readValue(requestJson, ControlRequest.class);

			// Then - verify request structure
			assertThat(request.request()).isInstanceOf(ControlRequest.HookCallbackRequest.class);
			var hookCallback = (ControlRequest.HookCallbackRequest) request.request();

			// When - create response (deny dangerous command)
			var output = HookOutput.builder()
				.continueExecution(false)
				.decision("block")
				.systemMessage("Dangerous command blocked")
				.reason("rm -rf / is not allowed")
				.hookSpecificOutput(HookOutput.HookSpecificOutput.preToolUseDeny("Dangerous command"))
				.build();

			var response = ControlResponse.success(request.requestId(), output);

			// Then - verify response format
			String responseJson = objectMapper.writeValueAsString(response);
			assertThat(responseJson).contains("\"type\":\"control_response\"");
			assertThat(responseJson).contains("\"request_id\":\"req_3_ghi789\"");
			assertThat(responseJson).contains("\"continue\":false");
			assertThat(responseJson).contains("\"decision\":\"block\"");
		}

	}

}
