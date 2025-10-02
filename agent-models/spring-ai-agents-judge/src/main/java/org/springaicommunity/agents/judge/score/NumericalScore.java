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

package org.springaicommunity.agents.judge.score;

/**
 * Numerical score with min/max bounds and normalization support.
 *
 * <p>
 * <strong>Design Inspiration:</strong> Normalization capability influenced by deepeval's
 * BaseMetric pattern which uses threshold-based scoring with 0-1 normalization for
 * consistent comparison across different scale metrics.
 * </p>
 *
 * @param value the score value
 * @param min the minimum possible value
 * @param max the maximum possible value
 * @author Mark Pollack
 * @since 0.1.0
 */
public record NumericalScore(double value, double min, double max) implements Score {

	public NumericalScore {
		if (value < min || value > max) {
			throw new IllegalArgumentException(String.format("Score %f must be between %f and %f", value, min, max));
		}
	}

	/**
	 * Get the normalized score in 0-1 range.
	 * @return normalized score between 0 and 1
	 */
	public double normalized() {
		if (max == min) {
			return 0.0;
		}
		return (value - min) / (max - min);
	}

	@Override
	public ScoreType type() {
		return ScoreType.NUMERICAL;
	}

	/**
	 * Create a 0-1 normalized score.
	 * @param value the score between 0 and 1
	 * @return a NumericalScore instance
	 */
	public static NumericalScore normalized(double value) {
		return new NumericalScore(value, 0.0, 1.0);
	}

	/**
	 * Create a 0-10 scale score.
	 * @param value the score between 0 and 10
	 * @return a NumericalScore instance
	 */
	public static NumericalScore outOfTen(double value) {
		return new NumericalScore(value, 0.0, 10.0);
	}

}
