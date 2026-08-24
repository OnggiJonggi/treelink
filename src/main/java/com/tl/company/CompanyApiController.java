package com.tl.company;

import java.util.HashSet;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tl.company.eval.EvalService;
import com.tl.company.eval.EvalVO;
import com.tl.global.common.SearchResultVO;
import com.tl.global.file.EvalDocVO;
import com.tl.global.security.CryptoComponent;
import com.tl.global.security.CustomUserDetails;
import com.tl.global.security.role.CanAccess;
import com.tl.global.security.role.HasRole;
import com.tl.global.security.role.RoleEnum;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
@Slf4j
public class CompanyApiController {
	private final BusinessNoCheckService businessNoCheckService;
	private final CompanyService companyService;
	private final EvalService evalService;
	private final CryptoComponent cryptoComponent;
	
	/**
	 * 사업자 등록번호 진위확인
	 * 
	 * 관리자
	 */
	@CanAccess(RoleEnum.ADMIN)
	@GetMapping("check-businessno")
	public ResponseEntity<String> checkBusinessNo(
			@Valid BusinessNoCheckVO.request businessNoCheckRequest
			,BindingResult bindingResult){
		
		if(bindingResult.hasErrors())
			return ResponseEntity.badRequest().build();
		
		businessNoCheckService.checkBusinessNo(businessNoCheckRequest);
		
		return ResponseEntity.ok().build();
	}
	
	/**
	 * 업체 목록
	 * 
	 * 비회원을 포함한 모든 권한
	 * 관리자 : 비활성화된 업체도 조회
	 */
	@GetMapping("")
	public ResponseEntity<SearchResultVO<CompanyVO.Detail>> goCompanyList(
			@ModelAttribute CompanyVO.Search companySearch,
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@HasRole(RoleEnum.ADMIN) boolean isAdmin
			) throws Exception{
		
		// 관리자 권한 없으면 활성화된 업체만 조회 가능
		if(!isAdmin) companySearch.setStatus(CompanyStatusEnum.ACTIVE);
		
		SearchResultVO<CompanyVO.Detail> result = companyService.getCompanyList(companySearch);
		
		// 식별번호 암호화
		for(CompanyVO.Detail company : result.getList()) {
			company.setEncCompanyNo(cryptoComponent.encrypt(company.getCompanyNo()));
			company.setCompanyNo(0);
		}
		
		return ResponseEntity.ok(result);
	}
	
	/**
	 * 업체 수정
	 * 
	 * 관리자
	 */
	@CanAccess(RoleEnum.ADMIN)
	@PutMapping("{encCompanyNo}")
	public ResponseEntity<Void> updateCompany(
			@PathVariable String encCompanyNo,
			@Valid CompanyVO.Registor company) throws Exception{
		
		// 업체 식별번호 복호화
		company.setCompanyNo(cryptoComponent.decrypt(encCompanyNo));
		
		companyService.updateCompany(company);
		
		return ResponseEntity.ok().build();
	}

	
	/**
	 * 업체 소개 생성/수정
	 * 
	 * 관리자
	 */
	@CanAccess(RoleEnum.ADMIN)
	@PutMapping("{encCompanyNo}/intro")
	public ResponseEntity<Void> insertIntro(
			@RequestParam String intro,
			@PathVariable String encCompanyNo,
			@AuthenticationPrincipal CustomUserDetails userDetails
			) throws Exception{
		
		int companyNo = cryptoComponent.decrypt(encCompanyNo);
		int memberNo = cryptoComponent.decrypt(userDetails.getEncMemberNo());
		
		companyService.updateIntro(intro, companyNo, memberNo);
		
		return ResponseEntity.ok().build();
	}
	
	/**
	 * 업체 위치 추가
	 * 
	 * 관리자
	 */
	@CanAccess(RoleEnum.ADMIN)
	@PostMapping("{encCompanyNo}/location")
	public ResponseEntity<Void> insertLocation(
			@PathVariable String encCompanyNo,
			@ModelAttribute @Valid CompanyVO.InsertLocation location
			) throws Exception{
		
		int companyNo = cryptoComponent.decrypt(encCompanyNo);
		location.setCompanyNo(companyNo);
		
		companyService.insertLocation(location);
		
		return ResponseEntity.ok().build();
	}
	
	/**
	 * 업체 위치 삭제
	 * 
	 * 관리자
	 */
	@CanAccess(RoleEnum.ADMIN)
	@DeleteMapping("{encCompanyNo}/location")
	public ResponseEntity<Void> deleteLocation(
			@PathVariable String encCompanyNo,
			@RequestParam String encLocationNo) throws Exception{
		
		int companyNo = cryptoComponent.decrypt(encCompanyNo);
		int locationNo = cryptoComponent.decrypt(encLocationNo);
		
		companyService.deleteLocation(companyNo, locationNo);
		
		return ResponseEntity.ok().build();
	}
	
	/**
	 * 업체 작업 현황의 페이징 바 조작
	 * 
	 * 비회원을 포함한 모든 권한
	 * 관리자 : 현황 추가/수정, 모든 작업 현황 조회
	 * 
	 * 업체가 진행 중 / 종료한 프로젝트 나열
	 * 현황 이름, 메모, 시작일, 종료일, 상태
	 * 공개 범위(VISIBLE)에 따라 보이는게 달라요
	 */
	@GetMapping("{encCompanyNo}/management")
	public ResponseEntity<ManagementVO.SearchResult> getManagement(
			@PathVariable String encCompanyNo,
			@AuthenticationPrincipal UserDetails userDetails,
			@RequestParam int page,
			@HasRole(RoleEnum.ADMIN) boolean isAdmin
			)throws Exception{
		
		int companyNo = cryptoComponent.decrypt(encCompanyNo);
		
		// 작업 현황
		ManagementVO.SearchResult result;
		
		ManagementVO.Search search = new ManagementVO.Search();
		search.setCompanyNo(companyNo);
		search.setPage(page);
		
		// 관리자 권한 : 전체 조회
		if(isAdmin) search.setVisible(true);
			
		// 권한 없으면 손가락이나 빠쇼
		else search.setVisible(false);
		
		result = companyService.getManagement(search);
		
		// 식별번호 암호화
		for(ManagementVO.Detail item : result.getResult().getList()) {
			item.setEncLocationNo(cryptoComponent.encrypt(item.getLocationNo()));
			item.setLocationNo(0);
		}
		
		return ResponseEntity.ok(result);
	}
	
	/**
	 * 새로운 작업 현황 추가
	 * 
	 * 관리자
	 */
	@CanAccess(RoleEnum.ADMIN)
	@PostMapping("{encCompanyNo}/management")
	public ResponseEntity<Void> insertManagement(
			@PathVariable String encCompanyNo,
			@ModelAttribute @Valid ManagementVO.Insert insert
			) throws Exception{
		
		int companyNo = cryptoComponent.decrypt(encCompanyNo);
		insert.setCompanyNo(companyNo);
		
		companyService.insertManagement(insert);
		
		return ResponseEntity.ok().build();
	}
	
	/**
	 * 작업 현황 수정
	 */
	@PutMapping("{encCompanyNo}/management")
	public ResponseEntity<Void> updateManagement(
			@PathVariable String encCompanyNo,
			@ModelAttribute @Valid ManagementVO.Insert insert,
			@RequestParam String encLocationNo) throws Exception{
		
		int companyNo = cryptoComponent.decrypt(encCompanyNo);
		int locationNo = cryptoComponent.decrypt(encLocationNo);
		
		insert.setCompanyNo(companyNo);
		
		companyService.updateManagement(insert, locationNo);
		
		return ResponseEntity.ok().build();
	}
	
	
	/**
	 * 평가 추가 / 수정
	 * 
	 * 관리자
	 * 평가자
	 * 
	 * @Parma EvalVO.Insert.actionReson : 
	 * 	EVALUATION_HISTORY 테이블에 들어가는 값으로
	 * 	신규 추가 시에도 프론트에서 넣어서 보내야 해요(@Valid때문에)
	 * 	지금은 'NEW'를 넣도록 해 뒀어요
	 * 
	 * 평가 수정 - 상위 항목 수정 없이 하위 항목만 수정한다면,
	 * 프론트는 상위 항목 점수를 수정해 보여주지만 수정된 점수를 보내주지는 않음
	 * 
	 * ☆☆☆☆☆유일하게 이 메서드만 @RequestBody 가 쓰여요!!
	 * 일반 파라미터 방식은 파라미터가 많아서 오류남
	 */
	@PostMapping("{encCompanyNo}/eval")
	public ResponseEntity<Void> insertEval(
			@PathVariable String encCompanyNo,
			@RequestBody @Valid EvalVO.Insert insert,
			@AuthenticationPrincipal CustomUserDetails userDetails
			) throws Exception{
		
		// 복호화
		int companyNo = cryptoComponent.decrypt(encCompanyNo);
		int memberNo = cryptoComponent.decrypt(userDetails.getEncMemberNo());
		insert.setEvaluationNo(companyNo);
		insert.setActionBy(memberNo);
		
		// 첨부파일 식별번호 암호화
		if(insert.getEncFileNos() != null && !insert.getEncFileNos().isEmpty()) {
			Set<Integer> fileNos = new HashSet<Integer>(); 
			
			for(String item : insert.getEncFileNos()) {
				int fileNo = cryptoComponent.decrypt(item);
				fileNos.add(fileNo);
			}
			
			insert.setFileNos(fileNos);
			insert.setEncFileNos(null);
		}
		
		// 평가 추가 / 수정
		evalService.insert(insert);
		
		return ResponseEntity.ok().build();
	}
	
	/**
	 * 평가 기록 목록 조회, 페이징 바 조작
	 * 
	 * HistoryDetail에 scores, files는 조회하지 않음
	 */
	@GetMapping("{encCompanyNo}/eval/history")
	public ResponseEntity<SearchResultVO<EvalVO.HistoryDetail>> getHistory(
			@PathVariable String encCompanyNo,
			@RequestParam(required = false) int page,
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@HasRole(RoleEnum.ADMIN) boolean isAdmin
			) throws Exception{
		
		int companyNo = cryptoComponent.decrypt(encCompanyNo);
		
		// 검색용 객체 생성
		EvalVO.Search search = new EvalVO.Search();
		search.setEvaluationNo(companyNo);
		search.setPage(page);
		
		// 모든 업체 조회
		if(isAdmin) search.setAll(true);
		
		// 활성화된 업체만 조회
		else search.setAll(false);
		
		// 검색
		SearchResultVO<EvalVO.HistoryDetail> result = evalService.search(search);
		
		return ResponseEntity.ok(result);
	}
	
	/**
	 * 리비전 조회
	 * 
	 * 비회원을 포함한 모든 권한 : 활성화된 업체 리비전 조회
	 * 관리자 : 모든 업체 리비전 조회
	 */
	@GetMapping("{encCompanyNo}/eval/history/{revisionNo}")
	public ResponseEntity<EvalVO.HistoryDetail> getRevision(
			@PathVariable String encCompanyNo,
			@PathVariable int revisionNo,
			@AuthenticationPrincipal UserDetails userDetails,
			@HasRole(RoleEnum.ADMIN) boolean isAdmin
			) throws Exception{
		int companyNo = cryptoComponent.decrypt(encCompanyNo);
		
		EvalVO.HistoryDetail result;
		
		// 관리자 권한이면 모든 업체 리비전 조회
		if(isAdmin) result  = evalService.getRevision(companyNo, revisionNo, true);
		
		// 관리자 아니면 활성화된 업체 리비전 조회
		else result = evalService.getRevision(companyNo, revisionNo, false);
		
		// 없으면 404
		if(result==null) return ResponseEntity.notFound().build();
		
		// 식별번호 지우기
		result.setEvaluationNo(0);
		
		// 파일 식별번호 암호화
		if(result.getFiles() != null && !result.getFiles().isEmpty()) {
			for(EvalDocVO.Detail item : result.getFiles()) {
				item.setEncFileNo(cryptoComponent.encrypt(item.getFileNo()));
				item.setFileNo(0);
			}
		}
		
		return ResponseEntity.ok(result);
	}
}
