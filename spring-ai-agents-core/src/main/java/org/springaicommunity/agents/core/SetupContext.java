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

package org.springaicommunity.agents.core;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Context object passed from setup phase to execute phase. Holds workspace path,
 * metadata, and success/failure state from setup operations.
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public class SetupContext {

	private final Path workspace;

	private final Map<String, Object> metadata;

	private final boolean successful;

	private final String error;

	private SetupContext(Builder builder) {
		this.workspace = builder.workspace;
		this.metadata = Collections.unmodifiableMap(new HashMap<>(builder.metadata));
		this.successful = builder.successful;
		this.error = builder.error;
	}

	/**
	 * Get the workspace path where agent will execute.
	 * @return workspace path, may be null if setup failed
	 */
	public Path getWorkspace() {
		return workspace;
	}

	/**
	 * Get metadata value by key with type casting.
	 * @param key metadata key
	 * @param <T> expected type
	 * @return metadata value cast to type T, or null if not found
	 */
	@SuppressWarnings("unchecked")
	public <T> T getMetadata(String key) {
		return (T) metadata.get(key);
	}

	/**
	 * Get metadata value by key with default fallback.
	 * @param key metadata key
	 * @param defaultValue value to return if key not found
	 * @param <T> expected type
	 * @return metadata value or defaultValue if not found
	 */
	@SuppressWarnings("unchecked")
	public <T> T getMetadataOrDefault(String key, T defaultValue) {
		Object value = metadata.get(key);
		return value != null ? (T) value : defaultValue;
	}

	/**
	 * Check if setup phase completed successfully.
	 * @return true if setup succeeded, false otherwise
	 */
	public boolean isSuccessful() {
		return successful;
	}

	/**
	 * Get error message from failed setup.
	 * @return error message if setup failed, null otherwise
	 */
	public String getError() {
		return error;
	}

	/**
	 * Create builder for SetupContext.
	 * @return new builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Create empty context for agents without setup phase.
	 * @return successful empty context
	 */
	public static SetupContext empty() {
		return builder().successful(true).build();
	}

	/**
	 * Builder for SetupContext.
	 */
	public static class Builder {

		private Path workspace;

		private Map<String, Object> metadata = new HashMap<>();

		private boolean successful;

		private String error;

		/**
		 * Set workspace path.
		 * @param workspace workspace directory
		 * @return this builder
		 */
		public Builder workspace(Path workspace) {
			this.workspace = workspace;
			return this;
		}

		/**
		 * Add metadata key-value pair.
		 * @param key metadata key
		 * @param value metadata value
		 * @return this builder
		 */
		public Builder metadata(String key, Object value) {
			this.metadata.put(key, value);
			return this;
		}

		/**
		 * Set success state.
		 * @param successful true if setup succeeded
		 * @return this builder
		 */
		public Builder successful(boolean successful) {
			this.successful = successful;
			return this;
		}

		/**
		 * Set error message (implies setup failure).
		 * @param error error message
		 * @return this builder
		 */
		public Builder error(String error) {
			this.error = error;
			this.successful = false;
			return this;
		}

		/**
		 * Build immutable SetupContext.
		 * @return SetupContext instance
		 */
		public SetupContext build() {
			return new SetupContext(this);
		}

	}

}
