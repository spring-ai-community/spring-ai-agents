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

package org.springaicommunity.agents.sweagentsdk.transport;

import java.util.Objects;
import java.util.Optional;

/**
 * Result of CLI availability check, containing status, version information, and error
 * details.
 *
 * <p>
 * This class encapsulates the result of checking whether the SWE Agent CLI is available
 * and functional. It provides detailed information about success or failure reasons.
 * </p>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * </p>
 * <pre>
 * CliAvailabilityResult result = SweCliDiscovery.checkSweCliAvailability();
 * if (result.isAvailable()) {
 *     System.out.println("CLI version: " + result.getVersion().orElse("unknown"));
 * } else {
 *     System.err.println("CLI not available: " + result.getReason().orElse("unknown"));
 * }
 * </pre>
 */
public class CliAvailabilityResult {

	private final boolean available;

	private final String version;

	private final String reason;

	private final Exception exception;

	/**
	 * Private constructor. Use factory methods to create instances.
	 * @param available whether the CLI is available
	 * @param version the CLI version (if available)
	 * @param reason the reason for unavailability (if not available)
	 * @param exception the exception that caused unavailability (if any)
	 */
	private CliAvailabilityResult(boolean available, String version, String reason, Exception exception) {
		this.available = available;
		this.version = version;
		this.reason = reason;
		this.exception = exception;
	}

	/**
	 * Creates a result indicating the CLI is available.
	 * @param version the CLI version string
	 * @return a successful availability result
	 */
	public static CliAvailabilityResult available(String version) {
		return new CliAvailabilityResult(true, version, null, null);
	}

	/**
	 * Creates a result indicating the CLI is not available.
	 * @param reason the reason why the CLI is not available
	 * @return an unsuccessful availability result
	 */
	public static CliAvailabilityResult unavailable(String reason) {
		return new CliAvailabilityResult(false, null, reason, null);
	}

	/**
	 * Creates a result indicating the CLI is not available due to an exception.
	 * @param reason the reason why the CLI is not available
	 * @param exception the exception that caused the unavailability
	 * @return an unsuccessful availability result
	 */
	public static CliAvailabilityResult unavailable(String reason, Exception exception) {
		return new CliAvailabilityResult(false, null, reason, exception);
	}

	/**
	 * Checks if the CLI is available.
	 * @return true if the CLI is available and functional
	 */
	public boolean isAvailable() {
		return available;
	}

	/**
	 * Gets the CLI version if available.
	 * @return an Optional containing the version string, or empty if not available
	 */
	public Optional<String> getVersion() {
		return Optional.ofNullable(version);
	}

	/**
	 * Gets the reason for unavailability.
	 * @return an Optional containing the reason string, or empty if the CLI is available
	 */
	public Optional<String> getReason() {
		return Optional.ofNullable(reason);
	}

	/**
	 * Gets the exception that caused unavailability.
	 * @return an Optional containing the exception, or empty if no exception or CLI is
	 * available
	 */
	public Optional<Exception> getException() {
		return Optional.ofNullable(exception);
	}

	@Override
	public String toString() {
		if (available) {
			return String.format("CliAvailabilityResult{available=true, version='%s'}", version);
		}
		else {
			return String.format("CliAvailabilityResult{available=false, reason='%s'}", reason);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		CliAvailabilityResult that = (CliAvailabilityResult) o;
		return available == that.available && Objects.equals(version, that.version)
				&& Objects.equals(reason, that.reason);
	}

	@Override
	public int hashCode() {
		return Objects.hash(available, version, reason);
	}

}