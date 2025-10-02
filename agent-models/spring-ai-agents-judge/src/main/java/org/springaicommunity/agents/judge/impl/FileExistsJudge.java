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

package org.springaicommunity.agents.judge.impl;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springaicommunity.agents.judge.context.JudgmentContext;
import org.springaicommunity.agents.judge.result.Check;
import org.springaicommunity.agents.judge.result.Judgment;
import org.springaicommunity.agents.judge.score.BooleanScore;

/**
 * Judge that verifies file existence in the workspace.
 *
 * <p>
 * This is a simple deterministic judge that checks if a file exists at the specified path
 * relative to the workspace directory.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * &#64;Component
 * public class MyFileExistsJudge extends FileExistsJudge {
 *     public MyFileExistsJudge() {
 *         super("output.txt");
 *     }
 * }
 * }</pre>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public class FileExistsJudge extends DeterministicJudge {

	private final String filePath;

	public FileExistsJudge(String filePath) {
		super("FileExistsJudge", "Verifies that file exists at path: " + filePath);
		this.filePath = filePath;
	}

	@Override
	public Judgment judge(JudgmentContext context) {
		Path targetFile = context.workspace().resolve(filePath);
		boolean exists = Files.exists(targetFile);

		return Judgment.builder()
			.score(new BooleanScore(exists))
			.pass(exists)
			.reasoning(exists ? String.format("File exists at %s", filePath)
					: String.format("File not found at %s", filePath))
			.check(exists ? Check.pass("file_exists", "File found at " + filePath)
					: Check.fail("file_exists", "File not found at " + filePath))
			.build();
	}

}
