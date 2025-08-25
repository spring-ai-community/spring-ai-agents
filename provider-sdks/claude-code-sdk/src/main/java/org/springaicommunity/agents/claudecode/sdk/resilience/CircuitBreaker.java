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

package org.springaicommunity.agents.claudecode.sdk.resilience;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Circuit breaker implementation for Claude SDK operations. Prevents cascade failures by
 * temporarily failing fast when error rate is high.
 */
public class CircuitBreaker {

	private static final Logger logger = LoggerFactory.getLogger(CircuitBreaker.class);

	public enum State {

		CLOSED, // Normal operation
		OPEN, // Failing fast
		HALF_OPEN // Testing if service recovered

	}

	private final String name;

	private final int failureThreshold;

	private final Duration recoveryTimeout;

	private final Duration slidingWindowDuration;

	private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);

	private final AtomicInteger failureCount = new AtomicInteger(0);

	private final AtomicInteger successCount = new AtomicInteger(0);

	private final AtomicLong lastFailureTime = new AtomicLong(0);

	private final AtomicLong lastSuccessTime = new AtomicLong(0);

	private final AtomicLong windowStartTime = new AtomicLong(System.currentTimeMillis());

	public CircuitBreaker(String name, int failureThreshold, Duration recoveryTimeout, Duration slidingWindowDuration) {
		this.name = name;
		this.failureThreshold = failureThreshold;
		this.recoveryTimeout = recoveryTimeout;
		this.slidingWindowDuration = slidingWindowDuration;
	}

	/**
	 * Creates a default circuit breaker for Claude CLI operations.
	 */
	public static CircuitBreaker defaultClaudeBreaker() {
		return new CircuitBreaker("claude-cli", 5, // 5 failures trigger open
				Duration.ofSeconds(30), // 30 second recovery timeout
				Duration.ofMinutes(2) // 2 minute sliding window
		);
	}

	/**
	 * Creates a sensitive circuit breaker for critical operations.
	 */
	public static CircuitBreaker sensitive(String name) {
		return new CircuitBreaker(name, 3, // 3 failures trigger open
				Duration.ofSeconds(60), // 1 minute recovery timeout
				Duration.ofMinutes(1) // 1 minute sliding window
		);
	}

	/**
	 * Creates a tolerant circuit breaker for background operations.
	 */
	public static CircuitBreaker tolerant(String name) {
		return new CircuitBreaker(name, 10, // 10 failures trigger open
				Duration.ofSeconds(15), // 15 second recovery timeout
				Duration.ofMinutes(5) // 5 minute sliding window
		);
	}

	/**
	 * Executes a supplier with circuit breaker protection.
	 */
	public <T> T execute(Supplier<T> operation) throws CircuitBreakerOpenException {
		State currentState = checkAndUpdateState();

		if (currentState == State.OPEN) {
			throw new CircuitBreakerOpenException("Circuit breaker '" + name + "' is OPEN - failing fast");
		}

		try {
			T result = operation.get();
			onSuccess();
			return result;
		}
		catch (Exception e) {
			onFailure();
			throw e;
		}
	}

	/**
	 * Gets the current state of the circuit breaker.
	 */
	public State getState() {
		return checkAndUpdateState();
	}

	/**
	 * Gets circuit breaker metrics.
	 */
	public CircuitBreakerMetrics getMetrics() {
		return new CircuitBreakerMetrics(name, state.get(), failureCount.get(), successCount.get(),
				calculateFailureRate(), Instant.ofEpochMilli(lastFailureTime.get()),
				Instant.ofEpochMilli(lastSuccessTime.get()));
	}

	/**
	 * Manually resets the circuit breaker to CLOSED state.
	 */
	public void reset() {
		logger.info("Circuit breaker '{}' manually reset to CLOSED", name);
		state.set(State.CLOSED);
		failureCount.set(0);
		successCount.set(0);
		windowStartTime.set(System.currentTimeMillis());
	}

	private State checkAndUpdateState() {
		State currentState = state.get();
		long now = System.currentTimeMillis();

		// Check if sliding window should reset
		if (now - windowStartTime.get() > slidingWindowDuration.toMillis()) {
			resetWindow(now);
		}

		switch (currentState) {
			case CLOSED:
				if (failureCount.get() >= failureThreshold) {
					if (state.compareAndSet(State.CLOSED, State.OPEN)) {
						lastFailureTime.set(now);
						logger.warn("Circuit breaker '{}' opened - failure threshold {} reached", name,
								failureThreshold);
					}
					return State.OPEN;
				}
				break;

			case OPEN:
				if (now - lastFailureTime.get() > recoveryTimeout.toMillis()) {
					if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
						logger.info("Circuit breaker '{}' moved to HALF_OPEN - testing recovery", name);
					}
					return State.HALF_OPEN;
				}
				break;

			case HALF_OPEN:
				// Stay in HALF_OPEN until next success or failure
				break;
		}

		return currentState;
	}

	private void onSuccess() {
		long now = System.currentTimeMillis();
		successCount.incrementAndGet();
		lastSuccessTime.set(now);

		State currentState = state.get();
		if (currentState == State.HALF_OPEN) {
			if (state.compareAndSet(State.HALF_OPEN, State.CLOSED)) {
				logger.info("Circuit breaker '{}' closed - recovery successful", name);
				failureCount.set(0); // Reset failure count on recovery
			}
		}
	}

	private void onFailure() {
		long now = System.currentTimeMillis();
		failureCount.incrementAndGet();
		lastFailureTime.set(now);

		State currentState = state.get();
		if (currentState == State.HALF_OPEN) {
			if (state.compareAndSet(State.HALF_OPEN, State.OPEN)) {
				logger.warn("Circuit breaker '{}' reopened - recovery failed", name);
			}
		}
	}

	private void resetWindow(long now) {
		windowStartTime.set(now);
		int oldFailures = failureCount.get();
		int oldSuccesses = successCount.get();

		// Exponential decay instead of hard reset
		failureCount.set(oldFailures / 2);
		successCount.set(oldSuccesses / 2);

		logger.debug("Circuit breaker '{}' sliding window reset - failures: {} -> {}, successes: {} -> {}", name,
				oldFailures, failureCount.get(), oldSuccesses, successCount.get());
	}

	private double calculateFailureRate() {
		int total = failureCount.get() + successCount.get();
		return total > 0 ? (double) failureCount.get() / total : 0.0;
	}

	/**
	 * Exception thrown when circuit breaker is in OPEN state.
	 */
	public static class CircuitBreakerOpenException extends RuntimeException {

		public CircuitBreakerOpenException(String message) {
			super(message);
		}

	}

	/**
	 * Metrics snapshot for circuit breaker monitoring.
	 */
	public record CircuitBreakerMetrics(String name, State state, int failureCount, int successCount,
			double failureRate, Instant lastFailureTime, Instant lastSuccessTime) {

		public boolean isHealthy() {
			return state == State.CLOSED && failureRate < 0.5;
		}

		public String getStatusDescription() {
			return String.format("CircuitBreaker[%s] state=%s, failures=%d, successes=%d, failureRate=%.2f%%", name,
					state, failureCount, successCount, failureRate * 100);
		}
	}

}