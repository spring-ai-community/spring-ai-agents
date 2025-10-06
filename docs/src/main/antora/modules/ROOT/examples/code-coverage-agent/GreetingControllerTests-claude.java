/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.restservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(GreetingController.class)
public class GreetingControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	public void greetingShouldReturnDefaultMessageWhenNoParameterProvided() throws Exception {
		mockMvc.perform(get("/greeting"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.content").value("Hello, World!"))
				.andExpect(jsonPath("$.id").isNumber());
	}

	@Test
	public void greetingShouldReturnCustomMessageWhenNameProvided() throws Exception {
		mockMvc.perform(get("/greeting").param("name", "Spring Community"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.content").value("Hello, Spring Community!"))
				.andExpect(jsonPath("$.id").isNumber());
	}

	@Test
	public void greetingShouldUseDefaultValueWhenEmptyStringProvided() throws Exception {
		// Spring treats empty string parameters as missing, so default value is used
		mockMvc.perform(get("/greeting").param("name", ""))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.content").value("Hello, World!"))
				.andExpect(jsonPath("$.id").isNumber());
	}

	@Test
	public void greetingShouldHandleSpecialCharactersInName() throws Exception {
		mockMvc.perform(get("/greeting").param("name", "José & María"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.content").value("Hello, José & María!"))
				.andExpect(jsonPath("$.id").isNumber());
	}

	@Test
	public void greetingShouldHandleUnicodeCharactersInName() throws Exception {
		mockMvc.perform(get("/greeting").param("name", "世界"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.content").value("Hello, 世界!"))
				.andExpect(jsonPath("$.id").isNumber());
	}

	@Test
	public void greetingShouldHandleLongNameParameter() throws Exception {
		String longName = "A".repeat(1000);
		String expectedContent = "Hello, " + longName + "!";

		mockMvc.perform(get("/greeting").param("name", longName))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.content").value(expectedContent))
				.andExpect(jsonPath("$.id").isNumber());
	}

	@Test
	public void greetingShouldIncrementIdOnEachCall() throws Exception {
		// First call
		MvcResult result1 = mockMvc.perform(get("/greeting"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").isNumber())
				.andReturn();

		// Second call
		MvcResult result2 = mockMvc.perform(get("/greeting"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").isNumber())
				.andReturn();

		// Extract and compare IDs using AssertJ
		String json1 = result1.getResponse().getContentAsString();
		String json2 = result2.getResponse().getContentAsString();

		// Parse JSON manually for ID comparison (simple approach for this test)
		long id1 = Long.parseLong(json1.split("\"id\":")[1].split(",")[0]);
		long id2 = Long.parseLong(json2.split("\"id\":")[1].split(",")[0]);

		assertThat(id2).isGreaterThan(id1);
	}

	@Test
	public void greetingShouldHandleUrlEncodedParameters() throws Exception {
		mockMvc.perform(get("/greeting").param("name", "John Doe"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.content").value("Hello, John Doe!"))
				.andExpect(jsonPath("$.id").isNumber());
	}

}