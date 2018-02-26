package br.com.willianantunes.playground;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.websocket.WebsocketComponent;
import org.apache.camel.component.websocket.WebsocketConstants;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.UseAdviceWith;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import br.com.willianantunes.MainApplication;
import br.com.willianantunes.playground.WebSocketProducerOnlyTest.ContextConfiguration;

@RunWith(CamelSpringBootRunner.class)
@UseAdviceWith
@SpringBootTest(classes = { MainApplication.class, ContextConfiguration.class })
@Ignore("It's meant to be run manually and not during build process")
public class WebSocketProducerOnlyTest {
	
	private static final Logger logger = LoggerFactory.getLogger(WebSocketProducerOnlyTest.class);
	
	@Autowired
	private ModelCamelContext camelContext;	
	
	@Before
	public void setUp() throws Exception {
		if (!camelContext.getStatus().isStarted()) {
			prepareCamelEnvironmentToRunOnlyMyTestRoute();
		}
	}
	
    @Test
    public void liveTest() throws Exception {
    	logger.info("Ready to test! Open URL http://localhost:3030/test-my-websocket.html");
        Thread.sleep(5 * 60 * 1000);
    }
    
	private void prepareCamelEnvironmentToRunOnlyMyTestRoute() throws Exception {
		List<RouteDefinition> routesToBeRemoved = camelContext.getRouteDefinitions().stream()
			.filter(r -> !r.getId().equals(WebSocketProducerOnlyRoute.ROUTE_ID))
			.collect(Collectors.toList());
		
		camelContext.removeRouteDefinitions(routesToBeRemoved);			
		camelContext.start();
	}    
	
    @Configuration
	static class ContextConfiguration {
		
    	@Bean
    	public WebSocketProducerOnlyRoute webSocketProducerOnlyRoute() {
    		return new WebSocketProducerOnlyRoute();
    	}
	}	
    
	@Component
	static class WebSocketProducerOnlyRoute extends RouteBuilder {

		public static final String ROUTE_ID = WebSocketProducerOnlyRoute.class.getSimpleName();
		
	    private String samplePayload = "{\n" + 
				"  \"id\" : 25,\n" + 
				"  \"userName\" : \"SpaceX\",\n" + 
				"  \"screenName\" : \"SpaceX\",\n" + 
				"  \"createdAt\" : \"2018-02-22T11:31:00\",\n" + 
				"  \"text\" : \"Successful deployment of PAZ satellite to low-Earth orbit confirmed.\"\n" + 
				"}";		
		
		@Override
		public void configure() throws Exception {
			setUpWebSocketComponent();		
			
            from("timer://foo?fixedRate=true&period=20000")
            	.routeId(ROUTE_ID)
            	.process(exchange -> {
            		exchange.getIn().setBody(samplePayload);	    
            	}).setHeader(WebsocketConstants.SEND_TO_ALL, constant(true)).to("websocket://test");;
		}

		private void setUpWebSocketComponent() {
            WebsocketComponent component = getContext().getComponent("websocket", WebsocketComponent.class);
            component.setHost("0.0.0.0");
            component.setPort(3030);
            component.setStaticResources("classpath:.");	
		}
	}    
}