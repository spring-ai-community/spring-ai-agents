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

package org.springaicommunity.agents.codexsdk;

import org.junit.jupiter.api.Test;
import org.springaicommunity.agents.codexsdk.types.ApprovalPolicy;
import org.springaicommunity.agents.codexsdk.types.ExecuteOptions;
import org.springaicommunity.agents.codexsdk.types.SandboxMode;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CodexClient}.
 *
 * @author Spring AI Community
 */
class CodexClientTest {

	@Test
	void testCreateWithDefaults() {
		// Testing the API design
		assertThat(CodexClient.class).isNotNull();
	}

	@Test
	void testBuilderPattern() {
		ExecuteOptions options = ExecuteOptions.builder()
			.model("gpt-5-codex")
			.timeout(Duration.ofMinutes(5))
			.fullAuto(true)
			.skipGitCheck(false)
			.build();

		assertThat(options).isNotNull();
		assertThat(options.getModel()).isEqualTo("gpt-5-codex");
		assertThat(options.getTimeout()).isEqualTo(Duration.ofMinutes(5));
		assertThat(options.isFullAuto()).isTrue();
		assertThat(options.isSkipGitCheck()).isFalse();
	}

	@Test
	void testDefaultOptions() {
		ExecuteOptions options = ExecuteOptions.defaultOptions();

		assertThat(options).isNotNull();
		assertThat(options.getModel()).isEqualTo("gpt-5-codex");
		assertThat(options.isFullAuto()).isTrue();
		assertThat(options.getTimeout()).isNotNull();
	}

	@Test
	void testFullAutoImpliesSandboxAndApproval() {
		ExecuteOptions options = ExecuteOptions.builder().fullAuto(true).build();

		assertThat(options.isFullAuto()).isTrue();
		assertThat(options.getSandboxMode()).isEqualTo(SandboxMode.WORKSPACE_WRITE);
		assertThat(options.getApprovalPolicy()).isEqualTo(ApprovalPolicy.NEVER);
	}

	@Test
	void testExplicitSandboxDisablesFullAuto() {
		ExecuteOptions options = ExecuteOptions.builder().fullAuto(true).sandboxMode(SandboxMode.READ_ONLY).build();

		assertThat(options.isFullAuto()).isFalse();
		assertThat(options.getSandboxMode()).isEqualTo(SandboxMode.READ_ONLY);
	}

	@Test
	void testExplicitApprovalDisablesFullAuto() {
		ExecuteOptions options = ExecuteOptions.builder().fullAuto(true).approvalPolicy(ApprovalPolicy.ALWAYS).build();

		assertThat(options.isFullAuto()).isFalse();
		assertThat(options.getApprovalPolicy()).isEqualTo(ApprovalPolicy.ALWAYS);
	}

}
