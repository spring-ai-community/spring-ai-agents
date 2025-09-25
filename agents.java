///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21
//DEPS org.springaicommunity.agents:spring-ai-agents-core:0.1.0-SNAPSHOT
//DEPS org.springaicommunity.agents:hello-world-agent:0.1.0-SNAPSHOT
//DEPS org.springaicommunity.agents:code-coverage-agent:0.1.0-SNAPSHOT

import org.springaicommunity.agents.core.*;

public class agents {
    public static void main(String[] argv) throws Exception {
        LauncherSpec spec = LocalConfigLoader.load(argv);
        Result r = AgentRunner.execute(spec);
        if (!r.success()) System.exit(1);
        System.out.println(r.message());
    }
}