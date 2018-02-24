package br.com.willianantunes.conf;

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.activemq.camel.component.ActiveMQConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JMSConfiguration {
	
	@Value("${custom.brokerURL}")
	private String brokerURL;
	
	@Bean
	public ActiveMQComponent activeMQComponent() {
		ActiveMQConfiguration activeMQConfiguration = new ActiveMQConfiguration();
		activeMQConfiguration.setBrokerURL(brokerURL);
		
		return new ActiveMQComponent(activeMQConfiguration);
	}
}