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

package org.springaicommunity.agents.geminisdk.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Represents metadata for a Gemini query including timing, cost, and usage information
 * from CLI response.
 */
public record Metadata(@JsonProperty("model") String model, @JsonProperty("timestamp") Instant timestamp,
		@JsonProperty("duration") Duration duration, @JsonProperty("usage") Usage usage,
		@JsonProperty("cost") Cost cost) {

	@JsonCreator
	public Metadata(@JsonProperty("model") String model, @JsonProperty("timestamp") Instant timestamp,
			@JsonProperty("duration") Duration duration, @JsonProperty("usage") Usage usage,
			@JsonProperty("cost") Cost cost) {
		this.model = model != null ? model : "unknown";
		this.timestamp = timestamp != null ? timestamp : Instant.now();
		this.duration = duration != null ? duration : Duration.ZERO;
		this.usage = usage != null ? usage : Usage.empty();
		this.cost = cost != null ? cost : Cost.zero();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static Metadata empty() {
		return new Metadata(null, null, null, null, null);
	}

	/**
	 * Calculates processing speed as tokens per second (only if usage data is available).
	 */
	public double getTokensPerSecond() {
		if (duration.isZero() || duration.isNegative() || usage.totalTokens() == 0)
			return 0.0;
		return (double) usage.totalTokens() / duration.toMillis() * 1000.0;
	}

	/**
	 * Calculates cost efficiency as cost per token (only if usage and cost data are
	 * available).
	 */
	public double getCostPerToken() {
		if (!usage.hasTokens())
			return 0.0;
		return cost.calculatePerToken(usage.totalTokens()).doubleValue();
	}

	/**
	 * Gets the cost per second (only if cost data is available).
	 */
	public double getCostPerSecond() {
		if (duration.isZero() || duration.isNegative())
			return 0.0;
		return cost.totalCost().doubleValue() / duration.toMillis() * 1000.0;
	}

	/**
	 * Calculates overall efficiency score (tokens per dollar per second).
	 */
	public double getEfficiencyScore() {
		double costPerToken = getCostPerToken();
		double tokensPerSecond = getTokensPerSecond();

		if (costPerToken == 0.0)
			return tokensPerSecond;
		return tokensPerSecond / costPerToken;
	}

	/**
	 * Gets the processing speed category.
	 */
	public String getSpeedCategory() {
		double tokensPerSecond = getTokensPerSecond();
		if (tokensPerSecond >= 100)
			return "very fast";
		if (tokensPerSecond >= 50)
			return "fast";
		if (tokensPerSecond >= 20)
			return "moderate";
		if (tokensPerSecond >= 10)
			return "slow";
		return "very slow";
	}

	/**
	 * Gets the cost category.
	 */
	public String getCostCategory() {
		double costPerToken = getCostPerToken();
		if (costPerToken >= 0.001)
			return "expensive";
		if (costPerToken >= 0.0005)
			return "moderate";
		if (costPerToken >= 0.0001)
			return "affordable";
		return "cheap";
	}

	/**
	 * Checks if the query was processed efficiently.
	 */
	public boolean isEfficient() {
		return getTokensPerSecond() >= 20 && getCostPerToken() <= 0.001;
	}

	public static class Builder {

		private String model;

		private Instant timestamp;

		private Duration duration;

		private Usage usage;

		private Cost cost;

		public Builder model(String model) {
			this.model = model;
			return this;
		}

		public Builder timestamp(Instant timestamp) {
			this.timestamp = timestamp;
			return this;
		}

		public Builder duration(Duration duration) {
			this.duration = duration;
			return this;
		}

		public Builder usage(Usage usage) {
			this.usage = usage;
			return this;
		}

		public Builder cost(Cost cost) {
			this.cost = cost;
			return this;
		}

		public Metadata build() {
			return new Metadata(model, timestamp, duration, usage, cost);
		}

	}
}