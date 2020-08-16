package org.jessjb.analyser.statementanalyser.service.core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import org.jessjb.analyser.statementanalyser.data.Classification;
import org.jessjb.analyser.statementanalyser.data.Transaction;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class ClassificationService {
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
	
	public String searchForKeyNew(ArrayList<String> keys, String narration) {
		for (String key : keys)
			if (narration.contains(key))
				return key;
		return null;
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
	
	public BigDecimal findPercent(BigDecimal sum, BigDecimal total) {
		BigDecimal multiplicand = new BigDecimal("100.0");
		return sum.divide(total, RoundingMode.HALF_UP).multiply(multiplicand);
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
}
