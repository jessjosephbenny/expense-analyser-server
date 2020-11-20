package org.jessjb.analyser.statementanalyser.service.core;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
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
public class PatternService {
	public Map<String, String> detectPattern(File pdfFile) {
		Map<String, String> pattern = new HashMap<String, String>();
		PDDocument pdfDocument = null;
		try {
			pdfDocument = PDDocument.load(pdfFile);
			ObjectExtractor extractor = new ObjectExtractor(pdfDocument);
			ExtractionAlgorithm extractionAlgorithm = null;
			NurminenDetectionAlgorithm detectionAlgorithm = new NurminenDetectionAlgorithm();
			SpreadsheetExtractionAlgorithm sea = new SpreadsheetExtractionAlgorithm();
			BasicExtractionAlgorithm bea = new BasicExtractionAlgorithm();
			PageIterator pages = extractor.extract();
			Page page = pages.next();
			List<Rectangle> tablesOnPage = detectionAlgorithm.detect(page);
			if (tablesOnPage.size() > 0) {
				Rectangle geometicArea = tablesOnPage.get(0);
				Page tableArea = page.getArea(geometicArea);
				extractionAlgorithm = sea.isTabular(tableArea) ? sea : bea;
				Table table = extractionAlgorithm.extract(tableArea).get(0);
				if (table.getRowCount() > 0) {
					List<RectangularTextContainer> row = table.getRows().get(0);
					for (int i = 0; i < row.size(); i++) {
						RectangularTextContainer header = row.get(i);
						String detectedType = detectColumn(header.getText());
						if (detectedType != null) {
							if (pattern.containsKey(detectedType))
								continue;
							pattern.put(detectedType, String.valueOf(i));
						}
					}
				}
				if (pattern.containsKey("date")) {
					int columnPosition = Integer.parseInt(pattern.get("date"));
					ArrayList<String> dates = new ArrayList<String>();
					List<List<RectangularTextContainer>> rows = table.getRows();
					for (int i = 1; i < table.getRowCount(); i++) {
						if (!rows.get(i).get(columnPosition).getText().equals(""))
							dates.add(rows.get(i).get(columnPosition).getText());
					}
					String format = findDateFormat(dates);
					pattern.put("dateFormat", format);
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (pdfDocument != null)
				try {
					pdfDocument.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		return pattern;
	}

	public String detectColumn(String header) {
		String[] date = { "Date" };
		String[] narration = { "Narration", "Description" };
		String[] refNo = { "Chq", "Ref", "Cheque", "No." };
		String[] Withdrawal = { "Debit", "Withdrawal" };
		String[] deposit = { "Credit", "Deposit" };
		String[] balance = { "Balance" };
		if (searchForKeyWord(date, header))
			return "date";
		else if (searchForKeyWord(narration, header))
			return "narration";
		else if (searchForKeyWord(refNo, header))
			return "refNo";
		else if (searchForKeyWord(Withdrawal, header))
			return "withdrawal";
		else if (searchForKeyWord(deposit, header))
			return "deposit";
		else if (searchForKeyWord(balance, header))
			return "balance";
		return null;
	}

	public boolean searchForKeyWord(String[] keys, String header) {
		for (String key : keys)
			if (header.contains(key))
				return true;
		return false;
	}

	public String findDateFormat(ArrayList<String> dates) {
		// find delimiter
		String testDate = dates.get(0);
		CharSequence delimiter = testDate.contains("/") ? "/"
				: testDate.contains("-") ? "-" : testDate.contains(" ") ? " " : null;
		if (delimiter == null)
			return null;
		// Checking for albhabets
		String dateWithoutDelimitter = testDate.replace(delimiter, "");
		Boolean alphanumeric = null;
		try {
			Integer.parseInt(dateWithoutDelimitter);
			alphanumeric = false;
		} catch (NumberFormatException e) {
			alphanumeric = true;
		}
		return alphanumeric ? findDateFormarForAlphaNumeric(dates,delimiter) : findDateFormarForNumeric(dates, delimiter);
	}

	public String findDateFormarForNumeric(ArrayList<String> dates, CharSequence delimiter) {
		// finding Year Format
		String testDate = dates.get(0);
		Boolean formatYYYY = testDate.length() == 10 ? true : testDate.length() == 8 ? false : null;
		if (formatYYYY == null)
			return null;
		String[] format = { "", "", "" };
		String[] dateSplit = testDate.split(delimiter.toString());
		// Finding Year if Year Format is YYYY
		if (formatYYYY) {
			int yIndex = 999;
			for (int i = 0; i < dateSplit.length; i++) {
				if (dateSplit[i].length() == 4) {
					format[i] = "YYYY";
					yIndex = i;
				}
			}
			// Trying to find month with greater than 12 condition
			for (String date : dates) {
				String[] dateSplitTemp = date.split(delimiter.toString());
				for (int i = 0; i < dateSplitTemp.length; i++) {
					if (i == yIndex)
						continue;
					if (Integer.parseInt(dateSplitTemp[i]) > 12) {
						format[i] = "DD";
						for (int j = 0; i < 3; i++)
							if (format[j] == "") {
								format[j] = "MM";
								return format[0] + delimiter + format[1] + delimiter + format[2];
							}
					}
				}
			}
			// Trying to find date with increasing value
			int a, b;
			int aCounter = 0, bCounter = 0;
			if (yIndex == 1) {
				a = 0;
				b = 2;
			}
			if (yIndex == 2) {
				a = 0;
				b = 1;
			} else {
				a = 1;
				b = 2;
			}
			int lastA = Integer.parseInt(dates.get(0).split(delimiter.toString())[a]);
			int lastB = Integer.parseInt(dates.get(0).split(delimiter.toString())[b]);
			for (String date : dates) {
				String[] dateSplitTemp = date.split(delimiter.toString());
				if (lastA != Integer.parseInt(dateSplitTemp[a])) {
					lastA = Integer.parseInt(dateSplitTemp[a]);
					aCounter++;
				}
				if (lastB != Integer.parseInt(dateSplitTemp[b])) {
					lastB = Integer.parseInt(dateSplitTemp[b]);
					bCounter++;
				}
			}
			if (aCounter > bCounter) {
				format[a] = "DD";
				format[b] = "MM";
			} else if (aCounter < bCounter) {
				format[b] = "DD";
				format[a] = "MM";
			} else
				return null;
			return format[0] + delimiter + format[1] + delimiter + format[2];
		}
		// finding all with incrementCounter logic
		else {
			int aCounter = 0, bCounter = 0, cCounter = 0;
			int lastA = Integer.parseInt(dates.get(0).split(delimiter.toString())[0]);
			int lastB = Integer.parseInt(dates.get(0).split(delimiter.toString())[1]);
			int lastC = Integer.parseInt(dates.get(0).split(delimiter.toString())[2]);
			for (String date : dates) {
				if (lastA != Integer.parseInt(date.split(delimiter.toString())[0])) {
					aCounter++;
					lastA = Integer.parseInt(date.split(delimiter.toString())[0]);
				}
				if (lastB != Integer.parseInt(date.split(delimiter.toString())[1])) {
					bCounter++;
					lastB = Integer.parseInt(date.split(delimiter.toString())[1]);
				}
				if (lastC != Integer.parseInt(date.split(delimiter.toString())[2])) {
					cCounter++;
					lastC = Integer.parseInt(date.split(delimiter.toString())[2]);
				}
			}
			if (aCounter == bCounter && bCounter == cCounter)
				return null;
			if (aCounter > bCounter) {
				if (aCounter > cCounter) {
					format[0] = "DD";
					if (cCounter > bCounter) {
						format[1] = "YY";
						format[2] = "MM";
					} else if (bCounter > cCounter) {
						format[1] = "MM";
						format[2] = "YY";
					} else {
						if (lastC > lastB && lastB < 12) {
							format[1] = "MM";
							format[2] = "YY";
						} else if (lastB > lastC && lastC < 12) {
							format[1] = "YY";
							format[2] = "MM";
						} else {
							return null;
						}
					}

				} else if (cCounter > aCounter) {
					format[2] = "DD";
					format[0] = "MM";
					format[1] = "YY";
				} else {
					return null;
				}
			}
			if (bCounter > aCounter) {
				if (bCounter > cCounter) {
					format[1] = "DD";
					if (cCounter > aCounter) {
						format[2] = "MM";
						format[0] = "YY";
					} else if (aCounter > cCounter) {
						format[0] = "MM";
						format[2] = "YY";
					} else {
						if (lastC > lastA && lastA < 12) {
							format[0] = "MM";
							format[2] = "YY";
						} else if (lastC < lastA && lastC < 12) {
							format[2] = "MM";
							format[0] = "YY";
						} else {
							return null;
						}
					}
				} else if (cCounter > bCounter) {
					format[2] = "DD";
					format[1] = "MM";
					format[0] = "YY";
				} else {
					return null;
				}
			}
			if (cCounter > aCounter) {
				if (cCounter > bCounter) {
					format[2] = "DD";
					if (aCounter > bCounter) {
						format[0] = "MM";
						format[1] = "YY";
					} else if (bCounter > aCounter) {
						format[1] = "MM";
						format[0] = "YY";
					} else {
						if (lastA > lastB && lastB < 12) {
							format[1] = "MM";
							format[0] = "YY";
						} else if (lastA < lastB && lastA < 12) {
							format[0] = "MM";
							format[1] = "YY";
						} else {
							return null;
						}
					}
				}
				if (bCounter > cCounter) {
					format[1] = "DD";
					format[2] = "MM";
					format[0] = "YY";
				} else {
					return null;
				}
			}
			return format[0] + delimiter + format[1] + delimiter + format[2];
		}
	}

	public String findDateFormarForAlphaNumeric(ArrayList<String> dates, CharSequence delimiter) {
		String[] format = { "", "", "" };
		int YIndex = 999, Mindex = 999;
		// checking partsSize is 3
		if (dates.get(0).split(delimiter.toString()).length != 3)
			return null;
		// finding month
		for (int i = 0; i < 3; i++) {
			try {
				Integer.parseInt(dates.get(0).split(delimiter.toString())[i]);
			} catch (NumberFormatException e) {
				Mindex = i;
				if (dates.get(0).split(delimiter.toString())[i].length() == 3)
					format[i] = "MMM";
				else
					format[i] = "MMMM";
			}
		}
		// checking for YYYY
		for (int i = 0; i < 3; i++) {
			if (i == Mindex)
				continue;
			if (dates.get(0).split(delimiter.toString())[i].length() == 4) {
				format[i] = "YYYY";
				for (int j = 0; j < 3; j++)
					if (format[j].equals(""))
						format[j] = "DD";
				return format[0] + delimiter + format[1] + delimiter + format[2];
			}

		}
		// checking dates with single digit and also counter
		int a, b;
		if (Mindex == 0) {
			a = 1;
			b = 2;
		} else if (Mindex == 1) {
			a = 0;
			b = 2;
		} else {
			a = 0;
			b = 1;
		}
		if(dates.get(0).split(delimiter.toString())[a].length()==1) {
			format[a] = "DD";
			format[b] = "YY";
			return format[0] + delimiter + format[1] + delimiter + format[2];
		}
		if(dates.get(0).split(delimiter.toString())[b].length()==1) {
			format[b] = "DD";
			format[a] = "YY";
			return format[0] + delimiter + format[1] + delimiter + format[2];
		}
		int lastA = Integer.parseInt(dates.get(0).split(delimiter.toString())[a]);
		int lastB = Integer.parseInt(dates.get(0).split(delimiter.toString())[b]);
		int aCounter=0,bCounter=0;
		for (String date : dates) {
			String[] dateSplitTemp = date.split(delimiter.toString());
			if (lastA != Integer.parseInt(dateSplitTemp[a])) {
				if(dateSplitTemp[a].length()==1) {
					format[a] = "DD";
					format[b] = "YY";
					return format[0] + delimiter + format[1] + delimiter + format[2];
				}
				lastA = Integer.parseInt(dateSplitTemp[a]);
				aCounter++;
			}
			if (lastB != Integer.parseInt(dateSplitTemp[b])) {
				if(dateSplitTemp[b].length()==1) {
					format[b] = "DD";
					format[a] = "YY";
					return format[0] + delimiter + format[1] + delimiter + format[2];
				}
				lastB = Integer.parseInt(dateSplitTemp[b]);
				bCounter++;
			}
		}
		if(aCounter>bCounter) {
			format[a] = "DD";
			format[b] = "YY";
		}
		else if(aCounter<bCounter) {
			format[b] = "DD";
			format[a] = "YY";
		}
		else {
			return null;
		}
		return format[0] + delimiter + format[1] + delimiter + format[2];
	}
}
