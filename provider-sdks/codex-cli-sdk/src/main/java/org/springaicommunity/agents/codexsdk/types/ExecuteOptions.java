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

package org.springaicommunity.agents.codexsdk.types;

import java.nio.file.Path;
import java.time.Duration;

/**
 * Configuration options for Codex CLI execution.
 *
 * @author Spring AI Community
 * @since 0.1.0
 */
public class ExecuteOptions {

	private final String model;

	private final Duration timeout;

	private final Path workingDirectory;

	private final SandboxMode sandboxMode;

	private final ApprovalPolicy approvalPolicy;

	private final boolean fullAuto;

	private final boolean skipGitCheck;

	private final boolean jsonOutput;

	private final Path outputSchema;

	private ExecuteOptions(Builder builder) {
		this.model = builder.model;
		this.timeout = builder.timeout;
		this.workingDirectory = builder.workingDirectory;
		this.sandboxMode = builder.sandboxMode;
		this.approvalPolicy = builder.approvalPolicy;
		this.fullAuto = builder.fullAuto;
		this.skipGitCheck = builder.skipGitCheck;
		this.jsonOutput = builder.jsonOutput;
		this.outputSchema = builder.outputSchema;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static ExecuteOptions defaultOptions() {
		return builder().build();
	}

	public String getModel() {
		return model;
	}

	public Duration getTimeout() {
		return timeout;
	}

	public Path getWorkingDirectory() {
		return workingDirectory;
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

	public boolean isJsonOutput() {
		return jsonOutput;
	}

	public Path getOutputSchema() {
		return outputSchema;
	}

	public static class Builder {

		private String model = "gpt-5-codex";

		private Duration timeout = Duration.ofMinutes(3);

		private Path workingDirectory;

		private SandboxMode sandboxMode = SandboxMode.WORKSPACE_WRITE;

		private ApprovalPolicy approvalPolicy = ApprovalPolicy.NEVER;

		private boolean fullAuto = true;

		private boolean skipGitCheck = false;

		private boolean jsonOutput = false;

		private Path outputSchema;

		public Builder model(String model) {
			this.model = model;
			return this;
		}

		public Builder timeout(Duration timeout) {
			this.timeout = timeout;
			return this;
		}

		public Builder workingDirectory(Path workingDirectory) {
			this.workingDirectory = workingDirectory;
			return this;
		}

		public Builder sandboxMode(SandboxMode sandboxMode) {
			this.sandboxMode = sandboxMode;
			this.fullAuto = false; // Explicit sandbox disables full-auto
			return this;
		}

		public Builder approvalPolicy(ApprovalPolicy approvalPolicy) {
			this.approvalPolicy = approvalPolicy;
			this.fullAuto = false; // Explicit approval disables full-auto
			return this;
		}

		public Builder fullAuto(boolean fullAuto) {
			this.fullAuto = fullAuto;
			if (fullAuto) {
				// Full-auto implies workspace-write and never approval
				this.sandboxMode = SandboxMode.WORKSPACE_WRITE;
				this.approvalPolicy = ApprovalPolicy.NEVER;
			}
			return this;
		}

		public Builder skipGitCheck(boolean skipGitCheck) {
			this.skipGitCheck = skipGitCheck;
			return this;
		}

		public Builder jsonOutput(boolean jsonOutput) {
			this.jsonOutput = jsonOutput;
			return this;
		}

		public Builder outputSchema(Path outputSchema) {
			this.outputSchema = outputSchema;
			return this;
		}

		public ExecuteOptions build() {
			return new ExecuteOptions(this);
		}

	}

}
