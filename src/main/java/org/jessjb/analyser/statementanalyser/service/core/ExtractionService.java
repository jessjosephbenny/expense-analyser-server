package org.jessjb.analyser.statementanalyser.service.core;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.jessjb.analyser.statementanalyser.data.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
public class ExtractionService {
	
	@Autowired
	ClassificationService classificationService;
	
	@SuppressWarnings("rawtypes")
	public List<Transaction> readTransactions(File Filepath) {
		List<Transaction> tList = new ArrayList<Transaction>();
		PDDocument pdfDocument = null;
		try {
			Map<String,ArrayList<String>> classificationKeyMap = classificationService.getClassificationsFromFile();
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
					//System.out.println("Tables on Page " + page.getPageNumber() + " " + tablesOnPage.size());
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
							if(!row.get(4).getText().equals(""))
								t.setWithdrawalAmount(new BigDecimal(row.get(4).getText().replaceAll(",", "")));
							else
								t.setWithdrawalAmount(new BigDecimal("0.00"));
							if(!row.get(5).getText().equals(""))
								t.setDepositAmount(new BigDecimal(row.get(5).getText().replaceAll(",", "")));
							else
								t.setDepositAmount(new BigDecimal("0.00"));
							t.setClosingBalance(new BigDecimal(row.get(6).getText().replaceAll(",", "")));
							String[] category = classificationService.findClassification(t, classificationKeyMap);
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
}
