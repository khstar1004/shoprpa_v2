from abc import ABC, abstractmethod
from typing import Any, Optional

from bs4 import BeautifulSoup, Comment, NavigableString


class HTMLProcessor(ABC):
    """
    HTML관리기기유형
    로가능컴포넌트선택프로세스HTML파싱가능의열기 연결
    """

    @abstractmethod
    def parse_html(self, html: str, **kwargs) -> dict[str, Any]:
        """
        파싱HTML내용, 가져오기원정보

        Args:
            html: HTML문자열내용
            **kwargs: 금액외부매개변수, 가능패키지, 위아래문서대기정보

        Returns:
            파싱결과딕셔너리, 형식유형지정
        """
        pass


class DefaultHTMLProcessor(HTMLProcessor):
    """
    HTML관리기기
    의HTML관리가능, 가능로매개
    """

    def parse_html(self, html: str, **kwargs) -> dict[str, Any]:
        """: 반환결과"""

        # 가능으로의HTML파싱
        # 또는출력NotImplementedError안내필요
        return {"elements": [], "message": "요청 의HTML파싱"}


# 단일방식의관리기기관리관리기기
class HTMLProcessorManager:
    """
    HTML관리기기관리관리기기
    지원회원가입및아니오의HTML관리기기
    """

    def __init__(self):
        self._processors: dict[str, HTMLProcessor] = {}
        self._current_processor: Optional[HTMLProcessor] = None

        # 회원가입관리기기
        self.register_processor("default", DefaultHTMLProcessor())
        self.set_current_processor("default")

    def register_processor(self, name: str, processor: HTMLProcessor) -> None:
        """
        회원가입HTML관리기기

        Args:
            name: 관리기기이름
            processor: 관리기기
        """
        if not isinstance(processor, HTMLProcessor):
            raise TypeError("관리기기HTMLProcessor")

        self._processors[name] = processor

    def set_current_processor(self, name: str) -> None:
        """
        현재사용의관리기기

        Args:
            name: 관리기기이름
        """
        if name not in self._processors:
            raise ValueError(f"찾을 수 없는 이름로 '{name}' 의관리기기")

        self._current_processor = self._processors[name]

    def get_current_processor(self) -> HTMLProcessor:
        """가져오기현재관리기기"""
        if self._current_processor is None:
            raise RuntimeError("미완료현재관리기기")

        return self._current_processor

    def get_available_processors(self) -> list[str]:
        """가져오기모든가능사용관리기기이름"""
        return list(self._processors.keys())

    def remove_processor(self, name: str) -> None:
        """제거관리기기"""
        if name == "default":
            raise ValueError("할 수 없음제거관리기기")

        if name in self._processors:
            del self._processors[name]

            # 결과가제거의예현재관리기기, 까지관리기기
            if self._current_processor == self._processors.get(name):
                self.set_current_processor("default")


# 전체영역관리기기관리관리기기
html_processor_manager = HTMLProcessorManager()


# 외부모듈선택직선연결가져오기사용아래의데이터
def parse_html(html: str = "", **kwargs) -> str:
    """
    사용현재HTML관리기기파싱HTML
    예외부의필요연결, 보관후내용
    """

    def is_visible(tag):
        """요소여부가능()"""
        if not tag or not hasattr(tag, "name"):
            return False
        # hidden 속성
        if tag.has_attr("hidden"):
            return False
        # style 속성중패키지 display: none 또는 visibility: hidden
        style = tag.get("style", "").lower()
        if "display: none" in style or "visibility: hidden" in style:
            return False
        return True

    def should_keep_tag(tag):
        """여부보관해당태그(여부가능또는있음위치 지정가격값)"""
        interactive_tags = {
            "input",
            "button",
            "a",
            "select",
            "textarea",
            "option",
            "label",
            "form",
            "div",
            "span",
            "ul",
            "li",
            "table",
            "tr",
            "td",
        }
        return tag.name in interactive_tags

    def should_keep_attr(attr_name):
        """보관있음사용의속성"""
        useful_attrs = {
            "id",
            "class",
            "name",
            "type",
            "value",
            "placeholder",
            "aria-label",
            "aria-labelledby",
            "role",
            "for",
            "href",
            "src",
            "alt",
            "title",
            "data-",  # 모든 data-* 속성
            "tabindex",
            "disabled",
            "readonly",
            "checked",
            "selected",
        }
        if attr_name.startswith("data-"):
            return True
        return attr_name in useful_attrs

    def remove_blank_text_nodes(soup):
        """제거빈의텍스트(패키지행, 대기)"""
        for element in soup.descendants:
            if isinstance(element, NavigableString):
                if not element.strip():  # 결과가제거빈후비어 있습니다
                    element.extract()  # 에서 DOM 중제거

    # 관리텍스트, 제거`\n    `중의행및공백
    def clean_text_children(tag):
        children = list(tag.children)
        new_children = []
        found_text = False
        for child in children:
            if isinstance(child, NavigableString):
                text = str(child)
                stripped = text.strip()
                if stripped:
                    if not found_text:
                        # 보관일개있음텍스트(예``)
                        new_children.append(stripped)
                        found_text = True
                    # 결과가완료까지텍스트, 후빈일
                else:
                    # 빈: 있음에서까지있음텍스트시가능보관(일아니오보관)
                    pass
            else:
                # 보관태그(예 <i>), 후삭제
                new_children.append(child)
        tag.clear()
        tag.extend(new_children)

    soup = BeautifulSoup(html, "html.parser", multi_valued_attributes=False)

    # 1. 제거 <script>, <style>, <noscript>, <meta>, <link> 대기 UI 요소
    for tag in soup(["script", "style", "noscript", "meta", "link", "head", "title"]):
        tag.decompose()

    # 2. 제거비고(제거방식보관)
    if True:
        for comment in soup.find_all(text=lambda text: isinstance(text, Comment)):
            comment.extract()

    # 3. 모든태그, 관리속성 + 제거할 수 없음/없음사용요소
    tags_to_remove = []
    for tag in soup.find_all():
        # 건너뛰기텍스트대기
        if not hasattr(tag, "name") or not tag.name:
            continue

        # 여부가능
        if not is_visible(tag):
            tags_to_remove.append(tag)
            continue

        # 여부값보관(가능또는내용기기)
        if not should_keep_tag(tag):
            # 결과가예텍스트내용기기없음있음사용속성, 가능제거
            if not tag.get("id") and not tag.get("class") and not tag.find():
                tags_to_remove.append(tag)
            continue

        # 보관있음사용속성, 관리
        attrs_to_remove = []
        for attr in list(tag.attrs.keys()):
            if not should_keep_attr(attr):
                attrs_to_remove.append(attr)
        for attr in attrs_to_remove:
            del tag[attr]

    # 실행제거
    for tag in tags_to_remove:
        tag.decompose()

    # 가능패키지`문서문자 + 행 + <i>`의태그행관리

    for li in soup.find_all("li", id=True):  #  id 의 li 예선택
        clean_text_children(li)

    # 4. 제거빈의내용기기(선택 가능)
    for tag in soup.find_all():
        if tag.name in {"div", "span", "ul", "li", "form", "section"}:
            # 결과가있음태그, 있음텍스트(또는있음빈), 있음닫기 속성, 이면제거
            if not tag.find() and (not tag.text.strip()) and not tag.get("id") and not tag.get("class"):
                tag.decompose()

    # 후일: 관리빈텍스트
    remove_blank_text_nodes(soup)

    # 반환 HTML
    return str(soup).replace("\n", "")