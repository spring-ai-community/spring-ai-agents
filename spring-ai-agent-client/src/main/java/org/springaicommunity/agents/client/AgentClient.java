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

package org.springaicommunity.agents.client;

import java.nio.file.Path;
import java.time.Duration;
import java.util.function.Consumer;

import org.springaicommunity.agents.model.AgentModel;
import org.springaicommunity.agents.model.AgentOptions;

/**
 * Client-level facade for agent interactions, following Spring AI's ChatClient pattern.
 *
 * <p>
 * This interface provides a fluent API for interacting with autonomous agents while
 * hiding the complexity of the underlying model layer. It follows Spring AI's two-layer
 * architecture where AgentClient is the high-level API and AgentModel is the low-level
 * model interface.
 * </p>
 *
 * <p>
 * Use {@link AgentClient#builder(AgentModel)} to prepare an instance.
 * </p>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public interface AgentClient {

	/**
	 * Create a new agent client from an agent model.
	 * @param agentModel the underlying agent model
	 * @return a new agent client
	 */
	static AgentClient create(AgentModel agentModel) {
		return builder(agentModel).build();
	}

	/**
	 * Create a new agent client builder.
	 * @param agentModel the underlying agent model
	 * @return a new builder
	 */
	static Builder builder(AgentModel agentModel) {
		return new DefaultAgentClientBuilder(agentModel);
	}

	/**
	 * Create a new agent client request spec - equivalent to ChatClient.prompt().
	 * @return a new request builder
	 */
	AgentClientRequestSpec goal();

	/**
	 * Create a new agent client builder with the specified goal - equivalent to
	 * ChatClient.prompt(String).
	 * @param goal the task goal content
	 * @return a new request builder
	 */
	AgentClientRequestSpec goal(String goal);

	/**
	 * Create a new agent client builder with the specified goal object - equivalent to
	 * ChatClient.prompt(Prompt).
	 * @param goal the goal object
	 * @return a new request builder
	 */
	AgentClientRequestSpec goal(Goal goal);

	/**
	 * Execute a simple goal and return response - convenience method matching
	 * example-code.md.
	 * @param goalText the task goal content
	 * @return the agent response
	 */
	AgentClientResponse run(String goalText);

	/**
	 * Execute a goal with options and return response - convenience method matching
	 * example-code.md.
	 * @param goalText the task goal content
	 * @param agentOptions the agent options
	 * @return the agent response
	 */
	AgentClientResponse run(String goalText, AgentOptions agentOptions);

	/**
	 * Return a {@link AgentClient.Builder} to create a new {@link AgentClient} whose
	 * settings are replicated from this client.
	 */
	Builder mutate();

	/**
	 * Request specification builder interface following ChatClient pattern.
	 */
	interface AgentClientRequestSpec {

		/**
		 * Set the goal content for the agent task.
		 * @param goalContent the task goal content
		 * @return this request spec for chaining
		 */
		AgentClientRequestSpec goal(String goalContent);

		/**
		 * Set the working directory for the agent task.
		 * @param workingDirectory the working directory path
		 * @return this request spec for chaining
		 */
		AgentClientRequestSpec workingDirectory(Path workingDirectory);

		/**
		 * Execute the agent task and return the result.
		 *
		 * In agent terminology, we **run a goal** (task execution). This differs from
		 * ChatClient, where you **call a prompt**.
		 * @return the agent response
		 */
		AgentClientResponse run();

	}

	/**
	 * A mutable builder for creating an {@link AgentClient}.
	 */
	interface Builder {

		/**
		 * Set default options for all agent requests.
		 * @param agentOptions default agent options
		 * @return this builder for chaining
		 */
		Builder defaultOptions(AgentOptions agentOptions);

		/**
		 * Set default working directory for all agent requests.
		 * @param workingDirectory default working directory
		 * @return this builder for chaining
		 */
		Builder defaultWorkingDirectory(Path workingDirectory);

		/**
		 * Set default timeout for all agent requests.
		 * @param timeout default timeout
		 * @return this builder for chaining
		 */
		Builder defaultTimeout(Duration timeout);

		/**
		 * Create a new {@link AgentClient} with the configured defaults.
		 * @return a new agent client
		 */
		AgentClient build();

	}

}