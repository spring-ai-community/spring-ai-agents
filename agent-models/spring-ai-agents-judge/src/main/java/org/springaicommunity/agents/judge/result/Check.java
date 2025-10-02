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

/**
 * Individual check within a judgment.
 *
 * <p>
 * Checks represent sub-assertions within a judgment. For example, a test judge might have
 * checks for "tests compiled", "tests ran", "no failures", "no errors".
 * </p>
 *
 * @param name the check name
 * @param passed whether this check passed
 * @param message optional message (error message if failed, confirmation if passed)
 * @author Mark Pollack
 * @since 0.1.0
 */
public record Check(String name, boolean passed, String message) {

	public static Check pass(String name) {
		return new Check(name, true, "");
	}

	public static Check pass(String name, String message) {
		return new Check(name, true, message);
	}

	public static Check fail(String name, String message) {
		return new Check(name, false, message);
	}

}
