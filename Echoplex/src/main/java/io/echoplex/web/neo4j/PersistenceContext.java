package io.echoplex.web.neo4j; 

import org.neo4j.ogm.session.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableNeo4jRepositories(basePackages = "io.echoplex.web.neo4j.repository")
@EnableTransactionManagement
@ComponentScan("io.echoplex.web.neo4j")
public class PersistenceContext {
	Logger log = LoggerFactory.getLogger(PersistenceContext.class); 

	public PersistenceContext() { 
		log.debug("Up");
	}

	@Bean
	public SessionFactory getSessionFactory() {
		return new SessionFactory(configuration(), "io.echoplex.web.neo4j.domain");
	}

	@Bean
	public Neo4jTransactionManager transactionManager() throws Exception {
		return new Neo4jTransactionManager(getSessionFactory());
	}

	@Bean
	public org.neo4j.ogm.config.Configuration configuration() {
		return new org.neo4j.ogm.config.Configuration.Builder().uri("bolt://hobby-khmlhiflopbhgbkejipacdpl.dbs.graphenedb.com:24786").credentials("echoplex", "").build();
	}
}