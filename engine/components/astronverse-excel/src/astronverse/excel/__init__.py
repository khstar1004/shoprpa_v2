from enum import Enum


class ApplicationType(Enum):
    """생성프로그램유형"""

    EXCEL = "Excel"  # Excel
    WPS = "WPS"  # WPS
    DEFAULT = "Default"  # 시스템선택


class FileExistenceType(Enum):
    """파일이름저장된 관리방식"""

    OVERWRITE = "overwrite"  # 덮어쓰기기존있음파일
    RENAME = "rename"  # 생성파일본
    CANCEL = "cancel"  # 건너뛰기저장


class SaveType(Enum):
    """저장유형"""

    SAVE = "save"  # 저장
    SAVE_AS = "save_as"  # 저장로
    ABORT = "abort"  # 아니오저장


class SaveType_ALL(Enum):
    """저장유형(닫기모든문서시)"""

    SAVE = "save"  # 저장
    ABORT = "abort"  # 아니오저장


class CloseType(Enum):
    """닫기유형"""

    NOTSAVE = "not_save"  # 아니오저장
    SAVE = "save"  # 저장
    SAVE_AS = "save_as"  # 저장로


class ReadRangeType(Enum):
    """가져오기 유형"""

    CELL = "cell"  # 셀
    ROW = "row"  # 행
    COLUMN = "column"  # 열
    AREA = "area"  # 
    ALL = "all"  # 완료


class EditRangeType(Enum):
    """유형"""

    ROW = "row"  # 행
    COLUMN = "column"  # 열
    AREA = "area"  # 
    CELL = "cell"  # 셀


class FontType(Enum):
    """문자유형"""

    NO_CHANGE = "no_change"  # 기존
    BOLD = "bold"  # 
    ITALIC = "italic"  # 
    BOLD_ITALIC = "bold_italic"  # 
    NORMAL = "normal"  # 일반


class PasteType(Enum):
    """붙여넣기유형"""

    ALL = "all"  # 전체붙여넣기
    VALUE_AND_FORMAT = "value_and_format"  # 값및숫자형식
    FORMAT = "format"  # 형식
    EXCLUDE_FRAME = "exclude_frame"  # 가장자리제거외부
    COL_WIDTH_ONLY = "col_width_only"  # 열너비
    FORMULA_ONLY = "formula_only"  # 방식
    FORMULA_AND_FORMAT = "formula_and_format"  # 방식및숫자형식
    PASTE_VALUE = "paste_value"  # 붙여넣기값


class NumberFormatType(Enum):
    """숫자형식유형"""

    NO_CHANGE = "no_change"  # 기존
    GENERAL = "G/통신사용형식"  # 일반
    NUMBER = "0.00"  # 숫자
    CURRENCY = "¥#,##0.00"  # 
    ACCOUNTING = "_(¥* #,##0.00_);_(¥* (#,##0.00);_(¥* -_0_0_);_(@_)"  # 계획사용
    SHORT_DATE = "yyyy/m/d"  # 짧음날짜
    LONG_DATE = "yyyy년mm월dd일"  # 길이날짜
    TIME = "h:mm:ss AM/PM"  # 시간
    PERCENT = "0.00%"  # 분
    FRACTION = "# ?/?"  # 분데이터
    SCIENTIFIC = "0.00E+00"  # Shoprpa데이터
    TEXT = "@"  # 텍스트
    CUSTOM = "other"  # 지정


class FontNameType(Enum):
    """문자이름유형"""

    NO_CHANGE = "기존"
    HEITI = ""
    FANGSONG = ""
    SONGTI = ""
    WEIRUANYAHEI = "소프트"
    WEIRUANYAHEI_LIGHT = "소프트 Light"
    HUAWENZHONGSONG = "문서중"
    HUAWENFANGSONG = "문서"
    HUAWENSONGTI = "문서"
    HUAWENCAIYUN = "문서"
    HUAWENXINWEI = "문서새"
    HUAWENKAITI = "문서"
    HUAWENHUPO = "문서"
    HUAWEIXIHEI = "문서"
    HUAWENXINGKAI = "문서행"
    HUAWENLISHU = "문서서"
    YOUYUAN = ""
    LISHU = "서"
    FANGZHENGYAOTI = "방법정상"
    FANGZHENGSHUTI = "방법정상"
    XINSONGTI = "새"
    WEIRUANZHENGHEITI_LIGHT = "정상 Light"
    WEIRUANZHENGHEITI = "정상"
    XIMINGTI = "_HKSCS-ExtB"
    DENGXIAN = "대기"
    DENGXIAN_LIGHT = "대기 Light"
    KAITI = ""
    XIMINGZHI = "-ExtB"
    XINXIMINGZHI = "새-ExtB"
    ONYX = "Onyx"
    MYANMAR_TEXT = "Myanmar Text"
    NIAGARA_ENGRAVED = "Niagara Engraved"
    NIAGARA_SOLID = "Niagara Solid"
    NIRMALA_UL = "Nirmala Ul"
    NIRMALA_UL_SEMILIGHT = "Nirmala Ul Semilight"
    OCR_A_EXTENDED = "OCR A Extended"
    OLD_ENGLISH_TEXT = "Old English Text MT"
    PALACE_SCRIPT_MT = "Palace Script MT"
    POOR_RICHARD = "Poor Richard"
    PAPYRUS = "Papyrus"
    PARCHMENT = "Parchment"
    PERPETUA = "Perpetua"
    PERPETUA_TILTING_MT = "Perpetua Tilting MT"
    PLAYBILL = "Playbill"
    MV_BOLI = "MV Boli"
    PRISTINA = "Pristina"
    RAGE_ITALIC = "Rage Italic"
    RAVIE = "Ravie"
    PALATO_LINOTYPE = "Palatino Linotype"
    MT_EXTRA = "MT Extra"
    MS_GOTHIC = "MS Gothic"
    MS_REFERENCE_SPECIALTY = "MS Reference Specialty"
    MARLETT = "Marlett"
    MATURA_MT_SCRIPT_CAPITALS = "Matura MT Script Capitals"
    MICROSOFT_HIMALAYA = "Microsoft Himalaya"
    MICROSOFT_JHENGHEI_UI = "Microsoft JhengHei UI"
    MICROSOFT_JHENGHEI_UI_LIGHT = "Microsoft JhengHei UI Light"
    MICROSOFT_NEW_TAI_LUE = "Microsoft New Tai Lue"
    MICROSOFT_PHAGSPA = "Microsoft PhagsPa"
    MICROSOFT_SANS_SERIF = "Microsoft Sans Serif"
    MICROSOFT_TAILE = "Microsoft Tai Le"
    MICROSOFT_UIGHUR = "Microsoft Uighur"
    MICROSOFT_YAHEI_Ul = "Microsoft Yahei Ul"
    MICROSOFT_yahei_Ul_LIGHT = "Microsoft YaHei Ul Light"
    MICROSOFT_YI_BAITI = "Microsoft Yi Baiti"
    MISTRAL = "Mistral"
    MODERN_NO20 = "Modern No.20"
    MOGOLIAN_BAITI = "Mogolian Baiti"


class HorizontalAlign(Enum):
    """수평평면방식"""

    NO_CHANGE = "no_change"  # 기존
    DEFAULT = "default"  # 일반
    LEFT = "left-aligned"  # 왼쪽
    RIGHT = "right-aligned"  # 오른쪽
    CENTER = "center"  # 중
    PADDING = "padding"  # 
    BOTH = "aligned_both_sides"  # 단말
    CROSS = "center_cross_column"  # 열중
    DISTRIBUTED = "distributed_align"  # 분


class VerticalAlign(Enum):
    """수직직선방식"""

    NO_CHANGE = "no_change"  # 기존
    UP = "up"  # 위
    MIDDLE = "middle"  # 중
    DOWN = "down"  # 아래
    BOTH = "aligned_both_sides"  # 단말
    DISTRIBUTED = "distributed_align"  # 분


class ClearType(Enum):
    """지우기유형"""

    CONTENT = "content"  # 지우기내용
    STYLE = "style"  # 지우기형식
    ALL = "all"  # 지우기내용및형식


class SheetRangeType(Enum):
    """테이블유형"""

    ACTIVATED = "activated"  # 현재테이블
    ALL = "all"  # 모든테이블


class DeleteCellDirection(Enum):
    """삭제셀후데이터방법"""

    LOWER_MOVE_UP = "lower_move_up"  # 아래방법셀위
    RIGHT_MOVE_LEFT = "right_move_left"  # 오른쪽셀왼쪽


class InsertType(Enum):
    """삽입유형"""

    ROW = "row"  # 행
    COLUMN = "column"  # 열


class EnhancedInsertType(Enum):
    """증가강함삽입유형"""

    ROW = "row"  # 지정행삽입
    COLUMN = "column"  # 지정열삽입
    ADD_ROWS = "add_rows"  # 에서후일행후삽입
    ADD_COLUMNS = "add_columns"  # 에서후일열후삽입


class RowDirectionType(Enum):
    """삽입행방법"""

    UPPER = "upper"  # 위삽입
    LOWER = "lower"  # 아래삽입


class ColumnDirectionType(Enum):
    """삽입열방법"""

    LEFT = "left"  # 왼쪽삽입
    RIGHT = "right"  # 오른쪽삽입


class MergeOrSplitType(Enum):
    """병합또는분할유형"""

    MERGE = "merge"  # 병합
    SPLIT = "split"  # 분할


class CopySheetType(Enum):
    """복사테이블유형"""

    CURRENT_WORKBOOK = "current_workbook"  # 현재
    OTHER_WORKBOOK = "other_workbook"  # 


class CopySheetLocationType(Enum):
    """복사테이블위치유형"""

    BEFORE = "before"  # 복사까지현재테이블전
    AFTER = "after"  # 복사까지현재테이블후
    FIRST = "first"  # 복사까지일개테이블
    LAST = "last"  # 복사까지후일개테이블


class MoveSheetType(Enum):
    """테이블유형"""

    MOVE_AFTER = "move_after"  # 까지목록 테이블후
    MOVE_BEFORE = "move_before"  # 까지목록 테이블전
    MOVE_TO_FIRST = "move_to_first"  # 까지일개테이블
    MOVE_TO_LAST = "move_to_last"  # 까지후일개테이블


class SearchRangeType(Enum):
    """조회유형"""

    ALL = "all"  # 완료
    ROW = "row"  # 행
    COLUMN = "column"  # 열
    AREA = "area"  # 


class SearchSheetType(Enum):
    """조회테이블유형"""

    ALL = "all"  # 전체테이블
    ONE = "one"  # 단일개테이블


class MatchCountType(Enum):
    """매칭수유형"""

    ALL = "all"  # 모든결과
    FIRST = "first"  # 일개결과


class SearchResultType(Enum):
    """조회결과출력유형"""

    CELL = "cell"  # 반환셀위치
    COL_AND_ROW = "col_and_row"  # 반환열및행


class ImageSizeType(Enum):
    """이미지크기제어유형"""

    SCALE = "scale"  # 조정
    NUMBER = "number"  # 조정높이정도및너비정도데이터값
    AUTO = "auto"  # 조정크기매칭


class InsertFormulaDirectionType(Enum):
    """방식삽입방법"""

    DOWN = "down"  # 아래삽입
    RIGHT = "right"  # 오른쪽삽입


class CreateCommentType(Enum):
    """비고삽입방식"""

    POSITION = "position"  # 셀위치삽입
    CONTENT = "content"  # 내용검색삽입


class ColumnOutputType(Enum):
    """열출력형식"""

    LETTER = "letter"  # 문자열
    NUMBER = "number"  # 숫자열


class RowType(Enum):
    """행유형"""

    ALL = "all"  # 모든행
    ONE_ROW = "one_row"  # 단일행


class ColumnType(Enum):
    """열유형"""

    ALL = "all"  # 모든열
    ONE_COLUMN = "one_column"  # 단일열


class SetType(Enum):
    """유형"""

    VALUE = "value"  # 값
    AUTO = "auto"  # 행높이/열너비


class CloseRangeType(Enum):
    """닫기문서"""

    ONE = "one"  # 현재문서
    ALL = "all"  # 모든문서


class SheetInsertType(Enum):
    """테이블삽입위치유형"""

    FIRST = "first"  # 새테이블성공로일개테이블
    LAST = "last"  # 새테이블성공로후일개테이블
    BEFORE = "before"  # 새테이블삽입까지...테이블전
    AFTER = "after"  # 새테이블삽입까지...테이블후


class EditType(Enum):
    """입력유형, 패키지추가 입력및덮어쓰기"""

    OVERWRITE = "overwrite"  # 덮어쓰기
    APPEND = "append"  # 추가 입력