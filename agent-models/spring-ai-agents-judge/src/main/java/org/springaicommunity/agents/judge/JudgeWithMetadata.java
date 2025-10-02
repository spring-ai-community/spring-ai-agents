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

/**
 * Marker interface for judges that provide metadata.
 *
 * <p>
 * This interface follows Spring Framework's pattern of marker interfaces (like
 * {@code Ordered}, {@code SmartInitializingSingleton}) to enable semantic pattern
 * matching for judges with metadata. This allows infrastructure code, monitoring tools,
 * documentation generators, and dashboards to discover and access judge metadata in a
 * type-safe manner.
 * </p>
 *
 * <p>
 * <strong>Design Pattern:</strong> Marker interface for optional capability. Judges are
 * not required to have metadata - lambda judges work perfectly without it. However, when
 * metadata is needed (for logging, monitoring, UI display), implementing this interface
 * provides a standard way to expose it.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * // Infrastructure code can pattern match
 * if (judge instanceof JudgeWithMetadata jwm) {
 *     log.info("Running judge: {}", jwm.metadata().name());
 *     metrics.recordJudgeExecution(jwm.metadata().name());
 * }
 *
 * // NamedJudge implements this interface
 * Judge lambda = ctx -> Judgment.pass("OK");
 * JudgeWithMetadata named = Judges.named(lambda, "MyJudge");
 * String name = named.metadata().name();
 *
 * // DeterministicJudge subclasses can also implement it
 * public class FileExistsJudge extends DeterministicJudge implements JudgeWithMetadata {
 *     // Already has metadata() method from DeterministicJudge
 * }
 * }</pre>
 *
 * @author Mark Pollack
 * @since 0.1.0
 * @see Judge
 * @see NamedJudge
 * @see JudgeMetadata
 */
public interface JudgeWithMetadata extends Judge {

	/**
	 * Get metadata about this judge.
	 * @return the judge metadata
	 */
	JudgeMetadata metadata();

}
