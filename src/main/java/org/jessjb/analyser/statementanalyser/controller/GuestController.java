package org.jessjb.analyser.statementanalyser.controller;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.jessjb.analyser.statementanalyser.service.ExpenseService;
import org.jessjb.analyser.statementanalyser.service.core.PatternService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/guest")
public class GuestController {

	@Autowired                    
	PatternService patternService;
	@Autowired 
	ExpenseService expenseService;
	
	@PostMapping("/predict")
	public Map<String,String> predict(@RequestParam("file") MultipartFile file) throws IllegalStateException, IOException{
		System.out.println(file.getOriginalFilename());
		File pdfFile = new File(System.getProperty("java.io.tmpdir")+"/"+file.getOriginalFilename());
		file.transferTo(pdfFile);
		return patternService.detectPattern(pdfFile);
	}
	@PostMapping("/uploadStatement")
	public Map<String,Object> handleStatementUpload(@RequestParam("file") MultipartFile file, @RequestParam("format") String format) throws IllegalStateException, IOException {
		System.out.println(file.getOriginalFilename());
		File pdfFile = new File(System.getProperty("java.io.tmpdir")+"/"+file.getOriginalFilename());
		file.transferTo(pdfFile);
		return expenseService.getGuestData(pdfFile,format);
	}
}
