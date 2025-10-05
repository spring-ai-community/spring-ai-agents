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

package org.springaicommunity.agents.ampsdk.exceptions;

/**
 * Runtime exception for Amp CLI SDK operations. All Amp SDK exceptions are runtime
 * exceptions following Spring AI Agents patterns.
 *
 * @author Spring AI Community
 * @since 0.1.0
 */
public class AmpSDKException extends RuntimeException {

	public AmpSDKException(String message) {
		super(message);
	}

	public AmpSDKException(String message, Throwable cause) {
		super(message, cause);
	}

	public AmpSDKException(Throwable cause) {
		super(cause);
	}

}
