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

import java.time.Duration;
import java.util.Objects;

/**
 * Result of executing a command via a {@link Sandbox}.
 *
 * <p>
 * Designed specifically for AI agent evaluation where the merged log output (stdout +
 * stderr interleaved) is analyzed by LLMs in a single pass. The temporal ordering of
 * output streams is preserved to maintain context for AI analysis.
 */
public record ExecResult(int exitCode, String mergedLog, Duration duration) {

	public ExecResult {
		Objects.requireNonNull(mergedLog, "mergedLog cannot be null");
		Objects.requireNonNull(duration, "duration cannot be null");
	}

	/**
	 * Indicates whether the command executed successfully.
	 * @return true if exit code is 0, false otherwise
	 */
	public boolean success() {
		return exitCode == 0;
	}

	/**
	 * Indicates whether the command failed.
	 * @return true if exit code is non-zero, false otherwise
	 */
	public boolean failed() {
		return !success();
	}

	/**
	 * Checks if the execution produced any output.
	 * @return true if mergedLog is not empty, false otherwise
	 */
	public boolean hasOutput() {
		return !mergedLog.isEmpty();
	}

	/**
	 * Gets the length of the merged log output. Useful for metrics and determining if
	 * output was truncated.
	 * @return length of mergedLog in characters
	 */
	public int outputLength() {
		return mergedLog.length();
	}

	/**
	 * Creates a summary string suitable for logging or display. Does not include the full
	 * output to avoid log spam.
	 * @return concise summary of the execution result
	 */
	public String summary() {
		return String.format("ExecResult{exitCode=%d, success=%s, duration=%s, outputLength=%d}", exitCode, success(),
				duration, outputLength());
	}

	@Override
	public String toString() {
		// For debugging - includes full output but marks it clearly
		return String.format("ExecResult{exitCode=%d, duration=%s, mergedLog='%s'}", exitCode, duration, mergedLog);
	}

}