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
import java.util.Map;

/**
 * Combined specification for agent execution. Contains merged configuration from
 * AgentSpec defaults, run.yaml, and CLI arguments.
 *
 * @param agentSpec resolved agent specification with input defaults
 * @param inputs merged input values (defaults + run.yaml + CLI)
 * @param cwd working directory for execution
 * @param env execution environment settings
 * @author Mark Pollack
 * @since 1.1.0
 */
public record LauncherSpec(AgentSpec agentSpec, Map<String, Object> inputs, Path cwd, Map<String, Object> env) {
}