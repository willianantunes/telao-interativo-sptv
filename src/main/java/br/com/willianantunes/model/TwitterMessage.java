package br.com.willianantunes.model;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder
@AllArgsConstructor @NoArgsConstructor
@Entity @Table(name = "TB_TWITTER_MESSAGE")
@NamedQueries({
	@NamedQuery(name = TwitterMessage.NAMED_QUERY_SELECT_ALL, query = "SELECT t FROM TwitterMessage t"),
	@NamedQuery(name = TwitterMessage.NAMED_QUERY_DELETE_ALL, query = "DELETE FROM TwitterMessage")
})
public class TwitterMessage {
	
	public static final String NAMED_QUERY_SELECT_ALL = "SELECT-ALL";
	public static final String NAMED_QUERY_DELETE_ALL = "DELETE-ALL";
	
	@Id
	@GeneratedValue
	private Integer id;
	@Column
	private String userName;
	@Column
	private String screenName;
	@Column
	private LocalDateTime createdAt;
	@Column
	private String text;
}