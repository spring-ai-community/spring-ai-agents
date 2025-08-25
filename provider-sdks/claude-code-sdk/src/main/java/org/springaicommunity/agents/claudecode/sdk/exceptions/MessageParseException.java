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

package org.springaicommunity.agents.claudecode.sdk.exceptions;

import java.util.Map;

/**
 * Raised when unable to parse a message from CLI output. Corresponds to MessageParseError
 * in Python SDK.
 */
public class MessageParseException extends ClaudeSDKException {

	private final Map<String, Object> data;

	public MessageParseException(String message) {
		this(message, (Map<String, Object>) null);
	}

	public MessageParseException(String message, Map<String, Object> data) {
		super(message);
		this.data = data;
	}

	public MessageParseException(String message, Throwable cause) {
		super(message, cause);
		this.data = null;
	}

	public Map<String, Object> getData() {
		return data;
	}

	/**
	 * Creates a MessageParseException with parsed data context.
	 */
	public static MessageParseException withData(String message, Map<String, Object> data) {
		return new MessageParseException(message, data);
	}

}