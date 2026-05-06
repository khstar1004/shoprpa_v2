import json
import os
from typing import Optional

from astronverse.scheduler.apis.response import ResCode, res_msg
from astronverse.scheduler.core.datatable.excel_service import ExcelService
from astronverse.scheduler.core.datatable.file_watcher import AsyncFileWatcher
from astronverse.scheduler.core.svc import Svc, get_svc
from astronverse.scheduler.logger import logger
from fastapi import APIRouter, Depends
from fastapi.responses import StreamingResponse
from pydantic import BaseModel

router = APIRouter()


# ==================== 요청 유형 ====================


class OpenDataTableRequest(BaseModel):
    """열기데이터테이블요청 """

    project_id: str  # ID
    filename: str  # 파일이름(아니오이름)


class UpdateCellsRequest(BaseModel):
    """업데이트셀요청 """

    project_id: str  # ID
    filename: str  # 파일이름
    updates: list[dict]  # 업데이트목록 [{"sheet": str, "row": int, "col": int, "value": any}]


class CloseDataTableRequest(BaseModel):
    """닫기데이터테이블요청 """

    project_id: str  # ID
    filename: str  # 파일이름


# ==================== 도구데이터 ====================


def get_project_dir(svc: Svc, project_id: str) -> str:
    """
    가져오기 디렉터리경로(ID디렉터리아래의astron디렉터리)

    Args:
        svc: 서비스위아래문서
        project_id: ID

    Returns:
        디렉터리의경로({venv_base_dir}/{project_id}/astron)
    """
    return os.path.join(svc.config.venv_base_dir, project_id, "astron")


def ensure_project_dir(svc: Svc, project_id: str) -> str:
    """
    확인디렉터리존재함, 결과가찾을 수 없습니다이면생성

    Args:
        svc: 서비스위아래문서
        project_id: ID

    Returns:
        디렉터리의경로
    """
    project_dir = get_project_dir(svc, project_id)
    if not os.path.exists(project_dir):
        os.makedirs(project_dir, exist_ok=True)
        logger.info(f"Created project directory: {project_dir}")
    return project_dir


def get_excel_service(svc: Svc, project_id: str) -> ExcelService:
    """
    가져오기 Excel 서비스

    Args:
        svc: 서비스위아래문서
        project_id: ID

    Returns:
        ExcelService 
    """
    project_dir = get_project_dir(svc, project_id)
    return ExcelService(project_dir)


# ==================== 의파일기기관리관리 ====================

# 저장의 AsyncFileWatcher , key 로파일 경로
_active_watchers: dict[str, AsyncFileWatcher] = {}


def get_active_watcher(file_path: str) -> Optional[AsyncFileWatcher]:
    """가져오기 의파일기기"""
    return _active_watchers.get(os.path.normpath(file_path))


def set_active_watcher(file_path: str, watcher: AsyncFileWatcher):
    """의파일기기"""
    _active_watchers[os.path.normpath(file_path)] = watcher


def remove_active_watcher(file_path: str):
    """제거의파일기기"""
    normalized_path = os.path.normpath(file_path)
    if normalized_path in _active_watchers:
        del _active_watchers[normalized_path]


# ==================== SSE 연결 ====================


@router.get("/stream")
async def datatable_stream(project_id: str, filename: str, svc: Svc = Depends(get_svc)):
    """
    SSE 방식연결: 열기/생성 Excel 파일, 방식반환데이터, 파일변수변경

    - 결과가디렉터리찾을 수 없습니다, 생성
    - 결과가파일찾을 수 없습니다, 생성빈 Excel 파일
    - 방식반환 Excel 데이터(행전송)
    - 후파일변수변경, 있음변수변경시알림

    Query Args:
        project_id: ID
        filename: Excel 파일이름(아니오이름)

    SSE Events:
        - created: 파일생성(전찾을 수 없습니다)
        - sheet_start: 테이블열기 , 패키지 sheet 이름및행열데이터
        - row: 행데이터
        - sheet_end: 테이블결과
        - complete: 데이터로드완료
        - file_changed: 파일외부모듈수정
        - file_deleted: 파일삭제
        - heartbeat: 보관연결
        - error: 오류정보
    """

    async def event_generator():
        watcher = None

        try:
            # 1. 확인디렉터리존재함
            ensure_project_dir(svc, project_id)

            excel_service = get_excel_service(svc, project_id)
            file_path = excel_service.get_file_path(filename)

            # 2. 조회파일여부존재함, 찾을 수 없습니다이면출력오류
            if not excel_service.file_exists(filename):
                raise FileNotFoundError(f"File not found: {filename}")

            # # 3. 방식가져오기 Excel 데이터
            # for row_data in excel_service.read_file_stream(filename):
            #     event_type = row_data.get("type", "data")
            #     yield format_sse_event(event_type, row_data)

            # 4. 시작파일
            watcher = AsyncFileWatcher(file_path)
            set_active_watcher(file_path, watcher)

            # 5. 파일변수변경
            async for event in watcher.start():
                yield format_sse_event(event["type"], event)

        except FileNotFoundError as e:
            logger.error(f"File not found: {e}")
            yield format_sse_event("error", {"message": str(e)})
        except Exception as e:
            logger.exception(f"Error in datatable stream: {e}")
            yield format_sse_event("error", {"message": str(e)})
        finally:
            # 관리
            if watcher:
                watcher.stop()
                remove_active_watcher(file_path)

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )


def format_sse_event(event_type: str, data: dict) -> str:
    """
    형식 SSE 파일

    Args:
        event_type: 파일유형
        data: 파일데이터

    Returns:
        SSE 형식의문자열
    """
    json_data = json.dumps(data, ensure_ascii=False)
    return f"event: {event_type}\ndata: {json_data}\n\n"


# ==================== REST API 연결 ====================


@router.post("/open")
def datatable_open(req: OpenDataTableRequest, svc: Svc = Depends(get_svc)):
    """
    열기/생성 Excel 파일(일반환전체데이터)

    - 결과가디렉터리찾을 수 없습니다, 생성
    - 결과가파일찾을 수 없습니다, 생성빈 Excel 파일
    - 결과가필요관리대파일, 생성사용 /stream 연결

    Args:
        req: 패키지 project_id 및 filename 의요청 

    Returns:
        Excel 파일의정수근거
    """
    try:
        # 1. 확인디렉터리존재함
        ensure_project_dir(svc, req.project_id)

        excel_service = get_excel_service(svc, req.project_id)

        # 2. 조회파일여부존재함, 찾을 수 없습니다이면생성
        created = False
        if not excel_service.file_exists(req.filename):
            excel_service.create_file(req.filename)
            created = True

        # 3. 가져오기파일데이터
        data = excel_service.read_file(req.filename)
        data["project_id"] = req.project_id
        data["created"] = created

        return res_msg(code=ResCode.SUCCESS, msg="ok", data=data)

    except Exception as e:
        logger.exception(f"Error opening datatable: {e}")
        return res_msg(code=ResCode.ERR, msg=str(e))


@router.post("/update-cells")
def datatable_update_cells(req: UpdateCellsRequest, svc: Svc = Depends(get_svc)):
    """
    업데이트지정셀(증가량업데이트)

    Args:
        req: 패키지 project_id, filename 및 updates 목록의요청 

    Returns:
        결과
    """
    try:
        # 조회디렉터리여부존재함
        project_dir = get_project_dir(svc, req.project_id)
        logger.info(f"Updating cells in {project_dir}")
        if not os.path.exists(project_dir):
            return res_msg(code=ResCode.ERR, msg=f"Project directory not found: {project_dir}")

        excel_service = get_excel_service(svc, req.project_id)

        # 조회파일여부존재함
        if not excel_service.file_exists(req.filename):
            return res_msg(code=ResCode.ERR, msg=f"File not found: {req.filename}")

        file_path = excel_service.get_file_path(req.filename)

        # 일시중지파일, 트리거
        watcher = get_active_watcher(file_path)
        if watcher:
            watcher.pause_watching(duration=2.0)

        # 업데이트셀
        excel_service.update_cells(req.filename, req.updates)

        return res_msg(
            code=ResCode.SUCCESS,
            msg="ok",
            data={"project_id": req.project_id, "filename": req.filename, "updated": len(req.updates)},
        )

    except FileNotFoundError as e:
        return res_msg(code=ResCode.ERR, msg=f"File not found: {req.filename}")
    except Exception as e:
        logger.exception(f"Error updating cells: {e}")
        return res_msg(code=ResCode.ERR, msg=str(e))


@router.post("/close")
def datatable_close(req: CloseDataTableRequest, svc: Svc = Depends(get_svc)):
    """
    닫기데이터테이블, 중지파일

    Args:
        req: 패키지 project_id 및 filename 의요청 

    Returns:
        결과
    """
    try:
        excel_service = get_excel_service(svc, req.project_id)
        file_path = excel_service.get_file_path(req.filename)

        # 중지파일
        watcher = get_active_watcher(file_path)
        if watcher:
            watcher.stop()
            remove_active_watcher(file_path)

        return res_msg(code=ResCode.SUCCESS, msg="ok", data={"project_id": req.project_id, "filename": req.filename})

    except Exception as e:
        logger.exception(f"Error closing datatable: {e}")
        return res_msg(code=ResCode.ERR, msg=str(e))


@router.post("/delete")
def datatable_delete(req: CloseDataTableRequest, svc: Svc = Depends(get_svc)):
    """
    삭제 Excel 파일

    Args:
        req: 패키지 project_id 및 filename 의요청 

    Returns:
        결과
    """
    try:
        excel_service = get_excel_service(svc, req.project_id)
        file_path = excel_service.get_file_path(req.filename)

        # 중지파일
        watcher = get_active_watcher(file_path)
        if watcher:
            watcher.stop()
            remove_active_watcher(file_path)

        # 삭제파일
        deleted = excel_service.delete_file(req.filename)

        if deleted:
            return res_msg(
                code=ResCode.SUCCESS,
                msg="ok",
                data={"project_id": req.project_id, "filename": req.filename, "deleted": True},
            )
        else:
            return res_msg(code=ResCode.ERR, msg="File not found")

    except Exception as e:
        logger.exception(f"Error deleting datatable: {e}")
        return res_msg(code=ResCode.ERR, msg=str(e))