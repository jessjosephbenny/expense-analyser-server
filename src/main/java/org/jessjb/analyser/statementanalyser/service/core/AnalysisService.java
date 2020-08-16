package org.jessjb.analyser.statementanalyser.service.core;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jessjb.analyser.statementanalyser.data.Transaction;
import org.springframework.stereotype.Service;

@Service
public class AnalysisService {
	public Map<String, BigDecimal> getSummary(List<Transaction> transactions) {
		Map<String, BigDecimal> summary = new HashMap<String, BigDecimal>();
		BigDecimal totalDeposit = new BigDecimal(0);
		BigDecimal totalWithdrawal = new BigDecimal(0);
		for (Transaction t : transactions) {
			totalDeposit = totalDeposit.add(t.getDepositAmount());
			totalWithdrawal = totalWithdrawal.add(t.getWithdrawalAmount());
		}
		summary.put("totalDeposit", totalDeposit);
		summary.put("totalWithdrawal", totalWithdrawal);
		summary.put("balance", totalDeposit.subtract(totalWithdrawal));
		LocalDate sDate = transactions.get(0).getTDate();
		LocalDate eDate = transactions.get(transactions.size() - 1).getTDate();
		int months = Period.between(sDate, eDate).getYears() * 12 + Period.between(sDate, eDate).getMonths();
		summary.put("average", totalWithdrawal.divide(new BigDecimal(months==0?1:months)));
		return summary;
	}
		
	public void magicWordFinder(List<Transaction> otherList) {
		Map<String, Integer> countMap = new HashMap<String, Integer>();
		otherList.forEach(transaction -> {
			String narration = transaction.getNarration();
			for (String word : narration.split(" ")) {
				if (countMap.containsKey(word))
					countMap.replace(word, countMap.get(word) + 1);
				else
					countMap.put(word, 1);
			}
		});
		List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(countMap.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
				return (o1.getValue()).compareTo(o2.getValue());
			}
		});
		HashMap<String, Integer> temp = new LinkedHashMap<String, Integer>();
		for (Map.Entry<String, Integer> a : list) {
			temp.put(a.getKey(), a.getValue());
		}
	}

	public Map<String, Object> expenseDailyData(List<Transaction> tList) {
		Map<String, Object> barChartData = new HashMap<String, Object>();
		Map<LocalDate, BigDecimal> dailyData = new HashMap<LocalDate, BigDecimal>();
		new HashMap<String, BigDecimal>();
		for (Transaction t : tList) {
			if (t.getWithdrawalAmount().floatValue() > 0) {
				if (dailyData.containsKey(t.getTDate())) {
					dailyData.replace(t.getTDate(), dailyData.get(t.getTDate()).add(t.getWithdrawalAmount()));
				} else {
					dailyData.put(t.getTDate(), t.getWithdrawalAmount());
				}
			}
		}
		LinkedHashMap<LocalDate, BigDecimal> sortedDailyData = dailyData.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
		LocalDate sDate = tList.get(0).getTDate();
		tList.get(tList.size() - 1).getTDate();
		Map<String, Integer> yearlyViewData = new LinkedHashMap<String, Integer>();
		Map<String, Integer> monthlyViewData = new LinkedHashMap<String, Integer>();
		Map<String, Integer> weeklyViewData = new LinkedHashMap<String, Integer>();
		Map<Integer, Map<Integer, Map<Integer, Integer>>> lifeTimeTotalData = new HashMap<Integer, Map<Integer, Map<Integer, Integer>>>();
		Map<Integer, Map<Integer, Integer>> yearlyTotalData = new HashMap<Integer, Map<Integer, Integer>>();
		Map<Integer, Integer> monthlyTotalData = new HashMap<Integer, Integer>();
		int currentWeekDay = sDate.getDayOfWeek().getValue();
		int currentMonth = sDate.getMonthValue();
		int currentYear = sDate.getYear();
		int currentWeek = 1;
		int currentMonthTotal = 0;
		int currentWeektotal = 0;
		int currentYearTotal = 0;
		for (LocalDate d : sortedDailyData.keySet()) {
			int day = d.getDayOfMonth();
			int month = d.getMonthValue();
			int year = d.getYear();
			if (year != currentYear) {
				monthlyTotalData.put(currentWeek, currentWeektotal);
				currentMonthTotal += currentWeektotal;
				monthlyTotalData.put(-1, currentMonthTotal);
				currentYearTotal += currentMonthTotal;
				yearlyTotalData.put(currentMonth, monthlyTotalData);
				lifeTimeTotalData.put(currentYear, yearlyTotalData);
				weeklyViewData.put(currentWeek + "/" + currentMonth + "/" + currentYear, currentWeektotal);
				monthlyViewData.put(currentMonth + "/" + currentYear, currentMonthTotal);
				yearlyViewData.put(String.valueOf(currentYear), currentYearTotal);
				currentYear = year;
				currentMonth = month;
				currentWeek = 1;
				currentWeektotal = 0;
				currentMonthTotal = 0;
				currentYearTotal = 0;
				yearlyTotalData = new HashMap<Integer, Map<Integer, Integer>>();
				monthlyTotalData = new HashMap<Integer, Integer>();
			}
			if (month != currentMonth) {
				monthlyTotalData.put(currentWeek, currentWeektotal);
				monthlyTotalData.put(-1, currentMonthTotal);
				yearlyTotalData.put(currentMonth, monthlyTotalData);
				weeklyViewData.put(currentWeek + "/" + currentMonth + "/" + currentYear, currentWeektotal);
				monthlyViewData.put(currentMonth + "/" + currentYear, currentMonthTotal);
				currentYearTotal += currentMonthTotal;
				currentMonth = month;
				currentWeek = 1;
				currentWeektotal = 0;
				currentMonthTotal = 0;
				monthlyTotalData = new HashMap<Integer, Integer>();
			}
			if (d.getDayOfWeek().getValue() <= currentWeekDay && day != 1) {
				monthlyTotalData.put(currentWeek, currentWeektotal);
				weeklyViewData.put(currentWeek + "/" + currentMonth + "/" + currentYear, currentWeektotal);
				currentMonthTotal += currentWeektotal;
				currentWeek += 1;
				currentWeektotal = 0;
			}
			currentWeektotal += sortedDailyData.get(d).intValue();
			currentWeekDay = d.getDayOfWeek().getValue();

		}
		monthlyTotalData.put(currentWeek, currentWeektotal);
		monthlyViewData.put(currentMonth + "/" + currentYear, currentMonthTotal);
		monthlyTotalData.put(-1, currentMonthTotal);
		currentYearTotal += currentMonthTotal;
		yearlyViewData.put(String.valueOf(currentYear), currentYearTotal);
		yearlyTotalData.put(currentMonth, monthlyTotalData);
		lifeTimeTotalData.put(currentYear, yearlyTotalData);
		barChartData.put("year", yearlyViewData);
		barChartData.put("month", monthlyViewData);
		barChartData.put("week", weeklyViewData);
		barChartData.put("Daily", dailyData);
		return barChartData;
	}

	public int findDaysInMonth(int m, int year) {
		if (m == 2) {
			if ((year % 4 == 0) && (year % 100 != 0) || (year % 400 == 0))
				return 29;
			else
				return 28;
		} else if (((m < 8) && (m % 2 == 1)) || ((m > 7) && (m % 2 == 0)))
			return 31;
		else
			return 30;
	}
}
