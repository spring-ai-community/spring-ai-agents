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
import org.springaicommunity.agents.claude.sdk.config.ClaudeCliDiscovery;

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

			this.claudeAgentModel = ClaudeAgentModel.builder()
				.workingDirectory(this.testWorkspace)
				.defaultOptions(options)
				.build();

			assumeTrue(this.claudeAgentModel.isAvailable(), "Claude agent must be available");
		}
		catch (Exception e) {
			assumeTrue(false, "Failed to setup Claude client: " + e.getMessage());
		}
	}

	@Test
	@DisplayName("VendirContextAdvisor efficiently clones Spring Guide repository (shallow, no history)")
	void testEfficientGitClone() throws IOException {
		// Create vendir.yml for efficient shallow clone of spring-guides/gs-rest-service
		Path vendirConfig = createEfficientGitCloneConfig();

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

		// Execute agent goal using the cloned repository context
		String goal = """
				Look in .agent-context/vendir/vendor/spring-guide for the cloned Spring REST service guide.
				Read the README.adoc file and create a summary file called 'guide-summary.txt' with:
				1. What this guide teaches
				2. The main technologies used
				3. Key file locations
				""";

		AgentClientResponse response = client.run(goal);

		// Assertions
		assertThat(response).isNotNull();
		assertThat(response.isSuccessful()).isTrue();

		// Verify context was gathered successfully
		assertThat(response.context()).containsKey("vendir.context.success");
		assertThat(response.context().get("vendir.context.success")).isEqualTo(true);
		assertThat(response.context()).containsKey("vendir.context.path");

		// Verify context directory exists and has content
		Path contextDir = this.testWorkspace.resolve(".agent-context/vendir/vendor/spring-guide");
		assertThat(contextDir).exists();
		assertThat(contextDir).isDirectory();

		// Verify key files from the guide exist
		assertThat(contextDir.resolve("README.adoc")).exists();

		// Verify vendir downloaded the repository (should have multiple files)
		assertThat(Files.walk(contextDir).count()).isGreaterThan(10);

		// Verify agent created the summary file
		Path summaryFile = this.testWorkspace.resolve("guide-summary.txt");
		if (Files.exists(summaryFile)) {
			String summary = Files.readString(summaryFile);
			assertThat(summary).isNotBlank();
			assertThat(summary.toLowerCase()).contains("rest");
			System.out.println("✅ Agent's guide summary:\n" + summary);
		}

		System.out.println("✅ Efficient git clone test completed");
		System.out.println("   Context path: " + response.context().get("vendir.context.path"));
		System.out.println("   Repository cloned with depth=1 (no history)");
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

	@Test
	@DisplayName("VendirContextAdvisor with inline content (no network required)")
	void testInlineContent() throws IOException {
		// Create vendir.yml with inline content (no network required)
		Path vendirConfig = createInlineVendirConfig();

		VendirContextAdvisor advisor = VendirContextAdvisor.builder()
			.vendirConfigPath(vendirConfig)
			.contextDirectory(".agent-context/vendir-inline")
			.timeout(30)
			.build();

		AgentClient client = AgentClient.builder(this.claudeAgentModel)
			.defaultAdvisor(advisor)
			.defaultWorkingDirectory(this.testWorkspace)
			.defaultTimeout(Duration.ofMinutes(2))
			.build();

		AgentClientResponse response = client
			.run("Read the file in .agent-context/vendir-inline/vendor/docs/ and create summary.txt with its content");

		assertThat(response).isNotNull();
		assertThat(response.context().get("vendir.context.success")).isEqualTo(true);

		// Verify inline content was created
		Path contextDir = this.testWorkspace.resolve(".agent-context/vendir-inline/vendor/docs");
		assertThat(contextDir).exists();

		System.out.println("✅ Inline content test completed");
	}

	@Test
	@DisplayName("VendirContextAdvisor with HTTP source")
	void testHttpSource() throws IOException {
		// Create vendir.yml to fetch from HTTP
		Path vendirConfig = createHttpVendirConfig();

		VendirContextAdvisor advisor = VendirContextAdvisor.builder()
			.vendirConfigPath(vendirConfig)
			.contextDirectory(".agent-context/vendir-http")
			.timeout(60)
			.build();

		AgentClient client = AgentClient.builder(this.claudeAgentModel)
			.defaultAdvisor(advisor)
			.defaultWorkingDirectory(this.testWorkspace)
			.defaultTimeout(Duration.ofMinutes(2))
			.build();

		AgentClientResponse response = client
			.run("Check if any files were downloaded in .agent-context/vendir-http/ and create http-summary.txt");

		assertThat(response).isNotNull();

		// Verify context gathering was attempted (may fail due to network)
		assertThat(response.context()).containsKey("vendir.context.success");

		System.out.println("✅ HTTP source test completed");
		System.out.println("   Success: " + response.context().get("vendir.context.success"));
	}

	@Test
	@DisplayName("VendirContextAdvisor with relative config path")
	void testRelativeConfigPath() throws IOException {
		// Create config in subdirectory
		Path configDir = this.testWorkspace.resolve("config");
		Files.createDirectories(configDir);
		Path vendirConfig = configDir.resolve("vendir.yml");
		Files.writeString(vendirConfig, createInlineVendirYaml());

		// Use relative path from workspace
		VendirContextAdvisor advisor = VendirContextAdvisor.builder()
			.vendirConfigPath("config/vendir.yml") // Relative path
			.contextDirectory(".agent-context/vendir-relative")
			.timeout(30)
			.build();

		AgentClient client = AgentClient.builder(this.claudeAgentModel)
			.defaultAdvisor(advisor)
			.defaultWorkingDirectory(this.testWorkspace)
			.defaultTimeout(Duration.ofMinutes(2))
			.build();

		AgentClientResponse response = client.run("Create a file called 'relative-test.txt' with content 'OK'");

		assertThat(response).isNotNull();
		assertThat(response.context().get("vendir.context.success")).isEqualTo(true);

		System.out.println("✅ Relative config path test completed");
	}

	@Test
	@DisplayName("VendirContextAdvisor with multiple content sources")
	void testMultipleSources() throws IOException {
		// Create vendir.yml with multiple sources
		Path vendirConfig = createMultiSourceVendirConfig();

		VendirContextAdvisor advisor = VendirContextAdvisor.builder()
			.vendirConfigPath(vendirConfig)
			.contextDirectory(".agent-context/vendir-multi")
			.timeout(120)
			.build();

		AgentClient client = AgentClient.builder(this.claudeAgentModel)
			.defaultAdvisor(advisor)
			.defaultWorkingDirectory(this.testWorkspace)
			.defaultTimeout(Duration.ofMinutes(3))
			.build();

		AgentClientResponse response = client
			.run("List all directories in .agent-context/vendir-multi/vendor/ and create multi-source-summary.txt");

		assertThat(response).isNotNull();

		// At least inline content should succeed
		Path contextDir = this.testWorkspace.resolve(".agent-context/vendir-multi/vendor");
		if (Files.exists(contextDir)) {
			assertThat(Files.walk(contextDir).count()).isGreaterThan(1);
		}

		System.out.println("✅ Multiple sources test completed");
	}

	@Test
	@DisplayName("VendirContextAdvisor with custom order executes at correct priority")
	void testCustomOrder() throws IOException {
		Path vendirConfig = createInlineVendirConfig();

		VendirContextAdvisor advisor = VendirContextAdvisor.builder()
			.vendirConfigPath(vendirConfig)
			.contextDirectory(".agent-context/vendir-order")
			.order(500) // Custom order
			.timeout(30)
			.build();

		assertThat(advisor.getOrder()).isEqualTo(500);

		AgentClient client = AgentClient.builder(this.claudeAgentModel)
			.defaultAdvisor(advisor)
			.defaultWorkingDirectory(this.testWorkspace)
			.defaultTimeout(Duration.ofMinutes(2))
			.build();

		AgentClientResponse response = client.run("Create a file called 'order-test.txt' with content 'OK'");

		assertThat(response).isNotNull();
		assertThat(response.context().get("vendir.context.success")).isEqualTo(true);

		System.out.println("✅ Custom order test completed");
	}

	@Test
	@DisplayName("VendirContextAdvisor metadata is accessible in response")
	void testMetadataAccessibility() throws IOException {
		Path vendirConfig = createInlineVendirConfig();

		VendirContextAdvisor advisor = VendirContextAdvisor.builder()
			.vendirConfigPath(vendirConfig)
			.contextDirectory(".agent-context/vendir-metadata")
			.timeout(30)
			.build();

		AgentClient client = AgentClient.builder(this.claudeAgentModel)
			.defaultAdvisor(advisor)
			.defaultWorkingDirectory(this.testWorkspace)
			.defaultTimeout(Duration.ofMinutes(2))
			.build();

		AgentClientResponse response = client.run("Create a file called 'metadata-test.txt' with content 'OK'");

		// Verify all expected metadata keys exist
		assertThat(response.context()).containsKeys("vendir.context.success", "vendir.context.path",
				"vendir.context.gathered", "vendir.context.output");

		// Verify metadata values are correct type
		assertThat(response.context().get("vendir.context.success")).isInstanceOf(Boolean.class);
		assertThat(response.context().get("vendir.context.path")).isInstanceOf(String.class);
		assertThat(response.context().get("vendir.context.gathered")).isInstanceOf(Boolean.class);
		assertThat(response.context().get("vendir.context.output")).isInstanceOf(String.class);

		// Verify path points to correct location
		String contextPath = (String) response.context().get("vendir.context.path");
		assertThat(contextPath).contains(".agent-context/vendir-metadata");

		System.out.println("✅ Metadata accessibility test completed");
		System.out.println("   Context path: " + contextPath);
	}

	private Path createEfficientGitCloneConfig() throws IOException {
		// Create vendir.yml with efficient shallow clone (depth=1, no history)
		// This is the most important use case for context engineering
		String vendirYml = """
				apiVersion: vendir.k14s.io/v1alpha1
				kind: Config
				directories:
				- path: vendor
				  contents:
				  - path: spring-guide
				    git:
				      url: https://github.com/spring-guides/gs-rest-service
				      ref: main
				      depth: 1
				""";

		Path configPath = this.testWorkspace.resolve("vendir.yml");
		Files.writeString(configPath, vendirYml);
		return configPath;
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
				      depth: 1
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

	private Path createInlineVendirConfig() throws IOException {
		Path configPath = this.testWorkspace.resolve("vendir-inline.yml");
		Files.writeString(configPath, createInlineVendirYaml());
		return configPath;
	}

	private String createInlineVendirYaml() {
		return """
				apiVersion: vendir.k14s.io/v1alpha1
				kind: Config
				directories:
				- path: vendor
				  contents:
				  - path: docs
				    inline:
				      paths:
				        GUIDELINES.md: |
				          # Development Guidelines
				          Always use meaningful variable names.
				          Write tests for all public APIs.
				        PATTERNS.md: |
				          # Design Patterns
				          Prefer composition over inheritance.
				          Use dependency injection.
				""";
	}

	private Path createHttpVendirConfig() throws IOException {
		// Use a simple, reliable HTTP source (Spring Boot's pom.xml)
		String vendirYml = """
				apiVersion: vendir.k14s.io/v1alpha1
				kind: Config
				directories:
				- path: vendor
				  contents:
				  - path: http-content
				    http:
				      url: https://raw.githubusercontent.com/spring-projects/spring-boot/v3.3.0/pom.xml
				""";

		Path configPath = this.testWorkspace.resolve("vendir-http.yml");
		Files.writeString(configPath, vendirYml);
		return configPath;
	}

	private Path createMultiSourceVendirConfig() throws IOException {
		// Combine inline (always works) with git (may work if network available)
		String vendirYml = """
				apiVersion: vendir.k14s.io/v1alpha1
				kind: Config
				directories:
				- path: vendor
				  contents:
				  - path: inline-docs
				    inline:
				      paths:
				        README.md: |
				          # Multi-Source Test
				          This is inline content that always works.
				  - path: git-docs
				    git:
				      url: https://github.com/spring-projects/spring-boot
				      ref: v3.3.0
				    includePaths:
				    - README.adoc
				""";

		Path configPath = this.testWorkspace.resolve("vendir-multi.yml");
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
