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
 * Provider interface for sandbox instances using Spring's dependency injection pattern.
 *
 * <p>
 * This interface enables clean separation of concerns and testability through Spring's
 * standard dependency injection mechanisms. AgentModels can depend on this interface
 * rather than concrete sandbox implementations.
 *
 * <p>
 * Spring auto-configuration will provide default implementations, while tests can easily
 * inject mock implementations.
 */
public interface SandboxProvider {

	/**
	 * Gets the sandbox instance for command execution.
	 * @return the sandbox instance
	 */
	Sandbox getSandbox();

}