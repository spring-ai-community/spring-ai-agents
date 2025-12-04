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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;
import java.util.Map;

/**
 * Control request wrapper for bidirectional communication with Claude CLI. The CLI sends
 * these requests to the SDK for permission checks, hook callbacks, etc.
 */
public record ControlRequest(@JsonProperty("type") String type, @JsonProperty("request_id") String requestId,
		@JsonProperty("request") ControlRequestPayload request) {

	public static final String TYPE = "control_request";

	/**
	 * Sealed interface for control request payload types.
	 */
	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "subtype")
	@JsonSubTypes({ @JsonSubTypes.Type(value = InitializeRequest.class, name = "initialize"),
			@JsonSubTypes.Type(value = CanUseToolRequest.class, name = "can_use_tool"),
			@JsonSubTypes.Type(value = HookCallbackRequest.class, name = "hook_callback"),
			@JsonSubTypes.Type(value = InterruptRequest.class, name = "interrupt"),
			@JsonSubTypes.Type(value = SetPermissionModeRequest.class, name = "set_permission_mode"),
			@JsonSubTypes.Type(value = SetModelRequest.class, name = "set_model"),
			@JsonSubTypes.Type(value = McpMessageRequest.class, name = "mcp_message") })
	public sealed interface ControlRequestPayload permits InitializeRequest, CanUseToolRequest, HookCallbackRequest,
			InterruptRequest, SetPermissionModeRequest, SetModelRequest, McpMessageRequest {

		String subtype();

	}

	/**
	 * Initialize request - sent by SDK to register hooks at startup.
	 */
	public record InitializeRequest(
			@JsonProperty("hooks") Map<String, List<HookMatcherConfig>> hooks) implements ControlRequestPayload {
		@Override
		public String subtype() {
			return "initialize";
		}
	}

	/**
	 * Hook matcher configuration sent during initialization.
	 */
	public record HookMatcherConfig(@JsonProperty("matcher") String matcher,
			@JsonProperty("hookCallbackIds") List<String> hookCallbackIds, @JsonProperty("timeout") Integer timeout) {
	}

	/**
	 * Permission request - CLI asks SDK if a tool can be used.
	 */
	public record CanUseToolRequest(@JsonProperty("tool_name") String toolName,
			@JsonProperty("input") Map<String, Object> input,
			@JsonProperty("permission_suggestions") List<Map<String, Object>> permissionSuggestions,
			@JsonProperty("blocked_path") String blockedPath) implements ControlRequestPayload {
		@Override
		public String subtype() {
			return "can_use_tool";
		}
	}

	/**
	 * Hook callback request - CLI asks SDK to execute a registered hook.
	 */
	public record HookCallbackRequest(@JsonProperty("callback_id") String callbackId,
			@JsonProperty("input") Map<String, Object> input,
			@JsonProperty("tool_use_id") String toolUseId) implements ControlRequestPayload {
		@Override
		public String subtype() {
			return "hook_callback";
		}
	}

	/**
	 * Interrupt request - SDK tells CLI to stop execution.
	 */
	public record InterruptRequest() implements ControlRequestPayload {
		@Override
		public String subtype() {
			return "interrupt";
		}
	}

	/**
	 * Set permission mode request - SDK changes permission mode dynamically.
	 */
	public record SetPermissionModeRequest(@JsonProperty("mode") String mode) implements ControlRequestPayload {
		@Override
		public String subtype() {
			return "set_permission_mode";
		}
	}

	/**
	 * Set model request - SDK changes model dynamically.
	 */
	public record SetModelRequest(@JsonProperty("model") String model) implements ControlRequestPayload {
		@Override
		public String subtype() {
			return "set_model";
		}
	}

	/**
	 * MCP message request - routes JSON-RPC messages to in-process SDK MCP servers.
	 * <p>
	 * The CLI sends this when it needs to invoke tools on SDK-managed MCP servers. The
	 * message field contains a JSON-RPC 2.0 request (with method, params, id fields).
	 *
	 * @param serverName the MCP server name (matches key in mcpServers config)
	 * @param message the JSON-RPC 2.0 message (request or notification)
	 */
	public record McpMessageRequest(@JsonProperty("server_name") String serverName,
			@JsonProperty("message") Map<String, Object> message) implements ControlRequestPayload {
		@Override
		public String subtype() {
			return "mcp_message";
		}

		/**
		 * Extracts the JSON-RPC method name from the message.
		 * @return the method name, or null if not present
		 */
		public String getMethod() {
			return message != null ? (String) message.get("method") : null;
		}

		/**
		 * Extracts the JSON-RPC request ID from the message.
		 * @return the request ID, or null if not present (notification)
		 */
		public Object getId() {
			return message != null ? message.get("id") : null;
		}

		/**
		 * Extracts the JSON-RPC params from the message.
		 * @return the params map, or null if not present
		 */
		@SuppressWarnings("unchecked")
		public Map<String, Object> getParams() {
			return message != null ? (Map<String, Object>) message.get("params") : null;
		}
	}

	/**
	 * Check if this is a control request by type.
	 */
	public boolean isControlRequest() {
		return TYPE.equals(type);
	}
}
