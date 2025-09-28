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
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class for loading agent specifications from YAML files. First tries classpath,
 * then falls back to filesystem (.agents directory). Handles parsing of agent metadata,
 * input definitions, and validation.
 *
 * @author Mark Pollack
 * @since 1.1.0
 */
public final class AgentSpecLoader {

	private static final Logger log = LoggerFactory.getLogger(AgentSpecLoader.class);

	private AgentSpecLoader() {
		// Utility class - prevent instantiation
	}

	/**
	 * Load agent specification from classpath or filesystem.
	 * @param agentId agent identifier
	 * @return agent specification or null if not found
	 */
	public static AgentSpec loadAgentSpec(String agentId) {
		// Try classpath first
		AgentSpec spec = loadFromClasspath(agentId);
		if (spec != null) {
			return spec;
		}

		// Fallback to filesystem
		return loadFromFilesystem(agentId);
	}

	/**
	 * Load agent specification from classpath.
	 * @param agentId agent identifier
	 * @return agent specification or null if not found
	 */
	private static AgentSpec loadFromClasspath(String agentId) {
		String resourcePath = "/agents/" + agentId + ".yaml";
		log.info("Attempting to load agent spec from classpath: {}", resourcePath);

		try (InputStream is = AgentSpecLoader.class.getResourceAsStream(resourcePath)) {
			if (is == null) {
				log.info("Agent spec not found on classpath: {}", resourcePath);
				return null;
			}

			Yaml yaml = new Yaml();
			Map<String, Object> data = yaml.load(is);
			log.info("Loaded YAML data for agent from classpath: {}", agentId);

			return parseAgentSpec(data, agentId);
		}
		catch (IOException e) {
			log.error("Failed to load agent spec from classpath: {}", resourcePath, e);
			throw new RuntimeException("Failed to load agent spec: " + resourcePath, e);
		}
	}

	/**
	 * Load agent specification from filesystem (.agents directory).
	 * @param agentId agent identifier
	 * @return agent specification or null if not found
	 */
	private static AgentSpec loadFromFilesystem(String agentId) {
		Path agentFile = Path.of(".agents", agentId + ".yaml");
		log.info("Attempting to load agent spec from filesystem: {}", agentFile.toAbsolutePath());

		if (!Files.exists(agentFile)) {
			log.warn("Agent spec not found: {}", agentFile.toAbsolutePath());
			return null;
		}

		try {
			String yamlContent = Files.readString(agentFile);
			Yaml yaml = new Yaml();
			Map<String, Object> data = yaml.load(yamlContent);
			log.info("Loaded YAML data for agent from filesystem: {}", agentId);

			return parseAgentSpec(data, agentId);
		}
		catch (IOException e) {
			log.error("Failed to load agent spec from filesystem: {}", agentFile, e);
			throw new RuntimeException("Failed to load agent spec: " + agentFile, e);
		}
	}

	/**
	 * Parse agent specification from YAML data.
	 * @param data YAML data
	 * @param agentId agent identifier for logging
	 * @return parsed agent specification
	 */
	private static AgentSpec parseAgentSpec(Map<String, Object> data, String agentId) {
		String id = getString(data, "id");
		String version = getString(data, "version");
		Map<String, AgentSpec.InputDef> inputs = parseInputDefs(getMap(data, "inputs"));

		log.info("Parsed agent spec: id={}, version={}, inputs={}", id, version,
				inputs != null ? inputs.keySet() : null);
		return new AgentSpec(id, version, inputs);
	}

	/**
	 * Parse input definitions from YAML map structure.
	 * @param inputsMap raw YAML input definitions
	 * @return parsed input definitions
	 */
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