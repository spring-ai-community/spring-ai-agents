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

package org.springaicommunity.agents.claude.sdk.types.control;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Control response wrapper for bidirectional communication with Claude CLI. The SDK sends
 * these responses back to the CLI.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ControlResponse(@JsonProperty("type") String type, @JsonProperty("response") ResponsePayload response) {

	public static final String TYPE = "control_response";

	/**
	 * Create a success response.
	 */
	public static ControlResponse success(String requestId, Object responseData) {
		return new ControlResponse(TYPE, new SuccessPayload("success", requestId, responseData));
	}

	/**
	 * Create an error response.
	 */
	public static ControlResponse error(String requestId, String errorMessage) {
		return new ControlResponse(TYPE, new ErrorPayload("error", requestId, errorMessage));
	}

	/**
	 * Sealed interface for response payload types.
	 */
	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "subtype")
	@JsonSubTypes({ @JsonSubTypes.Type(value = SuccessPayload.class, name = "success"),
			@JsonSubTypes.Type(value = ErrorPayload.class, name = "error") })
	public sealed interface ResponsePayload permits SuccessPayload, ErrorPayload {

		String subtype();

		String requestId();

	}

	/**
	 * Success response payload.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record SuccessPayload(@JsonProperty("subtype") String subtype, @JsonProperty("request_id") String requestId,
			@JsonProperty("response") Object response) implements ResponsePayload {
	}

	/**
	 * Error response payload.
	 */
	public record ErrorPayload(@JsonProperty("subtype") String subtype, @JsonProperty("request_id") String requestId,
			@JsonProperty("error") String error) implements ResponsePayload {
	}
}
