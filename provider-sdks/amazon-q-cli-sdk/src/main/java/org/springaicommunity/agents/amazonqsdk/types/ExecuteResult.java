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

package org.springaicommunity.agents.amazonqsdk.types;

import java.time.Duration;

/**
 * Result of Amazon Q CLI execution.
 *
 * @author Spring AI Community
 * @since 0.1.0
 */
public class ExecuteResult {

	private final String output;

	private final int exitCode;

	private final String model;

	private final Duration duration;

	private final String conversationId;

	public ExecuteResult(String output, int exitCode, String model, Duration duration, String conversationId) {
		this.output = output;
		this.exitCode = exitCode;
		this.model = model;
		this.duration = duration;
		this.conversationId = conversationId;
	}

	public String getOutput() {
		return output;
	}

	public int getExitCode() {
		return exitCode;
	}

	public String getModel() {
		return model;
	}

	public Duration getDuration() {
		return duration;
	}

	public String getConversationId() {
		return conversationId;
	}

	public boolean isSuccessful() {
		return exitCode == 0;
	}

	@Override
	public String toString() {
		return "ExecuteResult{" + "output='" + output + '\'' + ", exitCode=" + exitCode + ", model='" + model + '\''
				+ ", duration=" + duration + ", conversationId='" + conversationId + '\'' + '}';
	}

}
