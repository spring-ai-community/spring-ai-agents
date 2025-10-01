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

import java.util.Map;

/**
 * Tool use content block. Corresponds to ToolUseBlock dataclass in Python SDK.
 */
public record ToolUseBlock(@JsonProperty("id") String id,

		@JsonProperty("name") String name,

		@JsonProperty("input") Map<String, Object> input) implements ContentBlock {

	@Override
	public String getType() {
		return "tool_use";
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String id;

		private String name;

		private Map<String, Object> input;

		public Builder id(String id) {
			this.id = id;
			return this;
		}

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder input(Map<String, Object> input) {
			this.input = input;
			return this;
		}

		public ToolUseBlock build() {
			return new ToolUseBlock(id, name, input);
		}

	}
}