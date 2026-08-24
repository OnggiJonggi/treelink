package com.tl.global.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.tl.global.exception.CustomException;
import com.tl.global.exception.ErrorCodeEnum;
import com.tl.global.security.role.CanAccess;
import com.tl.global.security.role.RoleEnum;

@Controller
public class MainController {
	
	/**
	 * 메인 화면으로 끄져라.
	 * 
	 * 비 로그인, 로그인, 관리자, 컨설턴트에 따른 페이지 분기 
	 */
	@GetMapping("")
	public String goMain() {
		return "common/main";
	}
	
	
	/**
	 * 관리자 전용 페이지로
	 */
	@CanAccess(RoleEnum.ADMIN)
	@GetMapping("admin/main")
	public String goAdminMain() {
		return "admin/main";
	}
	
	
	/**
	 * 에러페이지 바로가기
	 */
	@GetMapping({"4xx", "4XX"})
	public void error4xx() {
		throw new CustomException(ErrorCodeEnum.DUMMY_ERROR_CODE_4XX);
	}
	
	@GetMapping("400")
	public void error400() {
		throw new CustomException(ErrorCodeEnum.DUMMY_ERROR_CODE_400);
	}
	
	@GetMapping("401")
	public void error401() {
		throw new CustomException(ErrorCodeEnum.DUMMY_ERROR_CODE_401);
	}
	
	@GetMapping("403")
	public void error403() {
		throw new CustomException(ErrorCodeEnum.DUMMY_ERROR_CODE_403);
	}
	
	@GetMapping("404")
	public void error404() {
		throw new CustomException(ErrorCodeEnum.DUMMY_ERROR_CODE_404);
	}
	
	@GetMapping("405")
	public void error405() {
		throw new CustomException(ErrorCodeEnum.DUMMY_ERROR_CODE_405);
	}
	
	@GetMapping({"5xx", "5XX"})
	public void error5xx() {
		throw new CustomException(ErrorCodeEnum.DUMMY_ERROR_CODE_5XX);
	}
	
	@GetMapping("500")
	public void error500() {
		throw new CustomException(ErrorCodeEnum.DUMMY_ERROR_CODE_500);
	}
	
	@GetMapping("common-error")
	public void commonError() {
		throw new CustomException(ErrorCodeEnum.DUMMY_ERROR_CODE_ERROR);
	}
}
