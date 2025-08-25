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

package org.springaicommunity.agents.sweagentsdk.transport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agents.sweagentsdk.types.SweAgentOptions;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SweCliApi.
 */
class SweCliApiTest {

	@Test
	void testDefaultConstructor() {
		SweCliApi api = new SweCliApi();
		assertThat(api).isNotNull();
	}

	@Test
	void testConstructorWithExecutablePath() {
		String customPath = "/usr/local/bin/mini-swe";
		SweCliApi api = new SweCliApi(customPath);
		assertThat(api).isNotNull();
	}

	@Test
	void testConstructorWithNullExecutablePath() {
		SweCliApi api = new SweCliApi(null);
		assertThat(api).isNotNull();
	}

	@Test
	void testSweResultConstructor() {
		SweCliApi.SweResult result = new SweCliApi.SweResult(SweCliApi.SweResultStatus.SUCCESS, "output content",
				"error content", null);

		assertThat(result.getStatus()).isEqualTo(SweCliApi.SweResultStatus.SUCCESS);
		assertThat(result.getOutput()).isEqualTo("output content");
		assertThat(result.getError()).isEqualTo("error content");
		assertThat(result.getMetadata()).isNull();
	}

	@Test
	void testSweResultFromJson() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		String jsonString = "{\"output\":\"test output\",\"success\":true}";
		JsonNode json = mapper.readTree(jsonString);

		SweCliApi.SweResult result = SweCliApi.SweResult.fromJson(json, 0);

		assertThat(result.getStatus()).isEqualTo(SweCliApi.SweResultStatus.SUCCESS);
		assertThat(result.getOutput()).isEqualTo("test output");
		assertThat(result.getError()).isEmpty();
		assertThat(result.getMetadata()).isEqualTo(json);
	}

	@Test
	void testSweResultFromJsonWithFailure() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		String jsonString = "{\"output\":\"test output\",\"success\":false}";
		JsonNode json = mapper.readTree(jsonString);

		SweCliApi.SweResult result = SweCliApi.SweResult.fromJson(json, 0);

		assertThat(result.getStatus()).isEqualTo(SweCliApi.SweResultStatus.ERROR);
	}

	@Test
	void testSweResultFromJsonWithNonZeroExitCode() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		String jsonString = "{\"output\":\"test output\"}";
		JsonNode json = mapper.readTree(jsonString);

		SweCliApi.SweResult result = SweCliApi.SweResult.fromJson(json, 1);

		assertThat(result.getStatus()).isEqualTo(SweCliApi.SweResultStatus.ERROR);
	}

	@Test
	void testSweCliException() {
		SweCliApi.SweCliException exception = new SweCliApi.SweCliException("test message");
		assertThat(exception.getMessage()).isEqualTo("test message");
		assertThat(exception.getCause()).isNull();

		Exception cause = new RuntimeException("cause");
		SweCliApi.SweCliException exceptionWithCause = new SweCliApi.SweCliException("test message", cause);
		assertThat(exceptionWithCause.getMessage()).isEqualTo("test message");
		assertThat(exceptionWithCause.getCause()).isEqualTo(cause);
	}

}