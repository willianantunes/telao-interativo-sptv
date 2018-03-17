package br.com.willianantunes.route;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.EnableRouteCoverage;
import org.apache.camel.test.spring.UseAdviceWith;
import org.assertj.core.api.Assertions;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketListener;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.google.common.collect.Iterables;

@RunWith(CamelSpringBootRunner.class)
@UseAdviceWith
@SpringBootTest
@EnableRouteCoverage
public class ReadQueueAndSaveEachMessageRouteTest {

    @Autowired
    private ModelCamelContext camelContext;
    @Autowired
    private ProducerTemplate producerTemplate;

    @PropertyInject("custom.websocket-port")
    private String webSocketport;     
    @EndpointInject(uri = "mock:result-before-file")
    private MockEndpoint mockedResultBeforeFile;

    private Integer port = AvailablePortFinder.getNextAvailable();
    private String temporaryDirectory = System.getProperty("java.io.tmpdir");
    private String samplePayload = "{\n" + 
            "  \"id\" : 25,\n" + 
            "  \"userName\" : \"SpaceX\",\n" + 
            "  \"screenName\" : \"SpaceX\",\n" + 
            "  \"createdAt\" : \"2018-02-22T11:31:00\",\n" + 
            "  \"text\" : \"Successful deployment of PAZ satellite to low-Earth orbit confirmed.\"\n" + 
            "}";

    private static List<Object> receivedMessages = new ArrayList<Object>();
    private static CountDownLatch latch;

    @Before
    public void setUp() throws Exception {
        if (!camelContext.getStatus().isStarted()) {
            prepareCamelEnvironment();
            latch = new CountDownLatch(1);
        }
    }

    @Test
    public void messageIsReceivedAndFileIsCreated() {
        producerTemplate.sendBody("direct:read-activemq", samplePayload);

        Optional<Exchange> storageFromBeforeFile = Optional
                .ofNullable(Iterables.getLast(mockedResultBeforeFile.getExchanges()));

        Assertions.assertThat(storageFromBeforeFile.isPresent()).isTrue();
        Assertions.assertThat(storageFromBeforeFile.get()).satisfies(e -> {
            String fileName = e.getIn().getHeader("CamelFileName").toString();
            Path myFile = Paths.get(temporaryDirectory + "/" + fileName);

            Assertions.assertThat(fileName).containsPattern("^SpaceX-[0-9]{8}-[0-9]{6}.json$");
            Assertions.assertThat(Files.exists(myFile)).isTrue();
            try {
                Assertions.assertThat(new String(Files.readAllBytes(Paths.get(temporaryDirectory + "/" + fileName))))
                        .isEqualTo(samplePayload);
            } catch (IOException e1) {
                new RuntimeException("It wasn't possible to read the file " + myFile.toString());
            }
        });
    }

    @Test
    public void messageIsReceivedAndSendItToWebSocketClient() throws InterruptedException, ExecutionException, IOException {
        AsyncHttpClient asyncHttpClient = new DefaultAsyncHttpClient();
        asyncHttpClient.prepareGet("ws://localhost:" + port + "/tweetsTrends")
                .execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(new WebSocketListener() {

                    @Override
                    public void onTextFrame(String payload, boolean finalFragment, int rsv) {
                        receivedMessages.add(payload);
                        latch.countDown();
                    }

                    @Override
                    public void onOpen(WebSocket websocket) { }

                    @Override
                    public void onError(Throwable t) { }

                    @Override
                    public void onClose(WebSocket websocket, int code, String reason) { }
                }).build()).get();

        producerTemplate.sendBody("direct:read-activemq", samplePayload);

        Assertions.assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        Assertions.assertThat(receivedMessages).hasSize(1).allSatisfy(m -> {
            String message = (String) m;

            Assertions.assertThat(message).isEqualTo(samplePayload);
        });

        asyncHttpClient.close();
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
                        weaveByToUri("file*").before().to("mock:result-before-file");
                        weaveByToUri("websocket://0.0.0.0:" + webSocketport + "/tweetsTrends?sendToAll=true&staticResources=classpath:.").replace()
                                .to("websocket://localhost:" + port + "/tweetsTrends?sendToAll=true");
                    }
                });

        camelContext.getRouteDefinition(PrepareTweetsAndEvaluateThemRoutes.ROUTE_ID_MESSAGE_COUNTER)
                .adviceWith(camelContext, new AdviceWithRouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        replaceFromWith("direct:evaluate-database");
                    }
                });

        camelContext.start();
    }
}