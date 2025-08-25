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
import java.util.Objects;

import org.springaicommunity.agents.model.AgentOptions;

/**
 * Represents an agent task goal, equivalent to Spring AI's Prompt class.
 *
 * <p>
 * This follows Spring AI's pattern where "Prompt" is the core abstraction at the API
 * level, and "Goal" serves the same purpose for agent tasks.
 * </p>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public class Goal {

	private final String content;

	private final Path workingDirectory;

	private final AgentOptions options;

	/**
	 * Create a simple goal with just content.
	 * @param content the goal content
	 */
	public Goal(String content) {
		this(content, null, null);
	}

	/**
	 * Create a goal with content and working directory.
	 * @param content the goal content
	 * @param workingDirectory the working directory (optional)
	 */
	public Goal(String content, Path workingDirectory) {
		this(content, workingDirectory, null);
	}

	/**
	 * Create a goal with all parameters.
	 * @param content the goal content
	 * @param workingDirectory the working directory (optional)
	 * @param options agent-specific options (optional)
	 */
	public Goal(String content, Path workingDirectory, AgentOptions options) {
		this.content = content != null ? content : "";
		this.workingDirectory = workingDirectory;
		this.options = options;
	}

	/**
	 * Get the goal content.
	 * @return the goal content
	 */
	public String getContent() {
		return this.content;
	}

	/**
	 * Get the working directory.
	 * @return the working directory, may be null
	 */
	public Path getWorkingDirectory() {
		return this.workingDirectory;
	}

	/**
	 * Get the agent options.
	 * @return the agent options, may be null
	 */
	public AgentOptions getOptions() {
		return this.options;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Goal goal))
			return false;
		return Objects.equals(this.content, goal.content)
				&& Objects.equals(this.workingDirectory, goal.workingDirectory)
				&& Objects.equals(this.options, goal.options);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.content, this.workingDirectory, this.options);
	}

	@Override
	public String toString() {
		return "Goal[" + "content='" + this.content + '\'' + ", workingDirectory=" + this.workingDirectory
				+ ", options=" + this.options + ']';
	}

}