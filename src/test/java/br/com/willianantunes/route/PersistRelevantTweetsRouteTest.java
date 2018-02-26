package br.com.willianantunes.route;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.Route;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.UseAdviceWith;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import br.com.willianantunes.model.TwitterMessage;
import twitter4j.Status;
import twitter4j.User;

/**
 * @see <a href="http://camel.apache.org/advicewith.html">AdviceWith</a>
 * @see <a href="https://github.com/apache/camel/blob/master/components/camel-spring-boot/src/test/java/org/apache/camel/spring/boot/mockendpoints/AdviceWithTest.java">AdviceWithTest</a>
 */
@RunWith(CamelSpringBootRunner.class)
@UseAdviceWith
@SpringBootTest
public class PersistRelevantTweetsRouteTest {

    @Autowired
    private ModelCamelContext camelContext;
    @Autowired
    private ProducerTemplate producerTemplate;

    @EndpointInject(uri = "mock:result-before-insert")
    private MockEndpoint mockedResultBeforeInsert;
    @EndpointInject(uri = "mock:result-after-insert")
    private MockEndpoint mockedResultAfterInsert;

    @Before
    public void setUp() throws Exception {
        if (!camelContext.getStatus().isStarted()) {
            prepareCamelEnvironment();
        }
    }

    @Test
    public void shouldDetectRoute() {
        Route route = camelContext.getRoute(PersistRelevantTweetsRoute.ROUTE_ID);

        Assertions.assertThat(route).isNotNull();
    }

    @Test
    public void tweetIsReadAndPersisted() throws InterruptedException {
        Status sampleTweet = generateFooStatus();
        mockedResultBeforeInsert.expectedMessageCount(1);

        producerTemplate.sendBody("direct:twitter-search", sampleTweet);

        // See you can use MockEndpoint to verify assertions, but why not use something better like AssertJ (more readable)
        mockedResultBeforeInsert.assertIsSatisfied();

        Assertions.assertThat(mockedResultBeforeInsert.getReceivedExchanges()).hasSize(1).allSatisfy(e -> {
            TwitterMessage body = e.getIn().getBody(TwitterMessage.class);

            Assertions.assertThat(body.getId()).isNull();
            Assertions.assertThat(body.getUserName()).isEqualTo(sampleTweet.getUser().getName());
            Assertions.assertThat(body.getScreenName()).isEqualTo(sampleTweet.getUser().getScreenName());
            Assertions.assertThat(body.getText()).isEqualTo(sampleTweet.getText());
            Assertions.assertThat(body.getCreatedAt())
                    .isEqualTo(LocalDateTime.ofInstant(sampleTweet.getCreatedAt().toInstant(), ZoneId.systemDefault()));
        });

        Assertions.assertThat(mockedResultAfterInsert.getReceivedExchanges()).hasSize(1).allSatisfy(e -> {
            TwitterMessage body = e.getIn().getBody(TwitterMessage.class);

            Assertions.assertThat(body.getId()).isNotNull().isEqualTo(1);
        });
    }

    private Status generateFooStatus() {
        Status mock = Mockito.mock(Status.class);

        Mockito.when(mock.getUser()).thenAnswer(i -> {
            User fooUser = Mockito.mock(User.class);
            Mockito.when(fooUser.getName()).thenReturn("Foo Name");
            Mockito.when(fooUser.getScreenName()).thenReturn("Foo_Screen_Name");
            return fooUser;
        });

        Mockito.when(mock.getCreatedAt()).thenReturn(Date.from(Instant.now()));
        Mockito.when(mock.getText()).thenReturn("This is a Tweet written by Foo");

        return mock;
    }

    private void prepareCamelEnvironment() throws Exception {
        camelContext.getRouteDefinition(PersistRelevantTweetsRoute.ROUTE_ID).adviceWith(camelContext,
                new AdviceWithRouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        replaceFromWith("direct:twitter-search");
                        weaveByToUri("jpa*").before().to("mock:result-before-insert");
                        weaveByToUri("jpa*").after().to("mock:result-after-insert");
                    }
                });

        camelContext.getRouteDefinition(ReadQueueAndSaveEachMessageRoute.ROUTE_ID).adviceWith(camelContext,
                new AdviceWithRouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        replaceFromWith("direct:read-activemq");
                        weaveByToUri("websocket://localhost:8095/tweetsTrends?sendToAll=true").replace()
                                .to("direct:websocket-server");
                    }
                });

        camelContext.start();
    }
}