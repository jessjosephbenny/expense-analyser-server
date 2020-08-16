package org.jessjb.analyser.statementanalyser.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class Transaction {
	public LocalDate tDate;
	public String narration;
	public String keyword;
	public String category;
	public String refNo;
	public BigDecimal withdrawalAmount;
	public BigDecimal depositAmount;
	public BigDecimal ClosingBalance;
}
