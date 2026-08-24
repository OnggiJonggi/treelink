package com.tl.member;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tl.global.common.SanitizeComponent;
import com.tl.global.common.SearchResultVO;
import com.tl.global.exception.CustomException;
import com.tl.global.exception.ErrorCodeEnum;
import com.tl.global.security.CryptoComponent;
import com.tl.global.security.role.RoleEnum;

import lombok.RequiredArgsConstructor;

/**
 * 회원 계정 관리 클라쓰
 * 로그인/로그아웃은 MemberSecurityService가서 찾아라
 */
@Service
@RequiredArgsConstructor
public class MemberService{
	private final MemberMapper memberMapper;
	private final PasswordEncoder passwordEncoder;
	private final SanitizeComponent sanitizeComponent;
	private final CryptoComponent cryptoComponent;
	
	/**
	 * 회원 가입
	 */
	@Transactional
	public void join(MemberVO.Join memberJoin) {
		// 아이디 중복 검사
		if(memberMapper.selectCheckId(memberJoin.getUserId()) > 0 )
			throw new CustomException(ErrorCodeEnum.ID_IS_DUPLICATED);
		
		// 닉네임 중복 검사
		if(memberMapper.selectCheckNickname(memberJoin.getNickname()) > 0)
			throw new CustomException(ErrorCodeEnum.NICKNAME_IS_DUPLICATED);
		
		// 비번 암호화
		memberJoin.setUserPwd(passwordEncoder.encode(memberJoin.getUserPwd()));
		
		// DB에 추가하기
		if(memberMapper.insertJoin(memberJoin) ==0)
			throw new CustomException(ErrorCodeEnum.CANNOT_CREATE_MEMBER);

		return;
	}

	/**
	 * 아이디 중복확인
	 */
	public void checkId(String userId) {
		if(memberMapper.selectCheckId(userId) > 0)
			throw new CustomException(ErrorCodeEnum.ID_IS_DUPLICATED);
	}
	
	/**
	 * 닉네임 중복 확인
	 */
	public void checkNick(String nickname) {
		if(memberMapper.selectCheckNickname(nickname) > 0)
			throw new CustomException(ErrorCodeEnum.NICKNAME_IS_DUPLICATED);
	}
	

	/**
	 * 회원 목록 검색
	 */
	public SearchResultVO<MemberVO.Detail> getList(MemberVO.Search search) throws Exception {
		
		// 검색어 소독
		search.setUserId(sanitizeComponent.searchWord(search.getUserId(), MemberRegexp.ID_MAX_LENGTH));
		search.setName(sanitizeComponent.searchWord(search.getName(), MemberRegexp.NAME_MAX_LENGTH));
		search.setNickname(sanitizeComponent.searchWord(search.getNickname(), MemberRegexp.NAME_MAX_LENGTH));
		search.setPhone(sanitizeComponent.searchWord(search.getPhone(), MemberRegexp.PHONE_MAX_LENGTH));
		
		// 목록 조회
		List<MemberVO.Detail> result = memberMapper.selectMemberList(search);
		
		// 검색 결과 수
		int totalCount = memberMapper.selectMemberListTotalCount(search);

		// SearchResultVO로 감싸기
		SearchResultVO<MemberVO.Detail> searchResult = new SearchResultVO<MemberVO.Detail>(
				result, totalCount, search.getPage());
		
		// 회원 식별번호 암호화
		for(MemberVO.Detail member : searchResult.getList()) {
			member.setEncMemberNo(cryptoComponent.encrypt(member.getMemberNo()));
			member.setMemberNo(0);
		}
		
		return searchResult;
	}

	/**
	 * 회원 기본 정보 조회
	 */
	public MemberVO.Detail getBasicInfo(int memberNo) {
		return memberMapper.selectMember(memberNo);
	}

	/**
	 * 회원 기본 정보 수정
	 */
	public void updateMemberBasicInfo(MemberVO.Update member) {
		
		// 별명 중복 확인
		if(memberMapper.selectUpdatedNickname(member.getMemberNo(), member.getNickname()) > 0)
			throw new CustomException(ErrorCodeEnum.NICKNAME_IS_DUPLICATED);
		
		// 비번 바뀌었으면 암호화
		if(member.getUserPwd() != null && !member.getUserPwd().isEmpty())
			member.setUserPwd(passwordEncoder.encode(member.getUserPwd()));

		int result = memberMapper.updateMember(member);
		if(result==0) throw new CustomException(ErrorCodeEnum.FAILED_UPDATE_MEMBER);
	}

	/**
	 * 회원 권한 수정 
	 */
	@Transactional
	public void updateMemberRole(int memberNo, RoleEnum role) {
		
		// 권한 지워버려
		int deleteResult = memberMapper.deleteRole(memberNo);
		if(deleteResult==0) throw new CustomException(ErrorCodeEnum.FAILED_DELETE_ROLE);
		
		// 권한 생성하기
		int result = memberMapper.insertRole(memberNo, role);
		if(result==0) throw new CustomException(ErrorCodeEnum.FAILED_GRANT_ROLE);
	}

	/**
	 * 회원 상태 수정
	 */
	public void updateMemberStatus(int memberNo, MemberStatusEnum status) {
		int result = memberMapper.updateStatus(memberNo,status);
		if(result==0) throw new CustomException(ErrorCodeEnum.FAILED_UPDATE_MEMBER);
	}

	/**
	 * 닉네임 중복 확인 (수정용)
	 */
	public void checkUpdatedNickname(int memberNo, String nickname) {
		if(memberMapper.selectUpdatedNickname(memberNo, nickname) > 0)
			throw new CustomException(ErrorCodeEnum.NICKNAME_IS_DUPLICATED);
	}
}
