package br.com.willianantunes.route;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.twitter.search.TwitterSearchComponent;
import org.springframework.stereotype.Component;

import br.com.willianantunes.model.TwitterMessage;
import twitter4j.Status;

/**
 * @see <a href="https://github.com/apache/camel/blob/master/components/camel-twitter/src/main/docs/twitter-search-component.adoc">Twitter Search Component</a>
 */
@Component
public class PersistRelevantTweetsRoute extends RouteBuilder {

    public static final String ROUTE_ID = "TwitterSnifferRoute";

    @PropertyInject("camel.component.twitter.consumer-key")
    private String consumerKey;
    @PropertyInject("camel.component.twitter.consumer-secret")
    private String consumerSecret;
    @PropertyInject("camel.component.twitter.access-token")
    private String accessToken;
    @PropertyInject("camel.component.twitter.access-token-secret")
    private String accessTokenSecret;

    @PropertyInject("custom.twitter.delay-each-request")
    private String delay;
    @PropertyInject("custom.twitter.keywords")
    private String keywords;

    @Override
    public void configure() throws Exception {
        setUpTwitterComponent();

        fromF("twitter-search:%s?type=polling&delay=%s", keywords, delay).routeId(ROUTE_ID)
            .log(LoggingLevel.DEBUG, "The user named ${body.user.name} posted the following text at ${body.createdAt}: ${body.text}")
            .process(getTweetAndPrepareItToBePersisted())
            .toF("jpa:%s", TwitterMessage.class.getName())
            .log("Inserted new TwitterMessage with ID ${body.id}");
    }

    private Processor getTweetAndPrepareItToBePersisted() {
        return myExchange -> {
            Status status = myExchange.getIn().getBody(Status.class);

            TwitterMessage myTwitterMessages = TwitterMessage.builder()
                .userName(status.getUser().getName())
                .screenName(status.getUser().getScreenName())
                .createdAt(LocalDateTime.ofInstant(status.getCreatedAt().toInstant(), ZoneId.systemDefault()))
                .text(status.getText()).build();

            myExchange.getIn().setBody(myTwitterMessages);
        };
    }

    private void setUpTwitterComponent() {
        TwitterSearchComponent twitterSearchComponent = getContext().getComponent("twitter-search", TwitterSearchComponent.class);
        twitterSearchComponent.setConsumerKey(consumerKey);
        twitterSearchComponent.setConsumerSecret(consumerSecret);
        twitterSearchComponent.setAccessToken(accessToken);
        twitterSearchComponent.setAccessTokenSecret(accessTokenSecret);
    }
}