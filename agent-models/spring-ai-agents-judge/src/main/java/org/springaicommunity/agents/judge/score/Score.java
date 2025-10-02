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
 * Sealed interface for type-safe scoring in judgments.
 *
 * <p>
 * Scores can be boolean (pass/fail), numerical (0-10, 0-1, etc.), or categorical
 * (good/fair/poor). Using a sealed interface provides compile-time type safety and
 * exhaustive pattern matching.
 * </p>
 *
 * <p>
 * <strong>Design Inspiration:</strong> Influenced by the "judges" framework's flexible
 * scoring types (bool | int | str) but adapted for Java's type system using sealed
 * interfaces. This provides the flexibility of union types with compile-time safety. The
 * BooleanScore, NumericalScore, and CategoricalScore variants map to the score_type
 * patterns from multiple evaluation frameworks (judges, deepeval, ragas).
 * </p>
 *
 * @author Mark Pollack
 * @since 0.1.0
 */
public sealed interface Score permits BooleanScore, NumericalScore, CategoricalScore {

	/**
	 * Get the score type.
	 * @return the score type enum
	 */
	ScoreType type();

}
