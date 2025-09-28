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
 * Complete run configuration combining agent selection, sandbox environment, and task
 * parameters. This is the primary configuration object that benchmark programs generate
 * arrays of.
 *
 * @param agent which agent to run (matches AgentSpec.id)
 * @param inputs runtime input values for the task
 * @param workingDirectory sandbox working directory (null for current directory)
 * @param env execution environment variables and sandbox settings
 * @author Mark Pollack
 * @since 1.1.0
 */
public record RunSpec(String agent, Map<String, Object> inputs, String workingDirectory, Map<String, Object> env) {
}