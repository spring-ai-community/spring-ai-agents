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

package org.springaicommunity.agents.claudecode.sdk.parsing;

import org.springaicommunity.agents.claudecode.sdk.exceptions.CLIJSONDecodeException;
import org.springaicommunity.agents.claudecode.sdk.exceptions.MessageParseException;
import org.springaicommunity.agents.claudecode.sdk.types.ResultMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Parser for Claude CLI JSON output format (--output-format json).
 *
 * <p>
 * This parser handles the single JSON object format returned by the Claude CLI when using
 * {@code --output-format json}. Unlike the streaming format, this returns a complete
 * result in a single JSON response containing the answer, metadata, usage statistics, and
 * cost information.
 * </p>
 *
 * <p>
 * Example JSON structure:
 * </p>
 * <pre>
 * {
 *   "type": "result",
 *   "subtype": "success",
 *   "is_error": false,
 *   "duration_ms": 2406,
 *   "duration_api_ms": 2153,
 *   "num_turns": 1,
 *   "result": "4",
 *   "session_id": "a61c8133-0f9c-4c47-99f3-24109e1e9711",
 *   "total_cost_usd": 0.0604716,
 *   "usage": {
 *     "input_tokens": 6,
 *     "cache_creation_input_tokens": 14284,
 *     "cache_read_input_tokens": 22662,
 *     "output_tokens": 6,
 *     "server_tool_use": {"web_search_requests": 0},
 *     "service_tier": "standard"
 *   },
 *   "permission_denials": []
 * }
 * </pre>
 */
public class JsonResultParser {

	private static final Logger logger = LoggerFactory.getLogger(JsonResultParser.class);

	private final ObjectMapper objectMapper;

	public JsonResultParser() {
		this.objectMapper = new ObjectMapper();
	}

	/**
	 * Parses a complete JSON result from Claude CLI into a ResultMessage.
	 * @param json the complete JSON response from Claude CLI
	 * @return a ResultMessage containing the parsed data
	 * @throws CLIJSONDecodeException if JSON parsing fails
	 * @throws MessageParseException if the JSON structure is invalid
	 */
	public ResultMessage parseJsonResult(String json) throws CLIJSONDecodeException, MessageParseException {
		try {
			JsonNode root = objectMapper.readTree(json);
			return parseResultFromNode(root);
		}
		catch (JsonProcessingException e) {
			throw CLIJSONDecodeException.create(json, e);
		}
	}

	/**
	 * Parses a JsonNode into a ResultMessage object.
	 */
	private ResultMessage parseResultFromNode(JsonNode node) throws MessageParseException {
		// Validate this is a result type
		String type = getStringField(node, "type");
		if (!"result".equals(type)) {
			throw new MessageParseException("Expected 'result' type, got: " + type);
		}

		// Extract all fields for ResultMessage
		String subtype = getStringField(node, "subtype");
		int durationMs = getIntField(node, "duration_ms", 0);
		int durationApiMs = getIntField(node, "duration_api_ms", 0);
		boolean isError = getBooleanField(node, "is_error", false);
		int numTurns = getIntField(node, "num_turns", 1);
		String sessionId = getStringField(node, "session_id");
		Double totalCostUsd = getDoubleField(node, "total_cost_usd");
		String result = getStringField(node, "result");

		// Parse usage information
		Map<String, Object> usage = parseUsageMap(node.get("usage"));

		// Build and return ResultMessage
		return ResultMessage.builder()
			.subtype(subtype)
			.durationMs(durationMs)
			.durationApiMs(durationApiMs)
			.isError(isError)
			.numTurns(numTurns)
			.sessionId(sessionId)
			.totalCostUsd(totalCostUsd)
			.usage(usage)
			.result(result)
			.build();
	}

	/**
	 * Parses the usage node into a Map structure.
	 */
	private Map<String, Object> parseUsageMap(JsonNode usageNode) {
		Map<String, Object> usage = new HashMap<>();

		if (usageNode == null || usageNode.isNull()) {
			return usage;
		}

		// Parse all usage fields
		usageNode.fields().forEachRemaining(entry -> {
			String key = entry.getKey();
			JsonNode value = entry.getValue();

			if (value.isInt()) {
				usage.put(key, value.asInt());
			}
			else if (value.isDouble()) {
				usage.put(key, value.asDouble());
			}
			else if (value.isTextual()) {
				usage.put(key, value.asText());
			}
			else if (value.isObject()) {
				// Handle nested objects like server_tool_use
				Map<String, Object> nestedMap = new HashMap<>();
				value.fields().forEachRemaining(nestedEntry -> {
					JsonNode nestedValue = nestedEntry.getValue();
					if (nestedValue.isInt()) {
						nestedMap.put(nestedEntry.getKey(), nestedValue.asInt());
					}
					else if (nestedValue.isTextual()) {
						nestedMap.put(nestedEntry.getKey(), nestedValue.asText());
					}
					else {
						nestedMap.put(nestedEntry.getKey(), nestedValue.toString());
					}
				});
				usage.put(key, nestedMap);
			}
			else {
				usage.put(key, value.toString());
			}
		});

		return usage;
	}

	private String getStringField(JsonNode node, String fieldName) {
		JsonNode field = node.get(fieldName);
		return (field != null && !field.isNull()) ? field.asText() : null;
	}

	private int getIntField(JsonNode node, String fieldName, int defaultValue) {
		JsonNode field = node.get(fieldName);
		return (field != null && !field.isNull()) ? field.asInt() : defaultValue;
	}

	private boolean getBooleanField(JsonNode node, String fieldName, boolean defaultValue) {
		JsonNode field = node.get(fieldName);
		return (field != null && !field.isNull()) ? field.asBoolean() : defaultValue;
	}

	private Double getDoubleField(JsonNode node, String fieldName) {
		JsonNode field = node.get(fieldName);
		return (field != null && !field.isNull()) ? field.asDouble() : null;
	}

}