package org.jessjb.analyser.statementanalyser.controller;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.jessjb.analyser.statementanalyser.data.Pattern;
import org.jessjb.analyser.statementanalyser.service.core.PatternService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/pattern")
public class PatternController {
	
	@Autowired
	PatternService patternService;
	
	@PostMapping("/detectcolumns")
	public Map<String,String> detectColoumns(@RequestParam("file") MultipartFile file) throws IllegalStateException, IOException {
		System.out.println(file.getOriginalFilename());
		File pdfFile = new File(System.getProperty("java.io.tmpdir")+"/"+file.getOriginalFilename());
		file.transferTo(pdfFile);
		return patternService.detectPattern(pdfFile);
	}
	@PostMapping("/savePattern")
	public Pattern savePattern(@RequestBody Pattern pattern) {
		System.out.println(pattern);
		return null;
	}
}
