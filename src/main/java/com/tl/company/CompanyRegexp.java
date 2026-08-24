package com.tl.company;

public class CompanyRegexp {
	// private생성자로 외부(Spring 포함)에서 객체 생성 못 하게 막음
	private CompanyRegexp() {}

	// 최대 길이 상수
	public static final int BUSINESS_NO_LENGTH = 10; // 고정 길이
	public static final int COMPANY_NAME_MAX_LENGTH = 100;
	public static final int REPRESENTATIVE_NAME_MAX_LENGTH = 10;
	public static final int PHONE_MAX_LENGTH = 15;
	public static final int EMAIL_MAX_LENGTH = 100;
	public static final int ETC_MEMO_MAX_LENGTH = 20;
	public static final int LOCATION_NAME_MAX_LENGTH = 20;
	
	public static final int OPTION_MAX_LENGTH = 15;

	// 정규식
	public static final String BUSINESS_NO_REGEXP = "^[0-9]{" + BUSINESS_NO_LENGTH + "}$";
	public static final String COMPANY_NAME_REGEXP = "^[ㄱ-ㅎ가-힣a-zA-Z0-9&.',\\-·]{1," + COMPANY_NAME_MAX_LENGTH + "}$";
	public static final String REPRESENTATIVE_NAME_REGEXP = "^[ㄱ-ㅎ가-힣a-zA-Z0-9]{1," + REPRESENTATIVE_NAME_MAX_LENGTH + "}$";
	public static final String PHONE_REGEXP = "^[0-9]{1," + PHONE_MAX_LENGTH + "}$";
	public static final String EMAIL_REGEXP = "^$|^(?=.{1," + EMAIL_MAX_LENGTH + "}$)[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$";
	public static final String ETC_MEMO_REGEXP = "^.{1," + ETC_MEMO_MAX_LENGTH + "}$";
	public static final String LOCATION_NAME_REGEXP = "^[ㄱ-ㅎ가-힣a-zA-Z0-9&.',\\-·]{1," + LOCATION_NAME_MAX_LENGTH + "}$";
}
