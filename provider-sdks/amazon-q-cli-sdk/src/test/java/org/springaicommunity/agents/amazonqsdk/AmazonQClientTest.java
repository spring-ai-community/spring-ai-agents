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

package org.springaicommunity.agents.amazonqsdk;

import org.junit.jupiter.api.Test;
import org.springaicommunity.agents.amazonqsdk.transport.CLITransport;
import org.springaicommunity.agents.amazonqsdk.types.ExecuteOptions;
import org.springaicommunity.agents.amazonqsdk.types.ExecuteResult;

import java.nio.file.Paths;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AmazonQClient.
 *
 * @author Spring AI Community
 */
class AmazonQClientTest {

	@Test
	void testExecuteWithOptions() {
		// Arrange
		CLITransport mockTransport = mock(CLITransport.class);
		ExecuteResult expectedResult = new ExecuteResult("Success", 0, "amazon-q-developer", Duration.ofSeconds(5),
				null);

		when(mockTransport.execute(anyString(), any(ExecuteOptions.class))).thenReturn(expectedResult);

		AmazonQClient client = new AmazonQClient(mockTransport, Paths.get("/tmp"));
		ExecuteOptions options = ExecuteOptions.builder().trustAllTools(true).build();

		// Act
		ExecuteResult result = client.execute("Test prompt", options);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getOutput()).isEqualTo("Success");
		assertThat(result.isSuccessful()).isTrue();
	}

	@Test
	void testExecuteWithDefaultOptions() {
		// Arrange
		CLITransport mockTransport = mock(CLITransport.class);
		ExecuteResult expectedResult = new ExecuteResult("Success", 0, "amazon-q-developer", Duration.ofSeconds(5),
				null);

		when(mockTransport.execute(anyString(), any(ExecuteOptions.class))).thenReturn(expectedResult);

		AmazonQClient client = new AmazonQClient(mockTransport, Paths.get("/tmp"));

		// Act
		ExecuteResult result = client.execute("Test prompt");

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.isSuccessful()).isTrue();
	}

	@Test
	void testResume() {
		// Arrange
		CLITransport mockTransport = mock(CLITransport.class);
		ExecuteResult expectedResult = new ExecuteResult("Resumed", 0, "amazon-q-developer", Duration.ofSeconds(3),
				null);

		when(mockTransport.execute(anyString(), any(ExecuteOptions.class))).thenReturn(expectedResult);

		AmazonQClient client = new AmazonQClient(mockTransport, Paths.get("/tmp"));

		// Act
		ExecuteResult result = client.resume("Continue task");

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getOutput()).isEqualTo("Resumed");
	}

}
