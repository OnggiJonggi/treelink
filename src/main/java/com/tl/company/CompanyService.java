package com.tl.company;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.tl.global.common.ExtractFromHtml;
import com.tl.global.common.SanitizeComponent;
import com.tl.global.common.SearchResultVO;
import com.tl.global.exception.CustomException;
import com.tl.global.exception.ErrorCodeEnum;
import com.tl.global.file.CompanyDocMapper;
import com.tl.global.file.FileInfoVO;
import com.tl.global.file.FileMapper;
import com.tl.global.file.component.FileStatusEnum;
import com.tl.global.location.LocationMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyService {
	private final CompanyMapper companyMapper;
	private final CompanyDocMapper companyDocMapper;
	private final FileMapper fileMapper;
	private final LocationMapper locationMapper;
	private final SanitizeComponent sanitizeComponent;
	private final ExtractFromHtml extractFromHtml;
	
	/**
	 * 업체 조회
	 */
	public SearchResultVO<CompanyVO.Detail> getCompanyList(CompanyVO.Search companySearch) {
		
		// 검색어 소독
		companySearch.setBusinessNo(sanitizeComponent.searchWord(companySearch.getBusinessNo(), CompanyRegexp.BUSINESS_NO_LENGTH));
		companySearch.setCompanyName(sanitizeComponent.searchWord(companySearch.getCompanyName(), CompanyRegexp.COMPANY_NAME_MAX_LENGTH));
		companySearch.setRepresentativeName(sanitizeComponent.searchWord(companySearch.getRepresentativeName(), CompanyRegexp.REPRESENTATIVE_NAME_MAX_LENGTH));
		companySearch.setOption(sanitizeComponent.searchWord(companySearch.getOption(), CompanyRegexp.OPTION_MAX_LENGTH));
		companySearch.setEtcMemo(sanitizeComponent.searchWord(companySearch.getEtcMemo(), CompanyRegexp.ETC_MEMO_MAX_LENGTH));
		
		// 목록 조회
		List<CompanyVO.Detail> result = companyMapper.selectList(companySearch);
		
		// 검색 수
		int totalCount = companyMapper.selectListTotalCount(companySearch);
		
		// searchResult로 감싸기
		SearchResultVO<CompanyVO.Detail> searchResult = new SearchResultVO<CompanyVO.Detail>(
				result, totalCount, companySearch.getPage());
		
		return searchResult;
	}

	
	/**
	 * 업체 등록
	 */
	@Transactional
	public int companyRegistor(CompanyVO.Registor companyRegistor) {
		// 업체 등록
		int result1 = companyMapper.insertCompany(companyRegistor);
		
		// 오류!
		if(result1 == 0)
			throw new CustomException(ErrorCodeEnum.FAILED_CREATE_COMPANY);
		
		
		// 주 종목 등록
		if(companyRegistor.getOption() != null && !companyRegistor.getOption().isEmpty()) {
			
			int result2 = companyMapper.insertCompanySpecialty(
					companyRegistor.getCompanyNo(),
					companyRegistor.getOption(),
					companyRegistor.getEtcMemo());
			
			if(result2 == 0)
				throw new CustomException(ErrorCodeEnum.FAILED_CREATE_COMPANY_SPECIALTY);
		}
		
		// 업체 식별번호 반환
		return companyRegistor.getCompanyNo();
	}

	/**
	 * 업체 기본 정보 조회
	 */
	public CompanyVO.Detail getCompanyBasicInfo(int companyNo) {
		CompanyVO.Detail result = companyMapper.selectCompanyDetail(companyNo);
		return result;
	}


	/**
	 * 업체 기본정보 업데이트
	 */
	@Transactional
	public void updateCompany(CompanyVO.Registor company) {
		
		// 기본정보 업데이트
		int result1 = companyMapper.updateCompany(company);
		if(result1 == 0) throw new CustomException(ErrorCodeEnum.FAILED_UPDATE_COMPANY);
		
		// 주 종목 삭제
		companyMapper.deleteCompanySpecialty(company.getCompanyNo());
		
		// 갈아치울 주 종목이 있으면 갈아버려
		if(company.getOption() != null && !company.getOption().isEmpty()) {
			
			// 주 종목 새로 삽입
			int result3 = companyMapper.insertCompanySpecialty(
					company.getCompanyNo(),
					company.getOption(),
					company.getEtcMemo());
			
			// 오류!
			if(result3 == 0) throw new CustomException(ErrorCodeEnum.FAILED_CREATE_COMPANY_SPECIALTY);
		}
	}


	/**
	 * 업체 소개문 보기
	 */
	public String getIntro(int companyNo, CompanyStatusEnum status) {
		String intro = companyMapper.selectIntro(companyNo, status);
		return intro;
	}


	/**
	 * 업체 소개문 생성 / 수정
	 * 
	 * 1. summernote로 작성한 내용 소독
	 * 2. COMPANY.INTRO에 반영
	 * 3. 소개문에서 이미지 파일 이름 추출
	 * 4. 사용하지 않은 이미지 파일 미사용 로그 생성
	 */
	@Transactional
	public void updateIntro(String intro, int companyNo, int memberNo) {
		
		// 나쁜 태그 대롱대롱 하지요
		intro = sanitizeComponent.summernote(intro);
		
		// 소개문 업데이트
		int result = companyMapper.updateIntro(companyNo, intro);
		if(result==0) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
		
		// 소개문에서 이미지 파일 이름 추출하기
		List<String> imageName = extractFromHtml.fileNameFromIntro(intro);
		
		// 비활성 대상 파일 정보 불러오기
		List<FileInfoVO.History> unusedIntroImage = companyDocMapper.selectUnusedIntro(companyNo, imageName);
		
		// 비활성 대상 파일이 있을 경우
		if(unusedIntroImage !=null && !unusedIntroImage.isEmpty()) {
			
			// 파일 식별번호만 추출 
			List<Integer> fileNo = unusedIntroImage.stream()
					.filter(h -> h.getFileNo() != null) // null제외
			        .map(FileInfoVO.History :: getFileNo)
			        .toList(); 
			
			// 사용하지 않은 이미지 파일 FILE_INFO에서 지우기
			int result1 = companyDocMapper.deleteUnusedIntroImage(fileNo);
			
			// 비활성 대상 파일과 비활성화 한 파일 수가 같지 않으면 오류!
			if(result1 != fileNo.size()) throw new CustomException(ErrorCodeEnum.FAILED_DELETE_UNUSED_INTRO_IMAGAE);
			
			// FILE_HISTORY에 미사용 로그 추가
			for(FileInfoVO.History item : unusedIntroImage) {
				item.setFileNo(null);
				item.setActionBy(memberNo);
				item.setAction(FileStatusEnum.UNUSED);
				
				// FILE_HISTORY 추가
				// 시퀀스 + insert all 혹은 foreach + insert는 오류 발생
				// 그러니 개별 행 삽입 요청
				int result3 = fileMapper.insertHistory(item);
				if(result3 == 0) throw new CustomException(ErrorCodeEnum.FAILED_CREATE_FILE_HISTORY);
			}
		}
	}
	
	/**
	 * 이 업체 위치 내놔
	 */
	public List<CompanyVO.LocationDetail> getLocaions(int companyNo) {

		List<CompanyVO.LocationDetail> result = companyMapper.selectCompanyLocations(companyNo);

		return result;
	}

	/**
	 * 업체 위치 추가하실게요
	 */
	@Transactional
	public void insertLocation(CompanyVO.InsertLocation companyLocation) {
		
		// LOCATION 테이블 삽입
		int result1 = locationMapper.insert(companyLocation);
		if(result1 == 0) throw new CustomException(ErrorCodeEnum.FAILED_CREATE_LOCATION);
		
		// COMPANY_LOCATION 테이블 삽입
		int result2 = companyMapper.insertLocation(companyLocation);
		if(result2 == 0) throw new CustomException(ErrorCodeEnum.FAILED_CREATE_COMPANY_LOCATION);
		
	}

	/**
	 * 업체 위치 삭제
	 */
	@Transactional
	public void deleteLocation(int companyNo, int locationNo) {
		
		// LOCATION 테이블 삭제
		int result1 = companyMapper.deleteLocation(companyNo, locationNo);
		if(result1 == 0) throw new CustomException(ErrorCodeEnum.FAILED_DELETE_LOCATION);
		
		// COMPANY_LOCATION 테이블 삭제
		int result2 = locationMapper.delete(locationNo);
		if(result2 == 0) throw new CustomException(ErrorCodeEnum.FAILED_DELETE_COMPANY_LOCATION);
	}


	/**
	 * 관리 현황 조회
	 * 
	 * isAll이 false면 VISIBLE이 ALL만 조회
	 * SEMI면 전체 현황 수에는 포함, 세부 조회는 안함
	 * HIDDEN이면 전체 현황 수에도 미포함
	 * visible이 true이면 semiCount = -1, allCount = -1
	 */
	public ManagementVO.SearchResult getManagement(ManagementVO.Search search) {
		
		// 조회
		List<ManagementVO.Detail> result = companyMapper.selectManagement(search);
		
		// 전체 현황 수
		ManagementVO.SearchCount count = companyMapper.selectManagementTotalCount(search);
		
		// searchResult로 감싸기
		SearchResultVO<ManagementVO.Detail> searchResult = new SearchResultVO<ManagementVO.Detail>(
				result, count.getTotalCount(), search.getPage()
				);
		ManagementVO.SearchResult searchResult2 = new ManagementVO.SearchResult(
				searchResult, count.getSemiCount());
		
		return searchResult2;
	}


	/**
	 * 작업 현황 추가
	 */
	public void insertManagement(ManagementVO.Insert insert) {
		
		int result1 = locationMapper.insert(insert.getLocation());
		if(result1==0) throw new CustomException(ErrorCodeEnum.FAILED_CREATE_LOCATION);
		
		int result2 = companyMapper.insertManagement(insert);
		if(result2==0) throw new CustomException(ErrorCodeEnum.FAILED_CREATE_COMPANY_MANAGEMNET);
	}
	
	/**
	 * 작업 현황 삭제
	 * 
	 * LOCATION 삭제 후 전체 재등록
	 */
	public void updateManagement(ManagementVO.Insert insert, int locationNo) {
		
		/**
		 * LOCATION 테이블에서 행 날리기
		 * MANAGEMENT_STATUS은 LOCATION과 ON DELETE CASCADE으로 연결되었으므로
		 * MANAGEMENT_STATUS 행도 같이 날라감
		 */
		int result1 = companyMapper.deleteLocationByCompanyNo(insert.getCompanyNo(), locationNo);
		if(result1==0) throw new CustomException(ErrorCodeEnum.FAILED_DELETE_LOCATION);
		
		int result2 = locationMapper.insert(insert.getLocation());
		if(result2==0) throw new CustomException(ErrorCodeEnum.FAILED_CREATE_LOCATION);
	
		int result3 = companyMapper.insertManagement(insert);
		if(result3==0) throw new CustomException(ErrorCodeEnum.FAILED_CREATE_COMPANY_MANAGEMNET);
	}
}
