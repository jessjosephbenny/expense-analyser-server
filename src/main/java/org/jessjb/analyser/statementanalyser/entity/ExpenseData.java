package org.jessjb.analyser.statementanalyser.entity;

import java.util.List;
import java.util.Map;

import org.jessjb.analyser.statementanalyser.data.Pattern;
import org.jessjb.analyser.statementanalyser.data.Transaction;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Document
@Data
public class ExpenseData {
	
	@Id
	private String _id;
	
	@Indexed(unique = true)
	private String username;
	
	private List<Transaction> transactions;
	
	private Map<String,Pattern> patterns;
}
