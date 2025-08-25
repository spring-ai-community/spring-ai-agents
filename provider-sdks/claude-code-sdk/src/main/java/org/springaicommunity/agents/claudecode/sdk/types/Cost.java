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

/**
 * Cost information with calculation methods for pricing logic. Provides rich behavior for
 * cost analysis and reporting.
 */
public record Cost(@JsonProperty("input_token_cost") double inputTokenCost,

		@JsonProperty("output_token_cost") double outputTokenCost,

		@JsonProperty("input_tokens") int inputTokens,

		@JsonProperty("output_tokens") int outputTokens,

		@JsonProperty("model") String model) {

	/**
	 * Calculates the total cost in USD.
	 */
	public double calculateTotal() {
		return inputTokenCost + outputTokenCost;
	}

	/**
	 * Calculates total cost with markup rate.
	 * @param markupRate markup rate (e.g., 0.15 for 15% markup)
	 */
	public double calculateWithMarkup(double markupRate) {
		return calculateTotal() * (1 + markupRate);
	}

	/**
	 * Calculates cost per token.
	 */
	public double calculatePerToken() {
		int totalTokens = inputTokens + outputTokens;
		return totalTokens > 0 ? calculateTotal() / totalTokens : 0;
	}

	/**
	 * Gets input cost per thousand tokens.
	 */
	public double getInputCostPerThousandTokens() {
		return inputTokens > 0 ? (inputTokenCost / inputTokens) * 1000 : 0;
	}

	/**
	 * Gets output cost per thousand tokens.
	 */
	public double getOutputCostPerThousandTokens() {
		return outputTokens > 0 ? (outputTokenCost / outputTokens) * 1000 : 0;
	}

	/**
	 * Returns true if this is considered an expensive query (>$0.10).
	 */
	public boolean isExpensive() {
		return calculateTotal() > 0.10;
	}

	/**
	 * Returns cost efficiency ratio (output tokens per dollar).
	 */
	public double getEfficiencyRatio() {
		double total = calculateTotal();
		return total > 0 ? outputTokens / total : 0;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private double inputTokenCost;

		private double outputTokenCost;

		private int inputTokens;

		private int outputTokens;

		private String model;

		public Builder inputTokenCost(double inputTokenCost) {
			this.inputTokenCost = inputTokenCost;
			return this;
		}

		public Builder outputTokenCost(double outputTokenCost) {
			this.outputTokenCost = outputTokenCost;
			return this;
		}

		public Builder inputTokens(int inputTokens) {
			this.inputTokens = inputTokens;
			return this;
		}

		public Builder outputTokens(int outputTokens) {
			this.outputTokens = outputTokens;
			return this;
		}

		public Builder model(String model) {
			this.model = model;
			return this;
		}

		public Cost build() {
			return new Cost(inputTokenCost, outputTokenCost, inputTokens, outputTokens, model);
		}

	}
}