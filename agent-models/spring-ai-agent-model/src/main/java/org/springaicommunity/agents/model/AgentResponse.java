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

import java.util.List;
import java.util.Objects;

import org.springframework.ai.model.ModelResponse;

/**
 * Model layer result - equivalent to ChatResponse following Spring AI conventions. This
 * class uses Spring AI's two-layer pattern with proper binary compatibility.
 *
 * <p>
 * Unlike records, this class ensures binary compatibility for future versions.
 * </p>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public class AgentResponse implements ModelResponse<AgentGeneration> {

	private final AgentResponseMetadata agentResponseMetadata;

	private final List<AgentGeneration> results;

	public AgentResponse(List<AgentGeneration> results) {
		this(results, new AgentResponseMetadata());
	}

	public AgentResponse(List<AgentGeneration> results, AgentResponseMetadata metadata) {
		this.agentResponseMetadata = metadata != null ? metadata : new AgentResponseMetadata();
		this.results = List.copyOf(results);
	}

	@Override
	public List<AgentGeneration> getResults() {
		return this.results;
	}

	@Override
	public AgentResponseMetadata getMetadata() {
		return this.agentResponseMetadata;
	}

	@Override
	public AgentGeneration getResult() {
		return this.results.isEmpty() ? null : this.results.get(0);
	}

	/**
	 * Get the primary text result from the first generation.
	 * @return the primary result text, or empty string if no results
	 */
	public String getText() {
		return this.results.isEmpty() ? "" : this.results.get(0).getOutput();
	}

	/**
	 * Get all text results from all generations.
	 * @return list of all result texts
	 */
	public List<String> getTexts() {
		return this.results.stream().map(AgentGeneration::getOutput).toList();
	}

	/**
	 * Check if the agent execution was successful based on generations.
	 * @return true if any generation has a successful finish reason
	 */
	public boolean isSuccessful() {
		return this.results.stream()
			.anyMatch(generation -> "SUCCESS".equals(generation.getMetadata().getFinishReason())
					|| "COMPLETE".equals(generation.getMetadata().getFinishReason()));
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof AgentResponse that))
			return false;
		return Objects.equals(this.agentResponseMetadata, that.agentResponseMetadata)
				&& Objects.equals(this.results, that.results);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.agentResponseMetadata, this.results);
	}

	@Override
	public String toString() {
		return "AgentResponse[" + "agentResponseMetadata=" + this.agentResponseMetadata + ", results=" + this.results
				+ ']';
	}

	/**
	 * Builder pattern like ChatResponse for convenient construction.
	 */
	public static final class Builder {

		private List<AgentGeneration> results;

		private AgentResponseMetadata.Builder metadataBuilder;

		private Builder() {
			this.metadataBuilder = AgentResponseMetadata.builder();
		}

		public Builder results(List<AgentGeneration> results) {
			this.results = results;
			return this;
		}

		public Builder metadata(AgentResponseMetadata metadata) {
			this.metadataBuilder = AgentResponseMetadata.builder().from(metadata);
			return this;
		}

		public Builder metadataBuilder(AgentResponseMetadata.Builder metadataBuilder) {
			this.metadataBuilder = metadataBuilder;
			return this;
		}

		public AgentResponse build() {
			return new AgentResponse(this.results, this.metadataBuilder.build());
		}

	}

}