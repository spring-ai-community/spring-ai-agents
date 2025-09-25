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

import org.springframework.ai.template.TemplateRenderer;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Main execution engine for agents. Loads agent specifications, merges configurations,
 * and delegates to appropriate agent implementations.
 *
 * @author Mark Pollack
 * @since 1.1.0
 */
public final class AgentRunner {

	private static final Map<String, AgentExecutor> AGENTS = new HashMap<>();

	static {
		// Register built-in agents
		AGENTS.put("hello-world", new HelloWorldExecutor());
		AGENTS.put("coverage", new CoverageExecutor());
	}

	/**
	 * Execute agent with given specification.
	 * @param spec launcher specification
	 * @return execution result
	 */
	public static Result execute(LauncherSpec spec) {
		try {
			// 1. Load agent specification
			AgentSpec agentSpec = loadAgentSpec(spec.agent());
			if (agentSpec == null) {
				return Result.fail("Unknown agent: " + spec.agent() + ". Available: " + AGENTS.keySet());
			}

			// 2. Merge inputs with defaults
			Map<String, Object> mergedInputs = mergeWithDefaults(spec.inputs(), agentSpec);

			// 3. Validate required inputs
			Result validation = validateInputs(mergedInputs, agentSpec);
			if (!validation.success()) {
				return validation;
			}

			// 4. Create final spec with merged inputs
			LauncherSpec finalSpec = new LauncherSpec(spec.agent(), mergedInputs, spec.tweak(), spec.cwd(), spec.env());

			// 5. Execute agent
			AgentExecutor executor = AGENTS.get(spec.agent());
			return executor.execute(finalSpec, agentSpec);
		}
		catch (Exception e) {
			return Result.fail("Agent execution failed: " + e.getMessage());
		}
	}

	/**
	 * Load agent specification from classpath.
	 * @param agentId agent identifier
	 * @return agent specification or null if not found
	 */
	static AgentSpec loadAgentSpec(String agentId) {
		String resourcePath = "/agents/" + agentId + ".yaml";
		try (InputStream is = AgentRunner.class.getResourceAsStream(resourcePath)) {
			if (is == null) {
				return null;
			}

			Yaml yaml = new Yaml();
			Map<String, Object> data = yaml.load(is);

			String id = getString(data, "id");
			String version = getString(data, "version");
			Map<String, AgentSpec.InputDef> inputs = parseInputDefs(getMap(data, "inputs"));
			AgentSpec.PromptSpec prompt = parsePromptSpec(getMap(data, "prompt"));

			return new AgentSpec(id, version, inputs, prompt);
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to load agent spec: " + resourcePath, e);
		}
	}

	/**
	 * Merge runtime inputs with agent defaults.
	 * @param runtimeInputs runtime input values
	 * @param agentSpec agent specification with defaults
	 * @return merged inputs
	 */
	static Map<String, Object> mergeWithDefaults(Map<String, Object> runtimeInputs, AgentSpec agentSpec) {
		Map<String, Object> merged = new LinkedHashMap<>();

		// Start with defaults from AgentSpec
		if (agentSpec.inputs() != null) {
			agentSpec.inputs().forEach((key, inputDef) -> {
				if (inputDef.defaultValue() != null) {
					merged.put(key, inputDef.defaultValue());
				}
			});
		}

		// Override with runtime values
		merged.putAll(runtimeInputs);

		return merged;
	}

	/**
	 * Validate required inputs are present.
	 * @param inputs merged input values
	 * @param agentSpec agent specification
	 * @return validation result
	 */
	static Result validateInputs(Map<String, Object> inputs, AgentSpec agentSpec) {
		if (agentSpec.inputs() == null) {
			return Result.ok("No inputs to validate");
		}

		List<String> missing = new ArrayList<>();
		agentSpec.inputs().forEach((key, inputDef) -> {
			if (inputDef.required() && !inputs.containsKey(key)) {
				missing.add(key + " (" + inputDef.type() + ")");
			}
		});

		if (!missing.isEmpty()) {
			return Result.fail("Missing required inputs: " + String.join(", ", missing));
		}

		return Result.ok("Inputs validated");
	}

	/**
	 * Render template with inputs and tweak using Spring AI's TemplateRenderer.
	 * @param template StringTemplate template
	 * @param inputs input values
	 * @param tweak tweak value (can be null)
	 * @return rendered string
	 */
	static String renderTemplate(String template, Map<String, Object> inputs, String tweak) {
		if (template == null) {
			return "";
		}

		Map<String, Object> context = new LinkedHashMap<>(inputs);
		if (tweak != null && !tweak.isBlank()) {
			context.put("tweak", tweak);
		}

		TemplateRenderer renderer = StTemplateRenderer.builder().build();
		return renderer.apply(template, context);
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

	private static AgentSpec.PromptSpec parsePromptSpec(Map<String, Object> promptMap) {
		if (promptMap == null || promptMap.isEmpty()) {
			return null;
		}
		String system = getString(promptMap, "system");
		String userTemplate = getString(promptMap, "userTemplate");
		return new AgentSpec.PromptSpec(system, userTemplate);
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

	/**
	 * Interface for agent executors.
	 */
	interface AgentExecutor {

		Result execute(LauncherSpec spec, AgentSpec agentSpec);

	}

	/**
	 * Hello World agent executor.
	 */
	static class HelloWorldExecutor implements AgentExecutor {

		@Override
		public Result execute(LauncherSpec spec, AgentSpec agentSpec) {
			try {
				String path = (String) spec.inputs().get("path");
				String content = (String) spec.inputs().get("content");

				if (path == null) {
					return Result.fail("Missing required input: path");
				}
				if (content == null) {
					return Result.fail("Missing required input: content");
				}

				java.nio.file.Path targetFile = spec.cwd().resolve(path);
				java.nio.file.Files.createDirectories(targetFile.getParent());
				java.nio.file.Files.writeString(targetFile, content);

				return Result.ok("Created file: " + targetFile.toAbsolutePath());
			}
			catch (Exception e) {
				return Result.fail("Failed to create file: " + e.getMessage());
			}
		}

	}

	/**
	 * Coverage agent executor (stub implementation).
	 */
	static class CoverageExecutor implements AgentExecutor {

		@Override
		public Result execute(LauncherSpec spec, AgentSpec agentSpec) {
			if (agentSpec.prompt() == null) {
				return Result.fail("Coverage agent requires prompt specification");
			}

			// Render the prompt template
			String systemPrompt = renderTemplate(agentSpec.prompt().system(), spec.inputs(), spec.tweak());
			String userPrompt = renderTemplate(agentSpec.prompt().userTemplate(), spec.inputs(), spec.tweak());

			String fullPrompt = systemPrompt + "\n\n" + userPrompt;

			// For now, just show the rendered prompt
			// Future: integrate with AgentModel from spring-ai-agent-model
			return Result.ok("Prepared coverage prompt (length=" + fullPrompt.length() + " chars):\n" + fullPrompt);
		}

	}

}