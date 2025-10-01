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

package org.springaicommunity.agents.claude.sdk.resilience;

import java.time.Duration;
import java.util.function.Predicate;

/**
 * Configuration for retry strategies in Claude SDK operations. Provides exponential
 * backoff and configurable retry conditions.
 */
public record RetryConfiguration(int maxAttempts, Duration initialDelay, double backoffMultiplier, Duration maxDelay,
		Predicate<Throwable> retryablePredicate) {

	public RetryConfiguration {
		if (maxAttempts < 1) {
			throw new IllegalArgumentException("maxAttempts must be at least 1");
		}
		if (initialDelay.isNegative() || initialDelay.isZero()) {
			throw new IllegalArgumentException("initialDelay must be positive");
		}
		if (backoffMultiplier <= 1.0) {
			throw new IllegalArgumentException("backoffMultiplier must be greater than 1.0");
		}
		if (maxDelay.compareTo(initialDelay) < 0) {
			throw new IllegalArgumentException("maxDelay must be >= initialDelay");
		}
		if (retryablePredicate == null) {
			retryablePredicate = throwable -> true;
		}
	}

	/**
	 * Default retry configuration for network operations.
	 */
	public static RetryConfiguration defaultNetwork() {
		return new RetryConfiguration(3, // 3 attempts
				Duration.ofSeconds(1), // 1 second initial delay
				2.0, // Double delay each time
				Duration.ofSeconds(10), // Max 10 second delay
				throwable -> isRetryableException(throwable));
	}

	/**
	 * Aggressive retry configuration for critical operations.
	 */
	public static RetryConfiguration aggressive() {
		return new RetryConfiguration(5, // 5 attempts
				Duration.ofMillis(500), // 500ms initial delay
				1.5, // 1.5x backoff
				Duration.ofSeconds(5), // Max 5 second delay
				throwable -> isRetryableException(throwable));
	}

	/**
	 * Conservative retry configuration for expensive operations.
	 */
	public static RetryConfiguration conservative() {
		return new RetryConfiguration(2, // Only 2 attempts
				Duration.ofSeconds(2), // 2 second initial delay
				3.0, // Triple delay
				Duration.ofSeconds(30), // Max 30 second delay
				throwable -> isNetworkException(throwable));
	}

	/**
	 * No retry configuration - fail fast.
	 */
	public static RetryConfiguration noRetry() {
		return new RetryConfiguration(1, // Single attempt only
				Duration.ofMillis(1), // Minimal delay
				1.0, // No backoff
				Duration.ofMillis(1), // No max delay
				throwable -> false // Never retry
		);
	}

	/**
	 * Calculates the delay for a given attempt number.
	 */
	public Duration calculateDelay(int attemptNumber) {
		if (attemptNumber <= 1) {
			return initialDelay;
		}

		double delay = initialDelay.toMillis() * Math.pow(backoffMultiplier, attemptNumber - 1);
		long delayMs = Math.min((long) delay, maxDelay.toMillis());
		return Duration.ofMillis(delayMs);
	}

	/**
	 * Checks if an exception should trigger a retry.
	 */
	public boolean shouldRetry(Throwable throwable, int attemptNumber) {
		return attemptNumber < maxAttempts && retryablePredicate.test(throwable);
	}

	private static boolean isRetryableException(Throwable throwable) {
		return isNetworkException(throwable) || isTemporaryException(throwable) || isProcessException(throwable);
	}

	private static boolean isNetworkException(Throwable throwable) {
		String className = throwable.getClass().getSimpleName().toLowerCase();
		String message = throwable.getMessage() != null ? throwable.getMessage().toLowerCase() : "";

		return className.contains("timeout") || className.contains("connection") || className.contains("io")
				|| message.contains("connection refused") || message.contains("timeout") || message.contains("network");
	}

	private static boolean isTemporaryException(Throwable throwable) {
		String message = throwable.getMessage() != null ? throwable.getMessage().toLowerCase() : "";

		return message.contains("busy") || message.contains("overloaded") || message.contains("rate limit")
				|| message.contains("service unavailable");
	}

	private static boolean isProcessException(Throwable throwable) {
		String className = throwable.getClass().getSimpleName();

		return className.contains("ProcessExecution") || className.contains("CLIConnection");
	}
}