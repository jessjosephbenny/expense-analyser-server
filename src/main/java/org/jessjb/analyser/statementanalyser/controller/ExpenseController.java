package org.jessjb.analyser.statementanalyser.controller;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.jessjb.analyser.statementanalyser.service.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@CrossOrigin
@RestController
public class ExpenseController {
	
	@Autowired
	ExpenseService expenseService;
	
	@GetMapping("/getExpenseData")
	public Map<String,Object> getExpenseData(HttpServletRequest req) {
		return expenseService.getExpenseData(req.getRemoteUser());
	}
	
	@PostMapping("/uploadStatement")
	public Map<String,Object> handleStatementUpload(@RequestParam("file") MultipartFile file, @RequestParam("format") String format,HttpServletRequest req) throws IllegalStateException, IOException {
		System.out.println(file.getOriginalFilename());
		File pdfFile = new File(System.getProperty("java.io.tmpdir")+"/"+file.getOriginalFilename());
		file.transferTo(pdfFile);
		return expenseService.saveAndGetExpenseData(pdfFile,format,req.getRemoteUser());
	}
}
