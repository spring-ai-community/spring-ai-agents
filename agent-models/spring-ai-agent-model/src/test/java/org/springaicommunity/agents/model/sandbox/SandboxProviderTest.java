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

package org.springaicommunity.agents.model.sandbox;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for sandbox provider interfaces and implementations.
 *
 * These tests prove the interface-based dependency injection design.
 */
class SandboxProviderTest {

	@Test
	void defaultSandboxProviderReturnsConsistentSandbox() {
		// Arrange
		DefaultSandboxProvider provider = new DefaultSandboxProvider();

		// Act
		Sandbox sandbox1 = provider.getSandbox();
		Sandbox sandbox2 = provider.getSandbox();

		// Assert: Same instance returned (singleton behavior)
		assertThat(sandbox1).isNotNull();
		assertThat(sandbox2).isNotNull();
		assertThat(sandbox1).isSameAs(sandbox2);
	}

	@Test
	void defaultSandboxProviderUsesAutoDetection() {
		// Arrange
		DefaultSandboxProvider provider = new DefaultSandboxProvider();

		// Act
		Sandbox sandbox = provider.getSandbox();

		// Assert: Returns either DockerSandbox or LocalSandbox based on Docker
		// availability
		assertThat(sandbox).isNotNull();
		assertThat(sandbox).isInstanceOfAny(DockerSandbox.class, LocalSandbox.class);
	}

	@Test
	void sandboxProviderInterfaceEnablesDependencyInjection() {
		// Arrange: Create a custom implementation
		SandboxProvider customProvider = new SandboxProvider() {
			@Override
			public Sandbox getSandbox() {
				return new LocalSandbox(); // Always return LocalSandbox for testing
			}
		};

		// Act
		Sandbox sandbox = customProvider.getSandbox();

		// Assert: Interface allows easy substitution
		assertThat(sandbox).isNotNull();
		assertThat(sandbox).isInstanceOf(LocalSandbox.class);
	}

	@Test
	void sandboxProviderEnablesTestability() {
		// This test demonstrates how easy it is to create test doubles

		// Arrange: Mock implementation
		MockSandbox mockSandbox = new MockSandbox();
		SandboxProvider testProvider = () -> mockSandbox;

		// Act
		Sandbox sandbox = testProvider.getSandbox();

		// Assert: Easy to inject test implementations
		assertThat(sandbox).isSameAs(mockSandbox);
	}

	// Simple test double for demonstration
	private static class MockSandbox implements Sandbox {

		@Override
		public ExecResult exec(ExecSpec spec) {
			return new ExecResult(0, "Mock output", java.time.Duration.ofSeconds(1));
		}

		@Override
		public java.nio.file.Path workDir() {
			return java.nio.file.Paths.get("/mock");
		}

		@Override
		public boolean isClosed() {
			return false;
		}

		@Override
		public void close() {
			// No-op for mock
		}

	}

}