package org.jessjb.analyser.statementanalyser.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jessjb.analyser.statementanalyser.data.Pattern;
import org.jessjb.analyser.statementanalyser.data.Transaction;
import org.jessjb.analyser.statementanalyser.entity.ExpenseData;
import org.jessjb.analyser.statementanalyser.repository.ExpenseDataRepository;
import org.jessjb.analyser.statementanalyser.service.core.AnalysisService;
import org.jessjb.analyser.statementanalyser.service.core.ClassificationService;
import org.jessjb.analyser.statementanalyser.service.core.ExtractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExpenseService {
	
	@Autowired
	ExtractionService extractionService;
	@Autowired
	ClassificationService classificationService;
	@Autowired 
	AnalysisService analysisService;
	@Autowired
	ExpenseDataRepository expenseDataRepository;
	
	public  Map<String,Object> getGuestData(File pdfFile,String format){
		Pattern pattern = expenseDataRepository.findByUsername("guest").getPatterns().get(format);
		System.out.println(pattern);
		List<Transaction> tList = extractionService.readTransactions(pdfFile,pattern);
		Map<String,Object> expenseData = new HashMap<String, Object>();
		expenseData.put("transactionData",tList);
		expenseData.put("summary",analysisService.getSummary(tList));
		expenseData.put("Classification", classificationService.classifier(tList));
		expenseData.put("dailyUsage",analysisService.expenseDailyData(tList));
		return expenseData;
		
	}
	public  Map<String,Object> getExpenseData(String username){
		
		Map<String,Object> expenseData = new HashMap<String, Object>();
		List<Transaction> tList = null;
		if(expenseDataRepository.existsByUsername(username)){
			tList = expenseDataRepository.findByUsername(username).getTransactions();
		}
		else {
			tList = new ArrayList<Transaction>();
			expenseData.put("transactionData",tList);
			return expenseData;
		}
		expenseData.put("transactionData",tList);
		expenseData.put("summary",analysisService.getSummary(tList));
		expenseData.put("Classification", classificationService.classifier(tList));
		expenseData.put("dailyUsage",analysisService.expenseDailyData(tList));
		expenseData.put("topKeywords",analysisService.findTopKeyword(tList));
		return expenseData;
	}
	
	public Map<String,Object> saveAndGetExpenseData(File pdfFile,String format,String username){
		Pattern pattern = expenseDataRepository.findByUsername("guest").getPatterns().get(format);
		List<Transaction> tList = extractionService.readTransactions(pdfFile,pattern);
		ExpenseData data = null;
		if(expenseDataRepository.existsByUsername(username)) {
			data = expenseDataRepository.findByUsername(username);
			List<Transaction> transactions = data.getTransactions();
			transactions.addAll(tList);
			expenseDataRepository.save(data);
		}
		else {
			data = new ExpenseData();
			data.setUsername(username);
			data.setTransactions(tList);
			expenseDataRepository.insert(data);
		}
		Map<String,Object> expenseData = new HashMap<String, Object>();
		expenseData.put("transactionData",data.getTransactions());
		expenseData.put("summary",analysisService.getSummary(data.getTransactions()));
		expenseData.put("Classification", classificationService.classifier(data.getTransactions()));
		expenseData.put("dailyUsage",analysisService.expenseDailyData(data.getTransactions()));
		expenseData.put("topKeywords",analysisService.findTopKeyword(tList));
		return expenseData;
	}
}
