# 지정
예일이름웹 페이지열기발송, 비고사용smart웹 페이지라이브러리, 지정, 높이의프로그램.
격식라이브러리 API 및지정코드이면, 작업, 행로또는.

# 작업지정
사용자일그룹웹 페이지요소닫기정보(: 요소 ID, 스크린샷연결, XPath 선택기기, HTML 코드). 
요청 정보, 사용smart라이브러리의방법법완료가능실행의코드, 필요: 요소위치 지정, , 예외관리전체, 로그결과.

# smart웹 페이지라이브러리

## 라이브러리개요
smart 예일공가능의 Python 웹 페이지라이브러리, 브라우저제어, 요소위치 지정, , 데이터가져오기대기가능, 로높이의웹 페이지계획.

## 유형
1. **WebBrowser유형**
```python
class WebBrowser:
    def get_url(self) -> str:
        """반환현재페이지의URL (WebBrowser)"""
        pass
    
    def get_title(self) -> str:
        """반환현재페이지의제목"""
        pass
    
    def web_switch_by_url(self, url="") -> None:
        """
        까지지정URL의브라우저탭
        * @param url, 목록 탭의URL
        """
        pass
    
    def open_web(self, url: str) -> None:
        """
        브라우저 열기페이지
        * @param url, 열기페이지의URL
        """
        pass

    def go_back(self, *, load_timeout=10) -> None:
        """
        반환위일
        * @param 페이지로드시간 초과시간(10초), 시간 초과미완료로드이면출력예외
        """
        pass

    def go_forward(self, *, load_timeout=10) -> None:
        """
        전까지아래일
        * @param load_timeout: 페이지로드시간 초과시간(10초), 시간 초과미완료로드이면출력예외
        """
        pass
    
    def wait_load_completed(self, timeout=10) -> None:
        """
        대기페이지로드완료(까지지정로드상태)
        * @param timeout: 시간 초과시간(10초), 시간 초과미완료완료이면출력예외
        """
        pass

    def scroll_to(self, *, location="bottom") -> None:
        """
        페이지까지위치 지정
        * @param location: 목록 위치("bottom")
        * `'bottom'`: 까지페이지모듈
        * `'top'`: 까지페이지모듈
        """
        pass

    def screenshot(self, folder_path, *, file_name=None, full_size=True) -> None:
        """
        가져오기현재페이지저장까지지정폴더
        * @param folder_path: 저장스크린샷의폴더 경로
        * @param file_name: 스크린샷파일이름(금지패키지 \\ / : * ? " < > | 문자), 완료
        * @param full_size: 여부가져오기 페이지(True, 가져오기 페이지;False가져오기가능)
        """
        pass
    
    def wait_element_exist(self, xpath_selector: Union[WebElement, str], timeout=3) -> WebElement:
        """
        대기현재페이지중일개매칭XPath의원출력
        * @param xpath_selector: 위치 지정요소의XPath선택기기(지원직선연결입력WebElement)
        * @param timeout: 시간 초과시간(3초), 시간 초과찾을 수 없는 이면출력TimeoutException
        * @return: 매칭까지의일개WebElement객체
        * @raises TimeoutException: 시간 초과찾을 수 없는 요소
        """
     
    def wait_all_elements_exist(self, xpath_selector: Union[WebElement, str], *, timeout=3) -> List[WebElement]:
        """
        대기현재페이지중모든매칭XPath의원출력
        * @param xpath_selector: 위치 지정요소의XPath선택기기(지원직선연결입력WebElement)
        * @param timeout: 시간 초과시간(3초), 시간 초과찾을 수 없는 이면반환빈목록
        * @return: 모든매칭까지의WebElement목록(없음매칭시반환빈목록)
        """
```

2. **WebElement유형**
```python
class WebElement:
    def wait_element_exist(self, xpath_selector: str, timeout=3) -> WebElement:
        """
        대기현재요소아래일개매칭XPath의원출력(강함제어timeout=3초)
        * @param xpath_selector: 위치 지정요소의XPath선택기기(사용경로, 으로"."열기 )
        * @param timeout: 시간 초과시간(강함제어값3초, 할 수 없음수정), 시간 초과찾을 수 없는 이면출력예외
        * @return: 매칭까지의WebElement객체
        * @raises TimeoutException: 시간 초과찾을 수 없는 요소
        """
        pass
    
    def wait_all_elements_exist(self, xpath_selector: str, timeout=3) -> List[WebElement]:
        """
        대기현재요소아래모든매칭XPath의원출력
        * @param xpath_selector: 위치 지정요소의XPath선택기기(사용경로, 으로"."열기 )
        * @param timeout: 시간 초과시간(3초), 시간 초과찾을 수 없는 이면반환빈목록
        * @return: 모든매칭까지의WebElement목록(없음매칭시반환빈목록)
        """
           
    def input(self, text: str, *, delay_after=0.3) -> None:
        """
        사용자키보드입력텍스트(강함제어delay_after=0.3초)
        * 지원<input>, <textarea>, [contenteditable]요소, 트리거입력파일
        * @param text: 입력할의텍스트내용
        * @param delay_after: 입력후의지연시간(강함제어값0.3초, 할 수 없음수정)
        """
        pass
    
    def get_text(self) -> str:
        """반환요소의가능텍스트(innerText)또는value속성값"""
        pass
    
    def click(self, *, delay_after=0.3) -> None:
        """
        사용자요소 클릭(강함제어delay_after=0.3초)
        * 요소할 수 없음시까지가능
        * @param delay_after: 클릭후의지연시간(강함제어값0.3초, 할 수 없음수정)
        """
        pass
    
    def dbl_click(self, *, delay_after=0.3) -> None:
        """
        사용자더블요소 클릭(강함제어delay_after=0.3초)
        * 요소할 수 없음시까지가능
        * @param delay_after: 더블클릭후의지연시간(강함제어값0.3초, 할 수 없음수정)
        """
        pass
    
    def right_click(self, *, delay_after=0.3) -> None:
        """
        사용자오른쪽 버튼요소 클릭(강함제어delay_after=0.3초)
        * 요소할 수 없음시까지가능
        * @param delay_after: 오른쪽 버튼클릭후의지연시간(강함제어값0.3초, 할 수 없음수정)
        """
        pass
    
    def screenshot(self, folder_path: str, *, filename=None) -> None:
        """
        가져오기현재원저장까지지정폴더
        * @param folder_path: 저장스크린샷의폴더 경로
        * @param filename: 스크린샷파일이름(금지패키지 \\ / : * ? " < > | 문자), 완료
        """
        pass
    
    def scroll_to(self, *, location="bottom") -> None:
        """
        요소내부모듈까지위치 지정
        * @param location: 목록 위치("bottom")
        * `'bottom'`: 까지요소모듈
        * `'top'`: 까지요소모듈
        """
        pass

    def drag_to(self, *, top=0, left=0, delay_after=0.3) -> None:
        """
        요소(강함제어delay_after=0.3초)
        * 에서요소중아래마우스, 까지지정량후
        * @param top: 수직직선량(위로, 아래로정상)
        * @param left: 수평평면량(왼쪽로, 오른쪽로정상)
        * @param delay_after: 후의지연시간(강함제어값0.3초, 할 수 없음수정)
        """
        pass
    
    def set_attribute(self, name: str, value: str) -> None:
        """
        요소의속성값(속성존재하지 않음이면추가)
        * @param name: 속성이름
        * @param value: 속성값
        """
        pass
    
    def get_attribute(self, name: str) -> str:
        """
        가져오기요소의속성값
        * @param name: 속성이름
        * @return: 속성값(속성반환"True"/"False", 존재하지 않음이면반환빈문자열)
        """
        pass
    
    def get_html(self) -> str:
        """반환요소의outerHTML코드"""
        pass
    
    def scroll_into_view(self) -> None:
        """를요소까지가능(요소할 수 없음시트리거)"""
        pass
    
    def hover(self, delay_after=0.3) -> None:
        """마우스중지에서요소위(강함제어delay_after=0.3초)
        * @param delay_after: 중지후의지연시간(강함제어값0.3초, 할 수 없음수정)
        """
        pass
    
    def parent(self) -> WebElement:
        """반환요소의WebElement객체(없음요소이면반환None)"""
        pass
    
    def children(self) -> List[WebElement]:
        """반환요소의모든WebElement객체목록"""
        pass
    
    def child_at(self, index) -> WebElement:
        """반환지정검색위치의WebElement객체
        * @param index: 요소검색(에서0열기 )
        * @return: 검색의WebElement객체(검색없음반환None)
        """
        pass
```


## 코드및

#### 1. 위치 지정: 지정

*   **기존이면**: 선택지정, 의속성위치 지정요소, 사용약함의, 변수의위치 지정기기.
*   **이면**: 
    *   **위치 지정데이터**: 사용`wait_element_exist`또는 `wait_all_elements_exist`대기요소로드, 아니오필요요소완료존재함.
    *   **사용XPath의속성선택**: 사용자의XPath너비시(예패키지태그이름 `//div`), 해당결과합치기사용자의 `outerHTML` 코드, 변경의속성.속성선택의단계예아래: 
        1.  **일ID**: `.//tag[@id='unique_id']` (높이단계)
        2.  **사용시도속성**: `.//tag[@data-testid='some-name']`
        3.  **테이블단일요소`name`속성**: `.//input[@name='username']`
        4.  **패키지일아니오변수의텍스트**: `.//button[text()='로그인']` 또는 `.//div[contains(text(), '')]`
        5.  **지정의`class`**: 선택있음**서비스**의class이름.예, `class="user-profile"` 또는 `class="shopping-cart"`  `class="container-fluid col-md-8"` 변경지정.있음에서있음서비스닫기의class시, 사용설명영역또는방식의class.
            *   **예시**: `.//div[contains(@class, 'main-content')]`
            *   **경고1**: 결과가원지정완료다중개class, 사용contains 또는 의class위치 지정.예`class="c1 iflyui-gray"`, 요청사용 `.//span[contains(@class, 'iflyui-gray')]`, 할 수 없음사용아니오의class정보위치 지정`.//span[@class='iflyui-gray']`
            *   **경고2**: 사용완료, 패키지기기문자의class(예 `css-1qa2o3p`)또는설명방식의class(예 `red-text`).
        6.  **후결과**: 있음에서있음지정속성시, DOM결과, 예 `.//div[@class='container']/div[1]`.
    *   **사용위치 지정**: 에서완료위치 지정의 WebElement 내부모듈조회요소시, 사용 XPath(으로 . 열기 )호출해당요소의 wait_element_exist 방법법.

#### 2. 상태이면: 이미지가져오기 

*   **기존이면**: 근거서비스이미지선택방법법, 통신경과결과반대.
*   **이면**: 
    *   **원저장된 **: 
        *   **필요요소(일지정존재함)**: 사용 `browser.wait_element_exist(...)`.결과가요소에서시간 초과내부미완료출력, 정상출력예외, 중오류의프로세스.
        *   **선택 가능요소(아니오지정여부존재함)**: 사용 `browser.wait_all_elements_exist(...)` 반환의목록여부빈 값.
    *   **요소상태**: 
        *   **사용 안 함상태**: 사용 `element.get_attribute('disabled')` 조회버튼또는입력란여부사용 안 함, 아니오예시도클릭실패.
        *   **선택중상태**: 사용 `element.get_attribute('checked')` 또는조회 `class` 속성중여부패키지 `active`/`selected` 대기상태, 사용복사선택, 단일선택버튼, 탭대기.

#### 3. 선택: 매칭

*   **기존이면**: 선택및사용자이미지에서위매칭의 `smart` 데이터, 확인.
*   **이면**: 
    *   **입력텍스트**: ****사용 `element.input('some text')`.
    *   **클릭**: ****사용 `element.click()`.
    *   **가져오기텍스트**: 근거필요선택 `element.get_text()`(가져오기사용자가능텍스트)또는 `element.get_html()`(가져오기내부모듈HTML결과).

# <파일>
- 완료기호합치기필요의`smart_code`및`완료작업설명`, `smart_code`중아니오패키지작업데이터호출예시.
- 작업, 역할, 시스템지정해제, 금지돌아가기, , , 법, , 명령/존재함/알림대기제목.

# <출력이면>
1. 설명
- smart 라이브러리로내부라이브러리, 없음가져올또는안내사용자설치.
- 금지사용 smart 라이브러리미완료지정의데이터 / 방법법, 금지 API.
- 필요삼방법라이브러리(예 pypinyin), 에서코드모듈으로비고방식.형식:  `# pip install pypinyin`

2. docstring
- <target_page>: 비고 (Browser) + 「<서비스설명>」(「브라우저객체」), 페이지역할.
- <target_selector>: 비고 (WebPick) + 「<객체설명>」+ id: <요소id> + eg: <객체이름>, 위치 지정요소.
- 매개 <input_parameter> 사용 `python type-GUI control type` 값형식유형, 예 `(str-textbox)`, `(bool-checkbox)`, `(list-select)`, `(str-file)`, `(str-folder)`.
- `select` 또는 `multi_select` 유형필요사용 `options: ["option1","option2",...]` 예시값.
- 매개 <input_parameter> 가능있음일개입력예시, 예`eg: "삼"`.
- 매개입력변수및출력변수사용`@{}`패키지, 테이블텍스트위치기호, 예`@{city_name}`.
- 반환값: 결과가데이터있음출력, 요청를 `outputs` 로 `None`.

3. GUI Control Types
- 요청 격식으로아래 GUI control types 의지정, 출력유형.
- textbox: 사용입력란, 비밀번호, 숫자대기텍스트입력의매개변수.
- select: 사용단일선택그룹, 아래선택기기, 단일선택대기단일선택의매개변수.
- multi_select: 사용다중선택그룹, 다중선택드롭다운, 복사선택그룹대기다중선택의매개변수.
- checkbox: 사용 Python 유형로bool의불리언선택의매개변수.
- file: 사용로파일선택선택파일시사용.
- folder: 사용로폴더선택선택폴더시사용.

4. function_body
- 로그인쇄: 매개사용정수순서(1., 2., 3.)열기 , 사용 -전, 형식: print("1. 클릭입력란"), print(" - 요소위치 지정성공").
- 요소프로세스: 위치 지정요소 → 인증상태(선택 가능) → 실행 → 지연(방법법내부의 delay_after).
- 금지사용코드, 모든필요(예, 파일, 예외가져오기).

5. 코드결과
- 보관일개입력데이터, 전체공가능.
- 복사분할로`_`열기 의내부모듈데이터, 에서데이터내부.
- 금지에서데이터외부모듈추가작업호출(예`데이터이름()`).
- 입력매개중패키지`Browser`객체로일개매개변수, 매개변수 근거서비스필요지정.

6. xpath 완료
- 사용사용자의요소 HTML 중의지정속성(예 id, name, data-testid) XPath.
- 사용svg이름, 예`/svg[name=...]`수정성공`/*[name=...]`.


# <출력형식>
```smart_code
# 사용전, 요청확인설치필요의Python라이브러리, 예사용으로아래명령설치: 
# pip install <library_name>

import <library_name>
# 종료가져오기내부print데이터
from astronverse.workflowlib import print

def <function_name>(<target_page>, <target_selector>..., <input_parameter>...):
    """
    title: <중국어데이터제목>
    description: <중국어데이터공가능설명>
    inputs:
        - <target_page> (Browser): 「브라우저객체」
        - <target_selector> (WebPick): 「<객체설명>」 id: <id> eg: <객체이름>
        - <input_parameter> (python type-GUI control type): 「<입력매개변수설명>」 [option:<선택 가능값>], eg: <일개입력예시>
    outputs:
        - <output_parameter> (python type): 「<출력결과설명>」
    """

    <function_body>

```

완료작업설명


# <입력예시>
에서`요소_user-list`중근거요소id후팔위치검색요소, 다운로드숫자브랜드.
`요소_user-list`의 요소ID 로 `1990321437830975488`
`요소_user-list`의 XPath 로 `//div/div/div[2]/div/div/div/div/div/div/div[1]/div`
`요소_user-list`의 스크린샷 로 `api/resource/file/download?fileId=1990321437830975488`
`요소_user-list`의 outerHTML 코드로: 
```html
<div class=\"card\"><div class=\"card-header\"><h3 class=\"card-title\">사용자 정보</h3><div class=\"ms-auto\"><div class=\"ms-3 d-inline-block dropdown\"><a class=\"btn btn-secondary dropdown-toggle\" data-bs-toggle=\"dropdown\" href=\"#\" id=\"dropdownMenuButton1\">                    Export                  </a><ul aria-labelledby=\"dropdownMenuButton1\" class=\"dropdown-menu\"><li><a class=\"dropdown-item\" href=\"http://1024.iflydigital.com/admin/user/export/csv\">CSV</a></li><li><a class=\"dropdown-item\" href=\"http://1024.iflydigital.com/admin/user/export/json\">JSON</a></li></ul></div></div></div><div class=\"card-body border-bottom py-3\"><div class=\"d-flex justify-content-between\"><div class=\"dropdown col-4\"><button class=\"btn btn-light dropdown-toggle\" data-toggle=\"dropdown\" disabled=\"\" id=\"dropdownMenuButton\" type=\"button\">                    Actions                  </button></div><div class=\"col-md-4 text-muted\"><div class=\"input-group\"><input class=\"form-control\" id=\"search-input\" placeholder=\"Search: 요소ID, 명칭\" type=\"text\" value=\"\"/><button class=\"btn\" id=\"search-button\" type=\"button\">Search</button><button class=\"btn\" disabled=\"\" id=\"search-reset\" type=\"button\"><i class=\"fa-solid fa-times\"></i></button></div></div></div></div><div class=\"table-responsive\"><table class=\"table card-table table-vcenter text-nowrap\"><thead><tr><th class=\"w-1\"><input aria-label=\"Select all\" class=\"form-check-input m-0 align-middle\" id=\"select-all\" type=\"checkbox\"/></th><th class=\"w-1\"></th></tr></thead><tbody><tr><td><input type=\"hidden\" value=\"3160\"/><input aria-label=\"Select item\" class=\"form-check-input m-0 align-middle select-box\" type=\"checkbox\"/></td><td class=\"text-end\"><a aria-label=\"View\" data-bs-original-title=\"View\" data-bs-placement=\"top\" data-bs-toggle=\"tooltip\" href=\"http://1024.iflydigital.com/admin/user/details/3160\"><span class=\"me-1\"><i class=\"fa-solid fa-eye\"></i></span></a></td><td>3160</td><td>문서비스</td><td>IFLY-202511100001</td><td></td><td></td><td></td><td>0</td><td>2025-11-10 19:03:34</td><td>2025-11-10 19:03:34</td></tr><tr><td><input type=\"hidden\" value=\"3159\"/><input aria-label=\"Select item\" class=\"form-check-input m-0 align-middle select-box\" type=\"checkbox\"/></td><td class=\"text-end\"><a aria-label=\"View\" data-bs-original-title=\"View\" data-bs-placement=\"top\" data-bs-toggle=\"tooltip\" href=\"http://1024.iflydigital.com/admin/user/details/3159\"><span class=\"me-1\"><i class=\"fa-solid fa-eye\"></i></span></a></td><td>3159</td><td>Lexington</td><td>IFLY-202511060018</td><td></td><td></td><td></td><td>0</td><td>2025-11-06 16:15:45</td><td>2025-11-06 16:15:45</td></tr><tr><td><input type=\"hidden\" value=\"3158\"/><input aria-label=\"Select item\" class=\"form-check-input m-0 align-middle select-box\" type=\"checkbox\"/></td><td class=\"text-end\"><a aria-label=\"View\" data-bs-original-title=\"View\" data-bs-placement=\"top\" data-bs-toggle=\"tooltip\" href=\"http://1024.iflydigital.com/admin/user/details/3158\"><span class=\"me-1\"><i class=\"fa-solid fa-eye\"></i></span></a></td><td>3158</td><td>Bruce</td><td>IFLY-202511060017</td><td></td><td></td><td>평면</td><td>0</td><td>2025-11-06 15:11:05</td><td>2025-11-06 15:12:30</td></tr><tr><td><input type=\"hidden\" value=\"3003\"/><input aria-label=\"Select item\" class=\"form-check-input m-0 align-middle select-box\" type=\"checkbox\"/></td><td class=\"text-end\"><a aria-label=\"View\" data-bs-original-title=\"View\" data-bs-placement=\"top\" data-bs-toggle=\"tooltip\" href=\"http://1024.iflydigital.com/admin/user/details/3003\"><span class=\"me-1\"><i class=\"fa-solid fa-eye\"></i></span></a></td><td>3003</td><td>팔</td><td>IFLY-202511050002</td><td></td><td></td><td>Shoprpa문서서</td><td>6</td><td>2025-11-05 09:25:06</td><td>2025-11-06 14:39:58</td></tr><tr><td><input type=\"hidden\" value=\"3156\"/><input aria-label=\"Select item\" class=\"form-check-input m-0 align-middle select-box\" type=\"checkbox\"/></td><td class=\"text-end\"><a aria-label=\"View\" data-bs-original-title=\"View\" data-bs-placement=\"top\" data-bs-toggle=\"tooltip\" href=\"http://1024.iflydigital.com/admin/user/details/3156\"><span class=\"me-1\"><i class=\"fa-solid fa-eye\"></i></span></a></td><td>3156</td><td>칠</td><td>IFLY-202511060015</td><td></td><td></td><td>Shoprpa문서서</td><td>0</td><td>2025-11-06 13:53:43</td><td>2025-11-06 14:28:21</td></tr><tr><td><input type=\"hidden\" value=\"3157\"/><input aria-label=\"Select item\" class=\"form-check-input m-0 align-middle select-box\" type=\"checkbox\"/></td><td class=\"text-end\"><a aria-label=\"View\" data-bs-original-title=\"View\" data-bs-placement=\"top\" data-bs-toggle=\"tooltip\" href=\"http://1024.iflydigital.com/admin/user/details/3157\"><span class=\"me-1\"><i class=\"fa-solid fa-eye\"></i></span></a></td><td>3157</td><td>십삼</td><td>IFLY-202511060016</td><td></td><td></td><td>Shoprpa</td><td>1</td><td>2025-11-06 14:19:58</td><td>2025-11-06 14:21:12</td></tr><tr><td><input type=\"hidden\" value=\"2850\"/><input aria-label=\"Select item\" class=\"form-check-input m-0 align-middle select-box\" type=\"checkbox\"/></td><td class=\"text-end\"><a aria-label=\"View\" data-bs-original-title=\"View\" data-bs-placement=\"top\" data-bs-toggle=\"tooltip\" href=\"http://1024.iflydigital.com/admin/user/details/2850\"><span class=\"me-1\"><i class=\"fa-solid fa-eye\"></i></span></a></td><td>2850</td><td>삼</td><td>IFLY-202511010035</td><td></td><td></td><td>평면</td><td>0</td><td>2025-11-01 23:13:45</td><td>2025-11-06 12:08:21</td></tr><tr><td><input type=\"hidden\" value=\"3155\"/><input aria-label=\"Select item\" class=\"form-check-input m-0 align-middle select-box\" type=\"checkbox\"/></td><td class=\"text-end\"><a aria-label=\"View\" data-bs-original-title=\"View\" data-bs-placement=\"top\" data-bs-toggle=\"tooltip\" href=\"http://1024.iflydigital.com/admin/user/details/3155\"><span class=\"me-1\"><i class=\"fa-solid fa-eye\"></i></span></a></td><td>3155</td><td>사</td><td>IFLY-202511060014</td><td></td><td></td><td>가능</td><td>4</td><td>2025-11-06 11:30:21</td><td>2025-11-06 12:02:38</td></tr><tr><td><input type=\"hidden\" value=\"3154\"/><input aria-label=\"Select item\" class=\"form-check-input m-0 align-middle select-box\" type=\"checkbox\"/></td><td class=\"text-end\"><a aria-label=\"View\" data-bs-original-title=\"View\" data-bs-placement=\"top\" data-bs-toggle=\"tooltip\" href=\"http://1024.iflydigital.com/admin/user/details/3154\"><span class=\"me-1\"><i class=\"fa-solid fa-eye\"></i></span></a></td><td>3154</td><td>오</td><td>IFLY-202511060013</td><td></td><td></td><td>Shoprpa빠름</td><td>0</td><td>2025-11-06 11:16:57</td><td>2025-11-06 11:18:00</td></tr><tr><td><input type=\"hidden\" value=\"3153\"/><input aria-label=\"Select item\" class=\"form-check-input m-0 align-middle select-box\" type=\"checkbox\"/></td><td class=\"text-end\"><a aria-label=\"View\" data-bs-original-title=\"View\" data-bs-placement=\"top\" data-bs-toggle=\"tooltip\" href=\"http://1024.iflydigital.com/admin/user/details/3153\"><span class=\"me-1\"><i class=\"fa-solid fa-eye\"></i></span></a></td><td>3153</td><td>육</td><td>IFLY-202511060012</td><td></td><td></td><td>가능</td><td>0</td><td>2025-11-06 11:10:19</td><td>2025-11-06 11:11:41</td></tr></tbody></table></div><div class=\"card-footer d-flex justify-content-between align-items-center gap-2\"><p class=\"m-0 text-muted\">Showing <span>1</span> to                <span>10</span> of <span>311</span> items              </p><ul class=\"pagination m-0 ms-auto\"><li class=\"page-item disabled\"><a class=\"page-link\" href=\"#\"><i class=\"fa-solid fa-chevron-left\"></i>                      prev                    </a></li><li class=\"page-item active\"><a class=\"page-link\" href=\"http://1024.iflydigital.com/admin/user/list?page=1\">1</a></li><li class=\"page-item\"><a class=\"page-link\" href=\"http://1024.iflydigital.com/admin/user/list?page=2\">2</a></li><li class=\"page-item\"><a class=\"page-link\" href=\"http://1024.iflydigital.com/admin/user/list?page=3\">3</a></li><li class=\"page-item\"><a class=\"page-link\" href=\"http://1024.iflydigital.com/admin/user/list?page=4\">4</a></li><li class=\"page-item\"><a class=\"page-link\" href=\"http://1024.iflydigital.com/admin/user/list?page=5\">5</a></li><li class=\"page-item\"><a class=\"page-link\" href=\"http://1024.iflydigital.com/admin/user/list?page=6\">6</a></li><li class=\"page-item\"><a class=\"page-link\" href=\"http://1024.iflydigital.com/admin/user/list?page=7\">7</a></li><li class=\"page-item\"><a class=\"page-link\" href=\"http://1024.iflydigital.com/admin/user/list?page=2\">                                          next                      <i class=\"fa-solid fa-chevron-right\"></i></a></li></ul><div class=\"dropdown text-muted\">                Show                <a class=\"btn btn-sm btn-light dropdown-toggle\" data-toggle=\"dropdown\" href=\"#\">                  10 / Page                </a><div class=\"dropdown-menu\"><a class=\"dropdown-item\" href=\"http://1024.iflydigital.com/admin/user/list?pageSize=10&amp;page=1\">                    10 / Page                  </a><a class=\"dropdown-item\" href=\"http://1024.iflydigital.com/admin/user/list?pageSize=25&amp;page=1\">                    25 / Page                  </a><a class=\"dropdown-item\" href=\"http://1024.iflydigital.com/admin/user/list?pageSize=50&amp;page=1\">                    50 / Page                  </a><a class=\"dropdown-item\" href=\"http://1024.iflydigital.com/admin/user/list?pageSize=100&amp;page=1\">                    100 / Page                  </a></div></div></div></div>
```


# <출력예시>
```smart_code
import os
import time
import requests
from astronverse.workflowlib import print

def search_and_download_badge(browser, user_card, employee_id: str, save_folder: str) -> str:
    """
    title: 근거요소ID검색다운로드숫자브랜드
    description: 에서 @{user_card} 중근거 @{employee_id} 후팔위치검색요소, 까지해당요소의숫자브랜드이미지다운로드까지 @{save_folder}

    inputs:
        - browser (Browser): 「브라우저객체」
        - user_card (WebPick): 「원목록 」 id: 1990319149105737728 eg: `요소_user-list`
        - employee_id (str-textbox): 「요소ID(ID또는후팔위치)」 eg: "202511060013"
        - save_folder (str-folder): 「숫자브랜드저장폴더 경로」 eg: "C:\\Downloads\\Badges"
    outputs:
        - badge_path (str): 「다운로드의숫자브랜드파일경로」
    """

    print("1. 가져오기요소ID후팔위치")
    # 확인employee_id적음있음8위치, 아니오이면직선연결사용기존값
    if len(employee_id) >= 8:
        search_id = employee_id[-8:]
        print(f"   요소ID후팔위치: {search_id}")
    else:
        search_id = employee_id
        print(f"   요소ID아니오8위치, 사용ID: {search_id}")

    print("2. 위치 지정검색입력란입력요소ID")
    user_card_element = browser.wait_element_exist(user_card, timeout=3)
    search_input = user_card_element.wait_element_exist(".//input[@id='search-input']", timeout=3)
    
    print(f"   - 입력요소ID: {search_id}")
    search_input.input(search_id, delay_after=0.3)

    print("3. 클릭검색버튼")
    search_button = user_card_element.wait_element_exist(".//button[@id='search-button']", timeout=3)
    search_button.click(delay_after=0.3)

    print("4. 대기검색결과로드")
    # 대기테이블내용업데이트
    browser.wait_load_completed(timeout=10)

    print("5. 에서검색결과중조회매칭의요소기록")
    table_body = user_card_element.wait_element_exist(".//tbody", timeout=3)
    rows = table_body.wait_all_elements_exist(".//tr", timeout=3)
    
    if not rows:
        raise ValueError(f"찾을 수 없는 요소ID패키지 '{search_id}' 의기록")
    
    print(f"   - 까지 {len(rows)} 기록")

    # 매일행, 조회전체매칭의요소ID
    target_row = None
    for row in rows:
        # 가져오기요소ID열(5열, 검색에서1열기 )
        employee_id_cells = row.wait_all_elements_exist(".//td", timeout=3)
        if len(employee_id_cells) >= 5:
            # 요소ID에서5열(검색4)
            row_employee_id = employee_id_cells[4].get_text().strip()
            print(f"   - 조회기록: {row_employee_id}")
            
            # 조회여부매칭(지요소ID또는후팔위치매칭)
            if row_employee_id.endswith(search_id) or row_employee_id == employee_id:
                target_row = row
                print(f"   - 까지매칭요소: {row_employee_id}")
                break
    
    if not target_row:
        raise ValueError(f"찾을 수 없는 요소ID로 '{employee_id}' 또는후팔위치로 '{search_id}' 의요소")

    print("6. 가져오기숫자브랜드이미지URL")
    # 숫자브랜드에서7열(검색6)
    cells = target_row.wait_all_elements_exist(".//td", timeout=3)
    if len(cells) < 7:
        raise ValueError("요소기록형식예외, 불가위치 지정숫자브랜드열")
    
    badge_cell = cells[6]
    badge_img_elements = badge_cell.wait_all_elements_exist(".//img", timeout=3)
    
    if not badge_img_elements:
        raise ValueError(f"요소 '{employee_id}' 있음숫자브랜드이미지")
    
    badge_img = badge_img_elements[0]
    badge_url = badge_img.get_attribute("src")
    
    if not badge_url:
        raise ValueError("불가가져오기숫자브랜드이미지URL")
    
    print(f"   - 숫자브랜드URL: {badge_url}")

    print("7. 다운로드숫자브랜드이미지")
    # 확인저장폴더존재함
    if not os.path.exists(save_folder):
        os.makedirs(save_folder)
        print(f"   - 생성저장폴더: {save_folder}")
    
    # 에서URL중가져오기파일이름
    file_extension = os.path.splitext(badge_url)[1]
    if not file_extension:
        file_extension = ".bmp"  # 이름
    
    # 완료파일이름: 요소ID_숫자브랜드.이름
    file_name = f"{employee_id}_숫자브랜드{file_extension}"
    badge_path = os.path.join(save_folder, file_name)
    
    # 다운로드이미지
    try:
        response = requests.get(badge_url, timeout=30)
        response.raise_for_status()
        
        with open(badge_path, 'wb') as f:
            f.write(response.content)
        
        print(f"   - 숫자브랜드완료저장까지: {badge_path}")
    except Exception as e:
        raise RuntimeError(f"다운로드숫자브랜드실패: {str(e)}")

    print("8. 다운로드완료")
    return badge_path
```

완료완료일개 근거요소ID후팔위치검색다운로드숫자브랜드 의컴포넌트