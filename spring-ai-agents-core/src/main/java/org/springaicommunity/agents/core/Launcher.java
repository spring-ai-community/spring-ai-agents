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

import java.util.HashMap;
import java.util.Map;

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
				Class<?> agentClass = Class.forName("org.springaicommunity.agents.helloworld.HelloWorldAgentRunner");
				AgentRunner agent = (AgentRunner) agentClass.getDeclaredConstructor().newInstance();
				agents.put("hello-world", agent);
				log.info("Loaded HelloWorldAgentRunner dynamically");
			}
			catch (Exception e) {
				log.warn("Failed to load HelloWorldAgentRunner: {}", e.getMessage());
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
			// AgentSpec is already loaded in LauncherSpec
			AgentSpec agentSpec = spec.agentSpec();
			log.info("Using agent spec: {}", agentSpec);

			// Execute agent (agent handles its own validation)
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
		return AgentSpecLoader.loadAgentSpec(agentId);
	}

}