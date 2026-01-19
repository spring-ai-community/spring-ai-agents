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

import org.springaicommunity.judge.coverage.JaCoCoReportParser.CoverageMetrics;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ClassPathResource;

import java.util.HashMap;
import java.util.Map;

/**
 * Builder for creating coverage improvement prompts using Spring AI's PromptTemplate
 * infrastructure.
 *
 * <p>
 * This design allows customers to extend and customize prompts for their own code
 * coverage rules and testing conventions.
 * </p>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public class CoveragePromptBuilder {

	private static final String MAIN_PROMPT_RESOURCE = "/META-INF/prompts/coverage-agent-prompt.txt";

	private static final String JACOCO_PLUGIN_RESOURCE = "/META-INF/prompts/jacoco-plugin.xml";

	private final PromptTemplate mainPromptTemplate;

	private final PromptTemplate jacocoPluginTemplate;

	private final Map<String, Object> variables;

	public CoveragePromptBuilder() {
		this.mainPromptTemplate = new PromptTemplate(new ClassPathResource(MAIN_PROMPT_RESOURCE));
		this.jacocoPluginTemplate = new PromptTemplate(new ClassPathResource(JACOCO_PLUGIN_RESOURCE));
		this.variables = new HashMap<>();
	}

	/**
	 * Set the baseline coverage metrics.
	 * @param baseline coverage metrics from initial run
	 * @return this builder
	 */
	public CoveragePromptBuilder withBaseline(CoverageMetrics baseline) {
		variables.put("baseline_line_coverage", String.format("%.1f", baseline.lineCoverage()));
		variables.put("baseline_branch_coverage", String.format("%.1f", baseline.branchCoverage()));
		return this;
	}

	/**
	 * Set the target coverage percentage.
	 * @param targetCoverage target line coverage percentage
	 * @return this builder
	 */
	public CoveragePromptBuilder withTargetCoverage(int targetCoverage) {
		variables.put("target_coverage", String.valueOf(targetCoverage));
		return this;
	}

	/**
	 * Set whether JaCoCo is already configured in the project.
	 * @param hasJaCoCo true if JaCoCo plugin is already present
	 * @return this builder
	 */
	public CoveragePromptBuilder withJacocoStatus(boolean hasJaCoCo) {
		String jacocoStatus = hasJaCoCo ? "configured" : "not configured";
		String jacocoStep = hasJaCoCo ? "✓ JaCoCo already configured"
				: "1. Add JaCoCo Maven plugin to pom.xml (configuration below)";

		variables.put("jacoco_status", jacocoStatus);
		variables.put("jacoco_step", jacocoStep);

		// Only include plugin config if not already configured
		if (!hasJaCoCo) {
			variables.put("jacoco_plugin_config", jacocoPluginTemplate.render());
		}
		else {
			variables.put("jacoco_plugin_config", "");
		}

		return this;
	}

	/**
	 * Add custom variable to the prompt template.
	 * @param name variable name
	 * @param value variable value
	 * @return this builder
	 */
	public CoveragePromptBuilder withVariable(String name, Object value) {
		variables.put(name, value);
		return this;
	}

	/**
	 * Build the final prompt string.
	 * @return rendered prompt
	 */
	public String build() {
		return mainPromptTemplate.render(variables);
	}

	/**
	 * Create a builder with common configuration.
	 * @param baseline coverage metrics from initial run
	 * @param hasJaCoCo whether JaCoCo is already configured
	 * @param targetCoverage target line coverage percentage
	 * @return configured builder
	 */
	public static CoveragePromptBuilder create(CoverageMetrics baseline, boolean hasJaCoCo, int targetCoverage) {
		return new CoveragePromptBuilder().withBaseline(baseline)
			.withJacocoStatus(hasJaCoCo)
			.withTargetCoverage(targetCoverage);
	}

}
