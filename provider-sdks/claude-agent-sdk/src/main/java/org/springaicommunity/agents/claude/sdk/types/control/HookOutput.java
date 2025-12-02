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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Output from a hook execution. Sent back to CLI as part of control response.
 *
 * <p>
 * Field naming note: Java uses camelCase but the protocol uses snake_case.
 * Jackson @JsonProperty handles the conversion. Additionally, "continue" and "async" are
 * Java keywords, so we use alternative names that get serialized correctly.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HookOutput(
		// Control fields - note: "continue" is a Java keyword, so we use a workaround
		@JsonProperty("continue") Boolean continueExecution, @JsonProperty("suppressOutput") Boolean suppressOutput,
		@JsonProperty("stopReason") String stopReason,

		// Decision fields
		@JsonProperty("decision") String decision, @JsonProperty("systemMessage") String systemMessage,
		@JsonProperty("reason") String reason,

		// Async support - note: "async" is a Java keyword in some contexts
		@JsonProperty("async") Boolean asyncExecution, @JsonProperty("asyncTimeout") Integer asyncTimeout,

		// Hook-specific output
		@JsonProperty("hookSpecificOutput") HookSpecificOutput hookSpecificOutput) {

	/**
	 * Create a simple "continue" output that allows execution to proceed.
	 */
	public static HookOutput allow() {
		return builder().continueExecution(true).build();
	}

	/**
	 * Create a "block" output with reason.
	 */
	public static HookOutput block(String reason) {
		return builder().continueExecution(false).decision("block").reason(reason).build();
	}

	/**
	 * Create builder for fluent construction.
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for HookOutput.
	 */
	public static class Builder {

		private Boolean continueExecution;

		private Boolean suppressOutput;

		private String stopReason;

		private String decision;

		private String systemMessage;

		private String reason;

		private Boolean asyncExecution;

		private Integer asyncTimeout;

		private HookSpecificOutput hookSpecificOutput;

		public Builder continueExecution(Boolean continueExecution) {
			this.continueExecution = continueExecution;
			return this;
		}

		public Builder suppressOutput(Boolean suppressOutput) {
			this.suppressOutput = suppressOutput;
			return this;
		}

		public Builder stopReason(String stopReason) {
			this.stopReason = stopReason;
			return this;
		}

		public Builder decision(String decision) {
			this.decision = decision;
			return this;
		}

		public Builder systemMessage(String systemMessage) {
			this.systemMessage = systemMessage;
			return this;
		}

		public Builder reason(String reason) {
			this.reason = reason;
			return this;
		}

		public Builder asyncExecution(Boolean asyncExecution) {
			this.asyncExecution = asyncExecution;
			return this;
		}

		public Builder asyncTimeout(Integer asyncTimeout) {
			this.asyncTimeout = asyncTimeout;
			return this;
		}

		public Builder hookSpecificOutput(HookSpecificOutput hookSpecificOutput) {
			this.hookSpecificOutput = hookSpecificOutput;
			return this;
		}

		public HookOutput build() {
			return new HookOutput(continueExecution, suppressOutput, stopReason, decision, systemMessage, reason,
					asyncExecution, asyncTimeout, hookSpecificOutput);
		}

	}

	/**
	 * Hook-specific output - varies by hook type.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record HookSpecificOutput(@JsonProperty("hookEventName") String hookEventName,

			// PreToolUse specific
			@JsonProperty("permissionDecision") String permissionDecision,
			@JsonProperty("permissionDecisionReason") String permissionDecisionReason,
			@JsonProperty("updatedInput") Map<String, Object> updatedInput,

			// PostToolUse / UserPromptSubmit specific
			@JsonProperty("additionalContext") String additionalContext) {

		/**
		 * Create PreToolUse output that allows execution.
		 */
		public static HookSpecificOutput preToolUseAllow() {
			return preToolUseAllow(null);
		}

		/**
		 * Create PreToolUse output that allows execution with reason.
		 */
		public static HookSpecificOutput preToolUseAllow(String reason) {
			return new HookSpecificOutput("PreToolUse", "allow", reason, null, null);
		}

		/**
		 * Create PreToolUse output that denies execution.
		 */
		public static HookSpecificOutput preToolUseDeny(String reason) {
			return new HookSpecificOutput("PreToolUse", "deny", reason, null, null);
		}

		/**
		 * Create PreToolUse output that asks for user permission.
		 */
		public static HookSpecificOutput preToolUseAsk() {
			return new HookSpecificOutput("PreToolUse", "ask", null, null, null);
		}

		/**
		 * Create PreToolUse output that modifies the input.
		 */
		public static HookSpecificOutput preToolUseModify(Map<String, Object> updatedInput) {
			return new HookSpecificOutput("PreToolUse", null, null, updatedInput, null);
		}

		/**
		 * Create PostToolUse output with additional context.
		 */
		public static HookSpecificOutput postToolUse(String additionalContext) {
			return new HookSpecificOutput("PostToolUse", null, null, null, additionalContext);
		}

		/**
		 * Create UserPromptSubmit output with additional context.
		 */
		public static HookSpecificOutput userPromptSubmit(String additionalContext) {
			return new HookSpecificOutput("UserPromptSubmit", null, null, null, additionalContext);
		}

		/**
		 * Builder for HookSpecificOutput.
		 */
		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {

			private String hookEventName;

			private String permissionDecision;

			private String permissionDecisionReason;

			private Map<String, Object> updatedInput;

			private String additionalContext;

			public Builder hookEventName(String hookEventName) {
				this.hookEventName = hookEventName;
				return this;
			}

			public Builder permissionDecision(String permissionDecision) {
				this.permissionDecision = permissionDecision;
				return this;
			}

			public Builder permissionDecisionReason(String permissionDecisionReason) {
				this.permissionDecisionReason = permissionDecisionReason;
				return this;
			}

			public Builder updatedInput(Map<String, Object> updatedInput) {
				this.updatedInput = updatedInput;
				return this;
			}

			public Builder additionalContext(String additionalContext) {
				this.additionalContext = additionalContext;
				return this;
			}

			public HookSpecificOutput build() {
				return new HookSpecificOutput(hookEventName, permissionDecision, permissionDecisionReason, updatedInput,
						additionalContext);
			}

		}
	}
}
