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

import java.util.Map;
import java.util.Optional;

/**
 * Base sealed interface for all hook input types. Each hook event receives a specific
 * input type with relevant data.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "hook_event_name",
		visible = true)
@JsonSubTypes({ @JsonSubTypes.Type(value = HookInput.PreToolUseInput.class, name = "PreToolUse"),
		@JsonSubTypes.Type(value = HookInput.PostToolUseInput.class, name = "PostToolUse"),
		@JsonSubTypes.Type(value = HookInput.UserPromptSubmitInput.class, name = "UserPromptSubmit"),
		@JsonSubTypes.Type(value = HookInput.StopInput.class, name = "Stop"),
		@JsonSubTypes.Type(value = HookInput.SubagentStopInput.class, name = "SubagentStop"),
		@JsonSubTypes.Type(value = HookInput.PreCompactInput.class, name = "PreCompact") })
public sealed interface HookInput {

	/**
	 * Get the hook event name.
	 */
	String hookEventName();

	/**
	 * Get the session ID.
	 */
	String sessionId();

	/**
	 * Get the transcript path.
	 */
	String transcriptPath();

	/**
	 * Get the current working directory.
	 */
	String cwd();

	/**
	 * Get the permission mode (optional).
	 */
	Optional<String> permissionMode();

	/**
	 * Input for PreToolUse hook - called before a tool is executed.
	 */
	record PreToolUseInput(@JsonProperty("hook_event_name") String hookEventName,
			@JsonProperty("session_id") String sessionId, @JsonProperty("transcript_path") String transcriptPath,
			@JsonProperty("cwd") String cwd, @JsonProperty("permission_mode") String permissionModeValue,
			@JsonProperty("tool_name") String toolName,
			@JsonProperty("tool_input") Map<String, Object> toolInput) implements HookInput {

		@Override
		public Optional<String> permissionMode() {
			return Optional.ofNullable(permissionModeValue);
		}

		/**
		 * Get a typed argument from tool input.
		 */
		@SuppressWarnings("unchecked")
		public <T> Optional<T> getArgument(String name, Class<T> type) {
			Object value = toolInput.get(name);
			if (value != null && type.isInstance(value)) {
				return Optional.of((T) value);
			}
			return Optional.empty();
		}
	}

	/**
	 * Input for PostToolUse hook - called after a tool is executed.
	 */
	record PostToolUseInput(@JsonProperty("hook_event_name") String hookEventName,
			@JsonProperty("session_id") String sessionId, @JsonProperty("transcript_path") String transcriptPath,
			@JsonProperty("cwd") String cwd, @JsonProperty("permission_mode") String permissionModeValue,
			@JsonProperty("tool_name") String toolName, @JsonProperty("tool_input") Map<String, Object> toolInput,
			@JsonProperty("tool_response") Object toolResponse) implements HookInput {

		@Override
		public Optional<String> permissionMode() {
			return Optional.ofNullable(permissionModeValue);
		}
	}

	/**
	 * Input for UserPromptSubmit hook - called before user prompt is sent.
	 */
	record UserPromptSubmitInput(@JsonProperty("hook_event_name") String hookEventName,
			@JsonProperty("session_id") String sessionId, @JsonProperty("transcript_path") String transcriptPath,
			@JsonProperty("cwd") String cwd, @JsonProperty("permission_mode") String permissionModeValue,
			@JsonProperty("prompt") String prompt) implements HookInput {

		@Override
		public Optional<String> permissionMode() {
			return Optional.ofNullable(permissionModeValue);
		}
	}

	/**
	 * Input for Stop hook - called when agent stops.
	 */
	record StopInput(@JsonProperty("hook_event_name") String hookEventName,
			@JsonProperty("session_id") String sessionId, @JsonProperty("transcript_path") String transcriptPath,
			@JsonProperty("cwd") String cwd, @JsonProperty("permission_mode") String permissionModeValue,
			@JsonProperty("stop_hook_active") boolean stopHookActive) implements HookInput {

		@Override
		public Optional<String> permissionMode() {
			return Optional.ofNullable(permissionModeValue);
		}
	}

	/**
	 * Input for SubagentStop hook - called when subagent stops.
	 */
	record SubagentStopInput(@JsonProperty("hook_event_name") String hookEventName,
			@JsonProperty("session_id") String sessionId, @JsonProperty("transcript_path") String transcriptPath,
			@JsonProperty("cwd") String cwd, @JsonProperty("permission_mode") String permissionModeValue,
			@JsonProperty("stop_hook_active") boolean stopHookActive) implements HookInput {

		@Override
		public Optional<String> permissionMode() {
			return Optional.ofNullable(permissionModeValue);
		}
	}

	/**
	 * Input for PreCompact hook - called before context compaction.
	 */
	record PreCompactInput(@JsonProperty("hook_event_name") String hookEventName,
			@JsonProperty("session_id") String sessionId, @JsonProperty("transcript_path") String transcriptPath,
			@JsonProperty("cwd") String cwd, @JsonProperty("permission_mode") String permissionModeValue,
			@JsonProperty("trigger") String trigger,
			@JsonProperty("custom_instructions") String customInstructions) implements HookInput {

		@Override
		public Optional<String> permissionMode() {
			return Optional.ofNullable(permissionModeValue);
		}

		/**
		 * Check if this was a manual trigger.
		 */
		public boolean isManualTrigger() {
			return "manual".equals(trigger);
		}

		/**
		 * Check if this was an automatic trigger.
		 */
		public boolean isAutoTrigger() {
			return "auto".equals(trigger);
		}
	}

}
