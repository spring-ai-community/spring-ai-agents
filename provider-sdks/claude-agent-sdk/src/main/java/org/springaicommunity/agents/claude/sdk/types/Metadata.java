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

package org.springaicommunity.agents.claude.sdk.types;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.util.Map;

/**
 * Rich contextual information aggregating Cost and Usage objects. Provides comprehensive
 * analytics and monitoring capabilities.
 */
public record Metadata(@JsonProperty("model") String model,

		@JsonProperty("cost") Cost cost,

		@JsonProperty("usage") Usage usage,

		@JsonProperty("duration_ms") long durationMs,

		@JsonProperty("api_duration_ms") long apiDurationMs,

		@JsonProperty("session_id") String sessionId,

		@JsonProperty("num_turns") int numTurns) {

	/**
	 * Gets the total duration as a Duration object.
	 */
	public Duration getDuration() {
		return Duration.ofMillis(durationMs);
	}

	/**
	 * Gets the API duration as a Duration object.
	 */
	public Duration getApiDuration() {
		return Duration.ofMillis(apiDurationMs);
	}

	/**
	 * Calculates efficiency score (tokens per millisecond).
	 */
	public double getEfficiencyScore() {
		return durationMs > 0 ? (double) usage.getTotalTokens() / durationMs : 0;
	}

	/**
	 * Calculates API overhead ratio (API time / total time).
	 */
	public double getApiOverheadRatio() {
		return durationMs > 0 ? (double) apiDurationMs / durationMs : 0;
	}

	/**
	 * Returns true if this is considered an expensive query.
	 */
	public boolean isExpensive() {
		return cost.isExpensive();
	}

	/**
	 * Gets tokens per second throughput.
	 */
	public double getTokensPerSecond() {
		return durationMs > 0 ? usage.getTotalTokens() * 1000.0 / durationMs : 0;
	}

	/**
	 * Gets the latency per token in milliseconds.
	 */
	public double getLatencyPerToken() {
		return usage.getTotalTokens() > 0 ? (double) durationMs / usage.getTotalTokens() : 0;
	}

	/**
	 * Returns true if this is a multi-turn conversation.
	 */
	public boolean isMultiTurn() {
		return numTurns > 1;
	}

	/**
	 * Returns true if the response was fast (< 2 seconds).
	 */
	public boolean isFastResponse() {
		return durationMs < 2000;
	}

	/**
	 * Returns a summary map for monitoring/logging systems.
	 */
	public Map<String, Object> toMetricsMap() {
		return Map.of("model", model, "totalCost", cost.calculateTotal(), "totalTokens", usage.getTotalTokens(),
				"durationMs", durationMs, "apiDurationMs", apiDurationMs, "efficiency", getEfficiencyScore(),
				"tokensPerSecond", getTokensPerSecond(), "numTurns", numTurns, "isExpensive", isExpensive(),
				"isMultiTurn", isMultiTurn());
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String model;

		private Cost cost;

		private Usage usage;

		private long durationMs;

		private long apiDurationMs;

		private String sessionId;

		private int numTurns;

		public Builder model(String model) {
			this.model = model;
			return this;
		}

		public Builder cost(Cost cost) {
			this.cost = cost;
			return this;
		}

		public Builder usage(Usage usage) {
			this.usage = usage;
			return this;
		}

		public Builder durationMs(long durationMs) {
			this.durationMs = durationMs;
			return this;
		}

		public Builder apiDurationMs(long apiDurationMs) {
			this.apiDurationMs = apiDurationMs;
			return this;
		}

		public Builder sessionId(String sessionId) {
			this.sessionId = sessionId;
			return this;
		}

		public Builder numTurns(int numTurns) {
			this.numTurns = numTurns;
			return this;
		}

		public Metadata build() {
			return new Metadata(model, cost, usage, durationMs, apiDurationMs, sessionId, numTurns);
		}

	}
}