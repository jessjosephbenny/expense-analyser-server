package org.jessjb.analyser.statementanalyser.controller;

import org.jessjb.analyser.statementanalyser.entity.ApplicationUser;
import org.jessjb.analyser.statementanalyser.repository.ApplicationUserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {
	
	private ApplicationUserRepository applicationUserRepository;
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	
	public UserController (ApplicationUserRepository applicationUserRepository,BCryptPasswordEncoder bCryptPasswordEncoder) {
		this.applicationUserRepository = applicationUserRepository;
		this.bCryptPasswordEncoder = bCryptPasswordEncoder;
	}
	
	@PostMapping("/sign-up")
	public void signup(@RequestBody ApplicationUser user) {
		user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
		applicationUserRepository.save(user);
	}
	@PostMapping("/reset-password")
	public void resetPassword(@RequestBody ApplicationUser user) {
		ApplicationUser oldUser = applicationUserRepository.findByUsername(user.getUsername());
		oldUser.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
		applicationUserRepository.save(oldUser);
	}
}
