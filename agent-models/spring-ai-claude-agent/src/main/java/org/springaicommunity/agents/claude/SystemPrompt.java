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

package org.springaicommunity.agents.claude;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base interface for system prompt configuration. Supports both string prompts and preset
 * prompts.
 *
 * @author Mark Pollack
 * @since 1.1.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({ @JsonSubTypes.Type(value = SystemPrompt.StringPrompt.class, name = "string"),
		@JsonSubTypes.Type(value = SystemPrompt.PresetPrompt.class, name = "preset") })
public sealed interface SystemPrompt permits SystemPrompt.StringPrompt, SystemPrompt.PresetPrompt {

	/**
	 * Simple string-based system prompt.
	 *
	 * @param prompt the system prompt text
	 */
	record StringPrompt(String prompt) implements SystemPrompt {
	}

	/**
	 * Preset-based system prompt configuration.
	 *
	 * @param preset the preset name (e.g., "claude_code")
	 * @param append optional text to append to the preset prompt
	 */
	record PresetPrompt(String preset, String append) implements SystemPrompt {

		/**
		 * Creates a preset prompt without additional text.
		 * @param preset the preset name
		 */
		public PresetPrompt(String preset) {
			this(preset, null);
		}

		/**
		 * Claude Code preset constant.
		 */
		public static final String CLAUDE_CODE = "claude_code";

		/**
		 * Creates a Claude Code preset prompt.
		 * @return a preset prompt for Claude Code
		 */
		public static PresetPrompt claudeCode() {
			return new PresetPrompt(CLAUDE_CODE);
		}

		/**
		 * Creates a Claude Code preset prompt with additional text.
		 * @param append text to append to the Claude Code preset
		 * @return a preset prompt for Claude Code with appended text
		 */
		public static PresetPrompt claudeCode(String append) {
			return new PresetPrompt(CLAUDE_CODE, append);
		}

	}

	/**
	 * Creates a simple string-based system prompt.
	 * @param prompt the prompt text
	 * @return a StringPrompt instance
	 */
	static StringPrompt of(String prompt) {
		return new StringPrompt(prompt);
	}

	/**
	 * Creates a preset-based system prompt.
	 * @param preset the preset name
	 * @return a PresetPrompt instance
	 */
	static PresetPrompt preset(String preset) {
		return new PresetPrompt(preset);
	}

	/**
	 * Creates a preset-based system prompt with additional text.
	 * @param preset the preset name
	 * @param append text to append
	 * @return a PresetPrompt instance
	 */
	static PresetPrompt preset(String preset, String append) {
		return new PresetPrompt(preset, append);
	}

}
