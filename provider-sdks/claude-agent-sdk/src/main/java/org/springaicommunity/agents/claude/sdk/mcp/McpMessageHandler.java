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

package org.springaicommunity.agents.claude.sdk.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Handles mcp_message control requests for in-process SDK MCP servers. Routes JSON-RPC
 * messages to appropriate MCP server instances and returns responses.
 * <p>
 * This handler bridges the Claude CLI control protocol with the MCP Java SDK, enabling
 * in-process MCP servers to respond to tool calls without external processes.
 */
public class McpMessageHandler {

	private static final Logger logger = LoggerFactory.getLogger(McpMessageHandler.class);

	private static final String JSONRPC_VERSION = "2.0";

	private final Map<String, McpSyncServer> sdkServers = new ConcurrentHashMap<>();

	private final ObjectMapper objectMapper;

	public McpMessageHandler() {
		this(new ObjectMapper());
	}

	public McpMessageHandler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/**
	 * Registers an MCP server for handling messages.
	 * @param name the server name (matches mcpServers config key)
	 * @param server the MCP server instance
	 */
	public void registerServer(String name, McpSyncServer server) {
		sdkServers.put(name, server);
		logger.debug("Registered MCP server: {}", name);
	}

	/**
	 * Unregisters an MCP server.
	 * @param name the server name
	 */
	public void unregisterServer(String name) {
		sdkServers.remove(name);
		logger.debug("Unregistered MCP server: {}", name);
	}

	/**
	 * Checks if a server is registered.
	 * @param name the server name
	 * @return true if registered
	 */
	public boolean hasServer(String name) {
		return sdkServers.containsKey(name);
	}

	/**
	 * Handles an MCP message from the CLI and routes it to the appropriate server.
	 * @param serverName the target MCP server name
	 * @param message the JSON-RPC message from CLI
	 * @return the JSON-RPC response to send back, or null for notifications
	 */
	public Map<String, Object> handleMcpMessage(String serverName, Map<String, Object> message) {
		McpSyncServer server = sdkServers.get(serverName);
		if (server == null) {
			logger.warn("Unknown MCP server: {}", serverName);
			return errorResponse(message.get("id"), -32601, "Unknown MCP server: " + serverName);
		}

		String method = (String) message.get("method");
		Object id = message.get("id");
		Object params = message.get("params");

		logger.debug("Handling MCP message: server={}, method={}, id={}", serverName, method, id);

		try {
			return switch (method) {
				case "initialize" -> handleInitialize(server, id, params);
				case "tools/list" -> handleToolsList(server, id);
				case "tools/call" -> handleToolsCall(server, id, params);
				case "resources/list" -> handleResourcesList(server, id);
				case "resources/read" -> handleResourcesRead(server, id, params);
				case "prompts/list" -> handlePromptsList(server, id);
				case "prompts/get" -> handlePromptsGet(server, id, params);
				case "notifications/initialized" -> {
					logger.debug("Received initialized notification");
					yield null; // Notifications don't require a response
				}
				default -> {
					logger.warn("Unknown MCP method: {}", method);
					yield errorResponse(id, -32601, "Method not found: " + method);
				}
			};
		}
		catch (Exception e) {
			logger.error("Error handling MCP message: {}", e.getMessage(), e);
			return errorResponse(id, -32603, "Internal error: " + e.getMessage());
		}
	}

	private Map<String, Object> handleInitialize(McpSyncServer server, Object id, Object params) {
		McpSchema.ServerCapabilities capabilities = server.getServerCapabilities();
		McpSchema.Implementation serverInfo = server.getServerInfo();

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("protocolVersion", "2024-11-05");

		if (capabilities != null) {
			Map<String, Object> caps = new LinkedHashMap<>();
			if (capabilities.tools() != null) {
				caps.put("tools", Map.of("listChanged", capabilities.tools().listChanged()));
			}
			if (capabilities.resources() != null) {
				Map<String, Object> resourceCaps = new LinkedHashMap<>();
				resourceCaps.put("subscribe", capabilities.resources().subscribe());
				resourceCaps.put("listChanged", capabilities.resources().listChanged());
				caps.put("resources", resourceCaps);
			}
			if (capabilities.prompts() != null) {
				caps.put("prompts", Map.of("listChanged", capabilities.prompts().listChanged()));
			}
			result.put("capabilities", caps);
		}

		if (serverInfo != null) {
			result.put("serverInfo", Map.of("name", serverInfo.name(), "version", serverInfo.version()));
		}

		return successResponse(id, result);
	}

	private Map<String, Object> handleToolsList(McpSyncServer server, Object id) {
		List<McpSchema.Tool> tools = server.listTools();
		List<Map<String, Object>> toolsList = tools.stream().map(this::toolToMap).collect(Collectors.toList());

		return successResponse(id, Map.of("tools", toolsList));
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> handleToolsCall(McpSyncServer server, Object id, Object params) {
		if (params == null) {
			return errorResponse(id, -32602, "Invalid params: missing parameters");
		}

		Map<String, Object> paramsMap = objectMapper.convertValue(params, new TypeReference<Map<String, Object>>() {
		});
		String toolName = (String) paramsMap.get("name");
		Map<String, Object> arguments = (Map<String, Object>) paramsMap.get("arguments");

		if (toolName == null) {
			return errorResponse(id, -32602, "Invalid params: missing tool name");
		}

		// Find and call the tool
		List<McpSchema.Tool> tools = server.listTools();
		McpSchema.Tool tool = tools.stream().filter(t -> t.name().equals(toolName)).findFirst().orElse(null);

		if (tool == null) {
			return errorResponse(id, -32602, "Tool not found: " + toolName);
		}

		// The MCP SDK's McpSyncServer doesn't expose a direct callTool method.
		// The tool calling happens through the async server's request handlers.
		// For now, we return an error indicating the tool call isn't supported
		// in this bridging mode. Full support would require a custom transport.
		//
		// TODO: Implement tool calling by either:
		// 1. Using McpStatelessAsyncServer with a custom handler
		// 2. Creating a custom transport that routes messages to the server
		// 3. Extracting the tool handler from the server's internal state

		return errorResponse(id, -32603, "Tool calling via mcp_message not yet implemented. "
				+ "Use external stdio/sse/http servers for tool calls.");
	}

	private Map<String, Object> handleResourcesList(McpSyncServer server, Object id) {
		List<McpSchema.Resource> resources = server.listResources();
		List<Map<String, Object>> resourcesList = resources.stream()
			.map(this::resourceToMap)
			.collect(Collectors.toList());

		return successResponse(id, Map.of("resources", resourcesList));
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> handleResourcesRead(McpSyncServer server, Object id, Object params) {
		// Resource reading not directly exposed by McpSyncServer
		return errorResponse(id, -32603, "Resource reading via mcp_message not yet implemented");
	}

	private Map<String, Object> handlePromptsList(McpSyncServer server, Object id) {
		List<McpSchema.Prompt> prompts = server.listPrompts();
		List<Map<String, Object>> promptsList = prompts.stream().map(this::promptToMap).collect(Collectors.toList());

		return successResponse(id, Map.of("prompts", promptsList));
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> handlePromptsGet(McpSyncServer server, Object id, Object params) {
		// Prompt retrieval not directly exposed by McpSyncServer
		return errorResponse(id, -32603, "Prompt retrieval via mcp_message not yet implemented");
	}

	// ============================================================
	// Helper Methods
	// ============================================================

	private Map<String, Object> toolToMap(McpSchema.Tool tool) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("name", tool.name());
		if (tool.description() != null) {
			map.put("description", tool.description());
		}
		if (tool.inputSchema() != null) {
			map.put("inputSchema",
					objectMapper.convertValue(tool.inputSchema(), new TypeReference<Map<String, Object>>() {
					}));
		}
		return map;
	}

	private Map<String, Object> resourceToMap(McpSchema.Resource resource) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("uri", resource.uri());
		if (resource.name() != null) {
			map.put("name", resource.name());
		}
		if (resource.description() != null) {
			map.put("description", resource.description());
		}
		if (resource.mimeType() != null) {
			map.put("mimeType", resource.mimeType());
		}
		return map;
	}

	private Map<String, Object> promptToMap(McpSchema.Prompt prompt) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("name", prompt.name());
		if (prompt.description() != null) {
			map.put("description", prompt.description());
		}
		if (prompt.arguments() != null) {
			map.put("arguments", prompt.arguments().stream().map(arg -> {
				Map<String, Object> argMap = new LinkedHashMap<>();
				argMap.put("name", arg.name());
				if (arg.description() != null) {
					argMap.put("description", arg.description());
				}
				argMap.put("required", arg.required());
				return argMap;
			}).collect(Collectors.toList()));
		}
		return map;
	}

	private Map<String, Object> successResponse(Object id, Map<String, Object> result) {
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("jsonrpc", JSONRPC_VERSION);
		response.put("id", id);
		response.put("result", result);
		return response;
	}

	private Map<String, Object> errorResponse(Object id, int code, String message) {
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("jsonrpc", JSONRPC_VERSION);
		response.put("id", id);
		response.put("error", Map.of("code", code, "message", message));
		return response;
	}

}
