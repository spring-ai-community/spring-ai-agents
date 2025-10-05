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

package org.springaicommunity.agents.codexsdk.exceptions;

/**
 * Runtime exception for Codex CLI SDK operations. All checked exceptions are wrapped
 * immediately at the call site to maintain a runtime-only exception design.
 *
 * @author Spring AI Community
 * @since 0.1.0
 */
public class CodexSDKException extends RuntimeException {

	public CodexSDKException(String message) {
		super(message);
	}

	public CodexSDKException(String message, Throwable cause) {
		super(message, cause);
	}

}
