package org.springaicommunity.agents.samples.prreview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application for PR Review Demo.
 * 
 * <p>This demo showcases how Spring AI Agents can automate pull request reviews 
 * with AI-powered analysis using real data from Spring AI PR #3794.</p>
 * 
 * <p>To run the demo:</p>
 * <ol>
 *   <li>Set your API key: {@code export ANTHROPIC_API_KEY="your-key"}</li>
 *   <li>Run the application: {@code mvn spring-boot:run}</li>
 *   <li>View the generated report in the console and demo-output directory</li>
 * </ol>
 */
@SpringBootApplication
public class PrReviewDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(PrReviewDemoApplication.class, args);
	}

}