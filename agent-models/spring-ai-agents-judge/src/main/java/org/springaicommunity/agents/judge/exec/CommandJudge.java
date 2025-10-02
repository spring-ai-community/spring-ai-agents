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

package org.springaicommunity.agents.judge.exec;

import org.springaicommunity.agents.judge.DeterministicJudge;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springaicommunity.agents.judge.context.JudgmentContext;
import org.springaicommunity.agents.judge.result.Check;
import org.springaicommunity.agents.judge.result.Judgment;
import org.springaicommunity.agents.judge.score.BooleanScore;
import org.springaicommunity.agents.model.sandbox.ExecResult;
import org.springaicommunity.agents.model.sandbox.ExecSpec;
import org.springaicommunity.agents.model.sandbox.LocalSandbox;

/**
 * Judge that executes a shell command and evaluates based on exit code.
 *
 * <p>
 * Executes a command in the agent's workspace using LocalSandbox and judges success based
 * on the exit code. This judge is useful for:
 * </p>
 * <ul>
 * <li>Running build commands (mvn compile, gradle build)</li>
 * <li>Running test suites (mvn test, npm test)</li>
 * <li>Running linters and code quality tools</li>
 * <li>Custom verification scripts</li>
 * </ul>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * // Simple command with default timeout (2 minutes) and expected exit code (0)
 * CommandJudge mvnCompile = new CommandJudge("mvn compile");
 *
 * // Custom exit code and timeout
 * CommandJudge customCommand = new CommandJudge("my-script.sh", 0, Duration.ofMinutes(5));
 *
 * // Check for non-zero exit (command expected to fail)
 * CommandJudge shouldFail = new CommandJudge("grep 'ERROR' build.log", 1);
 * }</pre>
 *
 * <p>
 * The judgment includes stdout, stderr, and exit code in metadata for detailed analysis.
 * </p>
 *
 * @author Mark Pollack
 * @since 0.1.0
 * @see LocalSandbox
 * @see ExecSpec
 */
public class CommandJudge extends DeterministicJudge {

	private final String command;

	private final int expectedExitCode;

	private final Duration timeout;

	/**
	 * Create a CommandJudge with default settings (exit code 0, 2 minute timeout).
	 * @param command the shell command to execute
	 */
	public CommandJudge(String command) {
		this(command, 0, Duration.ofMinutes(2));
	}

	/**
	 * Create a CommandJudge with custom exit code and timeout.
	 * @param command the shell command to execute
	 * @param expectedExitCode the expected exit code for success (typically 0)
	 * @param timeout maximum duration for command execution
	 */
	public CommandJudge(String command, int expectedExitCode, Duration timeout) {
		super("CommandJudge", String.format("Executes command: %s (expects exit code %d)", command, expectedExitCode));
		this.command = command;
		this.expectedExitCode = expectedExitCode;
		this.timeout = timeout;
	}

	@Override
	public Judgment judge(JudgmentContext context) {
		try (LocalSandbox sandbox = new LocalSandbox(context.workspace())) {

			ExecResult result = sandbox.exec(ExecSpec.builder().shellCommand(command).timeout(timeout).build());

			boolean pass = result.exitCode() == expectedExitCode;

			Map<String, Object> metadata = new HashMap<>();
			metadata.put("command", command);
			metadata.put("exitCode", result.exitCode());
			metadata.put("expectedExitCode", expectedExitCode);
			metadata.put("output", result.mergedLog());
			metadata.put("duration", result.duration().toString());

			String reasoning = pass ? String.format("Command succeeded with exit code %d", result.exitCode()) : String
				.format("Command failed. Expected exit code %d but got %d", expectedExitCode, result.exitCode());

			return Judgment.builder()
				.score(new BooleanScore(pass))
				.pass(pass)
				.reasoning(reasoning)
				.check(pass ? Check.pass("command_execution", "Command executed successfully")
						: Check.fail("command_execution", "Command execution failed"))
				.metadata(metadata)
				.build();
		}
		catch (Exception e) {
			return Judgment.builder()
				.score(new BooleanScore(false))
				.pass(false)
				.reasoning("Command execution failed: " + e.getMessage())
				.check(Check.fail("command_execution", "Execution error: " + e.getMessage()))
				.build();
		}
	}

}
