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

package org.springaicommunity.agents.claude.annotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springaicommunity.claude.agent.sdk.hooks.HookRegistry;
import org.springaicommunity.claude.agent.sdk.types.control.HookEvent;
import org.springaicommunity.claude.agent.sdk.types.control.HookInput;
import org.springaicommunity.claude.agent.sdk.types.control.HookOutput;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ClaudeHookBeanPostProcessor.
 */
class ClaudeHookBeanPostProcessorTest {

	private HookRegistry hookRegistry;

	private ClaudeHookBeanPostProcessor processor;

	@BeforeEach
	void setUp() {
		hookRegistry = new HookRegistry();
		processor = new ClaudeHookBeanPostProcessor(hookRegistry);
	}

	@Nested
	@DisplayName("PreToolUse Hook Tests")
	class PreToolUseHookTests {

		@Test
		@DisplayName("Should register @PreToolUse annotated method")
		void shouldRegisterPreToolUseAnnotatedMethod() {
			TestHooks bean = new TestHooks();
			processor.postProcessAfterInitialization(bean, "testHooks");

			assertThat(hookRegistry.getByEvent(HookEvent.PRE_TOOL_USE)).hasSize(1);
			assertThat(hookRegistry.getRegisteredIds()).contains("testHooks.preToolUseHook");
		}

		@Test
		@DisplayName("Should register @PreToolUse with pattern")
		void shouldRegisterPreToolUseWithPattern() {
			TestHooksWithPattern bean = new TestHooksWithPattern();
			processor.postProcessAfterInitialization(bean, "testHooksWithPattern");

			var registration = hookRegistry.getById("testHooksWithPattern.bashGuard");
			assertThat(registration).isNotNull();
			assertThat(registration.getPatternString()).isEqualTo("Bash");
		}

		@Test
		@DisplayName("Should execute @PreToolUse callback")
		void shouldExecutePreToolUseCallback() {
			TestHooks bean = new TestHooks();
			processor.postProcessAfterInitialization(bean, "testHooks");

			HookInput input = createPreToolUseInput("TestTool", Map.of());
			HookOutput output = hookRegistry.executeHook("testHooks.preToolUseHook", input);

			assertThat(output).isNotNull();
			assertThat(output.continueExecution()).isTrue();
		}

	}

	@Nested
	@DisplayName("PostToolUse Hook Tests")
	class PostToolUseHookTests {

		@Test
		@DisplayName("Should register @PostToolUse annotated method")
		void shouldRegisterPostToolUseAnnotatedMethod() {
			TestHooks bean = new TestHooks();
			processor.postProcessAfterInitialization(bean, "testHooks");

			assertThat(hookRegistry.getByEvent(HookEvent.POST_TOOL_USE)).hasSize(1);
			assertThat(hookRegistry.getRegisteredIds()).contains("testHooks.postToolUseHook");
		}

	}

	@Nested
	@DisplayName("UserPromptSubmit Hook Tests")
	class UserPromptSubmitHookTests {

		@Test
		@DisplayName("Should register @UserPromptSubmit annotated method")
		void shouldRegisterUserPromptSubmitAnnotatedMethod() {
			TestHooks bean = new TestHooks();
			processor.postProcessAfterInitialization(bean, "testHooks");

			assertThat(hookRegistry.getByEvent(HookEvent.USER_PROMPT_SUBMIT)).hasSize(1);
			assertThat(hookRegistry.getRegisteredIds()).contains("testHooks.userPromptSubmitHook");
		}

	}

	@Nested
	@DisplayName("Stop Hook Tests")
	class StopHookTests {

		@Test
		@DisplayName("Should register @Stop annotated method")
		void shouldRegisterStopAnnotatedMethod() {
			TestHooks bean = new TestHooks();
			processor.postProcessAfterInitialization(bean, "testHooks");

			assertThat(hookRegistry.getByEvent(HookEvent.STOP)).hasSize(1);
			assertThat(hookRegistry.getRegisteredIds()).contains("testHooks.stopHook");
		}

	}

	@Nested
	@DisplayName("Void Return Type Tests")
	class VoidReturnTypeTests {

		@Test
		@DisplayName("Should handle void return type as allow")
		void shouldHandleVoidReturnType() {
			TestHooksWithVoidReturn bean = new TestHooksWithVoidReturn();
			processor.postProcessAfterInitialization(bean, "voidHooks");

			HookInput input = createPreToolUseInput("TestTool", Map.of());
			HookOutput output = hookRegistry.executeHook("voidHooks.voidPreToolUse", input);

			assertThat(output).isNotNull();
			assertThat(output.continueExecution()).isTrue();
		}

	}

	@Nested
	@DisplayName("Method Signature Validation Tests")
	class MethodSignatureValidationTests {

		@Test
		@DisplayName("Should reject method with wrong parameter count")
		void shouldRejectMethodWithWrongParameterCount() {
			InvalidHooksWrongParamCount bean = new InvalidHooksWrongParamCount();

			assertThatThrownBy(() -> processor.postProcessAfterInitialization(bean, "invalidHooks"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("must have exactly one parameter");
		}

		@Test
		@DisplayName("Should reject method with wrong parameter type")
		void shouldRejectMethodWithWrongParameterType() {
			InvalidHooksWrongParamType bean = new InvalidHooksWrongParamType();

			assertThatThrownBy(() -> processor.postProcessAfterInitialization(bean, "invalidHooks"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("must have a parameter of type HookInput");
		}

		@Test
		@DisplayName("Should reject method with wrong return type")
		void shouldRejectMethodWithWrongReturnType() {
			InvalidHooksWrongReturnType bean = new InvalidHooksWrongReturnType();

			assertThatThrownBy(() -> processor.postProcessAfterInitialization(bean, "invalidHooks"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("must return void or HookOutput");
		}

	}

	@Nested
	@DisplayName("Multiple Hooks Tests")
	class MultipleHooksTests {

		@Test
		@DisplayName("Should register multiple hooks from same bean")
		void shouldRegisterMultipleHooksFromSameBean() {
			TestHooks bean = new TestHooks();
			processor.postProcessAfterInitialization(bean, "testHooks");

			assertThat(hookRegistry.getRegisteredIds()).hasSize(4);
		}

		@Test
		@DisplayName("Should register hooks from multiple beans")
		void shouldRegisterHooksFromMultipleBeans() {
			TestHooks bean1 = new TestHooks();
			TestHooksWithPattern bean2 = new TestHooksWithPattern();

			processor.postProcessAfterInitialization(bean1, "testHooks");
			processor.postProcessAfterInitialization(bean2, "testHooksWithPattern");

			assertThat(hookRegistry.getRegisteredIds()).hasSize(5);
		}

	}

	// Test helper classes

	static class TestHooks {

		@PreToolUse
		public HookOutput preToolUseHook(HookInput input) {
			return HookOutput.allow();
		}

		@PostToolUse
		public HookOutput postToolUseHook(HookInput input) {
			return HookOutput.allow();
		}

		@UserPromptSubmit
		public HookOutput userPromptSubmitHook(HookInput input) {
			return HookOutput.allow();
		}

		@Stop
		public HookOutput stopHook(HookInput input) {
			return HookOutput.allow();
		}

	}

	static class TestHooksWithPattern {

		@PreToolUse(pattern = "Bash")
		public HookOutput bashGuard(HookInput input) {
			return HookOutput.allow();
		}

	}

	static class TestHooksWithVoidReturn {

		@PreToolUse
		public void voidPreToolUse(HookInput input) {
			// Void return is valid - implies allow
		}

	}

	static class InvalidHooksWrongParamCount {

		@PreToolUse
		public HookOutput invalidHook() {
			return HookOutput.allow();
		}

	}

	static class InvalidHooksWrongParamType {

		@PreToolUse
		public HookOutput invalidHook(String input) {
			return HookOutput.allow();
		}

	}

	static class InvalidHooksWrongReturnType {

		@PreToolUse
		public String invalidHook(HookInput input) {
			return "invalid";
		}

	}

	// Helper method to create test HookInput
	private HookInput createPreToolUseInput(String toolName, Map<String, Object> input) {
		return new HookInput.PreToolUseInput("PreToolUse", "test-session", "/tmp/transcript", "/tmp",
				"bypassPermissions", toolName, "tool-use-123", input);
	}

}
