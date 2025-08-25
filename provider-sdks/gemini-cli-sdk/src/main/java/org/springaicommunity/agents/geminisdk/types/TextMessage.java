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
 * Represents a text message from Gemini CLI. This is the primary message type for Gemini
 * responses.
 */
public record TextMessage(@JsonProperty("type") MessageType type,
		@JsonProperty("content") String content) implements Message {

	@JsonCreator
	public TextMessage(@JsonProperty("type") MessageType type, @JsonProperty("content") String content) {
		this.type = type != null ? type : MessageType.ASSISTANT;
		this.content = content != null ? content : "";
	}

	public static TextMessage user(String content) {
		return new TextMessage(MessageType.USER, content);
	}

	public static TextMessage assistant(String content) {
		return new TextMessage(MessageType.ASSISTANT, content);
	}

	public static TextMessage system(String content) {
		return new TextMessage(MessageType.SYSTEM, content);
	}

	public static TextMessage error(String content) {
		return new TextMessage(MessageType.ERROR, content);
	}

	@Override
	public MessageType getType() {
		return type;
	}

	@Override
	public String getContent() {
		return content;
	}
}