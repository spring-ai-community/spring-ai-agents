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

import java.util.Map;

/**
 * Immutable specification defining what an agent does. Contains input definitions only.
 * Prompts are hardcoded in agent implementations as black boxes.
 *
 * @param id unique agent identifier (e.g., "hello-world", "coverage")
 * @param version agent version
 * @param inputs input definitions with types and defaults
 * @author Mark Pollack
 * @since 1.1.0
 */
public record AgentSpec(String id, String version, Map<String, InputDef> inputs) {

	/**
	 * Input definition with type information and defaults.
	 *
	 * @param type input type ("string", "integer", "boolean")
	 * @param defaultValue default value if not provided
	 * @param required whether input is required
	 */
	public record InputDef(String type, Object defaultValue, boolean required) {
	}

}