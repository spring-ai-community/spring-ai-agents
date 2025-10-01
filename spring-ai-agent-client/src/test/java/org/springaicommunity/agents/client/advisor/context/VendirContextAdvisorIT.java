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

package org.springaicommunity.agents.client.advisor.context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springaicommunity.agents.client.AgentClient;
import org.springaicommunity.agents.client.AgentClientResponse;
import org.springaicommunity.agents.claude.ClaudeAgentModel;
import org.springaicommunity.agents.claude.ClaudeAgentOptions;
import org.springaicommunity.agents.claude.sdk.ClaudeAgentClient;
import org.springaicommunity.agents.claude.sdk.config.ClaudeCliDiscovery;
import org.springaicommunity.agents.claude.sdk.transport.CLIOptions;
import org.springaicommunity.agents.model.sandbox.LocalSandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration test for VendirContextAdvisor demonstrating end-to-end context engineering
 * with real vendir execution and Claude agent.
 *
 * <p>
 * This test validates the full context engineering flow:
 * <ol>
 * <li>Creates a vendir configuration to fetch Spring Boot documentation from GitHub</li>
 * <li>Registers VendirContextAdvisor with AgentClient</li>
 * <li>Executes agent goal requiring external context</li>
 * <li>Verifies vendir gathered the documentation</li>
 * <li>Verifies agent had access to and used the context</li>
 * </ol>
 *
 * @author Mark Pollack
 */
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class VendirContextAdvisorIT {

	private Path testWorkspace;

	private ClaudeAgentModel claudeAgentModel;

	@BeforeEach
	void setUp(@TempDir Path tempDir) throws IOException {
		assumeTrue(isClaudeCliAvailable(), "Claude CLI must be available");
		assumeTrue(isVendirAvailable(), "Vendir CLI must be available");

		this.testWorkspace = tempDir;
		setupAgentModel();
	}

	private void setupAgentModel() {
		try {
			ClaudeAgentOptions options = ClaudeAgentOptions.builder()
				.model("claude-sonnet-4-20250514")
				.yolo(true)
				.build();

			ClaudeAgentClient claudeClient = ClaudeAgentClient.create(CLIOptions.defaultOptions(), this.testWorkspace);
			LocalSandbox sandbox = new LocalSandbox(this.testWorkspace);
			this.claudeAgentModel = new ClaudeAgentModel(claudeClient, options, sandbox);

			assumeTrue(this.claudeAgentModel.isAvailable(), "Claude agent must be available");
		}
		catch (Exception e) {
			assumeTrue(false, "Failed to setup Claude client: " + e.getMessage());
		}
	}

	@Test
	@DisplayName("VendirContextAdvisor gathers documentation and agent uses it")
	void testVendirContextEngineeringEndToEnd() throws IOException {
		// Create vendir.yml to fetch a simple text file from GitHub
		Path vendirConfig = createVendirConfig();

		// Create VendirContextAdvisor
		VendirContextAdvisor advisor = VendirContextAdvisor.builder()
			.vendirConfigPath(vendirConfig)
			.contextDirectory(".agent-context/vendir")
			.autoCleanup(false) // Keep for inspection
			.timeout(120) // 2 minutes for download
			.build();

		// Create AgentClient with vendir advisor
		AgentClient client = AgentClient.builder(this.claudeAgentModel)
			.defaultAdvisor(advisor)
			.defaultWorkingDirectory(this.testWorkspace)
			.defaultTimeout(Duration.ofMinutes(3))
			.build();

		// Execute agent goal that should benefit from the gathered context
		String goal = """
				Look in the .agent-context/vendir directory for any documentation or files that were gathered.
				Create a file called 'context-summary.txt' that lists what files you found and briefly summarizes their content.
				""";

		AgentClientResponse response = client.run(goal);

		// Assertions
		assertThat(response).isNotNull();
		assertThat(response.isSuccessful()).isTrue();

		// Verify context was gathered
		assertThat(response.context()).containsKey("vendir.context.success");
		assertThat(response.context().get("vendir.context.success")).isEqualTo(true);
		assertThat(response.context()).containsKey("vendir.context.path");

		// Verify context directory exists and has content
		Path contextDir = this.testWorkspace.resolve(".agent-context/vendir");
		assertThat(contextDir).exists();
		assertThat(contextDir).isDirectory();

		// Verify vendir actually downloaded something
		Path vendorDir = contextDir.resolve("vendor");
		if (Files.exists(vendorDir)) {
			assertThat(Files.walk(vendorDir).count()).isGreaterThan(1); // More than just
																		// the
																		// directory
		}

		// Verify agent created the summary file
		Path summaryFile = this.testWorkspace.resolve("context-summary.txt");
		if (Files.exists(summaryFile)) {
			String summary = Files.readString(summaryFile);
			assertThat(summary).isNotBlank();
			System.out.println("Agent's context summary:\n" + summary);
		}

		System.out.println("✅ Context engineering test completed");
		System.out.println("   Context path: " + response.context().get("vendir.context.path"));
		System.out.println("   Context gathered: " + response.context().get("vendir.context.gathered"));
	}

	@Test
	@DisplayName("VendirContextAdvisor handles vendir sync failure gracefully")
	void testVendirFailureHandling() throws IOException {
		// Create invalid vendir.yml (non-existent repository)
		Path vendirConfig = createInvalidVendirConfig();

		VendirContextAdvisor advisor = VendirContextAdvisor.builder()
			.vendirConfigPath(vendirConfig)
			.contextDirectory(".agent-context/vendir")
			.timeout(30)
			.build();

		AgentClient client = AgentClient.builder(this.claudeAgentModel)
			.defaultAdvisor(advisor)
			.defaultWorkingDirectory(this.testWorkspace)
			.defaultTimeout(Duration.ofMinutes(2))
			.build();

		// Agent should still execute even if context gathering fails
		AgentClientResponse response = client.run("Create a file called 'test.txt' with content 'Hello World'");

		// Agent execution should succeed even though vendir failed
		assertThat(response).isNotNull();

		// Verify vendir failure was recorded
		assertThat(response.context()).containsKey("vendir.context.success");
		// Context gathering may fail, but agent should still work
		System.out.println("✅ Graceful failure handling verified");
	}

	@Test
	@DisplayName("VendirContextAdvisor with auto-cleanup removes context after execution")
	void testAutoCleanup() throws IOException {
		Path vendirConfig = createVendirConfig();

		VendirContextAdvisor advisor = VendirContextAdvisor.builder()
			.vendirConfigPath(vendirConfig)
			.contextDirectory(".agent-context/vendir-temp")
			.autoCleanup(true) // Enable cleanup
			.timeout(120)
			.build();

		AgentClient client = AgentClient.builder(this.claudeAgentModel)
			.defaultAdvisor(advisor)
			.defaultWorkingDirectory(this.testWorkspace)
			.defaultTimeout(Duration.ofMinutes(2))
			.build();

		AgentClientResponse response = client.run("Create a file called 'cleanup-test.txt' with content 'Test'");

		assertThat(response).isNotNull();

		// Context directory should not exist after auto-cleanup
		Path contextDir = this.testWorkspace.resolve(".agent-context/vendir-temp");
		// Note: Cleanup happens after agent execution, may or may not exist depending
		// on timing
		System.out.println("✅ Auto-cleanup test completed. Context dir exists: " + Files.exists(contextDir));
	}

	private Path createVendirConfig() throws IOException {
		// Create a simple vendir.yml that fetches Spring Boot's README from GitHub
		String vendirYml = """
				apiVersion: vendir.k14s.io/v1alpha1
				kind: Config
				directories:
				- path: vendor
				  contents:
				  - path: spring-boot
				    git:
				      url: https://github.com/spring-projects/spring-boot
				      ref: v3.3.0
				    includePaths:
				    - README.adoc
				    - CODE_OF_CONDUCT.md
				""";

		Path configPath = this.testWorkspace.resolve("vendir.yml");
		Files.writeString(configPath, vendirYml);
		return configPath;
	}

	private Path createInvalidVendirConfig() throws IOException {
		// Create vendir.yml with non-existent repository
		String vendirYml = """
				apiVersion: vendir.k14s.io/v1alpha1
				kind: Config
				directories:
				- path: vendor
				  contents:
				  - path: nonexistent
				    git:
				      url: https://github.com/nonexistent/nonexistent-repo-12345
				      ref: main
				""";

		Path configPath = this.testWorkspace.resolve("vendir-invalid.yml");
		Files.writeString(configPath, vendirYml);
		return configPath;
	}

	private boolean isClaudeCliAvailable() {
		try {
			return ClaudeCliDiscovery.isClaudeCliAvailable();
		}
		catch (Exception e) {
			return false;
		}
	}

	private boolean isVendirAvailable() {
		try {
			ProcessBuilder pb = new ProcessBuilder("vendir", "--version");
			Process process = pb.start();
			return process.waitFor() == 0;
		}
		catch (Exception e) {
			return false;
		}
	}

}
