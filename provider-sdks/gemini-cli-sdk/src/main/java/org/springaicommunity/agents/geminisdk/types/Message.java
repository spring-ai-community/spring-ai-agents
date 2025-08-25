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

/**
 * Base interface for all message types in the Gemini SDK. Provides common behavior for
 * different message types.
 */
public interface Message {

	/**
	 * Gets the type of this message.
	 */
	MessageType getType();

	/**
	 * Gets the content of this message.
	 */
	String getContent();

	/**
	 * Checks if this message is empty.
	 */
	default boolean isEmpty() {
		return getContent() == null || getContent().trim().isEmpty();
	}

}