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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agents.claude.sdk.exceptions.CLIJSONDecodeException;
import org.springaicommunity.agents.claude.sdk.exceptions.MessageParseException;
import org.springaicommunity.agents.claude.sdk.types.Message;
import org.springaicommunity.agents.claude.sdk.types.control.ControlRequest;
import org.springaicommunity.agents.claude.sdk.types.control.ControlResponse;

/**
 * Parser for Claude CLI bidirectional control protocol messages. This parser handles both
 * regular messages (user, assistant, system, result) and control requests
 * (control_request).
 *
 * <p>
 * In bidirectional mode, the CLI uses line-delimited JSON where each line is either:
 * </p>
 * <ul>
 * <li>A regular message with type=user, assistant, system, or result</li>
 * <li>A control request with type=control_request</li>
 * </ul>
 *
 * <p>
 * This parser determines the message type from the "type" field and delegates to the
 * appropriate parser.
 * </p>
 *
 * @see ParsedMessage
 * @see ControlRequest
 */
public class ControlMessageParser {

	private static final Logger logger = LoggerFactory.getLogger(ControlMessageParser.class);

	private static final String TYPE_CONTROL_REQUEST = "control_request";

	private static final String TYPE_CONTROL_RESPONSE = "control_response";

	private final ObjectMapper objectMapper;

	private final MessageParser messageParser;

	/**
	 * Creates a new parser with default configuration.
	 */
	public ControlMessageParser() {
		this.objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		this.messageParser = new MessageParser();
	}

	/**
	 * Creates a new parser with a custom ObjectMapper.
	 * @param objectMapper the ObjectMapper to use for JSON processing
	 */
	public ControlMessageParser(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper.copy().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		this.messageParser = new MessageParser();
	}

	/**
	 * Parses a JSON string into either a regular message or a control request.
	 * @param json the JSON string to parse
	 * @return a ParsedMessage containing either a Message or ControlRequest
	 * @throws CLIJSONDecodeException if the JSON is malformed
	 * @throws MessageParseException if the message structure is invalid
	 */
	public ParsedMessage parse(String json) throws CLIJSONDecodeException, MessageParseException {
		if (json == null || json.isBlank()) {
			throw new MessageParseException("Cannot parse null or blank JSON");
		}

		try {
			JsonNode root = objectMapper.readTree(json);
			return parseFromNode(root, json);
		}
		catch (JsonProcessingException e) {
			throw CLIJSONDecodeException.create(json, e);
		}
	}

	/**
	 * Parses a JsonNode into either a regular message or a control request.
	 * @param node the JsonNode to parse
	 * @param originalJson the original JSON string (for error messages)
	 * @return a ParsedMessage containing either a Message or ControlRequest
	 * @throws MessageParseException if the message structure is invalid
	 */
	public ParsedMessage parseFromNode(JsonNode node, String originalJson) throws MessageParseException {
		JsonNode typeNode = node.get("type");
		if (typeNode == null || !typeNode.isTextual()) {
			throw new MessageParseException("Missing or invalid 'type' field in message");
		}

		String type = typeNode.asText();

		if (TYPE_CONTROL_REQUEST.equals(type)) {
			return parseControlRequest(node, originalJson);
		}
		else if (TYPE_CONTROL_RESPONSE.equals(type)) {
			return parseControlResponse(node, originalJson);
		}
		else {
			return parseRegularMessage(node);
		}
	}

	/**
	 * Parses a control request from a JsonNode.
	 */
	private ParsedMessage parseControlRequest(JsonNode node, String originalJson) throws MessageParseException {
		try {
			ControlRequest request = objectMapper.treeToValue(node, ControlRequest.class);

			if (request.requestId() == null) {
				throw new MessageParseException("Control request missing 'request_id' field");
			}

			logger.debug("Parsed control request: type={}, subtype={}, requestId={}", request.type(),
					request.request() != null ? request.request().subtype() : "null", request.requestId());

			return ParsedMessage.Control.of(request);
		}
		catch (JsonProcessingException e) {
			throw new MessageParseException("Failed to parse control request: " + e.getMessage(), e);
		}
	}

	/**
	 * Parses a control response from a JsonNode. These are responses from the CLI to our
	 * outgoing control requests (e.g., interrupt, set_model, set_permission_mode).
	 */
	private ParsedMessage parseControlResponse(JsonNode node, String originalJson) throws MessageParseException {
		try {
			ControlResponse response = objectMapper.treeToValue(node, ControlResponse.class);

			String requestId = response.response() != null ? response.response().requestId() : null;
			String subtype = response.response() != null ? response.response().subtype() : "null";

			logger.debug("Parsed control response: subtype={}, requestId={}", subtype, requestId);

			return ParsedMessage.ControlResponseMessage.of(response);
		}
		catch (JsonProcessingException e) {
			throw new MessageParseException("Failed to parse control response: " + e.getMessage(), e);
		}
	}

	/**
	 * Parses a regular message from a JsonNode.
	 */
	private ParsedMessage parseRegularMessage(JsonNode node) throws MessageParseException {
		Message message = messageParser.parseMessageFromNode(node);
		return ParsedMessage.RegularMessage.of(message);
	}

	/**
	 * Checks if the given JSON represents a control request without fully parsing it.
	 * This is useful for quick type checks before committing to full parsing.
	 * @param json the JSON string to check
	 * @return true if this is a control request, false otherwise
	 */
	public boolean isControlRequest(String json) {
		if (json == null || json.isBlank()) {
			return false;
		}

		try {
			JsonNode root = objectMapper.readTree(json);
			JsonNode typeNode = root.get("type");
			return typeNode != null && TYPE_CONTROL_REQUEST.equals(typeNode.asText());
		}
		catch (JsonProcessingException e) {
			return false;
		}
	}

	/**
	 * Extracts the request ID from a control request JSON without fully parsing it.
	 * Useful for correlation and logging.
	 * @param json the JSON string
	 * @return the request ID, or null if not a control request or missing
	 */
	public String extractRequestId(String json) {
		if (json == null || json.isBlank()) {
			return null;
		}

		try {
			JsonNode root = objectMapper.readTree(json);
			JsonNode requestIdNode = root.get("request_id");
			return requestIdNode != null && requestIdNode.isTextual() ? requestIdNode.asText() : null;
		}
		catch (JsonProcessingException e) {
			return null;
		}
	}

}
