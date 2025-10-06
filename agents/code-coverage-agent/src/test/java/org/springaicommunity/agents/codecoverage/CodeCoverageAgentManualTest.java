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

package org.springaicommunity.agents.codecoverage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;
import org.springaicommunity.agents.core.AgentSpec;
import org.springaicommunity.agents.core.AgentSpecLoader;
import org.springaicommunity.agents.core.LauncherSpec;
import org.springaicommunity.agents.core.Result;
import org.springaicommunity.agents.core.SetupContext;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for code coverage agent.
 *
 * @author Mark Pollack
 */
class CodeCoverageAgentIT {

	@Test
	@Timeout(value = 10, unit = TimeUnit.MINUTES)
	@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".*")
	void testCodeCoverageAgentSetupAndExecute() throws Exception {
		System.out.println("=== Code Coverage Agent Integration Test ===");

		// Load agent spec
		AgentSpec agentSpec = AgentSpecLoader.loadAgentSpec("coverage");
		assertNotNull(agentSpec);
		assertEquals("coverage", agentSpec.id());
		System.out.println("Loaded agent spec: " + agentSpec.id());

		// Create launcher spec with unique timestamped directory
		String timestamp = String.valueOf(System.currentTimeMillis());
		Path workingDir = Paths.get("/tmp/coverage-it-test-" + timestamp);
		workingDir.toFile().mkdirs();
		System.out.println("Working directory: " + workingDir);

		// Use low target coverage (20%) for faster development iteration
		LauncherSpec spec = new LauncherSpec(agentSpec, Map.of("target_coverage", 20), workingDir, Map.of());

		// Create agent
		CodeCoverageAgentRunner agent = new CodeCoverageAgentRunner();

		// Run setup phase
		System.out.println("\n=== SETUP PHASE ===");
		SetupContext setup = agent.setup(spec);

		if (!setup.isSuccessful()) {
			System.err.println("Setup failed: " + setup.getError());
			fail("Setup phase failed: " + setup.getError());
		}

		assertTrue(setup.isSuccessful(), "Setup should succeed");
		assertNotNull(setup.getWorkspace(), "Workspace should be set");
		System.out.println("✅ Setup completed successfully");
		System.out.println("Workspace: " + setup.getWorkspace());

		// Run execute phase
		System.out.println("\n=== EXECUTE PHASE ===");
		Result result = agent.run(setup, spec);

		System.out.println("\n=== RESULT ===");
		System.out.println("Success: " + result.success());
		System.out.println("Message: " + result.message());
		System.out.println("Data: " + result.data());

		// Basic assertions
		assertNotNull(result);
		assertNotNull(result.message());
	}

}
