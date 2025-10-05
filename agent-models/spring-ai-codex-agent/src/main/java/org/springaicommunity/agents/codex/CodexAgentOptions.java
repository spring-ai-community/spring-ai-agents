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

package org.springaicommunity.agents.codex;

import org.springaicommunity.agents.codexsdk.types.ApprovalPolicy;
import org.springaicommunity.agents.codexsdk.types.SandboxMode;
import org.springaicommunity.agents.model.AgentOptions;

import java.time.Duration;
import java.util.Map;

/**
 * Configuration options for Codex Agent Model implementations.
 *
 * @author Spring AI Community
 * @since 0.1.0
 */
public class CodexAgentOptions implements AgentOptions {

	private String model = "gpt-5-codex";

	private Duration timeout = Duration.ofMinutes(10);

	private SandboxMode sandboxMode = SandboxMode.WORKSPACE_WRITE;

	private ApprovalPolicy approvalPolicy = ApprovalPolicy.NEVER;

	private boolean fullAuto = true;

	private boolean skipGitCheck = false;

	private String executablePath;

	private String workingDirectory;

	private Map<String, String> environmentVariables = Map.of();

	private Map<String, Object> extras = Map.of();

	private CodexAgentOptions() {
	}

	public String getModel() {
		return model;
	}

	public Duration getTimeout() {
		return timeout;
	}

	@Override
	public Map<String, String> getEnvironmentVariables() {
		return environmentVariables;
	}

	public SandboxMode getSandboxMode() {
		return sandboxMode;
	}

	public ApprovalPolicy getApprovalPolicy() {
		return approvalPolicy;
	}

	public boolean isFullAuto() {
		return fullAuto;
	}

	public boolean isSkipGitCheck() {
		return skipGitCheck;
	}

	public String getExecutablePath() {
		return executablePath;
	}

	@Override
	public String getWorkingDirectory() {
		return workingDirectory;
	}

	@Override
	public Map<String, Object> getExtras() {
		return extras;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {

		private final CodexAgentOptions options = new CodexAgentOptions();

		private Builder() {
		}

		public Builder model(String model) {
			options.model = model;
			return this;
		}

		public Builder timeout(Duration timeout) {
			options.timeout = timeout;
			return this;
		}

		public Builder sandboxMode(SandboxMode sandboxMode) {
			options.sandboxMode = sandboxMode;
			options.fullAuto = false; // Explicit sandbox disables full-auto
			return this;
		}

		public Builder approvalPolicy(ApprovalPolicy approvalPolicy) {
			options.approvalPolicy = approvalPolicy;
			options.fullAuto = false; // Explicit approval disables full-auto
			return this;
		}

		public Builder fullAuto(boolean fullAuto) {
			options.fullAuto = fullAuto;
			if (fullAuto) {
				// Full-auto implies workspace-write and never approval
				options.sandboxMode = SandboxMode.WORKSPACE_WRITE;
				options.approvalPolicy = ApprovalPolicy.NEVER;
			}
			return this;
		}

		public Builder skipGitCheck(boolean skipGitCheck) {
			options.skipGitCheck = skipGitCheck;
			return this;
		}

		public Builder executablePath(String executablePath) {
			options.executablePath = executablePath;
			return this;
		}

		public Builder extras(Map<String, Object> extras) {
			options.extras = extras != null ? extras : Map.of();
			return this;
		}

		public CodexAgentOptions build() {
			return options;
		}

	}

}
