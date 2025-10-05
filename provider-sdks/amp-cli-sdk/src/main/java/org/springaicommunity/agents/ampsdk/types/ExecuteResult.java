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

package org.springaicommunity.agents.ampsdk.types;

import java.time.Duration;

/**
 * Result from Amp CLI execute mode operation.
 *
 * @author Spring AI Community
 * @since 0.1.0
 */
public class ExecuteResult {

	private final String output;

	private final int exitCode;

	private final Duration duration;

	private final String model;

	public ExecuteResult(String output, int exitCode, Duration duration, String model) {
		this.output = output;
		this.exitCode = exitCode;
		this.duration = duration;
		this.model = model;
	}

	public String getOutput() {
		return output;
	}

	public int getExitCode() {
		return exitCode;
	}

	public Duration getDuration() {
		return duration;
	}

	public String getModel() {
		return model;
	}

	public boolean isSuccessful() {
		return exitCode == 0;
	}

}
