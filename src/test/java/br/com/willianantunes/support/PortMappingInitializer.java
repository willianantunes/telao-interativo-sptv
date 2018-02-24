package br.com.willianantunes.support;

import java.io.IOException;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.support.TestPropertySourceUtils;

import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.connection.Container;

/**
 * @author Michael J. Simons, 2017-11-06
 * @see <a href="https://github.com/michael-simons/simple-meetup/blob/93f18f4dae0103e91ce477511a127f23dfd2a586/src/integrationTest/java/ac/simons/simplemeetup/support/PortMappingInitializer.java">PortMappingInitializer original class</a>
 */
public final class PortMappingInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	static final ThreadLocal<DockerComposeRule> DOCKER = ThreadLocal.withInitial(() -> null);

	@Override
	public void initialize(final ConfigurableApplicationContext applicationContext) {
		final DockerComposeRule docker = DOCKER.get();

		if (docker != null) {
			try {
				final ConfigurableEnvironment environment = applicationContext.getEnvironment();
				for (Container container : docker.containers().allContainers()) {
					container.ports().stream().forEach(p -> {
						TestPropertySourceUtils.addInlinedPropertiesToEnvironment(environment,
								container.getContainerName() + ".port = " + p.getExternalPort());
					});
				}
			} catch (InterruptedException | IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}