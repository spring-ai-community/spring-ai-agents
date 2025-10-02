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
import java.util.regex.Pattern;

import org.springaicommunity.agents.judge.context.JudgmentContext;
import org.springaicommunity.agents.judge.result.Check;
import org.springaicommunity.agents.judge.result.Judgment;
import org.springaicommunity.agents.judge.score.BooleanScore;

/**
 * Judge that verifies file content matches expected criteria.
 *
 * <p>
 * Supports three matching modes:
 * </p>
 * <ul>
 * <li>EXACT - Content must match exactly</li>
 * <li>CONTAINS - Content must contain the expected string</li>
 * <li>REGEX - Content must match the regular expression pattern</li>
 * </ul>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * // Exact match
 * &#64;Bean
 * public FileContentJudge exactMatch() {
 *     return new FileContentJudge("output.txt", "Hello World", MatchMode.EXACT);
 * }
 *
 * // Contains check
 * &#64;Bean
 * public FileContentJudge containsCheck() {
 *     return new FileContentJudge("log.txt", "SUCCESS", MatchMode.CONTAINS);
 * }
 *
 * // Regex pattern
 * &#64;Bean
 * public FileContentJudge regexCheck() {
 *     return new FileContentJudge("data.json", "\\{.*\"status\".*\\}", MatchMode.REGEX);
 * }
 * }</pre>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public class FileContentJudge extends DeterministicJudge {

	private final String filePath;

	private final String expectedContent;

	private final MatchMode matchMode;

	public FileContentJudge(String filePath, String expectedContent, MatchMode matchMode) {
		super("FileContentJudge",
				String.format("Verifies file content at %s (%s match)", filePath, matchMode.name().toLowerCase()));
		this.filePath = filePath;
		this.expectedContent = expectedContent;
		this.matchMode = matchMode;
	}

	public FileContentJudge(String filePath, String expectedContent) {
		this(filePath, expectedContent, MatchMode.EXACT);
	}

	@Override
	public Judgment judge(JudgmentContext context) {
		Path targetFile = context.workspace().resolve(filePath);

		// Check file exists
		if (!Files.exists(targetFile)) {
			return Judgment.builder()
				.score(new BooleanScore(false))
				.pass(false)
				.reasoning(String.format("File not found at %s", filePath))
				.check(Check.fail("file_exists", "File not found: " + filePath))
				.build();
		}

		// Read content
		String actualContent;
		try {
			actualContent = Files.readString(targetFile);
		}
		catch (Exception e) {
			return Judgment.builder()
				.score(new BooleanScore(false))
				.pass(false)
				.reasoning(String.format("Failed to read file: %s", e.getMessage()))
				.check(Check.pass("file_exists", "File exists"))
				.check(Check.fail("file_readable", "Failed to read: " + e.getMessage()))
				.build();
		}

		// Match content based on mode
		boolean matches = switch (matchMode) {
			case EXACT -> actualContent.equals(expectedContent);
			case CONTAINS -> actualContent.contains(expectedContent);
			case REGEX -> Pattern.compile(expectedContent).matcher(actualContent).find();
		};

		return Judgment.builder()
			.score(new BooleanScore(matches))
			.pass(matches)
			.reasoning(matches ? String.format("Content %s matches in %s", matchMode.name().toLowerCase(), filePath)
					: String.format("Content does not %s match in %s", matchMode.name().toLowerCase(), filePath))
			.check(Check.pass("file_exists", "File found"))
			.check(Check.pass("file_readable", "File readable"))
			.check(matches ? Check.pass("content_match", String.format("%s match successful", matchMode))
					: Check.fail("content_match", String.format("%s match failed", matchMode)))
			.build();
	}

	/**
	 * Content matching mode.
	 */
	public enum MatchMode {

		/**
		 * Content must match exactly.
		 */
		EXACT,

		/**
		 * Content must contain the expected string.
		 */
		CONTAINS,

		/**
		 * Content must match the regex pattern.
		 */
		REGEX

	}

}
