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

package org.springaicommunity.agents.judge.result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springaicommunity.agents.judge.score.Score;

/**
 * Result of a judgment containing score, pass/fail, reasoning, and checks.
 *
 * @param score the score (boolean, numerical, or categorical)
 * @param pass whether the judgment passed (true) or failed (false)
 * @param reasoning human-readable explanation of the judgment
 * @param checks individual check results (optional)
 * @param metadata additional judgment information (extensibility)
 * @author Mark Pollack
 * @since 0.1.0
 */
public record Judgment(Score score, boolean pass, String reasoning, List<Check> checks, Map<String, Object> metadata) {

	public Judgment {
		checks = List.copyOf(checks);
		metadata = Map.copyOf(metadata);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private Score score;

		private boolean pass;

		private String reasoning = "";

		private List<Check> checks = new ArrayList<>();

		private Map<String, Object> metadata = new HashMap<>();

		public Builder score(Score score) {
			this.score = score;
			return this;
		}

		public Builder pass(boolean pass) {
			this.pass = pass;
			return this;
		}

		public Builder reasoning(String reasoning) {
			this.reasoning = reasoning;
			return this;
		}

		public Builder checks(List<Check> checks) {
			this.checks = new ArrayList<>(checks);
			return this;
		}

		public Builder check(Check check) {
			this.checks.add(check);
			return this;
		}

		public Builder metadata(Map<String, Object> metadata) {
			this.metadata = new HashMap<>(metadata);
			return this;
		}

		public Builder metadata(String key, Object value) {
			this.metadata.put(key, value);
			return this;
		}

		public Judgment build() {
			return new Judgment(score, pass, reasoning, checks, metadata);
		}

	}

}
