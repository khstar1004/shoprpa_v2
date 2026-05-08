<!-- @format -->

# ShopRPA Browser Plugin API 문서

## 1. 개요

- **확장**: ShopRPA 클라이언트와 Chrome, Edge, Firefox 브라우저를 연결해 웹 페이지 자동화 기능을 제공합니다.
- **주요 기능**:

* 브라우저 탭 제어
* 웹 페이지 요소 선택 및 조작
* Cookie 조회 및 설정
* 사용자 입력 처리
* iframe 탐색

- **버전정보**: 5.2.4

## 2. 빠른 시작

- **설치**: `npm run build` 실행 후 생성된 확장 패키지를 브라우저의 확장 관리 화면에서 로드합니다.
- **실행 환경**: Chrome 계열 브라우저 기준으로 동작하며, Node.js v20 이상을 권장합니다.

## 3. API 매개

- **연결**: `http://127.0.0.1:{port}/browser/transition` (기본 port: 9082)
- **유형**: `POST`
- **요청**: `application/json`

```json
{
  "browser_type": "chrome", // firefox, edge ...
  "data": {
    "x": 854, "y": 314
  },
  "key": "getElement"
}
```

- **반환**: `application/json`

```json
{
  "code": "0000", // background code 
  "msg": "ok",
  "data": {
    "code": "0000", // content code 
    "data": {
      "matchTypes": [],
      "checkType": "visualization",
      "xpath": "//textarea[@id=\"APjFqb\"]",
      "cssSelector": "#APjFqb",
      "pathDirs": [
        {
          "tag": "textarea",
          "checked": true,
          "value": "textarea",
          "attrs": [
            {
              "name": "id",
              "value": "APjFqb",
              "checked": true,
              "type": 0
            },
            {
              "name": "class",
              "value": "gLFyf",
              "checked": false,
              "type": 1
            },
            {
              "name": "title",
              "value": "Google 검색",
              "checked": false,
              "type": 0
            }
          ]
        }
      ],
      "rect": {
        "left": 594,
        "top": 268,
        "width": 672,
        "height": 75,
        "right": 1265,
        "bottom": 343,
        "x": 594,
        "y": 268
      },
      "url": "https://www.google.com.hk/",
      "shadowRoot": false,
      "tag": "입력",
      "text": "unknown",
      "tabTitle": "Google",
      "tabUrl": "https://www.google.com.hk/",
      "isFrame": false,
      "frameId": 0
    },
    "msg": "success"
  }
}
```

```json
// 가져오기 tab
{
    "browser_type": "chrome",
    "data": {"": ""},
    "key": "getActiveTab"
}

// 열기웹 페이지
{
    "browser_type": "chrome",
    "data": { "url": "https://example.com/"},
    "key": "openNewTab"
}

// 비고입력JS
{
    "browser_type": "chrome",
    "data": {
        "code": "console.log(1);return 1",
        "url": "http://192.168.56.1:9010/aa/2.html?a=1",
        "tabUrl": "http://192.168.56.1:9010/1.html",
        "iframeXpath": "html/body/div[1]/iframe/html/body/div/button",
        "isFrame": true
    },
    "key": "runJS"
}

// 검증요소
{
    "browser_type": "chrome",
    "data": {
     "xpath": "//div[@id=\"abc\"]",
      "cssSelector": "#abc",
      ...
    },
    "key": "checkElement"
}
//검증요소반환의예요소위치

// optional
{
    "browser_type": "chrome",
    "data": {
      "xpath": "//div[@id=\"abc\"]",
      "cssSelector": "#abc",
      ...
    },
    "key": "scrollIntoView"
}

// 에서지정의tab, 지정의frame아래실행 code
{
    "browser_type": "chrome",
    "data": {
        "tabUrl": "",
        "url": "",
        "code": "console.log("ok");return "ok";"
    },
    "key": "executeScriptOnFrame"
}
// 대창
{
    "browser_type": "chrome",
    "data": {
      "url": ""
    },
    "key": "maxWindow"
}

// 가져오기tab 의Url
{
    "browser_type": "chrome",
    "data": {
      "": ""
    },
    "key": "getUrl"
}
// 가져오기tab 의 title
{
    "browser_type": "chrome",
    "data": {
      "": ""
    },
    "key": "getTitle"
}
// full-page screenshot
{
    "browser_type": "chrome",
    "data": {
      "": ""
    },
    "key": "captureScreen"
}

// 가져오기요소위치
{
    "browser_type": "chrome",
    "data": {
     "xpath": "//div[@id=\"app\"]/div/div/div[3]",
      "cssSelector": "#app>div.arrange>div:nth-child(1)>div:nth-child(3)",
      ...
    },
    "key": "getElementPos"
}


// 가져오기cookie
{
    "browser_type": "chrome",
    "data": {
      "url": "http://localhost:1420/",
      "name": "JSESSIONID"
    },
    "key": "getCookie"
}
// cookie
{
    "browser_type": "chrome",
    "data":{
      "url": "http://localhost:1420/",
      "name": "JSESSIONID",
          "value": "123"
    },
    "key": "setCookies"
}
// 삭제cookie
{
    "browser_type": "chrome",
    "data":{
      "url": "http://localhost:1420/",
      "name": "JSESSIONID"
    },
    "key": "removeCookie"
}

// 새로고침 tab페이지
{
    "browser_type": "chrome",
    "data": {
      "": ""
    },
    "key": "reloadTab"
}
// 가져오기 요소
{
  "browser_type": "chrome",
  "data": {
   "xpath": "//ul[@id=\"hotsearch-content-wrapper\"]/li[2]/a/span[2]",
    "cssSelector": "#hotsearch-content-wrapper>li:nth-child(2)>a:nth-child(1)>span:nth-child(4)",
    ...
  },
  "key": "similarElement"
}

// 통신경과요소정보가져오기요소정보, 사용단일요소다중요소, 사용요소대기
{
  "browser_type": "chrome",
   "data": {
      "xpath": "//div[@id=\"app\"]/div[2]/div/div[2]/section[1]/article/div[1]/div[1]/section[5]/section[1]/div/div/button/span",
      "cssSelector": "#app>div:nth-child(3)>div>div:nth-child(2)>section:nth-child(1)>article>div:nth-child(3)>div:nth-child(1)>section:nth-child(5)>section:nth-child(1)>div>div>button>span",
      ...
    },
  "key": "elementFromSelect"
}
// 반환결과예개요소정보의배열, 단일요소배열length 로1, 다중요소이면배열length > 1, 없음이면반환의예null
{
  "code": "0000",
  "msg": "ok",
  "data": {
    "code": "0000",
    "data": [
      {
        "xpath": "//div[@id=\"app\"]/div[2]/div/div[2]/section[1]/article/div[1]/div[1]/section[5]/section[1]/div/div[1]/button/span",
        "cssSelector": "#app>div:nth-child(3)>div>div:nth-child(2)>section:nth-child(1)>article>div:nth-child(3)>div:nth-child(1)>section:nth-child(5)>section:nth-child(1)>div>div:nth-child(1)>button>span",
        ...
      },
      {
        "xpath": "//div[@id=\"app\"]/div[2]/div/div[2]/section[1]/article/div[1]/div[1]/section[5]/section[1]/div/div[2]/button/span",
        "cssSelector": "#app>div:nth-child(3)>div>div:nth-child(2)>section:nth-child(1)>article>div:nth-child(3)>div:nth-child(1)>section:nth-child(5)>section:nth-child(1)>div>div:nth-child(2)>button>span",
        ...
      },
      {
        "xpath": "//div[@id=\"app\"]/div[2]/div/div[2]/section[1]/article/div[1]/div[1]/section[5]/section[1]/div/div[3]/button/span",
        "cssSelector": "#app>div:nth-child(3)>div>div:nth-child(2)>section:nth-child(1)>article>div:nth-child(3)>div:nth-child(1)>section:nth-child(5)>section:nth-child(1)>div>div:nth-child(3)>button>span",
        ...
      },
      ...
    ],
    "msg": "success"
  }
}
// 대기요소
{
    "browser_type": "chrome",
    "data": {
     "xpath": "//div[@id=\"abc\"]",
      "cssSelector": "#abc",
      ...
    },
    "key": "elementIsRender"
}

// 웹 페이지여부로드완료
{
    "browser_type": "chrome",
    "data": {
     "":""
    },
    "key": "loadComplete"
}
// 중지로드
{
    "browser_type": "chrome",
    "data": {"": ""},
    "key": "stopLoad"
}

// backward
{
    "browser_type": "chrome",
    "data": { "": ""},
    "key": "backward"
}
// forward
{
    "browser_type": "chrome",
    "data": { "": ""},
    "key": "forward"
}

// 웹 페이지 닫기
{
    "browser_type": "chrome",
    "data": { "url": "https://developer.mozilla.org/zh-CN/plus"},
    "key": "closeTab"
}
// 지정 페이지로 전환
{
    "browser_type": "chrome",
    "data": { "url": "https://developer.mozilla.org/zh-CN/plus"},
    "key": "switchTab"
}
{
    "browser_type": "chrome",
    "data": { "title": "MDN Plus"},
    "key": "switchTab"
}

// 가져오기전체웹 페이지
{
  "browser_type": "chrome",
  "data": {
      "": ""
   },
  "key": "capturePage"
}

// 데이터가져오기테이블
{
  "browser_type": "chrome",
  "data": {
      "xpath": "//div[@id=\"app\"]/div[2]/div/div[2]/section[1]/article/section[3]/table[1]/tbody/tr[5]/td[1]",
      "cssSelector": "#app>div:nth-child(3)>div.ant-row.css-9loccf>div:nth-child(2)>section:nth-child(1)>article>section:nth-child(4)>table:nth-child(4)>tbody>tr:nth-child(5)>td:nth-child(1)",
     ...
    },
  "key": "tableDataBatch"
}

// 데이터가져오기테이블단일열
{
  "browser_type": "chrome",
  "data": {
      "xpath": "//div[@id=\"app\"]/div[2]/div/div[2]/section[1]/article/section[3]/table[1]/tbody/tr[5]/td[1]",
      "cssSelector": "#app>div:nth-child(3)>div.ant-row.css-9loccf>div:nth-child(2)>section:nth-child(1)>article>section:nth-child(4)>table:nth-child(4)>tbody>tr:nth-child(5)>td:nth-child(1)",
      ...
    },
  "key": "tableColumnDataBatch"
}

// 데이터가져오기 요소
{
  "browser_type": "chrome",
  "data": {
      "xpath": "//div[@id=\"app\"]/div[2]/div/div[2]/section[1]/article/section[3]/table[1]/tbody/tr[5]/td[1]",
      "cssSelector": "#app>div:nth-child(3)>div.ant-row.css-9loccf>div:nth-child(2)>section:nth-child(1)>article>section:nth-child(4)>table:nth-child(4)>tbody>tr:nth-child(5)>td:nth-child(1)",
      ...
    },
  "key": "similarBatch"
}
// 업데이트현재tab, 업데이트현재tab 주소로 url 주소
{
  "browser_type": "chrome",
  "data": {
      "url": "https://www.abc.com"
  },
  "key": "updateTab"
}
// elementShot 요소스크린샷
{
  "browser_type": "chrome",
  "data": {
      "xpath": "//div[@id=\"container\"]/div[3]/div[2]/div/div[2]/div[1]/p[23]",
      "cssSelector": "p:nth-child(23)",
     ...
    },
  "key": "elementShot"
}

// elementIsReady 요소여부준비완료, 패키지페이지상태
{
  "browser_type": "chrome",
   "data": {
      "xpath": "//div[@id=\"container\"]/div[3]/div[2]/div/div[2]/div[1]/p[23]",
      "cssSelector": "p:nth-child(23)",
      ...
    },
  "key": "elementIsReady"
}

// 요소
{
  "browser_type": "chrome",
   "data": {
      "index": 0,
      "count": 5,
      "xpath": "//nav[@id=\"nav\"]/ul/li[24]/ul/li/a",
      "cssSelector": "#nav>ul>li:nth-child(24)>ul>li>a",
      ...
    },
  "key": "getSimilarIterator"
}
// 반환결과
{
  "code": "0000",
  "msg": "ok",
  "data": {
    "code": "0000",
    "data": [{
      "index": 0,
      "count": 5,
      "xpath": "//nav[@id=\"nav\"]/ul/li[24]/ul/li[13]/a",
      "cssSelector": "#nav>ul>li:nth-child(24)>ul>li:nth-child(13)>a",
      ...
      "similarCount": 15
    },
    ...
    ],
    "msg": "success"
  }
}

// 가져오기요소html
{
   "browser_type": "chrome",
    "data": {
      "x": 0,
      "y": 0
    },
    "key": "getOuterHTML"
}
// 반환결과
{
  "code": "0000",
  "msg": "ok",
  "data": {
    "code": "0000",
    "data": {
      "matchTypes": [],
      "checkType": "visualization",
      "xpath": "//div[@id=\"spa-mount-point\"]/div/div[1]/div/div[2]/div[2]/div[1]/div[1]/div[1]/div[1]/div/div",
      "cssSelector": "#spa-mount-point>div>div.ent-resource>div>div.ent-resource-main>div.ent-resource-body>div.branch-tabs-wrap>div.tabs-container>div.branch-tabs>div.el-tabs__header>div>div",
      "abXpath": "/html/body/div/section/section/main/div/div/div[1]/div/div[2]/div[2]/div[1]/div[1]/div[1]/div[1]/div/div",
      "outerHTML": "<div class=\"el-tabs__nav-scroll\"><div role=\"tablist\" class=\"el-tabs__nav is-top\" style=\"transform: translateX(0px);\"><div class=\"el-tabs__active-bar is-top\" style=\"width: 85px; transform: translateX(0px);\"></div><div id=\"tab-active\" aria-controls=\"pane-active\" role=\"tab\" aria-selected=\"true\" tabindex=\"0\" class=\"el-tabs__item is-top is-active\"><span data-v-7a2ea2be=\"\"><span data-v-7a2ea2be=\"\">분 </span><span data-v-23817c3f=\"\" data-v-7a2ea2be=\"\" class=\"count active\">4</span></span></div><div id=\"tab-my\" aria-controls=\"pane-my\" role=\"tab\" tabindex=\"-1\" class=\"el-tabs__item is-top\"><span data-v-7a2ea2be=\"\"><span data-v-7a2ea2be=\"\">의분 </span><span data-v-23817c3f=\"\" data-v-7a2ea2be=\"\" class=\"count\">5</span></span></div><div id=\"tab-all\" aria-controls=\"pane-all\" role=\"tab\" tabindex=\"-1\" class=\"el-tabs__item is-top\"><span data-v-7a2ea2be=\"\"><span data-v-7a2ea2be=\"\">전체분 </span><span data-v-23817c3f=\"\" data-v-7a2ea2be=\"\" class=\"count\">5</span></span></div><div id=\"tab-lazy\" aria-controls=\"pane-lazy\" role=\"tab\" tabindex=\"-1\" class=\"el-tabs__item is-top\"><span data-v-7a2ea2be=\"\"><span data-v-7a2ea2be=\"\">분 </span><span data-v-23817c3f=\"\" data-v-7a2ea2be=\"\" class=\"count\">1</span></span></div></div></div>"
    },
    "msg": "success"
  }
}
// 가져오기요소의요소, 대선택
{
   "browser_type": "chrome",
    "data": {
      "matchTypes": [],
      "checkType": "visualization",
      "xpath": "//div[@id=\"spa-mount-point\"]/div/div[1]/div/div[2]/div[2]/div[1]/div[1]/div[1]/div[1]/div/div",
      "cssSelector": "#spa-mount-point>div>div.ent-resource>div>div.ent-resource-main>div.ent-resource-body>div.branch-tabs-wrap>div.tabs-container>div.branch-tabs>div.el-tabs__header>div>div",
      "abXpath": "/html/body/div/section/section/main/div/div/div[1]/div/div[2]/div[2]/div[1]/div[1]/div[1]/div[1]/div/div",
      ...
    },
    "key": "getParentElement"
}

// 가져오기요소의요소, 소선택가져오기
{
  "browser_type": "chrome",
  "data": {
      "matchTypes": [],
      "checkType": "visualization",
      "xpath": "//div[@id=\"spa-mount-point\"]/div/div[1]/div/div[2]/div[2]/div[1]/div[1]/div[1]/div[1]/div/div",
      "cssSelector": "#spa-mount-point>div>div.ent-resource>div>div.ent-resource-main>div.ent-resource-body>div.branch-tabs-wrap>div.tabs-container>div.branch-tabs>div.el-tabs__header>div>div",
      "abXpath": "/html/body/div/section/section/main/div/div/div[1]/div/div[2]/div[2]/div[1]/div[1]/div[1]/div[1]/div/div",
      "originXpath": "/html/body/div/section/section/main/div/div/div[1]/div/div[2]/div[2]/div[1]/div[1]/div[1]/div[1]/div/div", // 선택 기준이 되는 원본 요소의 절대 XPath입니다.
      ...
   },
  "key": "getChildElement"
}

// 요소 객체 가져오기
{
  "browser_type": "chrome",
  "data": {
      "type": "xpath", // "xpath" | "cssSelector" | "text"
      "value": "//div",
      "returnType": "single" //  "single" | "list"
   },
  "key": "generateElement"
}
// 가져오기닫기 요소
{
    "key" : "getRelativeElement",
    "browser_type": "chrome",
    "data": {
           "matchTypes": [],
           "checkType": "visualization",
           "xpath": "",
           ...,
           "relativeOptions": {
              "relativeType": "child" | "parent" | "sibling",
              "elementGetType": "index" | "xpath" | "last" | "all" | "next" | "prev",
              "index": 1,
              "xpath": ""
           }
     }
}
```

## 4. 호출예시
```
curl -X POST 'http://127.0.0.1:9082/browser/transition' -H 'User-Agent: Reqable/2.30.3' -H 'Content-Type: application/json' -d '{
  "browser_type": "<<bt>>",
  "data": {
    "x": 0,
    "y": 0
  },
  "key": "getElement"
}'
```
## 5. 고급 브라우저 매칭

일반 브라우저는 브라우저 이름과 token으로 매칭합니다. 커스텀 Chromium 계열 브라우저는 실행 파일 이름을 기준으로 매칭할 수 있습니다.

## 6. 비고

- 확장은 ShopRPA 클라이언트 서비스와 통신합니다. 요청 전에 ShopRPA 클라이언트가 실행되어 있어야 합니다.
- 확장 등록 시 브라우저 token을 사용합니다. 예: Chrome 브라우저 token은 `$chrome$`입니다.

## 7. 자주 묻는 질문

- 일부 Chromium 기반 브라우저는 Chrome 확장을 로드할 수 있지만, 등록된 token과 브라우저 매칭 정보가 맞지 않으면 사용할 수 없습니다.

## 8. 기록

- **오류코드목록**: 
```
  SUCCESS = '0000', // 성공
  UNKNOWN_ERROR = '5001', // 알 수 없는 예외
  ELEMENT_NOT_FOUND = '5002', // 요소를 찾을 수 없음
  EXECUTE_ERROR = '5003', // 실행 오류
  VERSION_ERROR = '5004', // 버전 오류
```
