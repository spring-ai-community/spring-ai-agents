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

package org.springaicommunity.agents.samples.contextengineering;

import java.nio.file.Path;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springaicommunity.agents.client.AgentClient;
import org.springaicommunity.agents.client.AgentClientResponse;
import org.springaicommunity.agents.client.advisor.context.VendirContextAdvisor;
import org.springaicommunity.agents.model.AgentModel;

/**
 * Demonstrates VendirContextAdvisor for automatic context gathering from external
 * sources.
 *
 * <p>
 * This runner shows three patterns for context engineering:
 * </p>
 * <ol>
 * <li>Shallow Git Clone - Efficiently clone repositories with minimal history</li>
 * <li>HTTP Source - Fetch specific files from HTTP URLs</li>
 * <li>Inline Content - Provide static context without network access</li>
 * </ol>
 *
 * <p>
 * Prerequisites:
 * </p>
 * <ul>
 * <li>ANTHROPIC_API_KEY environment variable or Claude CLI session authentication</li>
 * <li>Claude CLI installed: npm install -g @anthropic-ai/claude-code</li>
 * <li>Vendir CLI installed: See https://carvel.dev/vendir/</li>
 * </ul>
 *
 * @author Spring AI Community
 */
@Component
public class ContextEngineeringRunner implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(ContextEngineeringRunner.class);

	private final AgentModel agentModel;

	public ContextEngineeringRunner(AgentModel agentModel) {
		this.agentModel = agentModel;
	}

	@Override
	public void run(String... args) {
		log.info("Starting Context Engineering sample...");

		// Working directory for all examples
		Path workingDir = Path.of(System.getProperty("user.dir"));

		// Example 1: Shallow Git Clone - Most common and efficient pattern
		runShallowGitCloneExample(workingDir);

		// Example 2: HTTP Source - Fetch specific files
		runHttpSourceExample(workingDir);

		// Example 3: Inline Content - Static context without network
		runInlineContentExample(workingDir);

		log.info("All context engineering examples completed!");
	}

	/**
	 * Example 1: Shallow Git Clone Pattern
	 *
	 * <p>
	 * This is the most efficient way to provide repository context to agents: - depth=1:
	 * Only latest commit (no history) - ref: Specific tag/branch for reproducibility -
	 * includePaths: Optional filtering for large repos
	 * </p>
	 */
	private void runShallowGitCloneExample(Path workingDir) {
		log.info("\n=== Example 1: Shallow Git Clone ===");

		// Create advisor pointing to vendir-git.yml
		VendirContextAdvisor advisor = VendirContextAdvisor.builder()
			.vendirConfigPath("vendir-git.yml")
			.contextDirectory(".agent-context/git")
			.autoCleanup(false) // Keep for inspection
			.timeout(120) // 2 minutes for git clone
			.build();

		// Create client with advisor
		AgentClient client = AgentClient.builder(this.agentModel)
			.defaultAdvisor(advisor)
			.defaultWorkingDirectory(workingDir)
			.defaultTimeout(Duration.ofMinutes(3))
			.build();

		// Agent goal using the cloned repository
		String goal = """
				Analyze the Spring REST service guide in .agent-context/git/vendor/spring-guide/.

				Create a file called 'guide-analysis.md' with:
				1. What this guide teaches
				2. The main Spring Boot features used
				3. Key class and package names
				4. How to run the application
				""";

		AgentClientResponse response = client.run(goal);

		logResponse("Shallow Git Clone", response);
	}

	/**
	 * Example 2: HTTP Source Pattern
	 *
	 * <p>
	 * Fetch specific files from HTTP URLs without cloning entire repositories. Useful
	 * for: - Configuration files - Documentation - API schemas
	 * </p>
	 */
	private void runHttpSourceExample(Path workingDir) {
		log.info("\n=== Example 2: HTTP Source ===");

		VendirContextAdvisor advisor = VendirContextAdvisor.builder()
			.vendirConfigPath("vendir-http.yml")
			.contextDirectory(".agent-context/http")
			.autoCleanup(false)
			.timeout(60)
			.build();

		AgentClient client = AgentClient.builder(this.agentModel)
			.defaultAdvisor(advisor)
			.defaultWorkingDirectory(workingDir)
			.defaultTimeout(Duration.ofMinutes(2))
			.build();

		String goal = """
				Analyze the Spring Boot parent POM in .agent-context/http/vendor/spring-boot-pom/.

				Create 'pom-analysis.md' with:
				1. Spring Boot version
				2. Java version requirements
				3. Key plugin configurations
				""";

		AgentClientResponse response = client.run(goal);

		logResponse("HTTP Source", response);
	}

	/**
	 * Example 3: Inline Content Pattern
	 *
	 * <p>
	 * Provide static context without network access. Useful for: - Development guidelines
	 * - API documentation - Code templates - Best practices
	 * </p>
	 */
	private void runInlineContentExample(Path workingDir) {
		log.info("\n=== Example 3: Inline Content ===");

		VendirContextAdvisor advisor = VendirContextAdvisor.builder()
			.vendirConfigPath("vendir-inline.yml")
			.contextDirectory(".agent-context/inline")
			.autoCleanup(false)
			.timeout(30)
			.build();

		AgentClient client = AgentClient.builder(this.agentModel)
			.defaultAdvisor(advisor)
			.defaultWorkingDirectory(workingDir)
			.defaultTimeout(Duration.ofMinutes(2))
			.build();

		String goal = """
				Read the guidelines in .agent-context/inline/vendor/docs/ and create a Java class
				called 'UserService.java' that follows these guidelines:
				- Uses meaningful variable names
				- Has constructor injection (no @Autowired)
				- Includes basic JavaDoc
				- Has a simple findById method
				""";

		AgentClientResponse response = client.run(goal);

		logResponse("Inline Content", response);
	}

	private void logResponse(String exampleName, AgentClientResponse response) {
		if (response.isSuccessful()) {
			log.info("✅ {} completed successfully!", exampleName);
			log.info("Context gathered: {}", response.context().get("vendir.context.gathered"));
			log.info("Context path: {}", response.context().get("vendir.context.path"));
		}
		else {
			log.error("❌ {} failed: {}", exampleName, response.getResult());
		}
	}

}
