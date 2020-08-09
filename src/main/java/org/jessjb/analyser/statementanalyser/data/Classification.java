package org.jessjb.analyser.statementanalyser.data;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
@Data
@AllArgsConstructor
public class Classification {
	public Classification() {
		// TODO Auto-generated constructor stub
	}
	String name;
	BigDecimal totalExpense;
	BigDecimal percent;
	int count;
	List<Transaction> transactions;
}
