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
package org.springaicommunity.agents.model.sandbox;

/**
 * Hook for last-mile mutation of an ExecSpec just before execution.
 *
 * <p>
 * Implementations MUST return a NEW immutable ExecSpec. This allows for dynamic
 * customization of execution parameters without breaking the immutability contract of
 * ExecSpec.
 *
 * <p>
 * Common use cases include:
 *
 * <ul>
 * <li>Adding environment variables for authentication
 * <li>Modifying timeout based on command type
 * <li>Adding debugging flags in development environments
 * </ul>
 */
@FunctionalInterface
public interface ExecSpecCustomizer {

	/**
	 * Customize the given ExecSpec.
	 *
	 * <p>
	 * <strong>IMPORTANT:</strong> This method MUST return a new ExecSpec instance. The
	 * original spec should never be modified directly.
	 * @param original the original ExecSpec to customize
	 * @return a new, potentially modified ExecSpec
	 * @throws IllegalArgumentException if the original spec is invalid
	 */
	ExecSpec customize(ExecSpec original);

	/**
	 * Returns a customizer that applies multiple customizers in sequence.
	 * @param customizers the customizers to chain
	 * @return a composite customizer
	 * @throws IllegalArgumentException if any customizer is null
	 */
	static ExecSpecCustomizer chain(ExecSpecCustomizer... customizers) {
		// Validate that no customizers are null
		for (int i = 0; i < customizers.length; i++) {
			if (customizers[i] == null) {
				throw new IllegalArgumentException("Customizer at index " + i + " cannot be null");
			}
		}

		return (spec) -> {
			ExecSpec result = spec;
			for (ExecSpecCustomizer customizer : customizers) {
				result = customizer.customize(result);
			}
			return result;
		};
	}

	/**
	 * Returns a no-op customizer that returns the spec unchanged.
	 * @return an identity customizer
	 */
	static ExecSpecCustomizer identity() {
		return spec -> spec;
	}

	/**
	 * Returns a customizer that only applies if the predicate matches.
	 * @param predicate condition to check
	 * @param customizer customizer to apply if predicate is true
	 * @return conditional customizer
	 * @throws IllegalArgumentException if predicate or customizer is null
	 */
	static ExecSpecCustomizer when(java.util.function.Predicate<ExecSpec> predicate, ExecSpecCustomizer customizer) {
		if (predicate == null) {
			throw new IllegalArgumentException("Predicate cannot be null");
		}
		if (customizer == null) {
			throw new IllegalArgumentException("Customizer cannot be null");
		}
		return spec -> predicate.test(spec) ? customizer.customize(spec) : spec;
	}

}