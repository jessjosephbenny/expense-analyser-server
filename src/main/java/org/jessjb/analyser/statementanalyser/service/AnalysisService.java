package org.jessjb.analyser.statementanalyser.service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
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
import java.util.TreeMap;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.jessjb.analyser.statementanalyser.data.Classification;
import org.jessjb.analyser.statementanalyser.data.Transaction;
import org.springframework.stereotype.Service;

import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.PageIterator;
import technology.tabula.Rectangle;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;
import technology.tabula.detectors.NurminenDetectionAlgorithm;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

@Service
public class AnalysisService {
	public List<Transaction> readTransactions(File Filepath) {
		List<Transaction> tList = new ArrayList<Transaction>();
		try {
			PDDocument pdfDocument = PDDocument.load(Filepath);
			ObjectExtractor extractor = new ObjectExtractor(pdfDocument);
			Map<Integer, List<Rectangle>> detectedTables = new HashMap<>();
			NurminenDetectionAlgorithm detectionAlgorithm = new NurminenDetectionAlgorithm();
			SpreadsheetExtractionAlgorithm se = new SpreadsheetExtractionAlgorithm();
			PageIterator pages = extractor.extract();
			while (pages.hasNext()) {
				Page page = pages.next();
				List<Rectangle> tablesOnPage = detectionAlgorithm.detect(page);
				if (tablesOnPage.size() > 0) {
					detectedTables.put(new Integer(page.getPageNumber()), tablesOnPage);
					System.out.println("Tables on Page " + page.getPageNumber() + " " + tablesOnPage.size());
					Table table = se.extract(page).get(1);
					for (List<RectangularTextContainer> row : table.getRows()) {
						if (row.get(0).getText().equals("Date"))
							continue;
						Transaction t = new Transaction();
						t.setTDate(new SimpleDateFormat("dd/MM/yyyy").parse(row.get(0).getText()));
						t.setNarration(row.get(1).getText());
						t.setRefNo(row.get(2).getText());
						t.setValueDate(new SimpleDateFormat("dd/MM/yyyy").parse(row.get(3).getText()));
						t.setWithdrawalAmount(new BigDecimal(row.get(4).getText().replaceAll(",", "")));
						t.setDepositAmount(new BigDecimal(row.get(5).getText().replaceAll(",", "")));
						t.setClosingBalance(new BigDecimal(row.get(6).getText().replaceAll(",", "")));
						tList.add(t);
					}
				}
			}
			//System.out.println("Found " + tList.size() + " Transactions");
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
		LocalDate sDate = transactions.get(0).getTDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		LocalDate eDate = transactions.get(transactions.size() - 1).getTDate().toInstant()
				.atZone(ZoneId.systemDefault()).toLocalDate();
		int months = Period.between(sDate, eDate).getYears() * 12 + Period.between(sDate, eDate).getMonths();
		System.out.println(sDate + " " + eDate + " " + months);
		summary.put("average", totalWithdrawal.divide(new BigDecimal(months)));
		System.out.println(summary);
		return summary;
	}
	
	public void findUPITransactions(List<Transaction> transactions) {
		List<Transaction> UPITransactions = new ArrayList<Transaction>();
		Map<String,Map<String,BigDecimal>> uniqueUpiIds = new HashMap<String, Map<String,BigDecimal>>();
		for (Transaction t : transactions) {
			if(t.getNarration().contains("UPI")) {
				UPITransactions.add(t);
				//uniqueUpiIds.add(t.getNarration().split("-")[2]);
			}	
		}
		for(Transaction t: UPITransactions) {
			String upiID = t.getNarration().split("-")[2];
			if (uniqueUpiIds.containsKey(upiID)){
				Map<String,BigDecimal> transactionSumUp = uniqueUpiIds.get(upiID);
				transactionSumUp.put("to", transactionSumUp.get("to").add(t.getWithdrawalAmount()));
				transactionSumUp.put("from", transactionSumUp.get("from").add(t.getDepositAmount()));
				uniqueUpiIds.replace(upiID, transactionSumUp);
			}
			else {
				Map<String,BigDecimal> transactionSumUp = new HashMap<String, BigDecimal>();
				transactionSumUp.put("to", t.getWithdrawalAmount());
				transactionSumUp.put("from", t.getDepositAmount());
				uniqueUpiIds.put(upiID, transactionSumUp);
			}
		}
		Map<String,Map<String,BigDecimal>> uniqueUpiIdsSorted = new TreeMap<String, Map<String,BigDecimal>>(uniqueUpiIds);
		//System.out.println(uniqueUpiIdsSorted);
	}
	public Map<String,Classification> classifier1(List<Transaction> transactions) {
		Map<String,String[]> keyWords = new HashMap<String, String[]>();
		String[] shoppingKeys = {"AMAZON","FLIPKART","BIGBASKET","BRAND FACTORY","TRENDS","DECATHLON","PROTEINS","FRUITS","PAYTM"};
		String[] travelKeys = {"IRCTC","GOIBIBO","UBER","ABHIBUSS","FUEL","PETROLEUM","RAILWAYS","MAKEMYTRIP"};
		String[] entertainmentKeys = {"CARNIVAL","BOOKMYSHOW","NETFLIX","YOUTUBE","HOTSTAR","ORIGIN","SPOTIFY","GOOGLE"};
		String[] foodKeys = {"HOTEL","SWIGGY","ZOMATO","CAKE","FAASOS","MC DONALDS","TASMAC","RESTAURANT","DOMINOS"};
		String[] rentKeys = {"ZOLO","RENT"};
		String[] investmentKeys = {"ZERODHA"};
		List<Transaction> shopping = new ArrayList<Transaction>();
		List<Transaction> entertainment = new ArrayList<Transaction>();
		List<Transaction> rent = new ArrayList<Transaction>();
		List<Transaction> investment = new ArrayList<Transaction>();
		List<Transaction> travel = new ArrayList<Transaction>();
		List<Transaction> food = new ArrayList<Transaction>();
		List<Transaction> others = new ArrayList<Transaction>();
		for(Transaction t: transactions) {
			String narration = t.getNarration();
			if(searchForKey(shoppingKeys, narration))
					shopping.add(t);
			else if(searchForKey(travelKeys, narration))
					travel.add(t);
			else if(searchForKey(entertainmentKeys, narration))
					entertainment.add(t);
			else if(searchForKey(foodKeys, narration))
				food.add(t);
			else if(searchForKey(rentKeys, narration))
				rent.add(t);
			else if(searchForKey(investmentKeys, narration))
				investment.add(t);
			else 
				others.add(t);
		}
		BigDecimal shoppingTotal = findTotalExpense(shopping);
		BigDecimal entertainmentTotal = findTotalExpense(entertainment);
		BigDecimal travelTotal = findTotalExpense(travel);
		BigDecimal foodTotal = findTotalExpense(food);
		BigDecimal rentTotal = findTotalExpense(rent);
		BigDecimal investmentTotal = findTotalExpense(investment);
		BigDecimal otherTotal = findTotalExpense(others);
		magicWordFinder(others);
		BigDecimal total = shoppingTotal.add(entertainmentTotal).
				add(foodTotal).
				add(entertainmentTotal).
				add(travelTotal).
				add(rentTotal).
				add(investmentTotal).
				add(otherTotal);
		Classification shoppingClassification = new Classification("Shopping",shoppingTotal,findPercent(shoppingTotal, total),shopping.size(),shopping);
		Classification entertainmentClassification = new Classification("Entertainment",entertainmentTotal,findPercent(entertainmentTotal, total),entertainment.size(),entertainment);
		Classification travelClassification = new Classification("Travel",travelTotal,findPercent(travelTotal, total),travel.size(),travel);
		Classification foodClassification = new Classification("Food",foodTotal,findPercent(foodTotal, total),food.size(),food);
		Classification rentClassification = new Classification("Rent",rentTotal,findPercent(rentTotal, total),rent.size(),rent);
		Classification investmentClassification = new Classification("Investment",investmentTotal,findPercent(investmentTotal, total),investment.size(),investment);
		Classification otherClassification = new Classification("Other", otherTotal, findPercent(otherTotal, total), others.size(), others);
		System.out.println("Shopping Total: "+shoppingTotal+" Percent: "+findPercent(shoppingTotal, total)+" Count "+shopping.size());
		System.out.println("Entertainment Total: "+entertainmentTotal+" Percent: "+findPercent(entertainmentTotal, total)+" Count "+entertainment.size());
		System.out.println("Travel Total: "+travelTotal+" Percent: "+findPercent(travelTotal, total)+" Count "+travel.size());
		System.out.println("Food Total: "+foodTotal+" Percent: "+findPercent(foodTotal, total)+" Count "+food.size());
		System.out.println("Rent Total: "+rentTotal+" Percent: "+findPercent(rentTotal, total)+" Count "+rent.size());
		System.out.println("Investment Total: "+investmentTotal+" Percent: "+findPercent(investmentTotal, total)+" Count "+investment.size());
		System.out.println("Other Total: "+otherTotal+" Percent: "+findPercent(otherTotal, total)+" Count "+others.size());	
		Map<String,Classification> classification = new HashMap<String, Classification>();
		classification.put("Shopping", shoppingClassification);
		classification.put("Entertainment", entertainmentClassification);
		classification.put("Travel", travelClassification);
		classification.put("Food", foodClassification);
		classification.put("Rent", rentClassification);
		classification.put("Investment", investmentClassification);
		classification.put("Other", otherClassification);
		return classification;
	}
	public Boolean searchForKey(String [] keys,String narration) {
		for(String key:keys) 
			if(narration.contains(key))
				return true;
		return false;
	}
	public BigDecimal findTotalExpense(List<Transaction> classificaation) {
		BigDecimal total = new BigDecimal(0);
		for(Transaction t: classificaation) {
			total = total.add(t.getWithdrawalAmount());
		}
		return total;
	}
	public BigDecimal findPercent(BigDecimal sum,BigDecimal total) {
		BigDecimal multiplicand = new BigDecimal("100.0");
		return sum.divide(total,RoundingMode.HALF_UP).multiply(multiplicand);
	}
	public void magicWordFinder(List<Transaction> otherList) {
		Map<String,Integer> countMap = new HashMap<String, Integer>();
		otherList.forEach(transaction->{
			String narration = transaction.getNarration();
			for(String word: narration.split(" ")) {
				if(countMap.containsKey(word))
					countMap.replace(word, countMap.get(word)+1);
				else
					countMap.put(word, 1);
			}
		});
		List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String,Integer>>(countMap.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String,Integer>>() {
			public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer>o2) {
				return (o1.getValue()).compareTo(o2.getValue());
			}
		});
		HashMap<String, Integer> temp = new LinkedHashMap<String, Integer>();
		for(Map.Entry<String, Integer> a: list) {
			temp.put(a.getKey(), a.getValue());
		}
		System.out.println(temp);
	}
}
