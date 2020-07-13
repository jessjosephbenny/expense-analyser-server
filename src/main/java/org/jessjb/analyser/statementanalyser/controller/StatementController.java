package org.jessjb.analyser.statementanalyser.controller;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.jessjb.analyser.statementanalyser.data.Transaction;
import org.jessjb.analyser.statementanalyser.service.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import technology.tabula.HasText;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.PageIterator;
import technology.tabula.Rectangle;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;
import technology.tabula.detectors.NurminenDetectionAlgorithm;
import technology.tabula.extractors.BasicExtractionAlgorithm;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;
import technology.tabula.writers.CSVWriter;

@CrossOrigin
@RestController
public class StatementController {
	
	@Autowired
	AnalysisService analysisService;
	
	List<Transaction> tList = new ArrayList<Transaction>();

	@GetMapping("/getExpenseData")
	public Map<String,Object> getExpenseData() {
		File pdfFile = new File(
				"C:\\Users\\jessj\\Downloads\\5010XXXXXX0260_4d008e10_30Sep2018_TO_26Jun2020_084838941_unlocked.pdf");
		if (tList.isEmpty()) {
			tList = analysisService.readTransactions(pdfFile);
		}
		Map<String,BigDecimal> summary = analysisService.getSummary(tList);
		//analysisService.findUPITransactions(tList);
		//analysisService.classifier1(tList);
		Map<String,Object> expenseData = new HashMap<String, Object>();
		expenseData.put("transactionData",tList);
		expenseData.put("summary",summary);
		expenseData.put("Classification", analysisService.classifier1(tList));
		return expenseData;
	}
	@PostMapping("/uploadStatement")
	public Map<String,Object> handleStatementUpload(@RequestParam("file") MultipartFile file ) throws IllegalStateException, IOException {
		System.out.println(file.getOriginalFilename());
		File pdfFile = new File(System.getProperty("java.io.tmpdir")+"/"+file.getOriginalFilename());
		file.transferTo(pdfFile);
		if(tList.isEmpty()) {			
			tList = analysisService.readTransactions(pdfFile);
		}
		Map<String,BigDecimal> summary = analysisService.getSummary(tList);
		analysisService.findUPITransactions(tList);
		Map<String,Object> expenseData = new HashMap<String, Object>();
		expenseData.put("transactionData",tList);
		expenseData.put("summary",summary);
		expenseData.put("Classification", analysisService.classifier1(tList));
		pdfFile.delete();
		return expenseData;
	}
}
