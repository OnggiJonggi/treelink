package com.tl.global.common;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/**
 * 사용자 입력값 소독
 */
@Component
public class SanitizeComponent {

	/**
	 * summernote입력값 소독해요
	 * 
	 * 사용자가 HTML 태그 사용이 가능한 입력칸에 멋대로 나쁜 태그를 으쌰으쌰하면 떽!하는 메소드지요
	 * summernote에 사용하는 태그는 통과하지요
	 */
	private static final Safelist POLICY = Safelist.relaxed()
		    .removeProtocols("img", "src", "http", "https")
		    .addProtocols("a", "href", "http", "https")
		    .addAttributes("a", "target")
		    .addEnforcedAttribute("a", "rel", "noopener noreferrer")
		    .addTags("s", "u", "font")
		    .addAttributes("font", "color")
		    .addAttributes("u", "style")
		    .addAttributes("p", "style")
		    .addAttributes("span", "style")
		    .addAttributes("div", "style")
		    .addAttributes(":all", "class");

	public String summernote(String rawHtml) {
		
		// 없는데 여길 왜 와?
		if (rawHtml == null || rawHtml.isBlank())
			return "";

		String cleaned = Jsoup.clean(rawHtml, "/", POLICY.preserveRelativeLinks(true));
		
		// data: URI 이미지 제거 필터
		Document doc = Jsoup.parse(cleaned);
		doc.select("img[src~=(?i)^data:]").remove();

		return doc.body().html();
	}
	
	/**
	 * 검색어 소독
	 * 
	 * 클라이언트에서 DB로 LIKE문법을 사용하는 조회를 할 때 사용
	 * 
	 * 1. trim()
	 * 2. 최대 길이 제한 - 엄청난 길이의 쿼리스트링으로 DB괴롭히기 멈춰!
	 * 3. LIKE예약어(%,_) 이스케이프
	 * 
	 * @param 검색 문자열
	 * @param 최대 허용 길이(정규식 저장소에서 얻어냄)
	 * @return 소독된 문자열
	 */
	public String searchWord(String searchWord, int maxLength) {
		
		// 없으면 가라
		if (searchWord == null) return null;

		// trim()
		searchWord = searchWord.trim();

		// 길면 싹둑
		if (searchWord.length() > maxLength)
			searchWord = searchWord.substring(0, maxLength);

		// 이스케이프 문자 : '/'
		searchWord = searchWord
				.replace("/", "//")
				.replace("%", "/%")
				.replace("_", "/_");

		return searchWord;
	}
	
	
	/**
	 * 검색어 소독
	 * 
	 * 클라이언트에서 DB로 LIKE문법을 사용하지 않는 조회를 할 때 사용
	 * 
	 * 1. trim()
	 * 2. 최대 길이 제한
	 * 
	 * @param 검색 문자열
	 * @param 최대 허용 길이(정규식 저장소에서 얻어냄)
	 * @return 소독된 문자열
	 */
	public String searchWordNotLike(String searchWord, int maxLength) {
		
		// 없으면 가라
		if (searchWord == null) return null;
		
		// trim()
		searchWord = searchWord.trim();
		
		// 길면 잘라
		if (searchWord.length() > maxLength)
			searchWord = searchWord.substring(0, maxLength);
		
		return searchWord;
	}
}
