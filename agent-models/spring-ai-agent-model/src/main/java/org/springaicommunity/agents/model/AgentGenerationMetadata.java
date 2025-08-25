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

package org.springaicommunity.agents.model;

import java.util.Map;
import java.util.Objects;

import org.springframework.ai.model.ResultMetadata;

/**
 * Per-generation metadata - equivalent to ChatGenerationMetadata following Spring AI
 * conventions.
 *
 * <p>
 * Follows Spring AI pattern of deriving success from finishReason rather than storing
 * boolean status.
 * </p>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public class AgentGenerationMetadata implements ResultMetadata {

	/**
	 * Null object pattern for default metadata.
	 */
	public static final AgentGenerationMetadata NULL = new AgentGenerationMetadata();

	private final String finishReason;

	private final Map<String, Object> providerFields;

	/**
	 * Default constructor creating empty metadata.
	 */
	public AgentGenerationMetadata() {
		this("", Map.of());
	}

	/**
	 * Constructor with finish reason and provider-specific fields.
	 * @param finishReason the reason task execution finished (SUCCESS, ERROR, TIMEOUT,
	 * etc.)
	 * @param providerFields provider-specific metadata fields
	 */
	public AgentGenerationMetadata(String finishReason, Map<String, Object> providerFields) {
		this.finishReason = finishReason != null ? finishReason : "";
		this.providerFields = providerFields != null ? Map.copyOf(providerFields) : Map.of();
	}

	/**
	 * Get the reason why task execution finished. Common values: SUCCESS, ERROR, TIMEOUT,
	 * CANCELLED
	 * @return the finish reason
	 */
	public String getFinishReason() {
		return this.finishReason;
	}

	/**
	 * Get provider-specific metadata fields.
	 * @return map of provider-specific fields
	 */
	public Map<String, Object> getProviderFields() {
		return this.providerFields;
	}

	/**
	 * Check if execution was successful based on finish reason.
	 * @return true if finish reason indicates success
	 */
	public boolean isSuccessful() {
		return "SUCCESS".equals(this.finishReason) || "COMPLETE".equals(this.finishReason);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof AgentGenerationMetadata that))
			return false;
		return Objects.equals(this.finishReason, that.finishReason)
				&& Objects.equals(this.providerFields, that.providerFields);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.finishReason, this.providerFields);
	}

	@Override
	public String toString() {
		return "AgentGenerationMetadata[" + "finishReason='" + this.finishReason + '\'' + ", providerFields="
				+ this.providerFields + ']';
	}

}