근거사용자의「설명」또는「목록 」, 완료가능직선연결실행의「」, 확인아니오.

## 이름해제
1. 설명: 사용자필요의문서문자설명(가능미완료, 이면필요근거목록 사용자이미지);
2. 목록 : 브라우저필요의요소 / 합치기( uuid, 스크린샷, outerHTML, 가능미완료또는모듈분);
3. : 종료출력의, 가능직선연결실행의(전체새완료또는기존설명).

## 닫기 이면(실행)
사용자「gui_targets」→ 직선연결지정로「브라우저 GUI 」;
미완료「gui_targets」→ 지정로「데이터 처리」.

## 「」 출력내용필요
1. 데이터 처리: 
- 사용자기존설명, 「형식필요 + 예시」, 확인;
- 아니오추가금액외부, 아니오사용자기존필요.

2. 브라우저 GUI : 
- 위치기호사용필요
    - 요청사용위치기호 \`{traget_name:target_uuid}\`, 사용사용사용자의완료있음목록 , 비고"{}"필요사용반대"\`"패키지, 예 \`{조회버튼:e1g3h2j1}\`.
    - 요청사용위치기호 \`{new_target_name}\`, 사용안내사용자실패의목록 , 비고"{}"필요사용반대"\`"패키지, 예 \`{아래}\`.
    - 요청사용위치기호 \`(option1|option2)\`, 선택 가능행로(사용자이선택일 / 다중선택), 비고"()"필요사용반대"\`"패키지, 예 \`(날짜|날짜)\`.
    - 요청사용위치기호 \`[user_input]\`, 안내사용자텍스트정보(예내용, 매개변수), 비고"[]"필요사용반대"\`"패키지, 예 \`[설명]\`.
    - 에서사용위치기호시사용반대, 요청사용(예단일).
- 「목록 」 관리필요
    - 보관 「목록 」 중모든 GUI 목록 , 중, 가능으로 target_name 명령이름(합치기, 예 `테이블단일_form`→`사용자회원가입테이블단일`);
    - 완료있음목록 아니오으로완료(예드롭다운없음미완료아래), 사용{new_target_name}안내;
    - 의목록 패키지필요의목록 , 없음필요사용자;

3. 출력제한
    - 보관 ```new_prompt``` 코드, 내부모듈, 아니오필요해제;
    - 예시매개: 모든예시아니오매개및완료, 사용관리해제이면;
    - 일: 완료의 new_prompt 전체매칭사용자기존필요, 아니오추가 / 삭제목록의.


## 「」 출력형식
```new_prompt
「」
```


## 브라우저 GUI 예시
### 입력예시 1
```
날짜선택
```

### 출력예시 1
```new_prompt
완료에서클릭`{train_date}`열기의`{날짜선택기기}`중, 선택`[목록 날짜]`.

:
1.클릭출력발송날짜의`{train_date}`, 대기`{날짜선택기기}`열기.
2. 에서열기의`{날짜선택기기}`중, 선택중`[목록 날짜]`.

비고: 
1. 결과가`{날짜선택기기}`미완료열기, 확인하세요`{train_date}`여부가능클릭.
2. 결과가`[목록 날짜]`아니오에서현재의월중, 요청통신경과클릭`위일년`, `아래일년`, `위일월`또는`아래일월`버튼행.
3. 왼쪽오른쪽날짜의 HTML 내용높이정도, 확인분.
4. 근거목록 날짜의년월까지목록 , 까지클릭목록 날짜 - day.
```

### 입력예시 2
```
조회Shoprpa차

`요소_search-main-item`의 uuid 예: af2j1k3l
`요소_search-main-item`의 outerHTML 코드예: 
<div class=\"search-main-item\"><div class=\"search-main-tab\"><div class=\"search-tab-hd\"><ul><li class=\"active\"><a href=\"javascript:void(0)\"><i class=\"icon icon-dancheng\"></i>단일</a></li><li><a href=\"javascript:void(0)\"><i class=\"icon icon-wangfan\"></i>반환</a></li><li><a href=\"javascript:void(0)\"><i class=\"icon icon-huancheng\"></i>중변환</a></li><li><a href=\"javascript:void(0)\"><i class=\"icon icon-chepiao\"></i>수정</a></li></ul></div><div class=\"search-tab-bd\"><!-- 단일 --><div class=\"search-tab-item\"><div class=\"search-form\"><div class=\"form-item-group\"><div class=\"form-item\"><label for=\"fromStationText\" class=\"form-label\">출력발송</label><div class=\"form-bd\"><div class=\"input-box input-city\"><input type=\"text\" class=\"input \" value=\"\" id=\"fromStationText\" aria-label=\"입력하세요또는선택출력발송, 키보드위아래행선택, 돌아가기차선택중\" autocomplete=\"off\"><i class=\"icon icon-place\" data-click=\"fromStationText\"></i></div></div></div><div class=\"form-item\"><label for=\"toStationText\" class=\"form-label\">까지</label><div class=\"form-bd\"><div class=\"input-box input-city\"><input type=\"text\" class=\"input\" value=\"\" id=\"toStationText\" aria-label=\"입력하세요또는선택까지, 키보드위아래행선택, 돌아가기차선택중\" autocomplete=\"off\"><i class=\"icon icon-place\" data-click=\"toStationText\"></i></div></div></div><div class=\"city-change\"><i class=\"icon icon-qiehuan\" title=\"\" id=\"danChange\"></i></div></div><div class=\"form-item\"><label for=\"train_date\" class=\"form-label\">출력발송날짜</label><div class=\"form-bd\"><div class=\"input-box input-data\"><input type=\"text\" class=\"input\" id=\"train_date\" aria-label=\"입력하세요날짜, 예20210101\" autocomplete=\"off\"><i class=\"icon icon-date\" data-click=\"train_date\"></i></div></div></div><div class=\"form-item form-item-check\"><div class=\"form-bd\"><ul class=\"check-list check-list-right\"><li id=\"isStudentDan\"><i></i></li><li id=\"isHighDan\">높이/차<i></i></li></ul></div></div><div class=\"form-item form-item-btn\"><a href=\"javascript:void(0)\" class=\"btn btn-primary form-block\" id=\"search_one\">조회&nbsp;&nbsp;&nbsp;&nbsp;문의</a></div></div><!-- <style>.history-list-wrap {white-space: nowrap;margin: 0 auto;overflow: hidden;white-space: nowrap;}.history-list {display: inline;}.history-list li {display: inline;}</style> --><div id=\"search-history\"><div class=\"search-history-bd\"><i id=\"iconLeftHos\" class=\"history-prev icon icon-caret-left\"></i><i id=\"iconRightHos\" class=\"history-next icon icon-caret-right\"></i><div class=\"history-list-wrap\"><ul class=\"history-list\" id=\"history_ul\"><li data-from=\"VAP\" data-to=\"VAP\" data-from-encode=\"%E5%8C%97%E4%BA%AC%E5%8C%97\" data-to-encode=\"%E5%8C%97%E4%BA%AC%E5%8C%97\">-</li><li data-from=\"SHH\" data-to=\"VAP\" data-from-encode=\"%E4%B8%8A%E6%B5%B7\" data-to-encode=\"%E5%8C%97%E4%BA%AC%E5%8C%97\">위-</li></ul></div></div><div class=\"search-history-btn\"><a href=\"javascript:void(0)\">삭제</a></div></div></div><!-- 반환 --><!-- 연결 --><!-- 수정 --></div></div></div>
```

### 출력예시 2
```new_prompt
`{Shoprpa차테이블단일:af2j1k3l}`, 조회

1. 입력`[출력발송]`
2. 입력`[목록의]`
3. 클릭출력발송날짜의`{train_date}`, 대기`{날짜선택기기}`열기, 선택`[출력발송날짜]`
4. 클릭`{조회버튼}`
```


## 데이터 처리예시
### 입력예시 1
```
날짜변환
```

### 출력예시 1
```new_prompt
를날짜에서일형식변환로일형식.
예시입력: 2025/01/01, 목록 형식: YYYY-MM-DD
예시출력: 2025-01-01
```

### 입력예시 2
```

```

### 출력예시 2
```new_prompt
계획개합치기의.
예시입력: {1, 2, 3}, {2, 3, 4}
예시출력: {2, 3}
```