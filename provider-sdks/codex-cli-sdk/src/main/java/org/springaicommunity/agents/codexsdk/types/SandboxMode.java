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

/**
 * Sandbox security modes for Codex CLI execution.
 *
 * @author Spring AI Community
 * @since 0.1.0
 */
public enum SandboxMode {

	/**
	 * Read-only mode - no file edits or network commands allowed. Default for exec mode.
	 */
	READ_ONLY("read-only"),

	/**
	 * Workspace write mode - can edit files in working directory, /tmp, and $TMPDIR. No
	 * network access. Recommended for most autonomous operations.
	 */
	WORKSPACE_WRITE("workspace-write"),

	/**
	 * Danger mode - full file system and network access. Use with extreme caution.
	 */
	DANGER_FULL_ACCESS("danger-full-access");

	private final String value;

	SandboxMode(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}
