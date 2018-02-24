package br.com.willianantunes.repository;

import org.springframework.data.repository.CrudRepository;

import br.com.willianantunes.model.TwitterMessage;

public interface TwitterMessageRepository extends CrudRepository<TwitterMessage, Integer> {

}