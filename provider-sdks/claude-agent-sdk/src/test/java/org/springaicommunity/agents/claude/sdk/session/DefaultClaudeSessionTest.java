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

package org.springaicommunity.agents.claude.sdk.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agents.claude.sdk.hooks.HookRegistry;
import org.springaicommunity.agents.claude.sdk.transport.CLIOptions;
import org.springaicommunity.agents.claude.sdk.types.control.HookEvent;
import org.springaicommunity.agents.claude.sdk.types.control.HookOutput;

import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for DefaultClaudeSession.
 */
class DefaultClaudeSessionTest {

	private Path workingDirectory;

	@BeforeEach
	void setUp() {
		workingDirectory = Path.of(System.getProperty("user.dir"));
	}

	@Nested
	@DisplayName("Builder Tests")
	class BuilderTests {

		@Test
		@DisplayName("should build session with required parameters")
		void shouldBuildWithRequiredParams() {
			DefaultClaudeSession session = DefaultClaudeSession.builder().workingDirectory(workingDirectory).build();

			assertThat(session).isNotNull();
			assertThat(session.isConnected()).isFalse();
		}

		@Test
		@DisplayName("should build session with all parameters")
		void shouldBuildWithAllParams() {
			HookRegistry registry = new HookRegistry();
			CLIOptions options = CLIOptions.builder().model("claude-sonnet-4-20250514").build();

			DefaultClaudeSession session = DefaultClaudeSession.builder()
				.workingDirectory(workingDirectory)
				.options(options)
				.timeout(Duration.ofMinutes(5))
				.claudePath("/usr/bin/claude")
				.hookRegistry(registry)
				.build();

			assertThat(session).isNotNull();
			assertThat(session.getServerInfo()).isEmpty();
		}

		@Test
		@DisplayName("should throw when working directory is null")
		void shouldThrowWhenWorkingDirNull() {
			assertThatThrownBy(() -> DefaultClaudeSession.builder().build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("workingDirectory");
		}

	}

	@Nested
	@DisplayName("Session State Tests")
	class SessionStateTests {

		@Test
		@DisplayName("should not be connected after creation")
		void shouldNotBeConnectedAfterCreation() {
			DefaultClaudeSession session = DefaultClaudeSession.builder().workingDirectory(workingDirectory).build();

			assertThat(session.isConnected()).isFalse();
		}

		@Test
		@DisplayName("should throw when querying without connection")
		void shouldThrowWhenQueryingWithoutConnection() {
			DefaultClaudeSession session = DefaultClaudeSession.builder().workingDirectory(workingDirectory).build();

			assertThatThrownBy(() -> session.query("test")).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("not connected");
		}

		@Test
		@DisplayName("should throw when interrupting without connection")
		void shouldThrowWhenInterruptingWithoutConnection() {
			DefaultClaudeSession session = DefaultClaudeSession.builder().workingDirectory(workingDirectory).build();

			assertThatThrownBy(() -> session.interrupt()).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("not connected");
		}

		@Test
		@DisplayName("should throw when setting permission mode without connection")
		void shouldThrowWhenSettingPermissionModeWithoutConnection() {
			DefaultClaudeSession session = DefaultClaudeSession.builder().workingDirectory(workingDirectory).build();

			assertThatThrownBy(() -> session.setPermissionMode("acceptEdits")).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("not connected");
		}

		@Test
		@DisplayName("should throw when setting model without connection")
		void shouldThrowWhenSettingModelWithoutConnection() {
			DefaultClaudeSession session = DefaultClaudeSession.builder().workingDirectory(workingDirectory).build();

			assertThatThrownBy(() -> session.setModel("claude-opus-4-20250514"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("not connected");
		}

	}

	@Nested
	@DisplayName("Hook Registration Tests")
	class HookRegistrationTests {

		@Test
		@DisplayName("should register hook")
		void shouldRegisterHook() {
			DefaultClaudeSession session = DefaultClaudeSession.builder().workingDirectory(workingDirectory).build();

			session.registerHook(HookEvent.PRE_TOOL_USE, "Bash", input -> HookOutput.allow());

			// No exception thrown = success
		}

		@Test
		@DisplayName("should register hook with null pattern for all tools")
		void shouldRegisterHookForAllTools() {
			DefaultClaudeSession session = DefaultClaudeSession.builder().workingDirectory(workingDirectory).build();

			session.registerHook(HookEvent.POST_TOOL_USE, null, input -> HookOutput.allow());

			// No exception thrown = success
		}

		@Test
		@DisplayName("should support fluent registration")
		void shouldSupportFluentRegistration() {
			DefaultClaudeSession session = DefaultClaudeSession.builder().workingDirectory(workingDirectory).build();

			DefaultClaudeSession result = session
				.registerHook(HookEvent.PRE_TOOL_USE, "Bash", input -> HookOutput.allow())
				.registerHook(HookEvent.POST_TOOL_USE, "Edit", input -> HookOutput.allow());

			assertThat(result).isSameAs(session);
		}

	}

	@Nested
	@DisplayName("Close Tests")
	class CloseTests {

		@Test
		@DisplayName("should be idempotent on close")
		void shouldBeIdempotentOnClose() {
			DefaultClaudeSession session = DefaultClaudeSession.builder().workingDirectory(workingDirectory).build();

			// Multiple closes should not throw
			session.close();
			session.close();
			session.close();
		}

		@Test
		@DisplayName("disconnect should be alias for close")
		void disconnectShouldAliasClose() {
			DefaultClaudeSession session = DefaultClaudeSession.builder().workingDirectory(workingDirectory).build();

			session.disconnect();
			assertThat(session.isConnected()).isFalse();
		}

	}

	@Nested
	@DisplayName("Server Info Tests")
	class ServerInfoTests {

		@Test
		@DisplayName("should return empty server info when not connected")
		void shouldReturnEmptyServerInfoWhenNotConnected() {
			DefaultClaudeSession session = DefaultClaudeSession.builder().workingDirectory(workingDirectory).build();

			assertThat(session.getServerInfo()).isEmpty();
		}

	}

}
