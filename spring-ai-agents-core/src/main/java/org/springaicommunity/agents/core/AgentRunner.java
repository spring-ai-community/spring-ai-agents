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

/**
 * Interface for running agents with two-phase lifecycle: setup and execute. Agent runners
 * encapsulate the agent's internal logic and implementation details as black boxes.
 *
 * <p>
 * The two-phase approach enables:
 * <ul>
 * <li>Fail-fast on prerequisites before consuming AI resources</li>
 * <li>Clear separation of deterministic setup vs autonomous AI execution</li>
 * <li>Context passing between phases (workspace, baseline metrics, etc.)</li>
 * <li>Runtime tool configuration for MCP integration</li>
 * </ul>
 *
 * @author Mark Pollack
 * @since 1.1.0
 */
public interface AgentRunner {

	/**
	 * Setup phase: Prepare infrastructure (DETERMINISTIC - NO AI).
	 *
	 * <p>
	 * This phase performs prerequisite operations before agent execution: - Clone
	 * repositories - Verify code compiles - Run existing tests - Measure baseline metrics
	 * - Configure runtime tools (MCP)
	 *
	 * <p>
	 * Failures here prevent wasting AI resources on broken prerequisites.
	 * @param spec launcher specification containing agent definition, inputs, and
	 * execution context
	 * @return setup context with workspace, metadata, and success/failure state
	 * @throws Exception if setup encounters unrecoverable error
	 */
	default SetupContext setup(LauncherSpec spec) throws Exception {
		return SetupContext.empty();
	}

	/**
	 * Execute phase: AI agent performs task (AUTONOMOUS EXECUTION).
	 *
	 * <p>
	 * Agent runs to completion autonomously. If execution fails at any step, failure
	 * information is captured in the Result. No human intervention expected.
	 * @param setup setup context from setup phase
	 * @param spec launcher specification
	 * @return execution result with structured outputs
	 * @throws Exception if execution encounters unrecoverable error
	 */
	Result run(SetupContext setup, LauncherSpec spec) throws Exception;

}