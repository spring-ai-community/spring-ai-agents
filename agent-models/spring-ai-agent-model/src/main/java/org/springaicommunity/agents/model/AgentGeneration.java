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

import java.util.Objects;

import org.springframework.ai.model.ModelResult;

/**
 * Individual result generation - equivalent to Generation following Spring AI
 * conventions. This class uses Spring AI's pattern with proper binary compatibility.
 *
 * <p>
 * Unlike records, this class ensures binary compatibility for future versions.
 * </p>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public class AgentGeneration implements ModelResult<String> {

	private final String text;

	private final AgentGenerationMetadata agentGenerationMetadata;

	public AgentGeneration(String text) {
		this(text, AgentGenerationMetadata.NULL);
	}

	public AgentGeneration(String text, AgentGenerationMetadata metadata) {
		this.text = text != null ? text : "";
		this.agentGenerationMetadata = metadata != null ? metadata : AgentGenerationMetadata.NULL;
	}

	@Override
	public String getOutput() {
		return this.text;
	}

	@Override
	public AgentGenerationMetadata getMetadata() {
		return this.agentGenerationMetadata;
	}

	/**
	 * Get the text content of this generation.
	 * @return the text content
	 */
	public String getText() {
		return this.text;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof AgentGeneration that))
			return false;
		return Objects.equals(this.text, that.text)
				&& Objects.equals(this.agentGenerationMetadata, that.agentGenerationMetadata);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.text, this.agentGenerationMetadata);
	}

	@Override
	public String toString() {
		return "AgentGeneration[" + "text=" + this.text + ", agentGenerationMetadata=" + this.agentGenerationMetadata
				+ ']';
	}

}