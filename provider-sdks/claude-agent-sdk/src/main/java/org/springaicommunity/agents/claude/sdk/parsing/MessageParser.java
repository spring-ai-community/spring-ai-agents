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

package org.springaicommunity.agents.claude.sdk.parsing;

import org.springaicommunity.agents.claude.sdk.exceptions.MessageParseException;
import org.springaicommunity.agents.claude.sdk.types.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses JSON messages from Claude CLI output into typed Message objects. Handles the
 * stream JSON format and converts to domain objects.
 */
public class MessageParser {

	private static final Logger logger = LoggerFactory.getLogger(MessageParser.class);

	private final ObjectMapper objectMapper;

	public MessageParser() {
		this.objectMapper = new ObjectMapper();
	}

	/**
	 * Parses a JSON string into a Message object.
	 */
	public Message parseMessage(String json) throws MessageParseException {
		try {
			JsonNode root = objectMapper.readTree(json);
			return parseMessageFromNode(root);
		}
		catch (JsonProcessingException e) {
			throw MessageParseException.jsonDecodeError(json, e);
		}
	}

	/**
	 * Parses a JsonNode into a Message object.
	 */
	public Message parseMessageFromNode(JsonNode node) throws MessageParseException {
		String type = getStringField(node, "type");
		if (type == null) {
			throw new MessageParseException("Missing 'type' field in message");
		}

		return switch (type) {
			case "user" -> parseUserMessage(node);
			case "assistant" -> parseAssistantMessage(node);
			case "system" -> parseSystemMessage(node);
			case "result" -> parseResultMessage(node);
			default -> throw new MessageParseException("Unknown message type: " + type);
		};
	}

	private UserMessage parseUserMessage(JsonNode node) throws MessageParseException {
		JsonNode messageNode = node.get("message");
		if (messageNode == null) {
			throw new MessageParseException("Missing 'message' field in user message");
		}

		JsonNode contentNode = messageNode.get("content");
		if (contentNode == null) {
			throw new MessageParseException("Missing 'content' field in user message");
		}

		if (contentNode.isTextual()) {
			return UserMessage.of(contentNode.asText());
		}
		else if (contentNode.isArray()) {
			List<ContentBlock> blocks = parseContentBlocks(contentNode);
			return UserMessage.of(blocks);
		}
		else {
			throw new MessageParseException("Invalid content format in user message");
		}
	}

	private AssistantMessage parseAssistantMessage(JsonNode node) throws MessageParseException {
		JsonNode messageNode = node.get("message");
		if (messageNode == null) {
			throw new MessageParseException("Missing 'message' field in assistant message");
		}

		JsonNode contentNode = messageNode.get("content");
		if (contentNode == null || !contentNode.isArray()) {
			throw new MessageParseException("Missing or invalid 'content' field in assistant message");
		}

		List<ContentBlock> blocks = parseContentBlocks(contentNode);
		return AssistantMessage.of(blocks);
	}

	private SystemMessage parseSystemMessage(JsonNode node) throws MessageParseException {
		// System messages have data directly in the root node, not nested under "message"
		String subtype = getStringField(node, "subtype");
		if (subtype == null) {
			throw new MessageParseException("Missing 'subtype' field in system message");
		}

		// Parse all fields as data (excluding type and subtype)
		Map<String, Object> data = parseDataMap(node);
		data.remove("type"); // Remove these metadata fields from data
		data.remove("subtype");

		return SystemMessage.of(subtype, data);
	}

	private ResultMessage parseResultMessage(JsonNode node) throws MessageParseException {
		// Result messages have data directly in the root node, not nested under "message"
		return ResultMessage.builder()
			.subtype(getStringField(node, "subtype"))
			.durationMs(getIntField(node, "duration_ms", 0))
			.durationApiMs(getIntField(node, "duration_api_ms", 0))
			.isError(getBooleanField(node, "is_error", false))
			.numTurns(getIntField(node, "num_turns", 1))
			.sessionId(getStringField(node, "session_id"))
			.totalCostUsd(getDoubleField(node, "total_cost_usd"))
			.usage(parseUsageMap(node.get("usage")))
			.result(getStringField(node, "result"))
			.build();
	}

	private List<ContentBlock> parseContentBlocks(JsonNode arrayNode) throws MessageParseException {
		List<ContentBlock> blocks = new ArrayList<>();

		for (JsonNode blockNode : arrayNode) {
			String type = getStringField(blockNode, "type");
			if (type == null) {
				throw new MessageParseException("Missing 'type' field in content block");
			}

			ContentBlock block = switch (type) {
				case "text" -> parseTextBlock(blockNode);
				case "tool_use" -> parseToolUseBlock(blockNode);
				case "tool_result" -> parseToolResultBlock(blockNode);
				case "thinking" -> parseThinkingBlock(blockNode);
				default -> throw new MessageParseException("Unknown content block type: " + type);
			};

			blocks.add(block);
		}

		return blocks;
	}

	private TextBlock parseTextBlock(JsonNode node) throws MessageParseException {
		String text = getStringField(node, "text");
		if (text == null) {
			throw new MessageParseException("Missing 'text' field in text block");
		}
		return TextBlock.of(text);
	}

	private ThinkingBlock parseThinkingBlock(JsonNode node) throws MessageParseException {
		String thinking = getStringField(node, "thinking");
		if (thinking == null) {
			throw new MessageParseException("Missing 'thinking' field in thinking block");
		}
		return ThinkingBlock.of(thinking);
	}

	private ToolUseBlock parseToolUseBlock(JsonNode node) throws MessageParseException {
		String id = getStringField(node, "id");
		String name = getStringField(node, "name");
		JsonNode inputNode = node.get("input");

		if (id == null || name == null) {
			throw new MessageParseException("Missing required fields in tool_use block");
		}

		Map<String, Object> input = inputNode != null ? parseDataMap(inputNode) : Map.of();

		return ToolUseBlock.builder().id(id).name(name).input(input).build();
	}

	private ToolResultBlock parseToolResultBlock(JsonNode node) throws MessageParseException {
		String toolUseId = getStringField(node, "tool_use_id");
		if (toolUseId == null) {
			throw new MessageParseException("Missing 'tool_use_id' field in tool_result block");
		}

		JsonNode contentNode = node.get("content");
		Object content = null;
		if (contentNode != null) {
			if (contentNode.isTextual()) {
				content = contentNode.asText();
			}
			else if (contentNode.isArray()) {
				content = parseDataList(contentNode);
			}
		}

		Boolean isError = getBooleanField(node, "is_error");

		ToolResultBlock.Builder builder = ToolResultBlock.builder().toolUseId(toolUseId).isError(isError);

		if (content instanceof String) {
			builder.content((String) content);
		}
		else if (content instanceof List) {
			builder.content((List<Map<String, Object>>) content);
		}

		return builder.build();
	}

	private Map<String, Object> parseDataMap(JsonNode node) {
		Map<String, Object> map = new HashMap<>();
		node.fields().forEachRemaining(entry -> {
			map.put(entry.getKey(), parseJsonValue(entry.getValue()));
		});
		return map;
	}

	private List<Object> parseDataList(JsonNode node) {
		List<Object> list = new ArrayList<>();
		for (JsonNode item : node) {
			list.add(parseJsonValue(item));
		}
		return list;
	}

	private Object parseJsonValue(JsonNode node) {
		if (node.isTextual()) {
			return node.asText();
		}
		else if (node.isNumber()) {
			return node.isInt() ? node.asInt() : node.asDouble();
		}
		else if (node.isBoolean()) {
			return node.asBoolean();
		}
		else if (node.isArray()) {
			return parseDataList(node);
		}
		else if (node.isObject()) {
			return parseDataMap(node);
		}
		else {
			return null;
		}
	}

	private Map<String, Object> parseUsageMap(JsonNode node) {
		return node != null ? parseDataMap(node) : Map.of();
	}

	// Utility methods for safe field extraction
	private String getStringField(JsonNode node, String fieldName) {
		JsonNode field = node.get(fieldName);
		return field != null && field.isTextual() ? field.asText() : null;
	}

	private int getIntField(JsonNode node, String fieldName, int defaultValue) {
		JsonNode field = node.get(fieldName);
		return field != null && field.isNumber() ? field.asInt() : defaultValue;
	}

	private boolean getBooleanField(JsonNode node, String fieldName, boolean defaultValue) {
		JsonNode field = node.get(fieldName);
		return field != null && field.isBoolean() ? field.asBoolean() : defaultValue;
	}

	private Boolean getBooleanField(JsonNode node, String fieldName) {
		JsonNode field = node.get(fieldName);
		return field != null && field.isBoolean() ? field.asBoolean() : null;
	}

	private Double getDoubleField(JsonNode node, String fieldName) {
		JsonNode field = node.get(fieldName);
		return field != null && field.isNumber() ? field.asDouble() : null;
	}

}
