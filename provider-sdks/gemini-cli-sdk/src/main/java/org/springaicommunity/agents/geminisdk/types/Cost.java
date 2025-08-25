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
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Represents cost calculations for a Gemini query. Provides detailed cost breakdown and
 * analysis.
 */
public record Cost(@JsonProperty("prompt_cost") BigDecimal promptCost,
		@JsonProperty("completion_cost") BigDecimal completionCost, @JsonProperty("total_cost") BigDecimal totalCost) {

	@JsonCreator
	public Cost(@JsonProperty("prompt_cost") BigDecimal promptCost,
			@JsonProperty("completion_cost") BigDecimal completionCost,
			@JsonProperty("total_cost") BigDecimal totalCost) {
		this.promptCost = promptCost != null ? promptCost : BigDecimal.ZERO;
		this.completionCost = completionCost != null ? completionCost : BigDecimal.ZERO;
		this.totalCost = totalCost != null ? totalCost : this.promptCost.add(this.completionCost);
	}

	public static Cost of(BigDecimal promptCost, BigDecimal completionCost) {
		return new Cost(promptCost, completionCost, promptCost.add(completionCost));
	}

	public static Cost of(double promptCost, double completionCost) {
		return of(BigDecimal.valueOf(promptCost), BigDecimal.valueOf(completionCost));
	}

	public static Cost zero() {
		return new Cost(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
	}

	/**
	 * Calculates total cost with markup.
	 */
	public BigDecimal calculateWithMarkup(double markupRate) {
		BigDecimal markup = BigDecimal.valueOf(1.0 + markupRate);
		return totalCost.multiply(markup).setScale(6, RoundingMode.HALF_UP);
	}

	/**
	 * Calculates cost per token.
	 */
	public BigDecimal calculatePerToken(int totalTokens) {
		if (totalTokens <= 0)
			return BigDecimal.ZERO;
		return totalCost.divide(BigDecimal.valueOf(totalTokens), 8, RoundingMode.HALF_UP);
	}

	/**
	 * Gets the cost per thousand tokens.
	 */
	public BigDecimal getCostPer1K(int totalTokens) {
		if (totalTokens <= 0)
			return BigDecimal.ZERO;
		return totalCost.divide(BigDecimal.valueOf(totalTokens), 8, RoundingMode.HALF_UP)
			.multiply(BigDecimal.valueOf(1000));
	}

	/**
	 * Gets the percentage of cost attributed to prompt tokens.
	 */
	public double getPromptCostPercentage() {
		if (totalCost.compareTo(BigDecimal.ZERO) == 0)
			return 0.0;
		return promptCost.divide(totalCost, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue();
	}

	/**
	 * Gets the percentage of cost attributed to completion tokens.
	 */
	public double getCompletionCostPercentage() {
		if (totalCost.compareTo(BigDecimal.ZERO) == 0)
			return 0.0;
		return completionCost.divide(totalCost, 4, RoundingMode.HALF_UP)
			.multiply(BigDecimal.valueOf(100))
			.doubleValue();
	}

	/**
	 * Checks if this cost has any value.
	 */
	public boolean hasCost() {
		return totalCost.compareTo(BigDecimal.ZERO) > 0;
	}

	/**
	 * Adds another cost to this one.
	 */
	public Cost add(Cost other) {
		if (other == null)
			return this;
		return new Cost(this.promptCost.add(other.promptCost), this.completionCost.add(other.completionCost),
				this.totalCost.add(other.totalCost));
	}

	/**
	 * Formats the total cost as a currency string.
	 */
	public String formatTotal() {
		return String.format("$%.6f", totalCost.doubleValue());
	}
}