package br.com.willianantunes.support.deser;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.common.base.Strings;

@SuppressWarnings("serial")
public class LocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {
	private static final Logger logger = Logger.getLogger(LocalDateTimeDeserializer.class.getName() );
	
	private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	
	public LocalDateTimeDeserializer() {		
		this(null);	
	}
	
	public LocalDateTimeDeserializer(Class<?> valueClass) {
		super(valueClass);
	}

	@Override
	public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {
		LocalDateTime localDateTime = null;
		String value = parser.getText();
		
		if (!Strings.isNullOrEmpty(value)) {
			try {
				localDateTime = formatter.parse(value, LocalDateTime::from);
			} catch (Exception e) {
				logger.log(Level.WARNING, "Não foi possível converter o valor {0}", value);
			}
		}
		
		return localDateTime;		
	}
}