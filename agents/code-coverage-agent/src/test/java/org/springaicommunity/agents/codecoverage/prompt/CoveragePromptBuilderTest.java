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

package org.springaicommunity.agents.codecoverage.prompt;

import org.junit.jupiter.api.Test;
import org.springaicommunity.agents.judge.coverage.JaCoCoReportParser.CoverageMetrics;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CoveragePromptBuilder}.
 *
 * <p>
 * These tests verify the modular and extensible design of prompt templating, allowing
 * customers to customize code coverage rules and testing conventions.
 * </p>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
class CoveragePromptBuilderTest {

	@Test
	void shouldBuildPromptWithBasicConfiguration() {
		CoverageMetrics baseline = new CoverageMetrics(45.5, 30.2, 0.0, 100, 200, 30, 100, 0, 0, "Baseline coverage");

		String prompt = CoveragePromptBuilder.create(baseline, false, 80).build();

		assertThat(prompt).contains("45.5% line");
		assertThat(prompt).contains("30.2% branch");
		assertThat(prompt).contains("not configured");
		assertThat(prompt).contains("80% line coverage");
		assertThat(prompt).contains("JaCoCo Maven plugin");
	}

	@Test
	void shouldIncludeJacocoPluginConfigWhenNotConfigured() {
		CoverageMetrics baseline = new CoverageMetrics(50.0, 40.0, 0.0, 100, 200, 40, 100, 0, 0, "Test");

		String prompt = CoveragePromptBuilder.create(baseline, false, 70).build();

		assertThat(prompt).contains("<plugin>");
		assertThat(prompt).contains("<groupId>org.jacoco</groupId>");
		assertThat(prompt).contains("<artifactId>jacoco-maven-plugin</artifactId>");
		assertThat(prompt).contains("<version>0.8.11</version>");
	}

	@Test
	void shouldExcludeJacocoPluginConfigWhenAlreadyConfigured() {
		CoverageMetrics baseline = new CoverageMetrics(60.0, 45.0, 0.0, 100, 200, 45, 100, 0, 0, "Test");

		String prompt = CoveragePromptBuilder.create(baseline, true, 80).build();

		assertThat(prompt).contains("✓ JaCoCo already configured");
		assertThat(prompt).doesNotContain("<plugin>");
		assertThat(prompt).doesNotContain("<groupId>org.jacoco</groupId>");
	}

	@Test
	void shouldSupportBuilderPattern() {
		CoverageMetrics baseline = new CoverageMetrics(35.0, 25.0, 0.0, 100, 200, 25, 100, 0, 0, "Test");

		String prompt = new CoveragePromptBuilder().withBaseline(baseline)
			.withJacocoStatus(false)
			.withTargetCoverage(75)
			.build();

		assertThat(prompt).contains("35.0% line");
		assertThat(prompt).contains("25.0% branch");
		assertThat(prompt).contains("75% line coverage");
	}

	@Test
	void shouldAllowCustomVariables() {
		CoverageMetrics baseline = new CoverageMetrics(40.0, 30.0, 0.0, 100, 200, 30, 100, 0, 0, "Test");

		String prompt = new CoveragePromptBuilder().withBaseline(baseline)
			.withJacocoStatus(true)
			.withTargetCoverage(80)
			.withVariable("custom_rule", "Use custom assertions")
			.build();

		// Verify standard variables work
		assertThat(prompt).contains("40.0% line");
		assertThat(prompt).contains("80% line coverage");

		// Custom variables would need template updates to be used
		// This test demonstrates the extensibility point for customers
	}

	@Test
	void shouldFormatCoveragePercentagesCorrectly() {
		CoverageMetrics baseline = new CoverageMetrics(71.42857, 45.6789, 0.0, 100, 200, 45, 100, 0, 0, "Test");

		String prompt = CoveragePromptBuilder.create(baseline, true, 90).build();

		// Should format to 1 decimal place
		assertThat(prompt).contains("71.4% line");
		assertThat(prompt).contains("45.7% branch");
	}

	@Test
	void shouldIncludeSpringTestingBestPractices() {
		CoverageMetrics baseline = new CoverageMetrics(50.0, 40.0, 0.0, 100, 200, 40, 100, 0, 0, "Test");

		String prompt = CoveragePromptBuilder.create(baseline, true, 80).build();

		// Verify Spring OSS testing conventions are in the prompt
		assertThat(prompt).contains("AssertJ");
		assertThat(prompt).contains("assertThat");
		assertThat(prompt).contains("BDD-style");
		assertThat(prompt).contains("SPRING OSS TESTING BEST PRACTICES");
	}

	@Test
	void shouldIncludeConstraintsAndGuidelines() {
		CoverageMetrics baseline = new CoverageMetrics(30.0, 20.0, 0.0, 100, 200, 20, 100, 0, 0, "Test");

		String prompt = CoveragePromptBuilder.create(baseline, false, 60).build();

		// Verify important constraints are present
		assertThat(prompt).contains("DO NOT modify production code");
		assertThat(prompt).contains("Tests MUST pass");
		assertThat(prompt).contains("CONSTRAINTS:");
		assertThat(prompt).contains("OUTPUT SUMMARY");
	}

}
