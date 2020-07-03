package org.jessjb.analyser.statementanalyser.data;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;

@Data
public class Transaction {
	public Date tDate;
	public String narration;
	public String refNo;
	public Date valueDate;
	public BigDecimal withdrawalAmount;
	public BigDecimal depositAmount;
	public BigDecimal ClosingBalance;
}
