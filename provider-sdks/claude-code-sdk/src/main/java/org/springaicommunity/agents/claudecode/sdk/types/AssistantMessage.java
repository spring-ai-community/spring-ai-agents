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

package org.springaicommunity.agents.claudecode.sdk.types;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Optional;

/**
 * Assistant message with content blocks. Corresponds to AssistantMessage dataclass in
 * Python SDK.
 */
public record AssistantMessage(@JsonProperty("content") List<ContentBlock> content) implements Message {

	@Override
	public String getType() {
		return "assistant";
	}

	/**
	 * Returns the first text content from this message, if any.
	 * @return optional text content
	 */
	public Optional<String> getTextContent() {
		return content.stream()
			.filter(block -> block instanceof TextBlock)
			.map(block -> ((TextBlock) block).text())
			.findFirst();
	}

	/**
	 * Returns all tool use blocks in this message.
	 * @return list of tool use blocks
	 */
	public List<ToolUseBlock> getToolUses() {
		return content.stream()
			.filter(block -> block instanceof ToolUseBlock)
			.map(block -> (ToolUseBlock) block)
			.toList();
	}

	/**
	 * Returns true if this message contains any tool use blocks.
	 * @return true if message contains tool use blocks
	 */
	public boolean hasToolUse() {
		return content.stream().anyMatch(block -> block instanceof ToolUseBlock);
	}

	/**
	 * Returns all text blocks in this message.
	 * @return list of text blocks
	 */
	public List<TextBlock> getTextBlocks() {
		return content.stream().filter(block -> block instanceof TextBlock).map(block -> (TextBlock) block).toList();
	}

	/**
	 * Factory method to create an AssistantMessage from content blocks.
	 * @param content the content blocks
	 * @return new AssistantMessage instance
	 */
	public static AssistantMessage of(List<ContentBlock> content) {
		return new AssistantMessage(content);
	}
}