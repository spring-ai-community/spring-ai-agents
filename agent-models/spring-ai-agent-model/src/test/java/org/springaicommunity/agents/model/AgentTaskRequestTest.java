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

package org.springaicommunity.agents.model;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AgentTaskRequest and its builder.
 *
 * @author Mark Pollack
 */
class AgentTaskRequestTest {

	@Test
	void builderWithDefaults() {
		Path workingDir = Path.of("/tmp/test");

		AgentTaskRequest request = AgentTaskRequest.builder("Fix the tests", workingDir).build();

		assertThat(request.goal()).isEqualTo("Fix the tests");
		assertThat(request.workingDirectory()).isEqualTo(workingDir);
		assertThat(request.options()).isNull();
	}

	@Test
	void builderWithNullOptions() {
		Path workingDir = Path.of("/tmp/test");

		AgentTaskRequest request = AgentTaskRequest.builder("Fix Java files", workingDir).options(null).build();

		assertThat(request.goal()).isEqualTo("Fix Java files");
		assertThat(request.workingDirectory()).isEqualTo(workingDir);
		assertThat(request.options()).isNull();
	}

}