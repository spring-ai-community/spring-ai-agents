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
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springaicommunity.agents.client.AgentClientRequest;
import org.springaicommunity.agents.client.AgentClientResponse;
import org.springaicommunity.agents.client.advisor.api.AgentCallAdvisorChain;
import org.springaicommunity.agents.client.Goal;
import org.springaicommunity.agents.model.AgentGeneration;
import org.springaicommunity.agents.model.AgentOptions;
import org.springaicommunity.agents.model.AgentResponse;
import org.springframework.core.Ordered;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for VendirContextAdvisor with mocked vendir execution.
 *
 * @author Mark Pollack
 */
class VendirContextAdvisorTest {

	@TempDir
	Path tempDir;

	private Path vendirConfigPath;

	private Path workingDirectory;

	@BeforeEach
	void setUp() throws IOException {
		this.workingDirectory = tempDir.resolve("workspace");
		Files.createDirectories(this.workingDirectory);

		// Create a minimal vendir.yml
		this.vendirConfigPath = this.workingDirectory.resolve("vendir.yml");
		Files.writeString(this.vendirConfigPath, """
				apiVersion: vendir.k14s.io/v1alpha1
				kind: Config
				directories:
				- path: vendor
				  contents:
				  - path: test
				    inline:
				      paths:
				        README.md: |
				          # Test Content
				""");
	}

	@Test
	@DisplayName("Builder requires vendirConfigPath")
	void testBuilderValidation() {
		assertThatThrownBy(() -> VendirContextAdvisor.builder().build()).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("vendirConfigPath must be set");
	}

	@Test
	@DisplayName("Builder creates advisor with defaults")
	void testBuilderDefaults() {
		VendirContextAdvisor advisor = VendirContextAdvisor.builder().vendirConfigPath(this.vendirConfigPath).build();

		assertThat(advisor).isNotNull();
		assertThat(advisor.getName()).isEqualTo("VendirContext");
		assertThat(advisor.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 100);
	}

	@Test
	@DisplayName("Builder accepts custom configuration")
	void testBuilderCustomConfiguration() {
		VendirContextAdvisor advisor = VendirContextAdvisor.builder()
			.vendirConfigPath(this.vendirConfigPath.toString())
			.contextDirectory("custom-context")
			.autoCleanup(true)
			.timeout(60)
			.order(500)
			.build();

		assertThat(advisor).isNotNull();
		assertThat(advisor.getName()).isEqualTo("VendirContext");
		assertThat(advisor.getOrder()).isEqualTo(500);
	}

	@Test
	@DisplayName("Advisor creates context directory")
	void testContextDirectoryCreation() {
		VendirContextAdvisor advisor = VendirContextAdvisor.builder()
			.vendirConfigPath(this.vendirConfigPath)
			.contextDirectory(".test-context")
			.build();

		AgentClientRequest request = createMockRequest();
		AgentCallAdvisorChain chain = createMockChain();

		advisor.adviseCall(request, chain);

		Path contextDir = this.workingDirectory.resolve(".test-context");
		assertThat(contextDir).exists().isDirectory();
	}

	@Test
	@DisplayName("Advisor adds metadata to request context")
	void testRequestContextMetadata() {
		VendirContextAdvisor advisor = VendirContextAdvisor.builder().vendirConfigPath(this.vendirConfigPath).build();

		AgentClientRequest request = createMockRequest();
		AgentCallAdvisorChain chain = createMockChain();

		advisor.adviseCall(request, chain);

		// Verify context metadata was added
		assertThat(request.context()).containsKey("vendir.context.path");
		assertThat(request.context()).containsKey("vendir.context.success");
		assertThat(request.context()).containsKey("vendir.context.output");

		// Context path should be set
		String contextPath = (String) request.context().get("vendir.context.path");
		assertThat(contextPath).contains(".agent-context/vendir");
	}

	@Test
	@DisplayName("Advisor adds metadata to response context")
	void testResponseContextMetadata() {
		VendirContextAdvisor advisor = VendirContextAdvisor.builder().vendirConfigPath(this.vendirConfigPath).build();

		AgentClientRequest request = createMockRequest();
		AgentCallAdvisorChain chain = createMockChain();

		AgentClientResponse response = advisor.adviseCall(request, chain);

		// Verify response context has gathered metadata
		assertThat(response.context()).containsKey("vendir.context.gathered");
	}

	@Test
	@DisplayName("Advisor calls next in chain")
	void testChainContinuation() {
		VendirContextAdvisor advisor = VendirContextAdvisor.builder().vendirConfigPath(this.vendirConfigPath).build();

		AgentClientRequest request = createMockRequest();
		AgentCallAdvisorChain chain = mock(AgentCallAdvisorChain.class);

		AgentClientResponse expectedResponse = new AgentClientResponse(mock(AgentResponse.class));
		when(chain.nextCall(any())).thenReturn(expectedResponse);

		AgentClientResponse actualResponse = advisor.adviseCall(request, chain);

		assertThat(actualResponse).isEqualTo(expectedResponse);
	}

	@Test
	@DisplayName("Advisor handles vendir failure gracefully")
	void testGracefulFailureHandling() {
		// Create config pointing to non-existent file to trigger failure
		Path invalidConfig = this.workingDirectory.resolve("nonexistent.yml");

		VendirContextAdvisor advisor = VendirContextAdvisor.builder()
			.vendirConfigPath(invalidConfig)
			.timeout(5) // Short timeout
			.build();

		AgentClientRequest request = createMockRequest();
		AgentCallAdvisorChain chain = createMockChain();

		// Should not throw - should handle gracefully and continue
		AgentClientResponse response = advisor.adviseCall(request, chain);

		assertThat(response).isNotNull();

		// Should record failure in context
		assertThat(request.context()).containsKey("vendir.context.success");
		Boolean success = (Boolean) request.context().get("vendir.context.success");
		assertThat(success).isFalse();

		// Should have error message
		assertThat(request.context()).containsKey("vendir.context.error");
	}

	@Test
	@DisplayName("Advisor respects custom context directory")
	void testCustomContextDirectory() {
		String customDir = "my-custom-context";

		VendirContextAdvisor advisor = VendirContextAdvisor.builder()
			.vendirConfigPath(this.vendirConfigPath)
			.contextDirectory(customDir)
			.build();

		AgentClientRequest request = createMockRequest();
		AgentCallAdvisorChain chain = createMockChain();

		advisor.adviseCall(request, chain);

		String contextPath = (String) request.context().get("vendir.context.path");
		assertThat(contextPath).contains(customDir);

		Path expectedPath = this.workingDirectory.resolve(customDir);
		assertThat(expectedPath).exists();
	}

	@Test
	@DisplayName("Advisor name is VendirContext")
	void testAdvisorName() {
		VendirContextAdvisor advisor = VendirContextAdvisor.builder().vendirConfigPath(this.vendirConfigPath).build();

		assertThat(advisor.getName()).isEqualTo("VendirContext");
	}

	@Test
	@DisplayName("Default order is HIGHEST_PRECEDENCE + 100")
	void testDefaultOrder() {
		VendirContextAdvisor advisor = VendirContextAdvisor.builder().vendirConfigPath(this.vendirConfigPath).build();

		assertThat(advisor.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 100);
	}

	@Test
	@DisplayName("Custom order is respected")
	void testCustomOrder() {
		int customOrder = 999;

		VendirContextAdvisor advisor = VendirContextAdvisor.builder()
			.vendirConfigPath(this.vendirConfigPath)
			.order(customOrder)
			.build();

		assertThat(advisor.getOrder()).isEqualTo(customOrder);
	}

	@Test
	@DisplayName("Absolute vendir config path is used directly")
	void testAbsoluteConfigPath() {
		Path absoluteConfig = tempDir.resolve("absolute-config.yml");
		try {
			Files.writeString(absoluteConfig, """
					apiVersion: vendir.k14s.io/v1alpha1
					kind: Config
					directories: []
					""");
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}

		VendirContextAdvisor advisor = VendirContextAdvisor.builder().vendirConfigPath(absoluteConfig).build();

		AgentClientRequest request = createMockRequest();
		AgentCallAdvisorChain chain = createMockChain();

		// Should not throw - vendir will run with absolute path
		advisor.adviseCall(request, chain);
	}

	// Helper methods

	private AgentClientRequest createMockRequest() {
		Map<String, Object> context = new HashMap<>();
		Goal goal = new Goal("Test goal");
		AgentOptions options = mock(AgentOptions.class);
		return new AgentClientRequest(goal, this.workingDirectory, options, context);
	}

	private AgentCallAdvisorChain createMockChain() {
		AgentCallAdvisorChain chain = mock(AgentCallAdvisorChain.class);
		AgentResponse agentResponse = new AgentResponse(java.util.List.of(mock(AgentGeneration.class)));
		AgentClientResponse response = new AgentClientResponse(agentResponse);
		when(chain.nextCall(any())).thenReturn(response);
		return chain;
	}

}
