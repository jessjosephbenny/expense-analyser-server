package org.jessjb.analyser.statementanalyser.service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.bouncycastle.jcajce.provider.asymmetric.edec.KeyFactorySpi.ED25519;
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
	public List<Transaction> readTransactions(File Filepath){
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
			System.out.println("Found " + tList.size() + " Transactions");
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
	public Map<String,BigDecimal> getSummary(List<Transaction> transactions){
		Map<String,BigDecimal> summary = new HashMap<String, BigDecimal>();
		BigDecimal totalDeposit = new BigDecimal(0);
		BigDecimal totalWithdrawal =new BigDecimal(0);
		for(Transaction t : transactions) {
			totalDeposit = totalDeposit.add(t.getDepositAmount());
			totalWithdrawal = totalWithdrawal.add(t.getWithdrawalAmount());
		}
		summary.put("totalDeposit", totalDeposit);
		summary.put("totalWithdrawal", totalWithdrawal);
		summary.put("balance", totalDeposit.subtract(totalWithdrawal));
		LocalDate sDate = transactions.get(0).getTDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		LocalDate eDate = transactions.get(transactions.size()-1).getTDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		int months = Period.between(sDate,eDate).getYears()*12 + Period.between(sDate, eDate).getMonths();
		System.out.println(sDate+" "+eDate+" "+months);
		summary.put("average", totalWithdrawal.divide(new BigDecimal(months)));
		System.out.println(summary);
		return summary;
	}
}
