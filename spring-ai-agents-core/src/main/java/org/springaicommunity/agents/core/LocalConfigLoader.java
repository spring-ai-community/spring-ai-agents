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

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * CLI-only configuration loader. Parses agent ID and inputs from command line arguments,
 * with optional runspec.yaml for environment settings.
 *
 * @author Mark Pollack
 * @since 1.1.0
 */
public final class LocalConfigLoader {

	private static final Pattern AGENT_ID = Pattern.compile("^[a-z0-9][a-z0-9-]{0,63}$");

	private static final String ENV_RUNSPEC = "SPRING_AI_RUNSPEC";

	private LocalConfigLoader() {
	}

	/**
	 * CLI always supplies inputs; optional runspec provides cwd/env only.
	 * @param argv command line arguments: &lt;agentId&gt; key=value key2=value2 ...
	 * @return LauncherSpec ready for execution
	 */
	public static LauncherSpec load(String[] argv) {
		return load(argv, Path.of("."));
	}

	/**
	 * Package-private method for testing with custom base directory.
	 * @param argv command line arguments
	 * @param baseDir base directory for resolving relative paths
	 * @return LauncherSpec ready for execution
	 */
	static LauncherSpec load(String[] argv, Path baseDir) {
		Objects.requireNonNull(argv, "argv");
		if (argv.length == 0) {
			throw new IllegalArgumentException("Usage: launcher <agentId> key=value [key2=value2 ...]");
		}

		// 1) agent id
		String agentId = argv[0];
		if (!AGENT_ID.matcher(agentId).matches()) {
			throw new IllegalArgumentException("Invalid agent id: " + agentId);
		}

		// 2) inputs from key=value (strings; split on first '='; last wins)
		Map<String, Object> inputs = new LinkedHashMap<>();
		for (int i = 1; i < argv.length; i++) {
			String token = argv[i];
			int eq = token.indexOf('=');
			if (eq <= 0) {
				throw new IllegalArgumentException("Expected key=value token, got: " + token);
			}
			String key = token.substring(0, eq);
			String val = token.substring(eq + 1); // may be empty or contain '='
			inputs.put(key, val);
		}

		// 3) load AgentSpec (classpath/.agents per existing loader)
		AgentSpec agentSpec = Launcher.loadAgentSpec(agentId);
		if (agentSpec == null) {
			throw new IllegalArgumentException("Unknown agent: " + agentId + ". Check agent availability.");
		}

		// 4) optional runspec for cwd/env
		Path cwd = baseDir;
		Map<String, Object> env = Map.of();

		Path runspec = resolveRunSpecPath(baseDir);
		if (runspec != null) {
			RunSpec runSpecData = loadRunSpec(runspec);
			String wd = runSpecData.workingDirectory();
			if (wd != null && !wd.isBlank()) {
				cwd = Path.of(wd);
			}

			Map<String, Object> envData = runSpecData.env();
			if (envData != null) {
				env = new LinkedHashMap<>(envData);
			}
		}

		return new LauncherSpec(agentSpec, inputs, cwd, env);
	}

	/**
	 * Resolve runspec file path using priority order.
	 * @param baseDir base directory for resolving relative paths
	 * @return Path to runspec file or null if none found
	 */
	private static Path resolveRunSpecPath(Path baseDir) {
		String override = System.getenv(ENV_RUNSPEC);
		if (override != null && !override.isBlank()) {
			Path p = Path.of(override);
			if (Files.exists(p)) {
				return p;
			}
			// If env var is set but file doesn't exist, just skip it (no error)
		}

		// Check .agents/ directory first (preferred)
		Path agentsRunYaml = baseDir.resolve(".agents").resolve("run.yaml");
		if (Files.exists(agentsRunYaml)) {
			return agentsRunYaml;
		}

		// Fallback to root level (backward compatibility)
		Path runYaml = baseDir.resolve("run.yaml");
		if (Files.exists(runYaml)) {
			return runYaml;
		}
		Path runspecYaml = baseDir.resolve("runspec.yaml");
		if (Files.exists(runspecYaml)) {
			return runspecYaml;
		}
		return null;
	}

	/**
	 * Load RunSpec from YAML file.
	 * @param file Path to YAML file
	 * @return RunSpec with workingDirectory and env
	 */
	private static RunSpec loadRunSpec(Path file) {
		try (InputStream in = Files.newInputStream(file)) {
			LoaderOptions opts = new LoaderOptions();
			opts.setAllowDuplicateKeys(false);
			opts.setMaxAliasesForCollections(50);
			Object obj = new Yaml(opts).load(in);
			if (!(obj instanceof Map<?, ?> map)) {
				throw new IllegalArgumentException("YAML root must be a mapping: " + file);
			}

			String workingDirectory = getString(map, "workingDirectory");
			Map<String, Object> env = getMap(map, "env");

			return new RunSpec(null, null, workingDirectory, env);
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Failed to read " + file + ": " + e.getMessage(), e);
		}
	}

	/**
	 * Get string value from map.
	 * @param map source map
	 * @param key key to lookup
	 * @return string value or null
	 */
	private static String getString(Map<?, ?> map, String key) {
		Object value = map.get(key);
		return value != null ? value.toString() : null;
	}

	/**
	 * Get map value from map.
	 * @param map source map
	 * @param key key to lookup
	 * @return map value or empty map
	 */
	@SuppressWarnings("unchecked")
	private static Map<String, Object> getMap(Map<?, ?> map, String key) {
		Object value = map.get(key);
		if (value instanceof Map<?, ?> m) {
			Map<String, Object> result = new LinkedHashMap<>();
			m.forEach((k, v) -> result.put(String.valueOf(k), v));
			return result;
		}
		return Map.of();
	}

}