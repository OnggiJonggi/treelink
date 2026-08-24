package com.tl.global.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PagingConstant {
	public static int PAGE_SIZE; // 페이지 당 데이터 수
	public static int BLOCK_SIZE; // 블록 당 페이지 수
	public static int MAX_PAGE_SIZE; // 최대 페이지 제한
	
    /**
     * properties 데이터 추출은 Bean으로 등록된 클래스만 가능
     * Bean은 클래스가 아닌 인스턴스라서
     * 클래스 자체에 속하는 static의 @Value 작동 안함
     * setter을 사용해 우회
     */
    @Value("${paging.page-size}")
    public void setPageSize(int pageSize) {
    		PagingConstant.PAGE_SIZE = pageSize;
    }
    
    @Value("${paging.block-size}")
    public void setBlockSize(int blockSize) {
    		PagingConstant.BLOCK_SIZE = blockSize;
    }
    
    @Value("${paging.max-page-size}")
    public void setMaxPageSize(int maxPageSize) {
    		PagingConstant.MAX_PAGE_SIZE = maxPageSize;
    }
}
