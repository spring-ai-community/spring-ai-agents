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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springaicommunity.agents.model.AgentResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive test for AgentClient's ChatClient-style API patterns.
 *
 * <p>
 * This test verifies that AgentClient provides the same developer experience as Spring
 * AI's ChatClient through identical factory methods, builder patterns, fluent APIs, and
 * convenience methods.
 * </p>
 *
 * @author Mark Pollack
 */
class AgentClientChatClientStyleTest {

	private MockAgentModel mockModel;

	private Path testWorkspace;

	@BeforeEach
	void setUp(@TempDir Path tempDir) {
		this.mockModel = new MockAgentModel();
		this.testWorkspace = tempDir;
	}

	// =====================================================
	// Static Factory Methods (matching ChatClient)
	// =====================================================

	@Test
	void testCreate_StaticFactory() {
		// Test: AgentClient.create(agentModel) - like ChatClient.create(chatModel)
		AgentClient client = AgentClient.create(this.mockModel);

		assertThat(client).isNotNull();
		assertThat(client).isInstanceOf(DefaultAgentClient.class);
	}

	@Test
	void testBuilder_StaticFactory() {
		// Test: AgentClient.builder(agentModel) - like ChatClient.builder(chatModel)
		AgentClient.Builder builder = AgentClient.builder(this.mockModel);

		assertThat(builder).isNotNull();
		assertThat(builder).isInstanceOf(DefaultAgentClientBuilder.class);
	}

	// =====================================================
	// Entry Point Methods (matching ChatClient.prompt())
	// =====================================================

	@Test
	void testGoal_EmptyEntryPoint() {
		// Test: client.goal() - like chatClient.prompt()
		AgentClient client = AgentClient.create(this.mockModel);

		AgentClient.AgentClientRequestSpec spec = client.goal();

		assertThat(spec).isNotNull();
	}

	@Test
	void testGoal_EmptyEntryPoint_RequiresGoalBeforeCall() {
		// Test: client.goal().run() should fail without setting goal
		AgentClient client = AgentClient.create(this.mockModel);

		assertThatThrownBy(() -> client.goal().run()).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Goal must be set before running");
	}

	@Test
	void testGoal_WithFluentChaining() {
		// Test: client.goal("task").workingDirectory().run() - direct goal with chaining
		AgentClient client = AgentClient.create(this.mockModel);

		AgentClientResponse response = client.goal("Test fluent goal").workingDirectory(this.testWorkspace).run();

		assertThat(response).isNotNull();
		assertThat(response.getResult()).isEqualTo("Mock response for: Test fluent goal");
		assertThat(this.mockModel.lastRequest.goal()).isEqualTo("Test fluent goal");

	}

	@Test
	void testGoal_WithString() {
		// Test: client.goal("task") - like chatClient.prompt("message")
		AgentClient client = AgentClient.create(this.mockModel);

		AgentClientResponse response = client.goal("Test string goal").workingDirectory(this.testWorkspace).run();

		assertThat(response).isNotNull();
		assertThat(response.getResult()).isEqualTo("Mock response for: Test string goal");
		assertThat(this.mockModel.lastRequest.goal()).isEqualTo("Test string goal");
	}

	@Test
	void testGoal_WithGoalObject() {
		// Test: client.goal(goalObject) - like chatClient.prompt(prompt)
		Goal goalObject = new Goal("Test goal object", this.testWorkspace, new DefaultAgentOptions());
		AgentClient client = AgentClient.create(this.mockModel);

		AgentClientResponse response = client.goal(goalObject).run();

		assertThat(response).isNotNull();
		assertThat(response.getResult()).isEqualTo("Mock response for: Test goal object");
		assertThat(this.mockModel.lastRequest.goal()).isEqualTo("Test goal object");
		assertThat(this.mockModel.lastRequest.workingDirectory()).isEqualTo(this.testWorkspace);
	}

	// =====================================================
	// Convenience Methods (from example-code.md)
	// =====================================================

	@Test
	void testRun_SimpleString() {
		// Test: client.run("task") - direct execution convenience method
		AgentClient client = AgentClient.create(this.mockModel);

		AgentClientResponse response = client.run("Test run method");

		assertThat(response).isNotNull();
		assertThat(response.getResult()).isEqualTo("Mock response for: Test run method");
		assertThat(this.mockModel.lastRequest.goal()).isEqualTo("Test run method");
	}

	@Test
	void testRun_WithOptions() {
		// Test: client.run("task", options) - with agent options
		DefaultAgentOptions options = DefaultAgentOptions.builder()
			.workingDirectory(this.testWorkspace.toString())
			.timeout(Duration.ofMinutes(2))
			.model("test-model")
			.build();

		AgentClient client = AgentClient.create(this.mockModel);

		AgentClientResponse response = client.run("Test run with options", options);

		assertThat(response).isNotNull();
		assertThat(response.getResult()).isEqualTo("Mock response for: Test run with options");
		assertThat(this.mockModel.lastRequest.goal()).isEqualTo("Test run with options");
	}

	// =====================================================
	// Builder Pattern (matching ChatClient.Builder)
	// =====================================================

	@Test
	void testBuilder_DefaultOptions() {
		// Test: AgentClient.builder().defaultOptions().build()
		DefaultAgentOptions defaultOptions = DefaultAgentOptions.builder()
			.timeout(Duration.ofMinutes(5))
			.model("test-model")
			.build();

		AgentClient client = AgentClient.builder(this.mockModel).defaultOptions(defaultOptions).build();

		assertThat(client).isNotNull();

		// Test that defaults are applied (this would need enhancement to MockAgentModel
		// to verify)
		AgentClientResponse response = client.goal("Test with defaults").workingDirectory(this.testWorkspace).run();
		assertThat(response).isNotNull();
	}

	@Test
	void testBuilder_DefaultWorkingDirectory() {
		// Test: AgentClient.builder().defaultWorkingDirectory().build()
		AgentClient client = AgentClient.builder(this.mockModel).defaultWorkingDirectory(this.testWorkspace).build();

		assertThat(client).isNotNull();
	}

	@Test
	void testBuilder_DefaultTimeout() {
		// Test: AgentClient.builder().defaultTimeout().build()
		AgentClient client = AgentClient.builder(this.mockModel).defaultTimeout(Duration.ofMinutes(3)).build();

		assertThat(client).isNotNull();
	}

	@Test
	void testBuilder_ChainedDefaults() {
		// Test: Full builder chain with multiple defaults
		AgentClient client = AgentClient.builder(this.mockModel)
			.defaultWorkingDirectory(this.testWorkspace)
			.defaultTimeout(Duration.ofMinutes(4))
			.build();

		assertThat(client).isNotNull();

		AgentClientResponse response = client.goal("Test chained defaults").run();
		assertThat(response).isNotNull();
	}

	// =====================================================
	// Mutation Pattern (matching ChatClient.mutate())
	// =====================================================

	@Test
	void testMutate_CreatesNewClientWithSameModel() {
		// Test: client.mutate() - creates new client with same underlying model
		AgentClient originalClient = AgentClient.create(this.mockModel);

		AgentClient.Builder mutatedBuilder = originalClient.mutate();

		assertThat(mutatedBuilder).isNotNull();
		assertThat(mutatedBuilder).isInstanceOf(DefaultAgentClientBuilder.class);

		AgentClient mutatedClient = mutatedBuilder.build();
		assertThat(mutatedClient).isNotNull();
		assertThat(mutatedClient).isNotSameAs(originalClient); // Different instances

		// Both should work with same model
		originalClient.run("Original client test");
		assertThat(this.mockModel.lastRequest.goal()).isEqualTo("Original client test");

		mutatedClient.run("Mutated client test");
		assertThat(this.mockModel.lastRequest.goal()).isEqualTo("Mutated client test");
	}

	@Test
	void testMutate_WithModifiedDefaults() {
		// Test: client.mutate().defaultTimeout().build() - mutate with changes
		AgentClient originalClient = AgentClient.create(this.mockModel);

		AgentClient mutatedClient = originalClient.mutate().defaultTimeout(Duration.ofMinutes(10)).build();

		assertThat(mutatedClient).isNotNull();
		assertThat(mutatedClient).isNotSameAs(originalClient);

		// Both clients should work independently
		AgentClientResponse response = mutatedClient.goal("Mutated test").workingDirectory(this.testWorkspace).run();
		assertThat(response).isNotNull();
	}

	// =====================================================
	// Fluent API Combinations (ChatClient-style chaining)
	// =====================================================

	@Test
	void testFluentChaining_AllMethods() {
		// Test: Complete fluent API chain like ChatClient
		AgentClient client = AgentClient.builder(this.mockModel).defaultTimeout(Duration.ofMinutes(5)).build();

		AgentClientResponse response = client.goal("Complex fluent test").workingDirectory(this.testWorkspace).run();

		assertThat(response).isNotNull();
		assertThat(response.getResult()).isEqualTo("Mock response for: Complex fluent test");
		assertThat(this.mockModel.lastRequest.goal()).isEqualTo("Complex fluent test");
		assertThat(this.mockModel.lastRequest.workingDirectory()).isEqualTo(this.testWorkspace);
	}

	@Test
	void testFluentChaining_MethodVariants() {
		// Test different method call patterns work identically
		AgentClient client = AgentClient.create(this.mockModel);

		// Pattern 1: goal(String).workingDirectory().run()
		AgentClientResponse response1 = client.goal("Pattern 1").workingDirectory(this.testWorkspace).run();
		assertThat(response1.getResult()).isEqualTo("Mock response for: Pattern 1");

		// Pattern 2: goal().goal(String).workingDirectory().run()
		AgentClientResponse response2 = client.goal().goal("Pattern 2").workingDirectory(this.testWorkspace).run();
		assertThat(response2.getResult()).isEqualTo("Mock response for: Pattern 2");

		// Pattern 3: run() convenience method
		AgentClientResponse response3 = client.run("Pattern 3");
		assertThat(response3.getResult()).isEqualTo("Mock response for: Pattern 3");
	}

	// =====================================================
	// Error Handling and Edge Cases
	// =====================================================

	@Test
	void testBuilder_NullAgentModel_ThrowsException() {
		assertThatThrownBy(() -> AgentClient.builder(null)).isInstanceOf(NullPointerException.class);
	}

	@Test
	void testCreate_NullAgentModel_ThrowsException() {
		assertThatThrownBy(() -> AgentClient.create(null)).isInstanceOf(NullPointerException.class);
	}

	// =====================================================
	// Additional Pattern Coverage from Integration Tests
	// =====================================================

	@Test
	void testWorkingDirectoryOverride() {
		// Pattern: Override working directory in fluent API
		Path subdirectory = this.testWorkspace.resolve("subdir");

		AgentClient client = AgentClient.create(this.mockModel);

		AgentClientResponse response = client.goal("Test working directory override")
			.workingDirectory(subdirectory)
			.run();

		assertThat(response).isNotNull();
		assertThat(response.getResult()).isEqualTo("Mock response for: Test working directory override");

		// Verify working directory was passed to the model
		assertThat(this.mockModel.lastRequest.workingDirectory()).isEqualTo(subdirectory);
	}

	@Test
	void testResponseMetadataAnalysis() {
		// Pattern: Comprehensive response inspection including metadata
		AgentClient client = AgentClient.create(this.mockModel);

		AgentClientResponse response = client.run("Test metadata analysis");

		// Verify response structure
		assertThat(response).isNotNull();
		assertThat(response.isSuccessful()).isTrue();
		assertThat(response.getResult()).isNotBlank();

		// Verify underlying AgentResponse access
		assertThat(response.getAgentResponse()).isNotNull();
		assertThat(response.getAgentResponse().getResult()).isNotNull();

		// Verify metadata access
		assertThat(response.getMetadata()).isNotNull();
		assertThat(response.getAgentResponse().getMetadata()).isNotNull();
	}

	@Test
	void testComplexGoalWithBuilderDefaults() {
		// Pattern: Complex goal using pre-configured client with defaults
		AgentClient configuredClient = AgentClient.builder(this.mockModel)
			.defaultWorkingDirectory(this.testWorkspace)
			.defaultTimeout(Duration.ofMinutes(5))
			.build();

		AgentClientResponse response = configuredClient.goal("Complex multi-step task").run(); // Uses
																								// builder
																								// defaults

		assertThat(response).isNotNull();
		assertThat(response.isSuccessful()).isTrue();
		assertThat(response.getResult()).isEqualTo("Mock response for: Complex multi-step task");

		// Verify request was made correctly
		assertThat(this.mockModel.lastRequest.goal()).isEqualTo("Complex multi-step task");
		assertThat(this.mockModel.lastRequest.workingDirectory()).isEqualTo(this.testWorkspace);
	}

	@Test
	void testClientMutationWithSpecializedTimeout() {
		// Pattern: Mutate client for specialized use case
		AgentClient originalClient = AgentClient.builder(this.mockModel).defaultTimeout(Duration.ofMinutes(10)).build();

		AgentClient quickTaskClient = originalClient.mutate()
			.defaultTimeout(Duration.ofMinutes(1)) // Specialized for quick tasks
			.build();

		AgentClientResponse response = quickTaskClient.goal("Quick specialized task")
			.workingDirectory(this.testWorkspace)
			.run();

		assertThat(response).isNotNull();
		assertThat(response.isSuccessful()).isTrue();

		// Verify mutated client works independently
		assertThat(this.mockModel.lastRequest.goal()).isEqualTo("Quick specialized task");
		assertThat(this.mockModel.lastRequest.workingDirectory()).isEqualTo(this.testWorkspace);

		// Both clients should work with same model but different configs
		AgentClientResponse originalResponse = originalClient.run("Original client task");
		assertThat(originalResponse.getResult()).isEqualTo("Mock response for: Original client task");
	}

	@Test
	void testBuilderDefaultOptionsArePropagated() {
		// Pattern: Verify builder default options are actually used when Goal has no
		// options
		DefaultAgentOptions builderDefaults = DefaultAgentOptions.builder()
			.timeout(Duration.ofMinutes(7))
			.model("test-model-from-builder")
			.workingDirectory(this.testWorkspace.toString())
			.build();

		AgentClient client = AgentClient.builder(this.mockModel).defaultOptions(builderDefaults).build();

		AgentClientResponse response = client.run("Test default options propagation");

		assertThat(response).isNotNull();
		assertThat(response.isSuccessful()).isTrue();

		// Verify that builder defaults were used
		assertThat(this.mockModel.lastRequest.options()).isNotNull();
		assertThat(this.mockModel.lastRequest.options()).isEqualTo(builderDefaults);
		assertThat(this.mockModel.lastRequest.workingDirectory()).isEqualTo(this.testWorkspace);
	}

	@Test
	void testFluentAPIDesign_GoalRunResult() {
		// Test demonstrating the complete "goal → run → result" API design
		AgentClient client = AgentClient.create(this.mockModel);

		// 1. Simple one-liner convenience method
		String result1 = client.run("Create hello.txt").getResult();
		assertThat(result1).isEqualTo("Mock response for: Create hello.txt");

		// 2. Fluent with working directory
		String result2 = client.goal("Implement Calculator class")
			.workingDirectory(this.testWorkspace)
			.run()
			.getResult();
		assertThat(result2).isEqualTo("Mock response for: Implement Calculator class");

		// 3. Access structured response (equivalent to ChatClient's chatResponse())
		AgentClientResponse response = client.goal("Generate API docs").run();
		AgentResponse modelResponse = response.getAgentResponse();
		assertThat(modelResponse).isNotNull();
		assertThat(modelResponse.getResult().getOutput()).isEqualTo("Mock response for: Generate API docs");

		// 4. Verify response provides access to the result
		assertThat(response.getResult()).isEqualTo("Mock response for: Generate API docs");

		// 5. Verify agent-appropriate terminology works naturally
		String naturalFlow = client.goal("Fix failing tests").workingDirectory(this.testWorkspace).run().getResult();
		assertThat(naturalFlow).isEqualTo("Mock response for: Fix failing tests");

		// The API reads naturally: goal → run → result (agent semantics)
		// vs ChatClient: prompt → call → content (chat semantics)
	}

}