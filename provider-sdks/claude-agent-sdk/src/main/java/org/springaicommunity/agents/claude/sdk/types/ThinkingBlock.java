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
 * Thinking content block for extended thinking responses. Corresponds to ThinkingBlock
 * dataclass in Python SDK.
 *
 * <p>
 * When Claude uses extended thinking, it may include ThinkingBlock content in responses.
 * The thinking field contains the model's reasoning process, and the signature field
 * contains a cryptographic signature for verification.
 * </p>
 *
 * @param thinking the thinking content from the model's reasoning process
 * @param signature cryptographic signature for verification
 */
public record ThinkingBlock(@JsonProperty("thinking") String thinking,
		@JsonProperty("signature") String signature) implements ContentBlock {

	@Override
	public String getType() {
		return "thinking";
	}

	/**
	 * Creates a new ThinkingBlock with the given thinking content.
	 * @param thinking the thinking content
	 * @return a new ThinkingBlock
	 */
	public static ThinkingBlock of(String thinking) {
		return new ThinkingBlock(thinking, null);
	}

	/**
	 * Creates a new ThinkingBlock with the given thinking content and signature.
	 * @param thinking the thinking content
	 * @param signature the cryptographic signature
	 * @return a new ThinkingBlock
	 */
	public static ThinkingBlock of(String thinking, String signature) {
		return new ThinkingBlock(thinking, signature);
	}

}
