package org.jessjb.analyser.statementanalyser.data;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KeyWord implements Comparable<KeyWord> {
	private String key;
	private String Category;
	private BigDecimal value;
	@Override
	public int compareTo(KeyWord c) {			
		return this.value.compareTo(c.value);
	}
}