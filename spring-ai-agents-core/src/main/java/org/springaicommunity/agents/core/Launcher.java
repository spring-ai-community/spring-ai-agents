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

package org.springaicommunity.agents.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.template.TemplateRenderer;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Agent launcher that loads specifications and executes agents. Handles agent discovery,
 * input validation, and execution delegation.
 *
 * @author Mark Pollack
 * @since 1.1.0
 */
public class Launcher {

	private static final Logger log = LoggerFactory.getLogger(Launcher.class);

	private final Map<String, AgentRunner> agents;

	/**
	 * Constructor for dependency injection.
	 * @param agents map of agent ID to runner implementations
	 */
	public Launcher(Map<String, AgentRunner> agents) {
		this.agents = agents != null ? agents : Map.of();
		log.info("Launcher initialized with {} agents: {}", this.agents.size(), this.agents.keySet());
	}

	/**
	 * Default constructor for static usage (temporary).
	 */
	private Launcher() {
		this.agents = Map.of();
		log.warn("Launcher created with no agents - using static methods");
	}

	/**
	 * Execute agent with given specification.
	 * @param spec launcher specification
	 * @return execution result
	 */
	public static Result execute(LauncherSpec spec) {
		// Temporary static method for backward compatibility
		// TODO: Remove when proper Spring DI is in place
		Map<String, AgentRunner> tempAgents = createTempAgentRegistry(spec.agentSpec().id());
		return new Launcher(tempAgents).executeInternal(spec);
	}

	/**
	 * Temporary method to create agent registry for static usage. TODO: Remove when
	 * proper Spring DI configuration is implemented.
	 */
	private static Map<String, AgentRunner> createTempAgentRegistry(String agentId) {
		Map<String, AgentRunner> agents = new HashMap<>();

		// Dynamically load agent implementation if available
		if ("hello-world".equals(agentId)) {
			try {
				Class<?> agentClass = Class.forName("org.springaicommunity.agents.helloworld.HelloWorldAgent");
				AgentRunner agent = (AgentRunner) agentClass.getDeclaredConstructor().newInstance();
				agents.put("hello-world", agent);
				log.info("Loaded HelloWorldAgent dynamically");
			}
			catch (Exception e) {
				log.warn("Failed to load HelloWorldAgent: {}", e.getMessage());
			}
		}

		return agents;
	}

	/**
	 * Execute agent with given specification (instance method).
	 * @param spec launcher specification
	 * @return execution result
	 */
	public Result executeInternal(LauncherSpec spec) {
		log.info("Starting agent execution for: {}", spec.agentSpec().id());
		log.info("Full spec: agent={}, inputs={}, cwd={}, env={}", spec.agentSpec().id(), spec.inputs(), spec.cwd(),
				spec.env());

		try {
			// AgentSpec is already loaded and validated in LauncherSpec
			AgentSpec agentSpec = spec.agentSpec();
			log.info("Using agent spec: {}", agentSpec);

			// Validate required inputs (inputs already merged in LocalConfigLoader)
			log.info("Validating required inputs");
			Result validation = validateInputs(spec.inputs(), agentSpec);
			if (!validation.success()) {
				log.warn("Input validation failed: {}", validation.message());
				return validation;
			}
			log.info("Input validation passed");

			// Execute agent
			log.info("Executing agent: {}", agentSpec.id());
			AgentRunner executor = agents.get(agentSpec.id());
			if (executor == null) {
				log.warn("No executor found for agent: {}. Available: {}", agentSpec.id(), agents.keySet());
				return Result
					.fail("No executor found for agent: " + agentSpec.id() + ". Available: " + agents.keySet());
			}

			Result result = executor.run(spec);
			log.info("Agent execution completed: success={}", result.success());
			if (!result.success()) {
				log.error("Agent execution failed: {}", result.message());
			}
			return result;
		}
		catch (Exception e) {
			log.error("Agent execution failed with exception", e);
			return Result.fail("Agent execution failed: " + e.getMessage());
		}
	}

	/**
	 * Load agent specification from classpath.
	 * @param agentId agent identifier
	 * @return agent specification or null if not found
	 */
	public static AgentSpec loadAgentSpec(String agentId) {
		String resourcePath = "/agents/" + agentId + ".yaml";
		log.info("Attempting to load agent spec from: {}", resourcePath);

		try (InputStream is = Launcher.class.getResourceAsStream(resourcePath)) {
			if (is == null) {
				log.warn("Agent spec not found: {}", resourcePath);
				return null;
			}

			Yaml yaml = new Yaml();
			Map<String, Object> data = yaml.load(is);
			log.info("Loaded YAML data for agent: {}", agentId);

			String id = getString(data, "id");
			String version = getString(data, "version");
			Map<String, AgentSpec.InputDef> inputs = parseInputDefs(getMap(data, "inputs"));

			log.info("Parsed agent spec: id={}, version={}, inputs={}", id, version,
					inputs != null ? inputs.keySet() : null);
			return new AgentSpec(id, version, inputs);
		}
		catch (IOException e) {
			log.error("Failed to load agent spec from: {}", resourcePath, e);
			throw new RuntimeException("Failed to load agent spec: " + resourcePath, e);
		}
	}

	/**
	 * Merge runtime inputs with agent defaults.
	 * @param runtimeInputs runtime input values
	 * @param agentSpec agent specification with defaults
	 * @return merged inputs
	 */
	public static Map<String, Object> mergeWithDefaults(Map<String, Object> runtimeInputs, AgentSpec agentSpec) {
		Map<String, Object> result = new LinkedHashMap<>();

		// Start with agent defaults
		if (agentSpec.inputs() != null) {
			agentSpec.inputs().forEach((key, inputDef) -> {
				if (inputDef.defaultValue() != null) {
					result.put(key, inputDef.defaultValue());
					log.info("Added default value for {}: {}", key, inputDef.defaultValue());
				}
			});
		}

		// Override with runtime values
		if (runtimeInputs != null) {
			runtimeInputs.forEach((key, value) -> {
				result.put(key, value);
				log.info("Override input {}: {}", key, value);
			});
		}

		log.info("Merged inputs: {}", result.keySet());
		return result;
	}

	/**
	 * Validate required inputs are present.
	 * @param inputs input values
	 * @param agentSpec agent specification
	 * @return validation result
	 */
	static Result validateInputs(Map<String, Object> inputs, AgentSpec agentSpec) {
		if (agentSpec.inputs() == null) {
			return Result.ok("No inputs defined");
		}

		List<String> missing = new ArrayList<>();
		agentSpec.inputs().forEach((key, inputDef) -> {
			if (inputDef.required() && !inputs.containsKey(key)) {
				missing.add(key);
			}
		});

		if (!missing.isEmpty()) {
			return Result.fail("Missing required inputs: " + String.join(", ", missing));
		}

		return Result.ok("Inputs validated");
	}

	/**
	 * Render template with inputs using Spring AI's TemplateRenderer.
	 * @param template StringTemplate template
	 * @param inputs input values
	 * @return rendered string
	 */
	static String renderTemplate(String template, Map<String, Object> inputs) {
		if (template == null) {
			log.info("Template is null, returning empty string");
			return "";
		}

		Map<String, Object> context = new LinkedHashMap<>(inputs);
		log.info("Rendering template with context: {}", context.keySet());
		TemplateRenderer renderer = StTemplateRenderer.builder().build();
		String result = renderer.apply(template, context);
		log.info("Template rendered successfully, result length: {}", result.length());
		return result;
	}

	// Parsing helper methods
	private static Map<String, AgentSpec.InputDef> parseInputDefs(Map<String, Object> inputsMap) {
		if (inputsMap == null) {
			return Map.of();
		}

		Map<String, AgentSpec.InputDef> result = new LinkedHashMap<>();
		inputsMap.forEach((key, value) -> {
			if (value instanceof Map<?, ?> defMap) {
				String type = getString(defMap, "type");
				Object defaultValue = defMap.get("default");
				boolean required = Boolean.parseBoolean(getString(defMap, "required"));
				result.put(key, new AgentSpec.InputDef(type, defaultValue, required));
			}
		});
		return result;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> getMap(Map<String, Object> map, String key) {
		Object value = map.get(key);
		return (value instanceof Map) ? (Map<String, Object>) value : Map.of();
	}

	@SuppressWarnings("unchecked")
	private static String getString(Map<?, ?> map, String key) {
		Object value = map.get(key);
		return value != null ? value.toString() : null;
	}

}