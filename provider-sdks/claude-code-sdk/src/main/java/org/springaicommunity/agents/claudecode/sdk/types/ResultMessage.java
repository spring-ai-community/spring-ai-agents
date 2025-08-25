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

package org.springaicommunity.agents.claudecode.sdk.types;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Result message with cost and usage information. Corresponds to ResultMessage dataclass
 * in Python SDK.
 */
public record ResultMessage(@JsonProperty("subtype") String subtype,

		@JsonProperty("duration_ms") int durationMs,

		@JsonProperty("duration_api_ms") int durationApiMs,

		@JsonProperty("is_error") boolean isError,

		@JsonProperty("num_turns") int numTurns,

		@JsonProperty("session_id") String sessionId,

		@JsonProperty("total_cost_usd") Double totalCostUsd,

		@JsonProperty("usage") Map<String, Object> usage,

		@JsonProperty("result") String result) implements Message {

	@Override
	public String getType() {
		return "result";
	}

	/**
	 * Converts this result message to a rich Metadata object. Extracts usage and cost
	 * information to create domain objects.
	 */
	public Metadata toMetadata(String model) {
		// Extract usage information
		Usage usageObj = extractUsage();

		// Extract cost information
		Cost costObj = extractCost(model, usageObj);

		return Metadata.builder()
			.model(model)
			.cost(costObj)
			.usage(usageObj)
			.durationMs(durationMs)
			.apiDurationMs(durationApiMs)
			.sessionId(sessionId)
			.numTurns(numTurns)
			.build();
	}

	private Usage extractUsage() {
		if (usage == null) {
			return new Usage(0, 0, 0);
		}

		int inputTokens = getIntFromUsage("input_tokens", 0);
		int outputTokens = getIntFromUsage("output_tokens", 0);
		int thinkingTokens = getIntFromUsage("thinking_tokens", 0);

		return new Usage(inputTokens, outputTokens, thinkingTokens);
	}

	private Cost extractCost(String model, Usage usageObj) {
		double totalCost = totalCostUsd != null ? totalCostUsd : 0.0;

		// Estimate input/output cost breakdown if total cost is available
		// This is an approximation - real implementation would use actual pricing
		double inputCost = totalCost * 0.4; // Rough estimate
		double outputCost = totalCost * 0.6; // Rough estimate

		return Cost.builder()
			.inputTokenCost(inputCost)
			.outputTokenCost(outputCost)
			.inputTokens(usageObj.inputTokens())
			.outputTokens(usageObj.outputTokens())
			.model(model)
			.build();
	}

	private int getIntFromUsage(String key, int defaultValue) {
		if (usage == null)
			return defaultValue;
		Object value = usage.get(key);
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		return defaultValue;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String subtype;

		private int durationMs;

		private int durationApiMs;

		private boolean isError;

		private int numTurns;

		private String sessionId;

		private Double totalCostUsd;

		private Map<String, Object> usage;

		private String result;

		public Builder subtype(String subtype) {
			this.subtype = subtype;
			return this;
		}

		public Builder durationMs(int durationMs) {
			this.durationMs = durationMs;
			return this;
		}

		public Builder durationApiMs(int durationApiMs) {
			this.durationApiMs = durationApiMs;
			return this;
		}

		public Builder isError(boolean isError) {
			this.isError = isError;
			return this;
		}

		public Builder numTurns(int numTurns) {
			this.numTurns = numTurns;
			return this;
		}

		public Builder sessionId(String sessionId) {
			this.sessionId = sessionId;
			return this;
		}

		public Builder totalCostUsd(Double totalCostUsd) {
			this.totalCostUsd = totalCostUsd;
			return this;
		}

		public Builder usage(Map<String, Object> usage) {
			this.usage = usage;
			return this;
		}

		public Builder result(String result) {
			this.result = result;
			return this;
		}

		public ResultMessage build() {
			return new ResultMessage(subtype, durationMs, durationApiMs, isError, numTurns, sessionId, totalCostUsd,
					usage, result);
		}

	}
}