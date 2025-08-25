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

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.ai.model.ResponseMetadata;

/**
 * Agent response metadata following Spring AI conventions.
 *
 * <p>
 * Contains execution metadata for agent tasks including model information, duration,
 * session identifier, and provider-specific fields.
 * </p>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public class AgentResponseMetadata extends HashMap<String, Object> implements ResponseMetadata {

	private final String model;

	private final Duration duration;

	private final String sessionId;

	/**
	 * Default constructor creating empty metadata.
	 */
	public AgentResponseMetadata() {
		this("", Duration.ZERO, "", Map.of());
	}

	/**
	 * Constructor with all metadata fields.
	 * @param model the model name used for execution
	 * @param duration task execution duration
	 * @param sessionId session identifier
	 * @param providerFields provider-specific metadata
	 */
	public AgentResponseMetadata(String model, Duration duration, String sessionId,
			Map<String, Object> providerFields) {
		super(providerFields != null ? providerFields : Map.of());
		this.model = model != null ? model : "";
		this.duration = duration != null ? duration : Duration.ZERO;
		this.sessionId = sessionId != null ? sessionId : "";
	}

	/**
	 * Get the model name used for execution.
	 * @return the model name
	 */
	public String getModel() {
		return this.model;
	}

	/**
	 * Get the duration of task execution.
	 * @return execution duration
	 */
	public Duration getDuration() {
		return this.duration;
	}

	/**
	 * Get the session identifier for the execution.
	 * @return session identifier
	 */
	public String getSessionId() {
		return this.sessionId;
	}

	/**
	 * Get provider-specific metadata fields.
	 * @return map of provider-specific fields
	 */
	public Map<String, Object> getProviderFields() {
		return this;
	}

	@Override
	public Object getOrDefault(Object key, Object defaultValue) {
		return super.getOrDefault(key, defaultValue);
	}

	@Override
	public <T> T get(String key) {
		return (T) super.get(key);
	}

	@Override
	public <T> T getRequired(Object key) {
		Object value = get(key);
		if (value == null) {
			throw new IllegalArgumentException("Required key '" + key + "' not found");
		}
		return (T) value;
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof AgentResponseMetadata that))
			return false;
		if (!super.equals(o))
			return false;
		return Objects.equals(this.model, that.model) && Objects.equals(this.duration, that.duration)
				&& Objects.equals(this.sessionId, that.sessionId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), this.model, this.duration, this.sessionId);
	}

	@Override
	public String toString() {
		return "AgentResponseMetadata[" + "model='" + this.model + '\'' + ", duration=" + this.duration
				+ ", sessionId='" + this.sessionId + '\'' + ", providerFields=" + super.toString() + ']';
	}

	/**
	 * Builder pattern for convenient construction.
	 */
	public static final class Builder {

		private String model = "";

		private Duration duration = Duration.ZERO;

		private String sessionId = "";

		private Map<String, Object> providerFields = Map.of();

		private Builder() {
		}

		public Builder model(String model) {
			this.model = model;
			return this;
		}

		public Builder duration(Duration duration) {
			this.duration = duration;
			return this;
		}

		public Builder sessionId(String sessionId) {
			this.sessionId = sessionId;
			return this;
		}

		public Builder providerFields(Map<String, Object> providerFields) {
			this.providerFields = providerFields;
			return this;
		}

		public Builder from(AgentResponseMetadata metadata) {
			if (metadata != null) {
				this.model = metadata.model;
				this.duration = metadata.duration;
				this.sessionId = metadata.sessionId;
				this.providerFields = Map.copyOf(metadata);
			}
			return this;
		}

		public AgentResponseMetadata build() {
			return new AgentResponseMetadata(this.model, this.duration, this.sessionId, this.providerFields);
		}

	}

}