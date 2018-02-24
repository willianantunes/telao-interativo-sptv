package br.com.willianantunes.support;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import br.com.willianantunes.model.TwitterMessage;
import br.com.willianantunes.repository.TwitterMessageRepository;
import br.com.willianantunes.support.deser.LocalDateTimeDeserializer;
import lombok.Getter;

@Component
public class ScenarioBuilder {
	@Autowired
	private TwitterMessageRepository twitterMessageRepository;
	
	@Getter
	private List<TwitterMessage> twitterMessages;	
	
	public ScenarioBuilder prepareDummyTweets() {
		twitterMessages = loadObjectList(TwitterMessage.class, "twitter-messages.csv");
		
		return this;
	}
	
	public void build() {
		Optional.ofNullable(twitterMessages).ifPresent(twitterMessageRepository::save);
	}
	
	private <T> List<T> loadObjectList(Class<T> type, String fileName) {
	    try {
	        CsvSchema bootstrapSchema = CsvSchema.emptySchema().withHeader();
	        CsvMapper mapper = new CsvMapper();

	        SimpleModule module = new SimpleModule();
	        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer());
	        mapper.registerModule(module);
	        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

	        File file = new ClassPathResource(fileName).getFile();
	        
	        MappingIterator<T> readValues = mapper.readerFor(type).with(bootstrapSchema).readValues(file);
	        return readValues.readAll();
	    } catch (Exception e) {
	    	throw new RuntimeException("Error occurred while loading object list from file " + fileName, e);
	    }
	}
}