package br.com.willianantunes.conf;

import org.springframework.context.annotation.Configuration;

/**
 * @see <a href="https://github.com/apache/camel/blob/master/components/camel-jackson/src/main/docs/json-jackson-dataformat.adoc">JSon Jackson DataFormat</a>
 */
@Configuration
public class JacksonConfiguration {
//	public static final String JACKSON_DATA_FORMAT_TWITTER_MESSAGE = "JACKSON_DATA_FORMAT_TWITTER_MESSAGE";
//	
//	@Bean(value = JACKSON_DATA_FORMAT_TWITTER_MESSAGE)
//	public JacksonDataFormat configureJacksonDataFormat() {
//		
//		JacksonDataFormat myJacksonDataFormat = new JacksonDataFormat(TwitterMessage.class);
//        myJacksonDataFormat.setDisableFeatures(String.format("%s,%s", DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES.name(), SerializationFeature.WRITE_DATES_AS_TIMESTAMPS.name()));
//        myJacksonDataFormat.addModule(new JavaTimeModule());
//        myJacksonDataFormat.setPrettyPrint(true);
//        
//        return myJacksonDataFormat;
//	}	
}
