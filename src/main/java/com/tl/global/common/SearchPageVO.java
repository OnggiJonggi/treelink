package com.tl.global.common;

import lombok.ToString;

/**
 * 검색 요청시 사용하는 VO
 * 이거 상속받아서 써라.
 */
@ToString
public class SearchPageVO {
    private int page = 1;
    // int startRow; - getter만 존재
    // int endRow; - getter만 존재

    public int getPage() {
    		return page;
	}
	public void setPage(int page) {
		
		// 페이지 하한, 상한 제한
		if(page < 1) {
			page = 1;
		} else if(page > PagingConstant.MAX_PAGE_SIZE) {
			page = PagingConstant.MAX_PAGE_SIZE;
		}
		
		this.page = page;
	}

    /**
     * startRow 검색 데이터 시작행
     * endRow 끝행
     * 계산 필드는 필드 없이 getter/setter만 작성해요
     */
    public int getStartRow() {
    		return (page - 1) * PagingConstant.PAGE_SIZE + 1;
    }
    public int getEndRow() {
    		return page * PagingConstant.PAGE_SIZE;
    }
}
