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

/**
 * Token usage metrics and analytics. Provides rich behavior for usage analysis and
 * monitoring.
 */
public record Usage(@JsonProperty("input_tokens") int inputTokens,

		@JsonProperty("output_tokens") int outputTokens,

		@JsonProperty("thinking_tokens") int thinkingTokens) {

	/**
	 * Gets the total number of tokens used.
	 */
	public int getTotalTokens() {
		return inputTokens + outputTokens + thinkingTokens;
	}

	/**
	 * Gets the compression ratio (output tokens / input tokens).
	 */
	public double getCompressionRatio() {
		return inputTokens > 0 ? (double) outputTokens / inputTokens : 0;
	}

	/**
	 * Gets the thinking ratio (thinking tokens / total tokens).
	 */
	public double getThinkingRatio() {
		int total = getTotalTokens();
		return total > 0 ? (double) thinkingTokens / total : 0;
	}

	/**
	 * Checks if usage exceeds the given token limit.
	 */
	public boolean exceedsLimit(int tokenLimit) {
		return getTotalTokens() > tokenLimit;
	}

	/**
	 * Gets the input token percentage of total tokens.
	 */
	public double getInputTokenPercentage() {
		int total = getTotalTokens();
		return total > 0 ? (double) inputTokens / total * 100 : 0;
	}

	/**
	 * Gets the output token percentage of total tokens.
	 */
	public double getOutputTokenPercentage() {
		int total = getTotalTokens();
		return total > 0 ? (double) outputTokens / total * 100 : 0;
	}

	/**
	 * Returns true if this is considered a large response (>5000 tokens).
	 */
	public boolean isLargeResponse() {
		return getTotalTokens() > 5000;
	}

	/**
	 * Returns true if thinking tokens are used significantly (>10% of total).
	 */
	public boolean hasSignificantThinking() {
		return getThinkingRatio() > 0.1;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private int inputTokens;

		private int outputTokens;

		private int thinkingTokens;

		public Builder inputTokens(int inputTokens) {
			this.inputTokens = inputTokens;
			return this;
		}

		public Builder outputTokens(int outputTokens) {
			this.outputTokens = outputTokens;
			return this;
		}

		public Builder thinkingTokens(int thinkingTokens) {
			this.thinkingTokens = thinkingTokens;
			return this;
		}

		public Usage build() {
			return new Usage(inputTokens, outputTokens, thinkingTokens);
		}

	}
}