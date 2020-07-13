package org.jessjb.analyser.statementanalyser.data;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
@Data
@AllArgsConstructor
public class Classification {
	String name;
	BigDecimal totalExpense;
	BigDecimal percent;
	int count;
	List<Transaction> transactions;
}
