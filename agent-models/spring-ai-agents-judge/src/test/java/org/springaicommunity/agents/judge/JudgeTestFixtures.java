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

package org.springaicommunity.agents.judge;

import org.springaicommunity.agents.judge.context.AgentExecutionStatus;
import org.springaicommunity.agents.judge.context.JudgmentContext;
import org.springaicommunity.agents.judge.jury.Verdict;
import org.springaicommunity.agents.judge.result.Judgment;
import org.springaicommunity.agents.judge.result.JudgmentStatus;
import org.springaicommunity.agents.judge.score.BooleanScore;
import org.springaicommunity.agents.judge.score.NumericalScore;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test fixtures and utilities for Judge framework testing.
 *
 * <p>
 * Provides reusable mocks, sample data, and convenience methods for creating test
 * scenarios across all judge types and jury configurations.
 * </p>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public final class JudgeTestFixtures {

	private JudgeTestFixtures() {
		// Utility class
	}

	// ==================== Mock Judges ====================

	/**
	 * Create a judge that always passes.
	 * @param name judge name
	 * @return always-pass judge
	 */
	public static Judge alwaysPass(String name) {
		return Judges.named(ctx -> Judgment.pass("Always passes"), name, null, JudgeType.DETERMINISTIC);
	}

	/**
	 * Create a judge that always fails.
	 * @param name judge name
	 * @return always-fail judge
	 */
	public static Judge alwaysFail(String name) {
		return Judges.named(ctx -> Judgment.fail("Always fails"), name, null, JudgeType.DETERMINISTIC);
	}

	/**
	 * Create a judge that always abstains.
	 * @param name judge name
	 * @return always-abstain judge
	 */
	public static Judge alwaysAbstain(String name) {
		return Judges.named(ctx -> Judgment.abstain("Cannot evaluate"), name, null, JudgeType.DETERMINISTIC);
	}

	/**
	 * Create a judge that always errors.
	 * @param name judge name
	 * @return always-error judge
	 */
	public static Judge alwaysError(String name) {
		return Judges.named(ctx -> Judgment.error("Evaluation error", new RuntimeException("Test error")), name, null,
				JudgeType.DETERMINISTIC);
	}

	/**
	 * Create a judge that returns a specific score.
	 * @param name judge name
	 * @param score numerical score value
	 * @return score-based judge
	 */
	public static Judge withScore(String name, double score) {
		JudgmentStatus status = score >= 0.5 ? JudgmentStatus.PASS : JudgmentStatus.FAIL;
		return Judges.named(ctx -> Judgment.builder()
			.score(new NumericalScore(score, 0.0, 1.0))
			.status(status)
			.reasoning("Score: " + score)
			.build(), name, null, JudgeType.DETERMINISTIC);
	}

	/**
	 * Create a recording judge that tracks all invocations.
	 * @param name judge name
	 * @param result judgment to return
	 * @return recording judge
	 */
	public static RecordingJudge recording(String name, Judgment result) {
		return new RecordingJudge(name, result);
	}

	/**
	 * Create a slow judge for timeout testing.
	 * @param name judge name
	 * @param delayMillis delay in milliseconds
	 * @param result judgment to return
	 * @return slow judge
	 */
	public static Judge slow(String name, long delayMillis, Judgment result) {
		return Judges.named(ctx -> {
			try {
				Thread.sleep(delayMillis);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return Judgment.error("Interrupted", e);
			}
			return result;
		}, name, null, JudgeType.DETERMINISTIC);
	}

	// ==================== Sample Contexts ====================

	/**
	 * Create a simple judgment context.
	 * @param goal the goal text
	 * @return judgment context
	 */
	public static JudgmentContext simpleContext(String goal) {
		return JudgmentContext.builder()
			.goal(goal)
			.agentOutput("Sample output")
			.status(AgentExecutionStatus.SUCCESS)
			.startedAt(Instant.now())
			.executionTime(Duration.ofSeconds(1))
			.build();
	}

	/**
	 * Create a context with workspace.
	 * @param goal the goal text
	 * @param workspace workspace path
	 * @return judgment context
	 */
	public static JudgmentContext withWorkspace(String goal, Path workspace) {
		return JudgmentContext.builder()
			.goal(goal)
			.workspace(workspace)
			.agentOutput("Sample output")
			.status(AgentExecutionStatus.SUCCESS)
			.startedAt(Instant.now())
			.executionTime(Duration.ofSeconds(1))
			.build();
	}

	/**
	 * Create a failed execution context.
	 * @param goal the goal text
	 * @return judgment context with failed status
	 */
	public static JudgmentContext failedContext(String goal) {
		return JudgmentContext.builder()
			.goal(goal)
			.agentOutput("Error occurred")
			.status(AgentExecutionStatus.FAILED)
			.startedAt(Instant.now())
			.executionTime(Duration.ofMillis(100))
			.build();
	}

	// ==================== Sample Judgments ====================

	/**
	 * Create a passing judgment with score.
	 * @param score numerical score
	 * @return passing judgment
	 */
	public static Judgment passJudgment(double score) {
		return Judgment.builder()
			.score(new NumericalScore(score, 0.0, 1.0))
			.status(JudgmentStatus.PASS)
			.reasoning("Test passed with score " + score)
			.build();
	}

	/**
	 * Create a failing judgment with score.
	 * @param score numerical score
	 * @return failing judgment
	 */
	public static Judgment failJudgment(double score) {
		return Judgment.builder()
			.score(new NumericalScore(score, 0.0, 1.0))
			.status(JudgmentStatus.FAIL)
			.reasoning("Test failed with score " + score)
			.build();
	}

	/**
	 * Create a boolean passing judgment.
	 * @param reasoning the reasoning text
	 * @return passing judgment
	 */
	public static Judgment booleanPass(String reasoning) {
		return Judgment.builder()
			.score(new BooleanScore(true))
			.status(JudgmentStatus.PASS)
			.reasoning(reasoning)
			.build();
	}

	/**
	 * Create a boolean failing judgment.
	 * @param reasoning the reasoning text
	 * @return failing judgment
	 */
	public static Judgment booleanFail(String reasoning) {
		return Judgment.builder()
			.score(new BooleanScore(false))
			.status(JudgmentStatus.FAIL)
			.reasoning(reasoning)
			.build();
	}

	// ==================== Sample Verdicts ====================

	/**
	 * Create a unanimous pass verdict.
	 * @param judgeCount number of judges
	 * @return verdict with all passing judgments
	 */
	public static Verdict unanimousPass(int judgeCount) {
		List<Judgment> individual = new ArrayList<>();
		Map<String, Judgment> byName = new HashMap<>();

		for (int i = 0; i < judgeCount; i++) {
			Judgment j = booleanPass("Judge " + (i + 1) + " passed");
			individual.add(j);
			byName.put("Judge#" + (i + 1), j);
		}

		return Verdict.builder()
			.aggregated(booleanPass("Unanimous pass"))
			.individual(individual)
			.individualByName(byName)
			.build();
	}

	/**
	 * Create a split verdict.
	 * @param passCount number of passing judges
	 * @param failCount number of failing judges
	 * @return verdict with mixed judgments
	 */
	public static Verdict split(int passCount, int failCount) {
		List<Judgment> individual = new ArrayList<>();
		Map<String, Judgment> byName = new HashMap<>();

		for (int i = 0; i < passCount; i++) {
			Judgment j = booleanPass("Pass " + (i + 1));
			individual.add(j);
			byName.put("PassJudge#" + (i + 1), j);
		}

		for (int i = 0; i < failCount; i++) {
			Judgment j = booleanFail("Fail " + (i + 1));
			individual.add(j);
			byName.put("FailJudge#" + (i + 1), j);
		}

		boolean majorityPass = passCount > failCount;
		Judgment aggregated = majorityPass ? booleanPass("Majority passed") : booleanFail("Majority failed");

		return Verdict.builder().aggregated(aggregated).individual(individual).individualByName(byName).build();
	}

	/**
	 * Create a verdict with all abstentions.
	 * @param judgeCount number of judges
	 * @return verdict with all abstaining judgments
	 */
	public static Verdict allAbstain(int judgeCount) {
		List<Judgment> individual = new ArrayList<>();
		Map<String, Judgment> byName = new HashMap<>();

		for (int i = 0; i < judgeCount; i++) {
			Judgment j = Judgment.abstain("Cannot evaluate");
			individual.add(j);
			byName.put("Judge#" + (i + 1), j);
		}

		return Verdict.builder()
			.aggregated(Judgment.abstain("All judges abstained"))
			.individual(individual)
			.individualByName(byName)
			.build();
	}

	// ==================== Recording Judge ====================

	/**
	 * Judge that records all invocations for verification.
	 */
	public static class RecordingJudge implements Judge, JudgeWithMetadata {

		private final JudgeMetadata metadata;

		private final Judgment result;

		private final List<JudgmentContext> invocations = new ArrayList<>();

		public RecordingJudge(String name, Judgment result) {
			this.metadata = new JudgeMetadata(name, "Recording judge", JudgeType.DETERMINISTIC);
			this.result = result;
		}

		@Override
		public Judgment judge(JudgmentContext context) {
			invocations.add(context);
			return result;
		}

		@Override
		public JudgeMetadata metadata() {
			return metadata;
		}

		public List<JudgmentContext> getInvocations() {
			return List.copyOf(invocations);
		}

		public int getInvocationCount() {
			return invocations.size();
		}

		public JudgmentContext getLastInvocation() {
			return invocations.isEmpty() ? null : invocations.get(invocations.size() - 1);
		}

	}

}
