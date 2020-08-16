package org.jessjb.analyser.statementanalyser.repository;

import org.jessjb.analyser.statementanalyser.entity.ApplicationUser;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ApplicationUserRepository extends MongoRepository<ApplicationUser, Long> {
	ApplicationUser findByUsername(String username);
}
