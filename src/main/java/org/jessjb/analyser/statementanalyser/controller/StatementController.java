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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
	List<Transaction> tList = new ArrayList<Transaction>();

	@GetMapping("/getExpenseData")
	public List<Transaction> test() {
		File pdfFile = new File(
				"C:\\Users\\jessj\\Downloads\\5010XXXXXX0260_4d008e10_30Sep2018_TO_26Jun2020_084838941_unlocked.pdf");
		if (tList.isEmpty()) {
			try {
				PDDocument pdfDocument = PDDocument.load(pdfFile);
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
		}
		return tList;
	}
}
