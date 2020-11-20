package org.jessjb.analyser.statementanalyser.security;

public class SecurityConstants {
	public static final String SECRET = "KeyExpenseFinderFor";
	public static final long EXPIRATION_TIME = 864_000_000;
	public static String TOKEN_PREFIX = "Bearer ";
	public static final String HEADER_STRING = "Authorization";
	public static final String SIGN_UP_URL = "/users/sign-up";
	public static final String[] PERMIT_ALL = {"/swagger-ui.html#","/v2/api-docs", "/configuration/**", "/swagger*/**", "/webjars/**","/guest/**"};
}
