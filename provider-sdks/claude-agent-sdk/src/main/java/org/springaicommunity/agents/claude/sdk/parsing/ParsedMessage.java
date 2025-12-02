/*
 * Copyright 2025 Spring AI Community
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

package org.springaicommunity.agents.claude.sdk.parsing;

import org.springaicommunity.agents.claude.sdk.types.Message;
import org.springaicommunity.agents.claude.sdk.types.control.ControlRequest;

/**
 * Represents a parsed message from Claude CLI output. In bidirectional mode, the CLI can
 * send either regular messages (user, assistant, system, result) or control requests.
 *
 * <p>
 * This sealed interface provides type-safe handling of both cases:
 * </p>
 *
 * <pre>{@code
 * ParsedMessage parsed = parser.parse(json);
 * switch (parsed) {
 *     case ParsedMessage.RegularMessage(var message) -> handleMessage(message);
 *     case ParsedMessage.Control(var request) -> handleControlRequest(request);
 * }
 * }</pre>
 */
public sealed interface ParsedMessage
		permits ParsedMessage.RegularMessage, ParsedMessage.Control, ParsedMessage.EndOfStream {

	/**
	 * Check if this is a regular message (user, assistant, system, result).
	 */
	default boolean isRegularMessage() {
		return this instanceof RegularMessage;
	}

	/**
	 * Check if this is a control request.
	 */
	default boolean isControlRequest() {
		return this instanceof Control;
	}

	/**
	 * Get as regular message, or null if this is a control request.
	 */
	default Message asMessage() {
		return this instanceof RegularMessage r ? r.message() : null;
	}

	/**
	 * Get as control request, or null if this is a regular message.
	 */
	default ControlRequest asControlRequest() {
		return this instanceof Control c ? c.request() : null;
	}

	/**
	 * Wrapper for regular messages (type=user, assistant, system, result).
	 */
	record RegularMessage(Message message) implements ParsedMessage {
		public RegularMessage {
			if (message == null) {
				throw new IllegalArgumentException("message must not be null");
			}
		}

		/**
		 * Factory method for creating a RegularMessage.
		 */
		public static RegularMessage of(Message message) {
			return new RegularMessage(message);
		}
	}

	/**
	 * Wrapper for control protocol requests (type=control_request).
	 */
	record Control(ControlRequest request) implements ParsedMessage {
		public Control {
			if (request == null) {
				throw new IllegalArgumentException("request must not be null");
			}
		}

		/**
		 * Factory method for creating a Control message.
		 */
		public static Control of(ControlRequest request) {
			return new Control(request);
		}
	}

	/**
	 * Sentinel value used internally by MessageStreamIterator to signal end of stream.
	 * This should not be used by application code.
	 */
	record EndOfStream() implements ParsedMessage {

		/**
		 * Singleton instance.
		 */
		public static final EndOfStream INSTANCE = new EndOfStream();

	}

}
