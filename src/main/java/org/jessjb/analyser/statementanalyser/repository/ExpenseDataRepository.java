package org.jessjb.analyser.statementanalyser.repository;

import java.util.Map;

import org.jessjb.analyser.statementanalyser.data.Pattern;
import org.jessjb.analyser.statementanalyser.entity.ExpenseData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ExpenseDataRepository extends MongoRepository<ExpenseData, String> {
	Map<String,Pattern> findPatternsByUsername(String username);
	ExpenseData findByUsername(String username);
	boolean existsByUsername(String username);
}
