package br.com.willianantunes.route;

import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.stereotype.Component;

import br.com.willianantunes.model.TwitterMessage;

/**
 * @see <a href="https://github.com/apache/camel/blob/master/camel-core/src/main/docs/scheduler-component.adoc">Scheduler Component</a>
 */
@Component
public class PrepareTweetsAndEvaluateThemRoutes extends RouteBuilder {
	
	@PropertyInject("custom.scheduler.delay-each-read")
	private String delay;
	
	@Override
	public void configure() throws Exception {
        fromF("scheduler://myScheduler?useFixedDelay=false&delay=%s", delay)
    	.routeId("TweetMessageCountRoute")
    	.pollEnrich(String.format("jpa:%s?consumer.namedQuery=%s&consumeDelete=%s", TwitterMessage.class.getName(), TwitterMessage.NAMED_QUERY_SELECT_ALL, false))
    	.choice()
    		.when(simple("${body} is 'java.util.List'"))
    			.log("We have ${body.size} tweet messages up until now...")
    			.to("direct:informMyQueue")
    		.otherwise()
    			.log("We have 1 tweet messages up until now...")
    		.endChoice();
        
        from("direct:informMyQueue")
	    	.routeId("ProducerTweetQueueRoute")
	    	.filter(simple("${body.size} > 15"))
	    		.log("Taking a List of TwitterMessages and creating one message by each element...")
	    		.marshal().json(JsonLibrary.Jackson)
	    		.convertBodyTo(String.class)
				.to("activemq:queue:Tweets.Trends")
				.log("All of the rows were sent to the queue Tweets.Trends")
				.to(String.format("jpa:br.com.willianantunes.test.entity.TwitterMessage?namedQuery=%s&useExecuteUpdate=%s", "DELETE-ALL", true))
				.log("The table was truncated...");        
	}
	
	
}