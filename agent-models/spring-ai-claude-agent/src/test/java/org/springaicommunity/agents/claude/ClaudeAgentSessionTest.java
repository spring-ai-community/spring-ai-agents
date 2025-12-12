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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springaicommunity.claude.agent.sdk.config.PermissionMode;
import org.springaicommunity.claude.agent.sdk.hooks.HookRegistry;
import org.springaicommunity.claude.agent.sdk.transport.CLIOptions;
import org.springaicommunity.claude.agent.sdk.types.control.HookEvent;
import org.springaicommunity.claude.agent.sdk.types.control.HookOutput;

import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ClaudeAgentSession.
 */
class ClaudeAgentSessionTest {

	private Path workingDirectory;

	@BeforeEach
	void setUp() {
		workingDirectory = Path.of(System.getProperty("user.dir"));
	}

	@Nested
	@DisplayName("Builder Tests")
	class BuilderTests {

		@Test
		@DisplayName("should build session with default working directory")
		void shouldBuildWithDefaults() {
			ClaudeAgentSession session = ClaudeAgentSession.builder().build();

			assertThat(session).isNotNull();
			assertThat(session.isConnected()).isFalse();
		}

		@Test
		@DisplayName("should build session with explicit working directory")
		void shouldBuildWithWorkingDirectory() {
			ClaudeAgentSession session = ClaudeAgentSession.builder().workingDirectory(workingDirectory).build();

			assertThat(session).isNotNull();
		}

		@Test
		@DisplayName("should build session with all options")
		void shouldBuildWithAllOptions() {
			CLIOptions options = CLIOptions.builder().model("claude-sonnet-4-20250514").build();
			HookRegistry registry = new HookRegistry();

			ClaudeAgentSession session = ClaudeAgentSession.builder()
				.workingDirectory(workingDirectory)
				.options(options)
				.timeout(Duration.ofMinutes(5))
				.claudePath("/custom/claude")
				.hookRegistry(registry)
				.build();

			assertThat(session).isNotNull();
		}

	}

	@Nested
	@DisplayName("Model Integration Tests")
	class ModelIntegrationTests {

		@Test
		@DisplayName("should create session from ClaudeAgentModel")
		void shouldCreateSessionFromModel() {
			ClaudeAgentModel model = ClaudeAgentModel.builder().workingDirectory(workingDirectory).build();

			ClaudeAgentSession session = model.createSession();

			assertThat(session).isNotNull();
			assertThat(session.isConnected()).isFalse();
		}

		@Test
		@DisplayName("should create session with custom options from model")
		void shouldCreateSessionWithOptions() {
			ClaudeAgentModel model = ClaudeAgentModel.builder().workingDirectory(workingDirectory).build();

			CLIOptions options = CLIOptions.builder().model("claude-opus-4-20250514").build();

			ClaudeAgentSession session = model.createSession(options);

			assertThat(session).isNotNull();
		}

	}

	@Nested
	@DisplayName("Session State Tests")
	class SessionStateTests {

		@Test
		@DisplayName("should not be connected after creation")
		void shouldNotBeConnectedAfterCreation() {
			ClaudeAgentSession session = ClaudeAgentSession.builder().workingDirectory(workingDirectory).build();

			assertThat(session.isConnected()).isFalse();
		}

		@Test
		@DisplayName("should throw when querying without connection")
		void shouldThrowWhenQueryingWithoutConnection() {
			ClaudeAgentSession session = ClaudeAgentSession.builder().workingDirectory(workingDirectory).build();

			assertThatThrownBy(() -> session.query("test")).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("not connected");
		}

		@Test
		@DisplayName("should throw when interrupting without connection")
		void shouldThrowWhenInterruptingWithoutConnection() {
			ClaudeAgentSession session = ClaudeAgentSession.builder().workingDirectory(workingDirectory).build();

			assertThatThrownBy(() -> session.interrupt()).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("not connected");
		}

	}

	@Nested
	@DisplayName("Hook Registration Tests")
	class HookRegistrationTests {

		@Test
		@DisplayName("should register PreToolUse hook")
		void shouldRegisterPreToolUseHook() {
			ClaudeAgentSession session = ClaudeAgentSession.builder().workingDirectory(workingDirectory).build();

			ClaudeAgentSession result = session.registerPreToolUse("Bash", input -> HookOutput.allow());

			assertThat(result).isSameAs(session);
		}

		@Test
		@DisplayName("should register PostToolUse hook")
		void shouldRegisterPostToolUseHook() {
			ClaudeAgentSession session = ClaudeAgentSession.builder().workingDirectory(workingDirectory).build();

			ClaudeAgentSession result = session.registerPostToolUse("Edit", input -> HookOutput.allow());

			assertThat(result).isSameAs(session);
		}

		@Test
		@DisplayName("should register hook by event type")
		void shouldRegisterByEventType() {
			ClaudeAgentSession session = ClaudeAgentSession.builder().workingDirectory(workingDirectory).build();

			ClaudeAgentSession result = session.registerHook(HookEvent.PRE_TOOL_USE, "Write|Edit",
					input -> HookOutput.allow());

			assertThat(result).isSameAs(session);
		}

		@Test
		@DisplayName("should support fluent hook registration")
		void shouldSupportFluentRegistration() {
			ClaudeAgentSession session = ClaudeAgentSession.builder().workingDirectory(workingDirectory).build();

			session.registerPreToolUse("Bash", input -> {
				// Block dangerous commands
				return HookOutput.allow();
			}).registerPostToolUse("Edit", input -> {
				// Log file changes
				return HookOutput.allow();
			});
		}

	}

	@Nested
	@DisplayName("Close Tests")
	class CloseTests {

		@Test
		@DisplayName("should be idempotent on close")
		void shouldBeIdempotentOnClose() {
			ClaudeAgentSession session = ClaudeAgentSession.builder().workingDirectory(workingDirectory).build();

			session.close();
			session.close();
			// No exception = success
		}

		@Test
		@DisplayName("disconnect should close the session")
		void disconnectShouldClose() {
			ClaudeAgentSession session = ClaudeAgentSession.builder().workingDirectory(workingDirectory).build();

			session.disconnect();
			assertThat(session.isConnected()).isFalse();
		}

	}

	@Nested
	@DisplayName("Server Info Tests")
	class ServerInfoTests {

		@Test
		@DisplayName("should return empty server info when not connected")
		void shouldReturnEmptyServerInfo() {
			ClaudeAgentSession session = ClaudeAgentSession.builder().workingDirectory(workingDirectory).build();

			assertThat(session.getServerInfo()).isEmpty();
		}

	}

}
