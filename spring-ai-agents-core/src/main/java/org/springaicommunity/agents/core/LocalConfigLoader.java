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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Loads and merges agent configuration from run.yaml and CLI arguments. Implements
 * precedence: AgentSpec defaults → run.yaml → CLI flags.
 *
 * @author Mark Pollack
 * @since 1.1.0
 */
public final class LocalConfigLoader {

	private static final Logger log = LoggerFactory.getLogger(LocalConfigLoader.class);

	private static final Set<String> ENVIRONMENT_FLAGS = Set.of("--sandbox", "--workdir");

	/**
	 * Load and merge configuration from run.yaml and CLI arguments.
	 * @param argv command line arguments
	 * @return merged LauncherSpec
	 * @throws IllegalArgumentException if configuration is invalid
	 */
	public static LauncherSpec load(String[] argv) {
		log.info("Loading launcher configuration from CLI args: {}", Arrays.toString(argv));
		Path cwd = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
		log.info("Current working directory: {}", cwd);

		// 1. Load run.yaml if present
		log.info("Loading run.yaml configuration");
		RunSpec runSpec = loadRunSpec(cwd.resolve("run.yaml"));

		// 2. Parse CLI arguments
		log.info("Parsing CLI arguments");
		RunSpec cliSpec = parseCliArgs(argv);

		// 3. Merge run specs (CLI overrides file)
		log.info("Merging configurations: file + CLI");
		RunSpec merged = mergeRunSpecs(runSpec, cliSpec);

		if (merged.agent() == null) {
			log.error("No agent specified in configuration");
			printUsage();
			throw new IllegalArgumentException("No agent specified. Use --agent <name> or set agent in run.yaml");
		}

		// 4. Load AgentSpec
		log.info("Loading AgentSpec for: {}", merged.agent());
		AgentSpec agentSpec = Launcher.loadAgentSpec(merged.agent());
		if (agentSpec == null) {
			log.error("Unknown agent: {}", merged.agent());
			throw new IllegalArgumentException("Unknown agent: " + merged.agent());
		}

		// 5. Merge inputs with AgentSpec defaults
		log.info("Merging inputs with AgentSpec defaults");
		Map<String, Object> finalInputs = Launcher.mergeWithDefaults(merged.inputs(), agentSpec);

		// 6. Resolve working directory
		Path workingDir = cwd;
		if (merged.workingDirectory() != null) {
			workingDir = Paths.get(merged.workingDirectory()).toAbsolutePath();
			log.info("Using custom working directory: {}", workingDir);
		}

		// 7. Build final LauncherSpec
		Map<String, Object> env = merged.env() != null ? new LinkedHashMap<>(merged.env()) : new LinkedHashMap<>();
		LauncherSpec launcher = new LauncherSpec(agentSpec, finalInputs, workingDir, env);
		log.info("Created LauncherSpec: agent={}, inputs={}", agentSpec.id(), finalInputs.keySet());

		return launcher;
	}

	/**
	 * Load RunSpec from YAML file.
	 * @param yamlPath path to YAML file
	 * @return RunSpec or empty spec if file doesn't exist
	 */
	static RunSpec loadRunSpec(Path yamlPath) {
		if (!Files.exists(yamlPath)) {
			log.info("No run.yaml found at: {}", yamlPath);
			return new RunSpec(null, Map.of(), null, Map.of());
		}

		try {
			String content = Files.readString(yamlPath);
			log.info("Loaded run.yaml content: {} chars", content.length());
			Yaml yaml = new Yaml();
			Map<String, Object> data = yaml.load(content);

			if (data == null) {
				log.info("Empty run.yaml file");
				return new RunSpec(null, Map.of(), null, Map.of());
			}

			String agent = getString(data, "agent");
			Map<String, Object> inputs = getMap(data, "inputs");
			String workingDirectory = getString(data, "workingDirectory");
			Map<String, Object> env = getMap(data, "env");

			log.info("Parsed run.yaml: agent={}, inputs={}, workingDirectory={}, env={}", agent, inputs.keySet(),
					workingDirectory, env.keySet());

			return new RunSpec(agent, inputs, workingDirectory, env);
		}
		catch (IOException e) {
			log.error("Failed to read run.yaml: {}", yamlPath, e);
			throw new RuntimeException("Failed to read " + yamlPath, e);
		}
	}

	/**
	 * Parse CLI arguments into RunSpec.
	 * @param argv command line arguments
	 * @return RunSpec with CLI values
	 */
	static RunSpec parseCliArgs(String[] argv) {
		String agent = null;
		String workingDirectory = null;
		Map<String, Object> inputs = new LinkedHashMap<>();
		Map<String, Object> env = new LinkedHashMap<>();

		for (int i = 0; i < argv.length; i++) {
			String arg = argv[i];

			if ("--agent".equals(arg) && i + 1 < argv.length) {
				agent = argv[++i];
				log.info("CLI agent: {}", agent);
			}
			else if ("--workdir".equals(arg) && i + 1 < argv.length) {
				workingDirectory = argv[++i];
				log.info("CLI working directory: {}", workingDirectory);
			}
			else if ("--sandbox".equals(arg) && i + 1 < argv.length) {
				String sandboxType = argv[++i];
				env.put("sandbox", sandboxType);
				log.info("CLI sandbox type: {}", sandboxType);
			}
			else if (arg.startsWith("--")) {
				// Generic input parameter
				String key = arg.substring(2).replace('-', '_');
				String value = (i + 1 < argv.length && !argv[i + 1].startsWith("--")) ? argv[++i] : "true";
				inputs.put(key, value);
				log.info("CLI input: {}={}", key, value);
			}
		}

		log.info("Parsed CLI args: agent={}, inputs={}, workingDirectory={}, env={}", agent, inputs.keySet(),
				workingDirectory, env.keySet());

		return new RunSpec(agent, inputs, workingDirectory, env);
	}

	/**
	 * Merge two RunSpecs with override taking precedence.
	 * @param base base RunSpec
	 * @param override overriding RunSpec
	 * @return merged RunSpec
	 */
	static RunSpec mergeRunSpecs(RunSpec base, RunSpec override) {
		String agent = override.agent() != null ? override.agent() : base.agent();
		String workingDirectory = override.workingDirectory() != null ? override.workingDirectory()
				: base.workingDirectory();

		Map<String, Object> inputs = new LinkedHashMap<>();
		if (base.inputs() != null) {
			inputs.putAll(base.inputs());
		}
		if (override.inputs() != null) {
			inputs.putAll(override.inputs());
		}

		Map<String, Object> env = new LinkedHashMap<>();
		if (base.env() != null) {
			env.putAll(base.env());
		}
		if (override.env() != null) {
			env.putAll(override.env());
		}

		log.info("Merged RunSpecs: agent={}, inputs={}, workingDirectory={}, env={}", agent, inputs.keySet(),
				workingDirectory, env.keySet());

		return new RunSpec(agent, inputs, workingDirectory, env);
	}

	/**
	 * Print usage information.
	 */
	static void printUsage() {
		System.err.println("Usage: jbang agents.java [options]");
		System.err.println("");
		System.err.println("Options:");
		System.err.println("  --agent <name>     Agent to run (hello-world, coverage)");
		System.err.println("  --sandbox <type>   Sandbox type (local, docker)");
		System.err.println("  --workdir <path>   Working directory for sandbox");
		System.err.println("  --<key> <value>    Agent input parameter");
		System.err.println("");
		System.err.println("");
		System.err.println("Examples:");
		System.err.println("  jbang agents.java --agent hello-world --path test.txt");
		System.err.println("  jbang agents.java --agent coverage --target_coverage 90");
		System.err.println("");
		System.err.println("Configuration file (run.yaml):");
		System.err.println("  agent: coverage");
		System.err.println("  inputs:");
		System.err.println("    target_coverage: 85");
	}

	// Helper methods
	private static String getString(Map<String, Object> map, String key) {
		Object value = map.get(key);
		return value != null ? value.toString() : null;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> getMap(Map<String, Object> map, String key) {
		Object value = map.get(key);
		if (value instanceof Map) {
			return (Map<String, Object>) value;
		}
		return Map.of();
	}

}