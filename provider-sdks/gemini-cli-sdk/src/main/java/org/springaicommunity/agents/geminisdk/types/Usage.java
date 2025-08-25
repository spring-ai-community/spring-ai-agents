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

/**
 * Represents token usage metrics for a Gemini query. Provides analytics and calculations
 * for token consumption.
 */
public record Usage(@JsonProperty("prompt_tokens") int promptTokens,
		@JsonProperty("completion_tokens") int completionTokens, @JsonProperty("total_tokens") int totalTokens) {

	@JsonCreator
	public Usage(@JsonProperty("prompt_tokens") int promptTokens,
			@JsonProperty("completion_tokens") int completionTokens, @JsonProperty("total_tokens") int totalTokens) {
		this.promptTokens = Math.max(0, promptTokens);
		this.completionTokens = Math.max(0, completionTokens);
		this.totalTokens = totalTokens > 0 ? totalTokens : (promptTokens + completionTokens);
	}

	public static Usage of(int promptTokens, int completionTokens) {
		return new Usage(promptTokens, completionTokens, promptTokens + completionTokens);
	}

	public static Usage empty() {
		return new Usage(0, 0, 0);
	}

	/**
	 * Calculates the compression ratio (total tokens / prompt tokens).
	 */
	public double getCompressionRatio() {
		return promptTokens > 0 ? (double) totalTokens / promptTokens : 0.0;
	}

	/**
	 * Calculates the expansion ratio (completion tokens / prompt tokens).
	 */
	public double getExpansionRatio() {
		return promptTokens > 0 ? (double) completionTokens / promptTokens : 0.0;
	}

	/**
	 * Gets the percentage of tokens used for the prompt.
	 */
	public double getPromptPercentage() {
		return totalTokens > 0 ? (double) promptTokens / totalTokens * 100 : 0.0;
	}

	/**
	 * Gets the percentage of tokens used for the completion.
	 */
	public double getCompletionPercentage() {
		return totalTokens > 0 ? (double) completionTokens / totalTokens * 100 : 0.0;
	}

	/**
	 * Checks if this usage has any tokens.
	 */
	public boolean hasTokens() {
		return totalTokens > 0;
	}

	/**
	 * Adds another usage to this one.
	 */
	public Usage add(Usage other) {
		if (other == null)
			return this;
		return new Usage(this.promptTokens + other.promptTokens, this.completionTokens + other.completionTokens,
				this.totalTokens + other.totalTokens);
	}
}