package org.jessjb.analyser.statementanalyser.data;

import lombok.Data;

@Data
public class Pattern {
	private String name;
	private int date;
	private String dateFormat;
	private int narration;
	private int refNo;
	private int withdrawal;
	private int deposit;
	private int balance;
	private boolean tabular;
}
