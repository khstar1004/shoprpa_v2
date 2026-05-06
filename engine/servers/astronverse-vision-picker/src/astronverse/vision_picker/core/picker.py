import json
import os
import platform
import socket
import sys
import threading
import time
from collections import deque

import cv2
import pyautogui
from astronverse.vision_picker.core import PickType, Status
from astronverse.vision_picker.core.core import IPickCore, IRectHandler
from astronverse.vision_picker.core.cv_match import AnchorMatch
from astronverse.vision_picker.core.cv_picker import ImageDetector
from astronverse.vision_picker.logger import logger
from pynput import keyboard

current_directory = os.getcwd()
desktop_filepath = os.path.join(current_directory, "imgs", "desktop.png")
partial_filepath = os.path.join(current_directory, "imgs", "partial.png")
alt_filepath = os.path.join(current_directory, "imgs", "alt_picker.png")
target_filepath = os.path.join(current_directory, "imgs", "target.png")
anchor_filepath = os.path.join(current_directory, "imgs", "anchor.png")

os.makedirs(os.path.join(current_directory, "imgs"), exist_ok=True)

if sys.platform == "win32":
    from astronverse.vision_picker.core.core_win import PickCore, RectHandler
elif platform.system() == "Linux":
    from astronverse.vision_picker.core.core_unix import PickCore, RectHandler
else:
    raise NotImplementedError("Your platform (%s) is not supported by (%s)." % (platform.system(), "clipboard"))

PickCore: IPickCore = PickCore()
RectHandler: IRectHandler = RectHandler()


class Socket:
    def __init__(self):
        #  socket 단말
        self.__socket_port = 11001
        # 생성 socket 객체, 및높이모듈분통신
        self.__socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.receive_data = None

    def __enter__(self):
        # 이동위아래문서관리관리기기
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        # 출력전높이
        try:
            self.hide_wnd()
        except Exception as e:
            pass

    def send_msg(self, message):
        # 전송메시지까지지정단말
        self.__socket.sendto(message, ("localhost", self.__socket_port))

    def hide_wnd(self):
        self.send_msg('{"operation":"hide","data":""}')

    def receive_rect(self):
        # 수신메시지파싱출력값
        operation = None
        rect = None
        try:
            data, _ = self.__socket.recvfrom(1024)
            if data:
                self.receive_data = data.decode("utf-8")
                logger.info(f"receive_data:{self.receive_data}")
                operation, rect = self.parse_response(self.receive_data)
            return operation, rect
        except OSError as e:
            pass

    def parse_response(self, data):
        try:
            # 파싱 JSON 데이터
            response = json.loads(data)
            # 가져오기 값
            operation = response.get("Operation")
            box = response.get("Boxes")[0] if response.get("Boxes") else None

            left = box.get("Left") if box else None
            top = box.get("Top") if box else None
            right = box.get("Right") if box else None
            bottom = box.get("Bottom") if box else None

            logger.info(
                f"Received coordinates:operation={operation} left={left}, top={top}, right={right}, bottom={bottom}"
            )
            return operation, (left, top, right, bottom)

        except json.JSONDecodeError as e:
            logger.info(f"Error decoding JSON: {e}")

    def send_rect(self, operation="picking", status="", rect=(0, 0, 0, 0), msg=""):
        try:
            # 전송정보
            message = {
                "Operation": operation,
                "Type": status,
                "Boxes": [
                    {
                        "Left": rect[0],
                        "Top": rect[1],
                        "Right": rect[0] + rect[2],
                        "Bottom": rect[1] + rect[3],
                        "Msg": msg,
                    }
                ],
            }
            # 를메시지변환로 JSON 형식
            json_message = json.dumps(message)
            logger.info(json_message)
            # 전송메시지
            self.send_msg(json_message.encode("utf-8"))
        except Exception as e:
            logger.info(f"Error sending message: {e}")

    def send_signal(self, operation, status):
        try:
            # 전송정보 메시지
            message = {"Operation": operation, "Type": status}
            json_message = json.dumps(message)
            self.send_msg(json_message.encode("utf-8"))
        except Exception as e:
            logger.info(f"Error sending message: {e}")


class CVPicker:
    _instance = None

    def __new__(cls, *args, **kwargs):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    def __init__(self, status: Status = Status.START, picktype: PickType = PickType.TARGET, anchor_pick_img=None):
        # 선택공가능
        self.bboxes = None
        self.x = None
        self.y = None
        self.screen_width = 0
        self.screen_height = 0

        # 열기시작키보드
        self.keyboard_listener = None
        self.mouse_listener = None
        self.receiver = None
        self.stop_event = threading.Event()
        self.operation = None

        # 저장사용의스크린샷및Rect
        self.win_rect = None  # 저장창Rect
        self.partial_screenshot = None  # 선택창스크린샷
        self.partial_rect = None  # 관리경과후의창Rect
        self.activate_win = None
        self.selected_boxes = None

        self.desktop_image = None  # 사용저장전체기존이미지의cv2이미지
        self.target_rect = None  # 사용저장목록 요소의위치
        self.anchor_rect = None  # 사용저장요소의위치
        self.draw_rect = None  # 사용제어요소높이의위치

        self.match_box = None
        self.match_rect = None

        # 선택상태
        self.__status = status
        self.pick_type = picktype
        self.pick_res = None

        self.start_time = None
        self.event = threading.Event()
        self.run_signal = True
        self.anchor_pick_img = anchor_pick_img

        self.current_keys = set()

    def set(self, status: Status = Status.START, picktype: PickType = PickType.TARGET, anchor_pick_img=None):
        self.__status = status
        self.pick_type = picktype
        self.anchor_pick_img = anchor_pick_img
        self.run_signal = True

    def start_keyboard_listener(self):
        self.keyboard_listener = keyboard.Listener(on_press=self.on_press, on_release=self.on_release)
        self.keyboard_listener.start()

    # 중지키보드
    def stop_keyboard_listener(self):
        if self.keyboard_listener:
            self.keyboard_listener.stop()

    def get_minbox(self, x_pos, y_pos, bboxes):
        min_bbox = None
        min_area = float("inf")
        if not x_pos or not y_pos:
            return min_bbox
        for bbox in bboxes:
            bx, by, bw, bh = bbox
            if bx <= x_pos < bx + bw and by <= y_pos < by + bh:
                # 계획현재의
                area = bw * bh
                if area < min_area:
                    min_area = area
                    min_bbox = bbox
                    break
        return min_bbox

    # 기록수신까지의socket정보 
    def receive_message(self, socket):
        while not self.stop_event.is_set():
            try:
                self.operation, self.target_rect = socket.receive_rect()
            except Exception as e:
                pass

    def get_rect(self):
        if self.bboxes is not None:
            min_bbox = self.get_minbox(self.x, self.y, self.bboxes)

            if min_bbox != self.draw_rect:
                if min_bbox is not None:
                    bx, by, bw, bh = min_bbox
                else:
                    bx, by, bw, bh = 0, 0, 0, 0
                    # bx, by, bw, bh = self.partial_rect[0], self.partial_rect[1], self.partial_rect[2], \
                    # self.partial_rect[3]

                bx = max(0, min(bx, self.screen_width))
                by = max(0, min(by, self.screen_height))
                bw = max(0, min(bw, self.screen_width - bx))
                bh = max(0, min(bh, self.screen_height - by))

                self.draw_rect = (bx, by, bw, bh)
        else:
            pass
        return self.draw_rect

    def on_press(self, key):
        """
        전체영역키보드파일
        :param key:
        :return:
        """

        self.current_keys.add(key)
        logger.info("press key num: {}".format(len(self.current_keys)))
        logger.info("key:{}".format(key))

        if key == keyboard.Key.esc:
            # 아래ESC 이동출력상태
            self.__status = Status.CANCEL
            logger.info("아래ESC: {}".format(self.__status))

        if (
            ({keyboard.Key.ctrl_l} == self.current_keys or {keyboard.Key.ctrl_r} == self.current_keys)
            and len(self.current_keys) == 1
            and self.__status == Status.WAIT_SIGNAL
        ):
            # 아래CTRL 이동통신선택상태
            self.__status = Status.CV_CTRL
            logger.info("아래CTRL: {}".format(self.__status))
            self.desktop_image = pyautogui.screenshot()

        if (
            ({keyboard.Key.alt_l} == self.current_keys or {keyboard.Key.alt_gr} == self.current_keys)
            and len(self.current_keys) == 1
            and self.__status == Status.WAIT_SIGNAL
        ):
            # 아래ALT 이동선택 가능상태
            self.__status = Status.CV_ALT
            logger.info("아래ALT: {}".format(self.__status))

        if (
            ({keyboard.Key.shift_l} == self.current_keys or {keyboard.Key.shift_r} == self.current_keys)
            and len(self.current_keys) == 1
            and self.pick_type == PickType.TARGET
        ):
            # 아래SHIFT 반환상태
            self.__status = Status.CV_SHIFT
            logger.info("아래SHIFT: {}".format(self.__status))

    def on_release(self, key):
        try:
            self.current_keys.remove(key)
            logger.info("release key num: {}".format(len(self.current_keys)))
            logger.info(len(self.current_keys))

        except KeyError:
            pass

    def take_screenshot(self, desktop_image=None):
        # 선택관리, 가져오기 스크린샷
        time.sleep(0.1)  # 대기0.1s,중지스크린샷시높이안내미완료
        if self.pick_type == PickType.TARGET:
            self.desktop_image = pyautogui.screenshot()
            # self.desktop_image.save(desktop_filepath)
        elif self.pick_type == PickType.ANCHOR:
            if not desktop_image:
                raise NotImplementedError("스크린샷찾을 수 없습니다")
            self.desktop_image = desktop_image
            logger.info(f"선택스크린샷: {self.desktop_image.width, self.desktop_image.height}")

        self.screen_width, self.screen_height = self.desktop_image.size

        if self.__status == Status.CV_ALT:
            if self.pick_type == PickType.TARGET:
                self.activate_win, title, self.win_rect = RectHandler.get_foreground_window_rect()
                logger.info("선택 가능방식창스크린샷: {}".format(self.win_rect))
                logger.info("선택 가능방식창이름: {}".format(title))
                x = max(self.win_rect[0], 0)
                y = max(self.win_rect[1], 0)
                w = min(self.win_rect[2] - self.win_rect[0], self.screen_width)
                h = min(self.win_rect[3] - self.win_rect[1], self.screen_height)

                self.partial_screenshot = self.desktop_image.crop((x, y, x + w, y + h))
                self.partial_rect = (x, y, w, h)

                # self.partial_screenshot = self.desktop_image
                # self.partial_rect = (0, 0, self.screen_width, self.screen_height)
                # self.partial_screenshot.save(partial_filepath)
            elif self.pick_type == PickType.ANCHOR:
                self.partial_rect = (0, 0, self.desktop_image.width, self.desktop_image.height)
                self.partial_screenshot = self.desktop_image

            # 선택 가능방식, 요소행분
            picker_cv = ImageDetector(self.partial_screenshot)
            output_img, self.selected_boxes = picker_cv.detect_objects("#00FF00", 1)
            cv2.imwrite(alt_filepath, output_img)

            for box in range(len(self.selected_boxes)):
                self.selected_boxes[box] = (
                    self.selected_boxes[box][0] + self.partial_rect[0],
                    self.selected_boxes[box][1] + self.partial_rect[1],
                    self.selected_boxes[box][2],
                    self.selected_boxes[box][3],
                )
            self.bboxes = sorted(self.selected_boxes, key=lambda bbox: bbox[2] * bbox[3])

        elif self.__status == Status.CV_CTRL:
            # 통신방식, 저장스크린샷
            self.partial_screenshot = self.desktop_image
            # self.partial_screenshot.save(partial_filepath)

        else:
            pass

    def check_timeout(self, start_time):
        # 선택여부시간 초과, 시간3min
        return time.time() - start_time > 60 * 3

    def stop_receiver(self):
        if self.receiver is not None:
            logger.info("event Stopping receiver...")
            self.stop_event.set()
            # self.receiver.join()
            self.receiver = None
            logger.info("Receiver stopped.")

    def run(self):
        # 사용 Socket 유형행통신
        with Socket() as hl:
            self.start_time = time.time()

            # 지정상태관리기기딕셔너리
            status_handler = {
                Status.INIT: self.handle_init,  # 상태
                Status.START: self.handle_start,  # 열기 상태
                Status.WAIT_SIGNAL: self.handle_wait_signal,  # 대기방식상태
                Status.CV_CTRL: self.handle_cv_ctrl,  # 통신선택방식
                Status.CV_ALT: self.handle_cv_alt,  # 선택 가능방식
                Status.SEND_WINDOW: self.handle_send_window,  # 전송창
                Status.SEND_TARGET: self.handle_send_target,  # 전송목록정보 
                Status.CONFIRM: self.handle_confirm,  # 정보 
                Status.CV_WAIT_TARGET: self.handle_cv_wait_target,  # 대기선택가져오기 정보 
                Status.CV_SHIFT: self.handle_cv_shift,  # 반환상태
                Status.CANCEL: self.handle_cancel,  # 가져오기 선택
                Status.OVER: self.handle_over,  # 선택완료
            }

            # , 근거상태실행의관리방법법
            while self.run_signal:
                # 조회여부시간 초과
                if self.check_timeout(self.start_time):
                    self.__status = Status.TIMEOUT
                    self.stop_receiver()
                    self.stop_keyboard_listener()
                    hl.send_signal(operation="initialize", status="ESC")
                    break
                # 가져오기현재상태의관리방법법
                handler_cv = status_handler.get(self.__status)
                if handler_cv:
                    handler_cv(hl)
                else:
                    raise NotImplementedError("실행상태있음오류")
            self.stop_keyboard_listener()
            # self.stop_mouse_listener()
            return self.__status, self.pick_res

    def handle_init(self, hl):
        self.initialize()
        self.stop_receiver()
        # 시작새
        self.stop_event.clear()

        self.receiver = threading.Thread(target=self.receive_message, args=(hl,))
        self.receiver.daemon = True  # 로
        self.receiver.start()
        logger.info("Receive start")
        self.start_keyboard_listener()
        if self.pick_type == PickType.TARGET:
            self.__status = Status.START
        elif self.pick_type == PickType.ANCHOR:
            self.__status = Status.CV_ALT

    def handle_start(self, hl):
        hl.send_signal(operation="start", status="CV")
        logger.info("전송상태{}".format(self.__status))
        self.__status = Status.WAIT_SIGNAL

    def handle_wait_signal(self, hl):
        pass

    def handle_cv_ctrl(self, hl):
        hl.send_signal(operation="start", status="hide")
        self.take_screenshot()
        hl.send_signal(operation="start", status="CV_CTRL")
        logger.info("상태{}완료전송".format(self.__status))
        self.__status = Status.CV_WAIT_TARGET

    def handle_cv_alt(self, hl):
        if self.pick_type == PickType.TARGET:
            hl.send_signal(operation="start", status="hide")
            self.take_screenshot()
            hl.send_signal(operation="start", status="CV_ALT")
        elif self.pick_type == PickType.ANCHOR:
            self.take_screenshot(self.anchor_pick_img)
        logger.info("상태{}완료전송".format(self.__status))
        self.__status = Status.SEND_WINDOW

    def handle_send_window(self, hl):
        hl.send_rect(operation="active_window", rect=self.partial_rect, msg="")
        self.__status = Status.SEND_TARGET

    def handle_send_target(self, hl):
        last_rect = None
        last_position = None
        # self.send_rect = queue.Queue(maxsize=2)
        self.send_rect = deque(maxlen=1)
        send_time = time.time()
        while self.__status == Status.SEND_TARGET and not self.check_timeout(self.start_time):
            if self.operation in ["confirm", "stop"]:
                self.__status = Status.CONFIRM
            time.sleep(0.1)
            current_position = PickCore.get_mouse_position()
            if current_position == last_position:
                continue

            last_position = current_position
            self.x, self.y = current_position

            self.draw_rect = self.get_rect()
            if self.draw_rect and self.draw_rect != last_rect:
                self.send_rect.append(self.draw_rect)

            if self.send_rect and time.time() - send_time > 0.1:
                send_time = time.time()
                last_rect = self.send_rect.pop()
                operation = "picking"
                msg = "이미지" if self.pick_type == PickType.ANCHOR else None
                hl.send_rect(operation=operation, rect=last_rect, msg=msg)

    def handle_confirm(self, hl):
        if self.operation == "confirm" and self.target_rect:
            if self.pick_type == PickType.TARGET:
                logger.info("목록 요소: {}".format(self.target_rect))
                self.pick_res = self.check_target(self.target_rect)
                logger.info("가져오기까지의목록데이터:{}".format(self.pick_res))
                if self.pick_res:
                    self.__status = Status.OVER
                else:
                    raise NotImplementedError("목록 가져오기실패")
            elif self.pick_type == PickType.ANCHOR:
                self.pick_res = self.check_anchor(self.target_rect)
                if self.pick_res:
                    hl.send_rect(operation="picking", status="valid", msg="이미지")
                    self.__status = Status.OVER
                else:
                    hl.send_rect(operation="picking", status="invalid", msg="이미지")
                    self.target_rect = None
                    while self.operation != "continue":
                        self.event.wait(0.1)
                    self.__status = Status.SEND_TARGET
        elif self.operation == "continue":
            self.__status = Status.SEND_TARGET
        else:
            pass

    def handle_cv_wait_target(self, hl):
        while self.__status == Status.CV_WAIT_TARGET and not self.check_timeout(self.start_time):
            if self.operation == "confirm" and self.target_rect is not None:
                self.pick_res = self.check_target(self.target_rect)
                if self.pick_res:
                    self.__status = Status.OVER
                else:
                    raise NotImplementedError()
            else:
                pass

    def handle_cv_shift(self, hl):
        hl.send_signal(operation="initialize", status="SHIFT")
        logger.info(f"완료전송상태, 현재상태로{self.__status}")
        self.__status = Status.START
        self.initialize()
        self.clear_selected_boxes()

    def handle_cancel(self, hl):
        hl.send_signal(operation="initialize", status="ESC")
        logger.info(f"완료전송가져오기 , 현재상태로{self.__status}")
        self.current_keys.clear()
        self.run_signal = False

    def handle_over(self, hl):
        hl.send_signal(operation="initialize", status="ESC")
        logger.info(f"완료전송가져오기 , 현재상태로{self.__status}")
        self.current_keys.clear()
        self.run_signal = False

    def check_target(self, target_rect):
        if target_rect:
            logger.info("이동요소일검증")
            target_img = self.desktop_image.crop(target_rect)
            logger.info("target_rect:{}".format(target_rect))
            logger.info("desktop_image:{}".format(self.desktop_image.size))
            # target_img.save(target_filepath)
            res = None

            if not AnchorMatch.check_if_multiple_elements(self.desktop_image, target_img, match_similarity=0.95):
                logger.info("요소아니오일, 선택가져오기 ")
                if not self.bboxes:
                    picker_cv = ImageDetector(self.partial_screenshot)
                    output_img, self.selected_boxes = picker_cv.detect_objects("#00FF00", 1)
                    self.bboxes = sorted(self.selected_boxes, key=lambda bbox: bbox[2] * bbox[3])

                for box in self.bboxes[::-1]:
                    anchor_img = self.desktop_image.crop((box[0], box[1], box[0] + box[2], box[1] + box[3]))
                    if AnchorMatch.check_if_multiple_elements(self.desktop_image, anchor_img, match_similarity=0.95):
                        self.anchor_rect = box
                        # anchor_img.save(anchor_filepath)
                        res = PickCore.json_res(target_img, target_rect, anchor_img, box, self.desktop_image)
                        break
            else:
                res = PickCore.json_res(target_img, target_rect, None, None, self.desktop_image)
        else:
            raise NotImplementedError("목록 요소비어 있습니다")
        return res

    def check_anchor(self, anchor_rect):
        if anchor_rect:
            start_time = time.time()
            logger.info("이동일검증:{}".format(start_time))
            anchor_img = self.desktop_image.crop(anchor_rect)
            # anchor_img.save(anchor_filepath)
            res = None
            if not AnchorMatch.check_if_multiple_elements(self.desktop_image, anchor_img, match_similarity=0.95):
                logger.info("로일요소, 요청다시 선택가져오기")
            else:
                res = PickCore.json_res(None, None, anchor_img, anchor_rect, self.desktop_image)

            logger.info("일검증결과:{}".format(time.time() - start_time))
            return res

    def initialize(self):
        self.desktop_image = None
        self.win_rect = None
        self.partial_screenshot = None
        self.partial_rect = None
        self.pick_res = None
        self.target_rect = None
        self.anchor_rect = None
        self.match_box = None
        self.match_rect = None
        self.bboxes = None
        self.draw_rect = None
        self.operation = None
        self.current_keys.clear()

    def clear_selected_boxes(self):
        self.target_rect = None
        self.anchor_rect = None
        self.match_box = None
        self.match_rect = None


if __name__ == "__main__":
    picker = CVPicker()
    picker.run()
    sys.exit(-1)