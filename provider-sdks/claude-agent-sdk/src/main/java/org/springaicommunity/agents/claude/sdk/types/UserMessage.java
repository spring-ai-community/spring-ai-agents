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

/**
 * User message. Corresponds to UserMessage dataclass in Python SDK.
 */
public record UserMessage(@JsonProperty("content") Object content // Can be String or
																	// List<ContentBlock>
) implements Message {

	@Override
	public String getType() {
		return "user";
	}

	/**
	 * Returns content as a string if it's a string, null otherwise.
	 * @return the content as a string or null
	 */
	public String getContentAsString() {
		return content instanceof String ? (String) content : null;
	}

	/**
	 * Returns content as a list of content blocks if it's a list, null otherwise.
	 * @return the content as a list of blocks or null
	 */
	@SuppressWarnings("unchecked")
	public List<ContentBlock> getContentAsBlocks() {
		return content instanceof List ? (List<ContentBlock>) content : null;
	}

	/**
	 * Factory method to create a UserMessage from string content.
	 * @param content the string content
	 * @return new UserMessage instance
	 */
	public static UserMessage of(String content) {
		return new UserMessage(content);
	}

	/**
	 * Factory method to create a UserMessage from content blocks.
	 * @param content the content blocks
	 * @return new UserMessage instance
	 */
	public static UserMessage of(List<ContentBlock> content) {
		return new UserMessage(content);
	}
}