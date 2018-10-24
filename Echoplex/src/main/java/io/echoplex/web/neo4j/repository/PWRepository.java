package io.echoplex.web.neo4j.repository;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import io.echoplex.web.neo4j.domain.PWRequest;


public interface PWRepository extends Neo4jRepository<PWRequest, Long> {

	
	 // derived finder
	PWRequest findByUserIdAndType(String username, PWRequest.Type type);
	Iterable <PWRequest> findAllByUserIdAndType(String username, PWRequest.Type type);

	
	
}
