package br.com.willianantunes.support;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.palantir.docker.compose.DockerComposeRule;

/**
 * @author Michael J. Simons, 2017-11-06
 * @see <a href="https://github.com/michael-simons/simple-meetup/blob/93f18f4dae0103e91ce477511a127f23dfd2a586/src/integrationTest/java/ac/simons/simplemeetup/support/PropagateDockerRule.java">PropagateDockerRule original class</a>
 */
public final class PropagateDockerRule implements TestRule {

    private final DockerComposeRule docker;

    public PropagateDockerRule(final DockerComposeRule docker) {
        this.docker = docker;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                PortMappingInitializer.DOCKER.set(docker);
                base.evaluate();
            }
        };
    }
}