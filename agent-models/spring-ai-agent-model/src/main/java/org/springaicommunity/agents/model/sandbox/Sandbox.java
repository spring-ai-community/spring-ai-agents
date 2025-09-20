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
package org.springaicommunity.agents.model.sandbox;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Sandbox interface for executing commands in isolated environments.
 *
 * <p>
 * Provides secure execution of agent commands with proper isolation and resource
 * management. Implementations should ensure commands cannot affect the host system.
 */
public interface Sandbox extends AutoCloseable {

	/**
	 * Execute a command specification in the sandbox.
	 * @param spec the execution specification containing command, environment, etc.
	 * @return the execution result
	 * @throws IOException if execution fails due to I/O errors
	 * @throws InterruptedException if execution is interrupted
	 * @throws TimeoutException if execution times out
	 */
	ExecResult exec(ExecSpec spec) throws IOException, InterruptedException, TimeoutException;

	/**
	 * Get the working directory path within the sandbox.
	 * @return the sandbox working directory
	 */
	Path workDir();

	/**
	 * Check if this sandbox has been closed.
	 * @return true if closed, false otherwise
	 */
	boolean isClosed();

	/**
	 * Close the sandbox and release resources.
	 * @throws IOException if cleanup fails
	 */
	@Override
	void close() throws IOException;

}