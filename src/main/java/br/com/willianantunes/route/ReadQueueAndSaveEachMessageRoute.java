package br.com.willianantunes.route;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.stereotype.Component;

import br.com.willianantunes.model.TwitterMessage;

@Component
public class ReadQueueAndSaveEachMessageRoute extends RouteBuilder {
	
	public static final String ROUTE_ID = "ConsumerTweetQueueRoute";

	private String temporaryDirectory = System.getProperty("java.io.tmpdir");
	
	@Override
	public void configure() throws Exception {
        from("activemq:queue:Tweets.Trends")
	    	.routeId(ROUTE_ID)
	    	.unmarshal().json(JsonLibrary.Jackson, TwitterMessage.class)
	    	.log("The following twitter user is passing by: ${body.userName}")
	    	.setHeader("CamelFileName", simple("${body.userName}-${date:now:yyyyMMdd-hhmmss}.json"))
	    	.marshal().json(JsonLibrary.Jackson).convertBodyTo(String.class)
	    	.to("file:" + temporaryDirectory);
	}	
}