package org.jessjb.analyser.statementanalyser.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.time.Year;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ClassPathResource;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.jessjb.analyser.statementanalyser.algo.NurminenDetectionAlgorithm1;
import org.jessjb.analyser.statementanalyser.data.Classification;
import org.jessjb.analyser.statementanalyser.data.Transaction;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import net.bytebuddy.asm.Advice.Local;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.PageIterator;
import technology.tabula.Rectangle;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;
import technology.tabula.detectors.NurminenDetectionAlgorithm;
import technology.tabula.extractors.BasicExtractionAlgorithm;
import technology.tabula.extractors.ExtractionAlgorithm;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

@Service
public class AnalysisService {
	public List<Transaction> readTransactions(File Filepath) {
		List<Transaction> tList = new ArrayList<Transaction>();
		PDDocument pdfDocument = null;
		try {
			Map<String,ArrayList<String>> classificationKeyMap = getClassificationsFromFile();
			pdfDocument = PDDocument.load(Filepath);
			ObjectExtractor extractor = new ObjectExtractor(pdfDocument);
			Map<Integer, List<Rectangle>> detectedTables = new HashMap<>();
			ExtractionAlgorithm extractionAlgorithm = null;
			NurminenDetectionAlgorithm detectionAlgorithm = new NurminenDetectionAlgorithm();
			SpreadsheetExtractionAlgorithm sea = new SpreadsheetExtractionAlgorithm();
			BasicExtractionAlgorithm bea = new BasicExtractionAlgorithm();
			PageIterator pages = extractor.extract();
			while (pages.hasNext()) {
				Page page = pages.next(); 
				List<Rectangle> tablesOnPage = detectionAlgorithm.detect(page);
				if (tablesOnPage.size() > 0) {
					detectedTables.put(new Integer(page.getPageNumber()), tablesOnPage);
					System.out.println("Tables on Page " + page.getPageNumber() + " " + tablesOnPage.size());
					Rectangle geometicArea = tablesOnPage.get(0);
						geometicArea.height+=1;
						geometicArea.width+=1;
					Page tableArea = page.getArea(geometicArea);
					extractionAlgorithm = sea.isTabular(tableArea)?sea:bea;
					Table table = extractionAlgorithm.extract(tableArea).get(0);
					for (List<RectangularTextContainer> row : table.getRows()) {
						if (row.get(0).getText().equals("Date"))
							continue;
						if(row.get(0).getText().equals("")) {
							Transaction t = tList.get(tList.size()-1);
							t.setNarration(t.getNarration().concat(row.get(1).getText()));
							tList.set(tList.size()-1, t);
						}
						else {
							Transaction t = new Transaction();
							t.setTDate(new SimpleDateFormat(row.get(0).getText().length()==10? "dd/MM/yyyy":"dd/MM/yy").parse(row.get(0).getText()).toInstant()
									.atZone(ZoneId.systemDefault()).toLocalDate());
							t.setNarration(row.get(1).getText());
							t.setRefNo(row.get(2).getText());
							t.setValueDate(new SimpleDateFormat(row.get(3).getText().length()==10? "dd/MM/yyyy":"dd/MM/yy").parse(row.get(3).getText()));
							if(!row.get(4).getText().equals(""))
								t.setWithdrawalAmount(new BigDecimal(row.get(4).getText().replaceAll(",", "")));
							else
								t.setWithdrawalAmount(new BigDecimal("0.00"));
							if(!row.get(5).getText().equals(""))
								t.setDepositAmount(new BigDecimal(row.get(5).getText().replaceAll(",", "")));
							else
								t.setDepositAmount(new BigDecimal("0.00"));
							t.setClosingBalance(new BigDecimal(row.get(6).getText().replaceAll(",", "")));
							String[] category = findClassification(t, classificationKeyMap);
							t.setCategory(category[0]);
							t.setKeyword(category[1]);
							tList.add(t);
						}
					}		
				}
			}
			System.out.println(tList.size()+" Rows Found");
			pdfDocument.close();
		} catch (InvalidPasswordException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			try {
				pdfDocument.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return tList;
	}
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

	public Map<String, Map<String, BigDecimal>> findUPITransactions(List<Transaction> transactions) {
		List<Transaction> UPITransactions = new ArrayList<Transaction>();
		Map<String, Map<String, BigDecimal>> uniqueUpiIds = new HashMap<String, Map<String, BigDecimal>>();
		for (Transaction t : transactions) {
			if (t.getNarration().contains("UPI")) {
				UPITransactions.add(t);
				// uniqueUpiIds.add(t.getNarration().split("-")[2]);
			}
		}
		for (Transaction t : UPITransactions) {
			String upiID = t.getNarration().split("-")[2];
			if (uniqueUpiIds.containsKey(upiID)) {
				Map<String, BigDecimal> transactionSumUp = uniqueUpiIds.get(upiID);
				transactionSumUp.put("to", transactionSumUp.get("to").add(t.getWithdrawalAmount()));
				transactionSumUp.put("from", transactionSumUp.get("from").add(t.getDepositAmount()));
				uniqueUpiIds.replace(upiID, transactionSumUp);
			} else {
				Map<String, BigDecimal> transactionSumUp = new HashMap<String, BigDecimal>();
				transactionSumUp.put("to", t.getWithdrawalAmount());
				transactionSumUp.put("from", t.getDepositAmount());
				uniqueUpiIds.put(upiID, transactionSumUp);
			}
		}
		Map<String, Map<String, BigDecimal>> uniqueUpiIdsSorted = new TreeMap<String, Map<String, BigDecimal>>(
				uniqueUpiIds);
		return uniqueUpiIdsSorted;
	}
	
	public Map<String,Classification> classifier(List<Transaction> transactions){
		Map<String,Classification> classifications = new HashMap<String, Classification>();
		for(Transaction t: transactions) {
			String category =  t.getCategory();
			if(classifications.containsKey(category)) {
				Classification c = classifications.get(category);
				c.setCount(c.getCount()+1);
				c.setTotalExpense(c.getTotalExpense().add(t.getWithdrawalAmount()));
				List<Transaction> cTransactions = c.getTransactions();
				cTransactions.add(t);
				c.setTransactions(cTransactions);
				classifications.replace(category,c);
			}
			else {
				Classification c = new Classification();
				List<Transaction> cTransactions = new ArrayList<Transaction>();
				cTransactions.add(t);
				c.setName(category);
				c.setTotalExpense(t.getWithdrawalAmount());
				c.setCount(1);
				c.setTransactions(cTransactions);
				classifications.put(category, c);
			}
		}
		BigDecimal total = new BigDecimal("0.00");
		for(String c: classifications.keySet()) {
			total = total.add(classifications.get(c).getTotalExpense());
		}
		for(String c: classifications.keySet()) {
			BigDecimal percent = findPercent(classifications.get(c).getTotalExpense(), total);
			classifications.get(c).setPercent(percent);
		}
		return classifications;
	}

	public Boolean searchForKey(String[] keys, String narration) {
		for (String key : keys)
			if (narration.contains(key))
				return true;
		return false;
	}
	public String searchForKeyNew(ArrayList<String> keys, String narration) {
		for (String key : keys)
			if (narration.contains(key))
				return key;
		return null;
	}

	public BigDecimal findTotalExpense(List<Transaction> classificaation) {
		BigDecimal total = new BigDecimal(0);
		for (Transaction t : classificaation) {
			total = total.add(t.getWithdrawalAmount());
		}
		return total;
	}

	public BigDecimal findPercent(BigDecimal sum, BigDecimal total) {
		BigDecimal multiplicand = new BigDecimal("100.0");
		return sum.divide(total, RoundingMode.HALF_UP).multiply(multiplicand);
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
		Map<String, BigDecimal> monthlyData = new HashMap<String, BigDecimal>();
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
		LocalDate eDate = tList.get(tList.size() - 1).getTDate();
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
	
	public Map<String,ArrayList<String>> getClassificationsFromFile(){
		Map<String, ArrayList<String>> keyWords = new HashMap<String, ArrayList<String>>();
		try {
			Scanner sc;
			Resource resource = new ClassPathResource("keywords.csv");
			sc = new Scanner(resource.getFile());
			sc.useDelimiter("\n");
			String categoryString = sc.next();
			String[] category = categoryString.split(",");
			while (sc.hasNext()) {
				String[] keyWordString = sc.next().split(",");
				for (int i = 0; i < category.length; i++) {
					String keyword = keyWordString[i];
					if (!keyword.equals("\r") && !keyword.equals("")) {
						if(keyword.contains("\r"))
							keyword = keyword.substring(0, keyword.length()-1);
						if (keyWords.containsKey(category[i])) {
							ArrayList<String> keywordList = keyWords.get(category[i]);
							keywordList.add(keyword);
							keyWords.replace(category[i], keywordList);
						} else {
							ArrayList<String> keywordList = new ArrayList<String>();
							keywordList.add(keyword);
							keyWords.put(category[i], keywordList);
						}
					}
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return keyWords;
	}
	
	public String[] findClassification(Transaction t,Map<String,ArrayList<String>> classificationKeys) {
		String narration = t.getNarration();
		String[] classification = {"Other",null};
		for(String key: classificationKeys.keySet()) {
			ArrayList<String> keys = classificationKeys.get(key);
			String keyWord = searchForKeyNew(keys, narration);
			if(keyWord!=null) {
				classification[0] = key;
				if(keyWord.equals("UPI"))
					classification[1] = narration.split("-").length<3?key:narration.split("-")[2];
				else
					classification[1] = keyWord;
				break;
			}
		}
		return classification;
	}

}
