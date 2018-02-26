package br.com.willianantunes.route;

import java.util.List;
import java.util.Optional;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.PropertyInject;
import org.apache.camel.Route;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.UseAdviceWith;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import com.google.common.collect.Iterables;
import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.connection.waiting.HealthChecks;

import br.com.willianantunes.support.PortMappingInitializer;
import br.com.willianantunes.support.PropagateDockerRule;
import br.com.willianantunes.support.ScenarioBuilder;

@RunWith(CamelSpringBootRunner.class)
@UseAdviceWith
@SpringBootTest
@ActiveProfiles("it")
@ContextConfiguration(initializers = PortMappingInitializer.class)
public class PrepareTweetsAndEvaluateThemRoutesIT {
    private static DockerComposeRule docker = DockerComposeRule.builder().file("src/test/resources/docker-compose.yml")
            .waitingForService("activemq", HealthChecks.toHaveAllPortsOpen()).build();

    @ClassRule
    public static TestRule exposePortMappings = RuleChain.outerRule(docker).around(new PropagateDockerRule(docker));

    @Autowired
    private ModelCamelContext camelContext;
    @Autowired
    private ProducerTemplate producerTemplate;
    @Autowired
    private ScenarioBuilder scenarioBuilder;

    @PropertyInject("custom.websocket-port")
    private String webSocketport;    
    @EndpointInject(uri = "mock:result-fist-route-id-queue")
    private MockEndpoint mockedResultFirstRouteIdQueue;
    @EndpointInject(uri = "mock:result-before-activemq-route-id-queue")
    private MockEndpoint mockedResultBeforeActiveMQFilterRouteIdQueue;

    @Before
    public void setUp() throws Exception {
        if (!camelContext.getStatus().isStarted()) {
            prepareCamelEnvironment();
        }
    }

    @Test
    public void shouldDetectRoutes() {
        Route routeMessageCounter = camelContext.getRoute(PrepareTweetsAndEvaluateThemRoutes.ROUTE_ID_MESSAGE_COUNTER);
        Route routeQueue = camelContext.getRoute(PrepareTweetsAndEvaluateThemRoutes.ROUTE_ID_QUEUE);

        Assertions.assertThat(routeMessageCounter).isNotNull();
        Assertions.assertThat(routeQueue).isNotNull();
    }

    @Test
    public void messagesAreForwardedToQueueRouteAndNotQueued() {
        scenarioBuilder.unbuild().prepareDummyTweets(15).build();
        producerTemplate.sendBody("direct:evaluate-database", null);

        Optional<Exchange> storageFromFirstNode = Optional
                .ofNullable(Iterables.getLast(mockedResultFirstRouteIdQueue.getReceivedExchanges(), null));
        Optional<Exchange> storageFromBeforeActiveMQ = Optional.ofNullable(
                Iterables.getLast(mockedResultBeforeActiveMQFilterRouteIdQueue.getReceivedExchanges(), null));

        Assertions.assertThat(storageFromBeforeActiveMQ.isPresent()).isFalse();
        Assertions.assertThat(storageFromFirstNode.isPresent()).isTrue();
        Assertions.assertThat(storageFromFirstNode.get()).satisfies(e -> {
            Assertions.assertThat(e.getIn().getBody(List.class)).hasSize(scenarioBuilder.getTwitterMessages().size());
        });
        Assertions.assertThat(scenarioBuilder.allTwitterMessagesSaved())
                .hasSize(scenarioBuilder.getTwitterMessages().size());
    }

    @Test
    public void messagesAreForwardToQueueRouteAndQueuedAndRepositoryIsEmpty() {
        scenarioBuilder.unbuild().prepareDummyTweets(16).build();
        producerTemplate.sendBody("direct:evaluate-database", null);

        Optional<Exchange> storageFromFirstNode = Optional
                .ofNullable(Iterables.getLast(mockedResultFirstRouteIdQueue.getReceivedExchanges(), null));
        Optional<Exchange> storageFromBeforeActiveMQ = Optional.ofNullable(
                Iterables.getLast(mockedResultBeforeActiveMQFilterRouteIdQueue.getReceivedExchanges(), null));

        Assertions.assertThat(storageFromBeforeActiveMQ.isPresent()).isTrue();
        Assertions.assertThat(storageFromFirstNode.isPresent()).isTrue();
        Assertions.assertThat(storageFromFirstNode.get()).satisfies(e -> {
            Assertions.assertThat(e.getIn().getBody(List.class)).hasSize(scenarioBuilder.getTwitterMessages().size());
        });
        Assertions.assertThat(scenarioBuilder.allTwitterMessagesSaved()).isEmpty();
    }

    private void prepareCamelEnvironment() throws Exception {
        camelContext.getRouteDefinition(PersistRelevantTweetsRoute.ROUTE_ID).adviceWith(camelContext,
                new AdviceWithRouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        replaceFromWith("direct:twitter-search");
                    }
                });

        camelContext.getRouteDefinition(ReadQueueAndSaveEachMessageRoute.ROUTE_ID).adviceWith(camelContext,
                new AdviceWithRouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        replaceFromWith("direct:read-activemq");
                        weaveByToUri("websocket://0.0.0.0:" + webSocketport + "/tweetsTrends?sendToAll=true&staticResources=classpath:.").replace()
                                .to("direct:websocket-server");
                    }
                });

        camelContext.getRouteDefinition(PrepareTweetsAndEvaluateThemRoutes.ROUTE_ID_MESSAGE_COUNTER)
                .adviceWith(camelContext, new AdviceWithRouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        replaceFromWith("direct:evaluate-database");
                    }
                });

        camelContext.getRouteDefinition(PrepareTweetsAndEvaluateThemRoutes.ROUTE_ID_QUEUE).adviceWith(camelContext,
                new AdviceWithRouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        weaveAddFirst().to("mock:result-fist-route-id-queue");
                        interceptSendToEndpoint("activemq:queue:Tweets.Trends")
                                .to("mock:result-before-activemq-route-id-queue");
                    }
                });

        camelContext.start();
    }
}