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
 * Options for Amp CLI execute mode operations.
 *
 * @author Spring AI Community
 * @since 0.1.0
 */
public class ExecuteOptions {

	private final boolean dangerouslyAllowAll;

	private final Duration timeout;

	private final String model;

	private ExecuteOptions(Builder builder) {
		this.dangerouslyAllowAll = builder.dangerouslyAllowAll;
		this.timeout = builder.timeout;
		this.model = builder.model;
	}

	public boolean isDangerouslyAllowAll() {
		return dangerouslyAllowAll;
	}

	public Duration getTimeout() {
		return timeout;
	}

	public String getModel() {
		return model;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static ExecuteOptions defaultOptions() {
		return builder().build();
	}

	public static final class Builder {

		private boolean dangerouslyAllowAll = true;

		private Duration timeout = Duration.ofMinutes(5);

		private String model;

		private Builder() {
		}

		public Builder dangerouslyAllowAll(boolean dangerouslyAllowAll) {
			this.dangerouslyAllowAll = dangerouslyAllowAll;
			return this;
		}

		public Builder timeout(Duration timeout) {
			this.timeout = timeout;
			return this;
		}

		public Builder model(String model) {
			this.model = model;
			return this;
		}

		public ExecuteOptions build() {
			return new ExecuteOptions(this);
		}

	}

}
