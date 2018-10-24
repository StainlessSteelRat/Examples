package io.echoplex.web.neo4j.repository;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import io.echoplex.web.neo4j.domain.EchoUser;


public interface UserRepository extends Neo4jRepository<EchoUser, Long> {

	 // derived finder
	  EchoUser findByUserId(String username);
	  
	  EchoUser findByUserIdAndPassword(String username, String password);
}
