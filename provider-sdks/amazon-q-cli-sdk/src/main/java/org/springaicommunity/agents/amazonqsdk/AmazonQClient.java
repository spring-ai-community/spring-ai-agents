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

package org.springaicommunity.agents.amazonqsdk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agents.amazonqsdk.transport.AmazonQCliDiscovery;
import org.springaicommunity.agents.amazonqsdk.transport.CLITransport;
import org.springaicommunity.agents.amazonqsdk.types.ExecuteOptions;
import org.springaicommunity.agents.amazonqsdk.types.ExecuteResult;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Client for interacting with the Amazon Q Developer CLI.
 *
 * @author Spring AI Community
 * @since 0.1.0
 */
public class AmazonQClient {

	private static final Logger logger = LoggerFactory.getLogger(AmazonQClient.class);

	private final CLITransport transport;

	private final Path workingDirectory;

	/**
	 * Creates a new AmazonQClient with the given transport and working directory.
	 * @param transport the CLI transport
	 * @param workingDirectory the working directory for execution
	 */
	public AmazonQClient(CLITransport transport, Path workingDirectory) {
		this.transport = transport;
		this.workingDirectory = workingDirectory;
	}

	/**
	 * Creates a new AmazonQClient with auto-discovered Q CLI and default working
	 * directory.
	 * @return a new AmazonQClient instance
	 */
	public static AmazonQClient create() {
		return create(Paths.get(System.getProperty("user.dir")));
	}

	/**
	 * Creates a new AmazonQClient with auto-discovered Q CLI and specified working
	 * directory.
	 * @param workingDirectory the working directory
	 * @return a new AmazonQClient instance
	 */
	public static AmazonQClient create(Path workingDirectory) {
		String qCliPath = AmazonQCliDiscovery.discoverQCli();
		return create(workingDirectory, qCliPath);
	}

	/**
	 * Creates a new AmazonQClient with specified working directory and Q CLI path.
	 * @param workingDirectory the working directory
	 * @param qCliPath the path to Q CLI executable
	 * @return a new AmazonQClient instance
	 */
	public static AmazonQClient create(Path workingDirectory, String qCliPath) {
		CLITransport transport = new CLITransport(qCliPath, workingDirectory);
		return new AmazonQClient(transport, workingDirectory);
	}

	/**
	 * Executes a prompt with the given options.
	 * @param prompt the prompt to execute
	 * @param options execution options
	 * @return the execution result
	 */
	public ExecuteResult execute(String prompt, ExecuteOptions options) {
		logger.info("Executing prompt with Amazon Q");
		return transport.execute(prompt, options);
	}

	/**
	 * Executes a prompt with default options.
	 * @param prompt the prompt to execute
	 * @return the execution result
	 */
	public ExecuteResult execute(String prompt) {
		return execute(prompt, ExecuteOptions.builder().build());
	}

	/**
	 * Resumes a previous conversation with a new prompt.
	 * @param prompt the new prompt
	 * @param options execution options (resume flag will be automatically set)
	 * @return the execution result
	 */
	public ExecuteResult resume(String prompt, ExecuteOptions options) {
		logger.info("Resuming conversation with Amazon Q");
		ExecuteOptions resumeOptions = ExecuteOptions.builder()
			.model(options.getModel())
			.timeout(options.getTimeout())
			.trustAllTools(options.isTrustAllTools())
			.trustTools(options.getTrustTools())
			.noInteractive(options.isNoInteractive())
			.agent(options.getAgent())
			.resume(true) // Force resume mode
			.verbose(options.isVerbose())
			.build();

		return transport.execute(prompt, resumeOptions);
	}

	/**
	 * Resumes a previous conversation with default options.
	 * @param prompt the new prompt
	 * @return the execution result
	 */
	public ExecuteResult resume(String prompt) {
		return resume(prompt, ExecuteOptions.builder().build());
	}

	/**
	 * Checks if Amazon Q CLI is available.
	 * @return true if available
	 */
	public boolean isAvailable() {
		return AmazonQCliDiscovery.isAvailable();
	}

	public Path getWorkingDirectory() {
		return workingDirectory;
	}

}
