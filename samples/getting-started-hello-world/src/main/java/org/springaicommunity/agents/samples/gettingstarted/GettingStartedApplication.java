package org.springaicommunity.agents.samples.gettingstarted;

import java.nio.file.Path;

import org.springaicommunity.agents.advisors.judge.JudgeAdvisor;
import org.springaicommunity.agents.client.AgentClient;
import org.springaicommunity.agents.client.AgentClientResponse;
import org.springaicommunity.agents.judge.fs.FileExistsJudge;
import org.springaicommunity.agents.judge.result.Judgment;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GettingStartedApplication {

	public static void main(String[] args) {
		SpringApplication.run(GettingStartedApplication.class, args);
	}

	@Bean
	CommandLineRunner demo(AgentClient.Builder agentClientBuilder) {
		return args -> {
			AgentClientResponse response = agentClientBuilder.build()
				.goal("Create a file named hello.txt with content 'Hello World'")
				.workingDirectory(Path.of(System.getProperty("user.dir")))
				.advisors(JudgeAdvisor.builder().judge(new FileExistsJudge("hello.txt")).build())
				.run();

			Judgment judgment = response.getJudgment();
			System.out.println("Agent completed!");
			System.out.println("Result: " + response.getResult());
			System.out.println("Judge: " + (judgment != null && judgment.pass() ? "PASSED" : "FAILED"));
		};
	}

}
