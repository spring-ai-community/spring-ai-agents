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

package org.springaicommunity.agents.helloworld;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agents.core.AgentRunner;
import org.springaicommunity.agents.core.LauncherSpec;
import org.springaicommunity.agents.core.Result;
import org.springaicommunity.agents.core.SetupContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Hello World agent implementation. Creates files with specified content. This agent
 * demonstrates the black-box agent pattern where internal logic is encapsulated and only
 * typed inputs/outputs are exposed.
 *
 * @author Mark Pollack
 * @since 1.1.0
 */
public class HelloWorldAgentRunner implements AgentRunner {

	private static final Logger log = LoggerFactory.getLogger(HelloWorldAgentRunner.class);

	@Override
	public Result run(SetupContext setup, LauncherSpec spec) {
		log.info("Executing hello-world agent");
		try {
			// Parse and validate inputs
			Map<String, Object> inputs = spec.inputs();
			String path = (String) inputs.get("path");
			String content = (String) inputs.get("content");

			// Apply defaults
			if (content == null) {
				content = "HelloWorld";
			}

			log.info("Hello-world inputs: path={}, content={}", path, content.length() + " chars");

			// Validate required inputs
			if (path == null || path.isBlank()) {
				return Result.fail("Missing required input: path");
			}

			Path targetFile = spec.cwd().resolve(path);
			log.info("Target file path: {}", targetFile.toAbsolutePath());

			Files.createDirectories(targetFile.getParent());
			Files.writeString(targetFile, content);

			log.info("Successfully created file: {}", targetFile.toAbsolutePath());
			return Result.ok("Created file: " + targetFile.toAbsolutePath(),
					Map.of("path", targetFile.toAbsolutePath().toString(), "content_length", content.length()));
		}
		catch (Exception e) {
			log.error("Failed to create file", e);
			return Result.fail("Failed to create file: " + e.getMessage());
		}
	}

}