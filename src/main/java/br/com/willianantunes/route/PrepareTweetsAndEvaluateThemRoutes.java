package br.com.willianantunes.route;

import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.stereotype.Component;

import br.com.willianantunes.model.TwitterMessage;

/**
 * @see <a href="https://github.com/apache/camel/blob/master/camel-core/src/main/docs/scheduler-component.adoc">Scheduler Component</a>
 * @see <a href="https://github.com/apache/camel/blob/master/components/camel-jpa/src/main/docs/jpa-component.adoc">JPA Component</a>
 * @see <a href="https://github.com/apache/camel/blob/master/components/camel-jms/src/main/docs/jms-component.adoc#using-activemq">Using ActiveMQ</a>
 */
@Component
public class PrepareTweetsAndEvaluateThemRoutes extends RouteBuilder {

    public static final String ROUTE_ID_MESSAGE_COUNTER = "TweetMessageCountRoute";
    public static final String ROUTE_ID_QUEUE = "ProducerTweetQueueRoute";

    @PropertyInject("custom.scheduler.delay-each-read")
    private String delay;

    @Override
    public void configure() throws Exception {
        fromF("scheduler://myScheduler?useFixedDelay=false&delay=%s", delay).routeId(ROUTE_ID_MESSAGE_COUNTER)
            .pollEnrich(String.format("jpa:%s?consumer.namedQuery=%s&consumeDelete=%s",
                    TwitterMessage.class.getName(), TwitterMessage.NAMED_QUERY_SELECT_ALL, false))
            .choice().when(simple("${body} is 'java.util.List'"))
                .log("We have ${body.size} tweet messages up until now...")
                .to("direct:informMyQueue")
            .otherwise()
                .log("We have 1 tweet message up until now...")
            .endChoice();

        from("direct:informMyQueue").routeId(ROUTE_ID_QUEUE)
            .filter(simple("${body.size} > 15"))
                .log("Taking a List of TwitterMessages and creating one message by each element...")
                .split(body())
                .setHeader("id", simple("${body.id}"))
                .marshal().json(JsonLibrary.Jackson).convertBodyTo(String.class)
                .to("activemq:queue:Tweets.Trends")
                .log("All of the rows were sent to the queue Tweets.Trends")
                .toD(String.format("jpa:%s?namedQuery=%s&useExecuteUpdate=%s&parameters={\"id\":${headers.id}}",
                        TwitterMessage.class.getName(), TwitterMessage.NAMED_QUERY_DELETE_ONE, true))
                .log("TwitterMessage with id ${headers.id} was deleted...");
    }
}