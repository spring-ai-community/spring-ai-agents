/*
 * Copyright 2025 Spring AI Community
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

package org.springaicommunity.agents.claude;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agents.claude.sdk.hooks.HookRegistry;
import org.springaicommunity.agents.claude.sdk.types.control.HookEvent;
import org.springaicommunity.agents.claude.sdk.types.control.HookOutput;
import org.springaicommunity.agents.model.AgentModel;
import org.springaicommunity.agents.model.AgentTaskRequest;
import org.springaicommunity.agents.model.IterableAgentModel;
import org.springaicommunity.agents.model.StreamingAgentModel;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ClaudeAgentModel - testing builder patterns, hook registration,
 * interface compliance, and API contracts.
 *
 * <p>
 * Tests verify the consolidated model implements all three programming models:
 * </p>
 * <ul>
 * <li>{@link AgentModel} - Blocking/imperative</li>
 * <li>{@link StreamingAgentModel} - Reactive with Flux</li>
 * <li>{@link IterableAgentModel} - Iterator-based</li>
 * </ul>
 *
 * @author Spring AI Community
 */
class ClaudeAgentModelTest {

	private ClaudeAgentModel model;

	private static final Path TEST_WORKING_DIR = Paths.get(System.getProperty("user.dir"));

	@AfterEach
	void tearDown() {
		if (model != null) {
			model.close();
		}
	}

	@Nested
	@DisplayName("Builder Tests")
	class BuilderTests {

		@Test
		@DisplayName("Should build with defaults")
		void buildWithDefaults() {
			model = ClaudeAgentModel.builder().build();

			assertThat(model).isNotNull();
			assertThat(model.getHookRegistry()).isNotNull();
		}

		@Test
		@DisplayName("Should build with custom working directory")
		void buildWithWorkingDirectory() {
			Path customDir = Paths.get("/tmp/test");
			model = ClaudeAgentModel.builder().workingDirectory(customDir).build();

			assertThat(model).isNotNull();
		}

		@Test
		@DisplayName("Should build with custom timeout")
		void buildWithTimeout() {
			model = ClaudeAgentModel.builder().timeout(Duration.ofMinutes(5)).build();

			assertThat(model).isNotNull();
		}

		@Test
		@DisplayName("Should build with custom Claude path")
		void buildWithClaudePath() {
			model = ClaudeAgentModel.builder().claudePath("/usr/local/bin/claude").build();

			assertThat(model).isNotNull();
		}

		@Test
		@DisplayName("Should build with pre-configured hook registry")
		void buildWithHookRegistry() {
			HookRegistry registry = new HookRegistry();
			registry.registerPreToolUse("Bash", input -> HookOutput.allow());

			model = ClaudeAgentModel.builder().hookRegistry(registry).build();

			assertThat(model.getHookRegistry()).isSameAs(registry);
			assertThat(model.getHookRegistry().hasHooks()).isTrue();
		}

		@Test
		@DisplayName("Should build with default options")
		void buildWithDefaultOptions() {
			ClaudeAgentOptions options = new ClaudeAgentOptions();
			options.setModel("claude-opus-4-20250514");

			model = ClaudeAgentModel.builder().defaultOptions(options).build();

			assertThat(model).isNotNull();
		}

		@Test
		@DisplayName("Should support fluent chaining")
		void fluentChaining() {
			model = ClaudeAgentModel.builder()
				.workingDirectory(TEST_WORKING_DIR)
				.timeout(Duration.ofMinutes(5))
				.claudePath("/usr/local/bin/claude")
				.defaultOptions(new ClaudeAgentOptions())
				.build();

			assertThat(model).isNotNull();
		}

	}

	@Nested
	@DisplayName("Hook Registration Tests")
	class HookRegistrationTests {

		@BeforeEach
		void setUp() {
			model = ClaudeAgentModel.builder().workingDirectory(TEST_WORKING_DIR).build();
		}

		@Test
		@DisplayName("Should register PreToolUse hook with pattern")
		void registerPreToolUseWithPattern() {
			String hookId = model.registerPreToolUse("Bash", input -> HookOutput.allow());

			assertThat(hookId).isNotNull().startsWith("hook_");
			assertThat(model.getHookRegistry().hasHooks()).isTrue();
			assertThat(model.getHookRegistry().getById(hookId)).isNotNull();
		}

		@Test
		@DisplayName("Should register PreToolUse hook for all tools")
		void registerPreToolUseForAll() {
			String hookId = model.registerPreToolUse(input -> HookOutput.allow());

			assertThat(hookId).isNotNull();
			assertThat(model.getHookRegistry().getByEvent(HookEvent.PRE_TOOL_USE)).hasSize(1);
		}

		@Test
		@DisplayName("Should register PostToolUse hook with pattern")
		void registerPostToolUseWithPattern() {
			String hookId = model.registerPostToolUse("Write", input -> HookOutput.allow());

			assertThat(hookId).isNotNull();
			assertThat(model.getHookRegistry().getByEvent(HookEvent.POST_TOOL_USE)).hasSize(1);
		}

		@Test
		@DisplayName("Should register PostToolUse hook for all tools")
		void registerPostToolUseForAll() {
			String hookId = model.registerPostToolUse(input -> HookOutput.allow());

			assertThat(hookId).isNotNull();
		}

		@Test
		@DisplayName("Should register UserPromptSubmit hook")
		void registerUserPromptSubmit() {
			String hookId = model.registerUserPromptSubmit(input -> HookOutput.allow());

			assertThat(hookId).isNotNull();
			assertThat(model.getHookRegistry().getByEvent(HookEvent.USER_PROMPT_SUBMIT)).hasSize(1);
		}

		@Test
		@DisplayName("Should register Stop hook")
		void registerStopHook() {
			String hookId = model.registerStop(input -> HookOutput.allow());

			assertThat(hookId).isNotNull();
			assertThat(model.getHookRegistry().getByEvent(HookEvent.STOP)).hasSize(1);
		}

		@Test
		@DisplayName("Should unregister hook by ID")
		void unregisterHook() {
			String hookId = model.registerPreToolUse(input -> HookOutput.allow());
			assertThat(model.getHookRegistry().hasHooks()).isTrue();

			boolean removed = model.unregisterHook(hookId);

			assertThat(removed).isTrue();
			assertThat(model.getHookRegistry().hasHooks()).isFalse();
		}

		@Test
		@DisplayName("Should return false when unregistering non-existent hook")
		void unregisterNonExistent() {
			boolean removed = model.unregisterHook("non_existent");

			assertThat(removed).isFalse();
		}

		@Test
		@DisplayName("Should register multiple hooks")
		void registerMultipleHooks() {
			model.registerPreToolUse("Bash", input -> HookOutput.allow());
			model.registerPreToolUse("Write", input -> HookOutput.allow());
			model.registerPostToolUse(input -> HookOutput.allow());
			model.registerStop(input -> HookOutput.allow());

			Set<String> ids = model.getHookRegistry().getRegisteredIds();
			assertThat(ids).hasSize(4);
		}

		@Test
		@DisplayName("Hook callbacks should be executable")
		void hookCallbacksExecutable() {
			String hookId = model.registerPreToolUse(input -> HookOutput.block("Blocked for testing"));

			HookOutput output = model.getHookRegistry().executeHook(hookId, null);

			assertThat(output).isNotNull();
			assertThat(output.continueExecution()).isFalse();
			assertThat(output.reason()).isEqualTo("Blocked for testing");
		}

	}

	@Nested
	@DisplayName("Interface Compliance Tests")
	class InterfaceComplianceTests {

		@BeforeEach
		void setUp() {
			model = ClaudeAgentModel.builder().workingDirectory(TEST_WORKING_DIR).build();
		}

		@Test
		@DisplayName("Should implement AgentModel (blocking)")
		void implementsAgentModel() {
			assertThat(model).isInstanceOf(AgentModel.class);
		}

		@Test
		@DisplayName("Should implement StreamingAgentModel (reactive)")
		void implementsStreamingAgentModel() {
			assertThat(model).isInstanceOf(StreamingAgentModel.class);
		}

		@Test
		@DisplayName("Should implement IterableAgentModel (iterator)")
		void implementsIterableAgentModel() {
			assertThat(model).isInstanceOf(IterableAgentModel.class);
		}

		@Test
		@DisplayName("Should implement AutoCloseable")
		void implementsAutoCloseable() {
			assertThat(model).isInstanceOf(AutoCloseable.class);
		}

		@Test
		@DisplayName("close() should clear hooks")
		void closeClearsHooks() {
			model.registerPreToolUse(input -> HookOutput.allow());
			assertThat(model.getHookRegistry().hasHooks()).isTrue();

			model.close();

			assertThat(model.getHookRegistry().hasHooks()).isFalse();
		}

	}

	@Nested
	@DisplayName("Hook Registry Configuration Tests")
	class HookRegistryConfigurationTests {

		@BeforeEach
		void setUp() {
			model = ClaudeAgentModel.builder().workingDirectory(TEST_WORKING_DIR).build();
		}

		@Test
		@DisplayName("Should build hook config for CLI initialization")
		void buildHookConfig() {
			model.registerPreToolUse("Bash", input -> HookOutput.allow());
			model.registerPreToolUse("Write", input -> HookOutput.allow());
			model.registerPostToolUse(input -> HookOutput.allow());

			var config = model.getHookRegistry().buildHookConfig();

			assertThat(config).containsKey("PreToolUse");
			assertThat(config).containsKey("PostToolUse");
		}

		@Test
		@DisplayName("Should create initialize request with hooks")
		void createInitializeRequest() {
			model.registerPreToolUse(input -> HookOutput.allow());

			var request = model.getHookRegistry().createInitializeRequest("req_1");

			assertThat(request).isNotNull();
			assertThat(request.requestId()).isEqualTo("req_1");
			assertThat(request.request()).isNotNull();
		}

	}

	@Nested
	@DisplayName("Agent Options Tests")
	class AgentOptionsTests {

		@Test
		@DisplayName("Should use default options when not specified")
		void useDefaultOptions() {
			model = ClaudeAgentModel.builder().workingDirectory(TEST_WORKING_DIR).build();

			// Model should be created without errors
			assertThat(model).isNotNull();
		}

		@Test
		@DisplayName("Should use provided default options")
		void useProvidedOptions() {
			ClaudeAgentOptions options = new ClaudeAgentOptions();
			options.setModel("claude-opus-4-20250514");
			options.setYolo(true);
			options.setTimeout(Duration.ofMinutes(30));

			model = ClaudeAgentModel.builder().workingDirectory(TEST_WORKING_DIR).defaultOptions(options).build();

			assertThat(model).isNotNull();
		}

	}

	@Nested
	@DisplayName("Functional Interface Usage Tests")
	class FunctionalInterfaceTests {

		@BeforeEach
		void setUp() {
			model = ClaudeAgentModel.builder().workingDirectory(TEST_WORKING_DIR).build();
		}

		@Test
		@DisplayName("Model can be assigned to AgentModel functional interface")
		void canAssignToAgentModel() {
			// Verify functional interface assignment works
			AgentModel blockingModel = model;
			assertThat(blockingModel).isNotNull();
		}

		@Test
		@DisplayName("Model can be assigned to StreamingAgentModel functional interface")
		void canAssignToStreamingAgentModel() {
			// Verify functional interface assignment works
			StreamingAgentModel streamingModel = model;
			assertThat(streamingModel).isNotNull();
		}

		@Test
		@DisplayName("Model can be assigned to IterableAgentModel functional interface")
		void canAssignToIterableAgentModel() {
			// Verify functional interface assignment works
			IterableAgentModel iterableModel = model;
			assertThat(iterableModel).isNotNull();
		}

		@Test
		@DisplayName("All three interfaces reference same instance")
		void sameInstanceForAllInterfaces() {
			AgentModel blockingModel = model;
			StreamingAgentModel streamingModel = model;
			IterableAgentModel iterableModel = model;

			assertThat(blockingModel).isSameAs(streamingModel);
			assertThat(streamingModel).isSameAs(iterableModel);
		}

	}

}
