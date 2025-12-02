/*
 * Copyright 2025 Spring AI Community
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

package org.springaicommunity.agents.claude.sdk.session;

import org.springaicommunity.agents.claude.sdk.exceptions.ClaudeSDKException;
import org.springaicommunity.agents.claude.sdk.parsing.ParsedMessage;

import java.util.Iterator;
import java.util.Map;

/**
 * Interface for persistent Claude CLI sessions that support multi-turn conversations.
 *
 * <p>
 * A session maintains a persistent connection to the Claude CLI process, allowing
 * multiple queries within the same context. This enables multi-turn conversations where
 * Claude remembers previous messages in the session.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * try (ClaudeSession session = new DefaultClaudeSession(options)) {
 *     session.connect("Help me understand this problem");
 *
 *     for (ParsedMessage msg : session.receiveResponse()) {
 *         // Process initial response
 *     }
 *
 *     session.query("Now implement the solution");
 *
 *     for (ParsedMessage msg : session.receiveResponse()) {
 *         // Process follow-up response
 *     }
 * }
 * }</pre>
 *
 * <p>
 * This interface mirrors the Python SDK's {@code ClaudeSDKClient} functionality.
 * </p>
 *
 * @see DefaultClaudeSession
 */
public interface ClaudeSession extends AutoCloseable {

	/**
	 * Connects to the Claude CLI without an initial prompt. The session is ready for
	 * queries after this call.
	 * @throws ClaudeSDKException if connection fails
	 */
	void connect() throws ClaudeSDKException;

	/**
	 * Connects to the Claude CLI with an initial prompt.
	 * @param initialPrompt the first prompt to send
	 * @throws ClaudeSDKException if connection fails
	 */
	void connect(String initialPrompt) throws ClaudeSDKException;

	/**
	 * Sends a follow-up query in the existing session context. The query will be
	 * processed in the context of previous messages.
	 * @param prompt the prompt to send
	 * @throws ClaudeSDKException if sending fails
	 */
	void query(String prompt) throws ClaudeSDKException;

	/**
	 * Sends a follow-up query with a specific session ID.
	 * @param prompt the prompt to send
	 * @param sessionId the session ID to use
	 * @throws ClaudeSDKException if sending fails
	 */
	void query(String prompt, String sessionId) throws ClaudeSDKException;

	/**
	 * Returns an iterator over all messages from the CLI. This iterator yields messages
	 * indefinitely until the session ends.
	 * @return iterator over parsed messages
	 */
	Iterator<ParsedMessage> receiveMessages();

	/**
	 * Returns an iterator that yields messages until a ResultMessage is received. This is
	 * useful for processing a single response before sending another query.
	 * @return iterator over parsed messages, stops after ResultMessage
	 */
	Iterator<ParsedMessage> receiveResponse();

	/**
	 * Interrupts the current operation. Sends an interrupt signal to the CLI to stop the
	 * current processing.
	 * @throws ClaudeSDKException if interrupt fails
	 */
	void interrupt() throws ClaudeSDKException;

	/**
	 * Changes the permission mode mid-session.
	 * @param mode the new permission mode (e.g., "default", "acceptEdits", "plan")
	 * @throws ClaudeSDKException if setting mode fails
	 */
	void setPermissionMode(String mode) throws ClaudeSDKException;

	/**
	 * Changes the model mid-session.
	 * @param model the new model name (e.g., "claude-sonnet-4-20250514")
	 * @throws ClaudeSDKException if setting model fails
	 */
	void setModel(String model) throws ClaudeSDKException;

	/**
	 * Returns information about the server/CLI from initialization.
	 * @return map of server information, or empty map if not available
	 */
	Map<String, Object> getServerInfo();

	/**
	 * Checks if the session is currently connected.
	 * @return true if connected and ready for queries
	 */
	boolean isConnected();

	/**
	 * Disconnects the session and releases resources. This is an alias for
	 * {@link #close()} for API clarity.
	 */
	void disconnect();

	/**
	 * Closes the session and releases all resources. After calling this method, the
	 * session cannot be reused.
	 */
	@Override
	void close();

}
