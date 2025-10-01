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

import java.util.List;
import java.util.Map;

/**
 * Tool result content block. Corresponds to ToolResultBlock dataclass in Python SDK.
 */
public record ToolResultBlock(@JsonProperty("tool_use_id") String toolUseId,

		@JsonProperty("content") Object content, // Can be String, List<Map<String,
													// Object>>, or null

		@JsonProperty("is_error") Boolean isError) implements ContentBlock {

	@Override
	public String getType() {
		return "tool_result";
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Returns content as a string if it's a string, null otherwise
	 */
	public String getContentAsString() {
		return content instanceof String ? (String) content : null;
	}

	/**
	 * Returns content as a list if it's a list, null otherwise
	 */
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> getContentAsList() {
		return content instanceof List ? (List<Map<String, Object>>) content : null;
	}

	public static class Builder {

		private String toolUseId;

		private Object content;

		private Boolean isError;

		public Builder toolUseId(String toolUseId) {
			this.toolUseId = toolUseId;
			return this;
		}

		public Builder content(String content) {
			this.content = content;
			return this;
		}

		public Builder content(List<Map<String, Object>> content) {
			this.content = content;
			return this;
		}

		public Builder isError(Boolean isError) {
			this.isError = isError;
			return this;
		}

		public ToolResultBlock build() {
			return new ToolResultBlock(toolUseId, content, isError);
		}

	}
}