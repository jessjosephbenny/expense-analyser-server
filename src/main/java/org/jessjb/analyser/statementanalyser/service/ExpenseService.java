package org.jessjb.analyser.statementanalyser.service;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jessjb.analyser.statementanalyser.data.Transaction;
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
	
	public  Map<String,Object> getGuestData(File pdfFile){
		List<Transaction> tList = extractionService.readTransactions(pdfFile);
		Map<String,Object> expenseData = new HashMap<String, Object>();
		expenseData.put("transactionData",tList);
		expenseData.put("summary",analysisService.getSummary(tList));
		expenseData.put("Classification", classificationService.classifier(tList));
		expenseData.put("dailyUsage",analysisService.expenseDailyData(tList));
		return expenseData;
		
	}
}
