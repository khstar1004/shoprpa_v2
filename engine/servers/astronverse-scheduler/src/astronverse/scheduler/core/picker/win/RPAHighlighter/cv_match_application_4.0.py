import json
import logging
import os
import time

# 입력PyQT사용의QT_QPA_PLATFORM_PLUGIN_PATH경로, 근거의행수정
os.environ.update(
    {
        "QT_QPA_PLATFORM_PLUGIN_PATH": "/home/horizon/RPA/그룹합치기/python_base_linux/lib/python3.7/site-packages/PyQt5/Qt5/plugins/platforms/"
    }
)

import sys

from PyQt5.QtCore import QRect, Qt, QTimer, pyqtSignal
from PyQt5.QtGui import (
    QBrush,
    QColor,
    QFont,
    QGuiApplication,
    QPainter,
    QPen,
)
from PyQt5.QtNetwork import QHostAddress, QUdpSocket
from PyQt5.QtWidgets import (
    QApplication,
    QHBoxLayout,
    QLabel,
    QMainWindow,
    QPushButton,
    QVBoxLayout,
    QWidget,
)

logger = logging.getLogger(__name__)


class DrawStatus:
    waitDraw = 0
    drawing = 1
    clicked = 2


class HighlightForm(QWidget):
    message_signal = pyqtSignal(str)

    def __init__(self):
        super().__init__()
        self.setWindowTitle("HighlightForm")
        self.screenshot_widget = None
        self.mode = "normal"
        self.validate_rects = None

        # 개식별자기호
        self.draw_status = DrawStatus.waitDraw

        self.setWindowFlags(
            Qt.WindowTransparentForInput
            | Qt.WindowStaysOnTopHint
            | Qt.FramelessWindowHint
            | Qt.Dialog
            | Qt.Tool
            | Qt.X11BypassWindowManagerHint
        )
        self.setAttribute(Qt.WA_TranslucentBackground)
        # self.setAttribute(Qt.WA_TransparentForMouseEvents, True)

        self.draw_rect = QRect(0, 0, 1, 1)

        self.init_toolbar()
        self.full_screen()
        self.update()

    def init_toolbar(self):
        # 도구
        self.toolbar = QWidget(self)

        # 도구
        mainToolLayout = QHBoxLayout()

        # 생성버튼
        self.btn_cancel = QPushButton("출력")
        self.btn_saveFile = QPushButton("저장")

        # 버튼방식
        button_style = """
                    QPushButton {
                        background-color: #f0f0f0;
                        border: 1px solid #c0c0c0;
                        border-radius: 5px;
                        padding: 5px 15px;
                        color: #333;
                        font-size: 14px;
                    }
                    QPushButton:hover {
                        background-color: #e0e0e0;
                        border: 1px solid #b0b0b0;
                    }
                    QPushButton:pressed {
                        background-color: #d0d0d0;
                        border: 1px solid #a0a0a0;
                    }
                """
        self.btn_cancel.setStyleSheet(button_style)
        self.btn_saveFile.setStyleSheet(button_style)

        # 추가버튼까지영역
        mainToolLayout.addWidget(self.btn_saveFile)
        mainToolLayout.addWidget(self.btn_cancel)
        mainToolLayout.setContentsMargins(0, 0, 0, 0)  # 제거가장자리
        mainToolLayout.setSpacing(5)

        # # 도구영역
        toolLayout = QVBoxLayout(self.toolbar)
        toolLayout.addLayout(mainToolLayout)
        toolLayout.setContentsMargins(0, 0, 0, 0)  # 제거가장자리
        toolLayout.setSpacing(0)

        # 도구의가능
        self.toolbar.setVisible(True)

        # 연결버튼정보 
        self.btn_cancel.clicked.connect(self.slt_cancel)
        self.btn_saveFile.clicked.connect(self.slt_saveFile)

        # 계획도구
        self.toolbar.adjustSize()
        self.toolBarWidth = self.toolbar.width()
        self.toolBarHeight = self.toolbar.height()

        # 도구
        self.hideToolBar()

    def full_screen(self):
        self.screen = QGuiApplication.primaryScreen()
        self.screenshot = self.screen.grabWindow(0)
        self.size = self.screenshot.size()
        self.setGeometry(0, 0, self.size.width(), self.size.height())
        self.show()

    def slt_cancel(self):
        send_json = {"Operation": "continue"}
        self.send_message(json.dumps(send_json))
        self.draw_status = DrawStatus.drawing
        self.hideToolBar()

    def slt_saveFile(self):
        send_json = {
            "Operation": "confirm",
            "Boxes": [
                {
                    "Left": self.draw_rect.left(),
                    "Top": self.draw_rect.top(),
                    "Right": self.draw_rect.right(),
                    "Bottom": self.draw_rect.bottom(),
                    "Msg": "",
                }
            ],
        }
        self.send_message(json.dumps(send_json))
        self.draw_status = DrawStatus.waitDraw
        self.hideToolBar()

    def hideToolBar(self):
        # 도구의
        self.toolbar.setVisible(False)

    def handle_invalid(self):
        self.hideToolBar()
        self.draw_status = DrawStatus.drawing

    def paintEvent(self, event):
        self.raise_()
        painter = QPainter(self)
        logger.debug("paintEvent mode=%s rect=%s", self.mode, self.draw_rect)
        if self.mode != "validate":
            pen = QPen(QColor(255, 255, 224))
            pen.setWidth(3)
            brush = QBrush(QColor(255, 192, 203, 155))
            painter.setPen(pen)
            painter.setBrush(brush)
            logger.debug("drawing rect=%s", self.draw_rect)
            painter.drawRect(self.draw_rect)
        else:
            pen = QPen(QColor(255, 0, 0))
            painter.setPen(pen)
            pen.setWidth(5)
            for rect in self.validate_rects:
                painter.drawRect(rect)

    def mousePressEvent(self, event):
        if event.button() == Qt.LeftButton:
            # Check if the click is within the rectangle
            if self.draw_rect.contains(event.pos()):
                self.draw_status = DrawStatus.clicked
                toolbar_position = self.draw_rect.bottomLeft()
                toolbar_position.setY(toolbar_position.y() + 3)
                self.toolbar.move(toolbar_position)
                send_json = {"Operation": "stop"}
                self.send_message(json.dumps(send_json))
                self.toolbar.show()

            else:
                self.hideToolBar()
                send_json = {"Operation": "continue"}
                self.send_message(json.dumps(send_json))
                self.draw_status = DrawStatus.drawing

    def update_rect(self, rect):
        self.draw_rect = rect

        if self.draw_status != DrawStatus.clicked:
            logger.debug("update_rect rect=%s status=%s", self.draw_rect, self.draw_status)
            self.repaint()

    def update_validate_rects(self, rects):
        self.validate_rects = rects
        self.mode = "validate"
        self.repaint()

    def update_mode(self, mode):
        self.mode = mode
        if self.mode.find("CV") != -1:
            # self.setAttribute(Qt.WA_TransparentForMouseEvents, False)
            self.setWindowFlags(
                Qt.WindowStaysOnTopHint | Qt.FramelessWindowHint | Qt.Dialog | Qt.Tool | Qt.X11BypassWindowManagerHint
            )

    def activate_screenshot_form(self):
        if not self.screenshot_widget:
            self.screenshot_widget = ScreenshotWidget()
            self.screenshot_widget.show()
            self.raise_()  # 를높이창까지전

    def close_activate_screenshot_form(self):
        if self.screenshot_widget:
            self.screenshot_widget.close()
            self.screenshot_widget = None

    def send_message(self, send_string):
        # 에서개방법법중트리거정보 , 전송메시지
        self.message_signal.emit(send_string)

    def initialize(self):
        self.hideToolBar()
        if self.screenshot_widget:
            self.screenshot_widget.hide()
        self.draw_status = DrawStatus.waitDraw
        self.draw_rect = QRect(0, 0, 1, 1)
        self.validate_rects = None
        self.mode = "normal"
        self.update()


class ScreenshotWidget(QWidget):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("ScreenshotForm")

        # Get the screen and take a screenshot
        screen = app.primaryScreen()
        screenshot = screen.grabWindow(0)

        # Set the window to be frameless and stay behind
        self.setWindowFlags(Qt.FramelessWindowHint | Qt.WindowStaysOnTopHint | Qt.WindowTransparentForInput)

        # Set the initial size and position to cover the entire screen
        self.setGeometry(screenshot.rect())

        # Use a QLabel to display the screenshot
        self.label = QLabel(self)
        self.label.setPixmap(screenshot)


class CtrlWidget(QWidget):
    message_signal = pyqtSignal(str)

    def __init__(self):
        super().__init__()
        self.setWindowTitle("CtrlForm")
        self.showFullScreen()
        logger.debug("CtrlForm initial rect=%s", self.rect())
        self.start_point = None
        self.end_point = None
        self.selecting = False
        self.selection_rect = None

        # Set the window to be frameless
        self.setWindowFlags(Qt.FramelessWindowHint | Qt.WindowStaysOnTopHint)

        # Capture the desktop as a background
        screen = QApplication.primaryScreen()
        self.background = screen.grabWindow(0)
        self.setGeometry(self.background.rect())

        # self.toolbar = None
        logger.debug("CtrlForm rect before toolbar=%s", self.rect())
        self.toolbar = None
        self.toolbar_show = False
        self.init_toolbar()
        self.update()
        logger.debug("CtrlForm rect after toolbar=%s", self.rect())
        # self.raise_()

    def init_toolbar(self):
        # 도구
        self.toolbar = QWidget(self)

        # 도구의지정
        self.toolbar.setFixedSize(200, 50)  # 너비정도200, 높이정도50

        # 생성버튼지정로도구의파일
        self.btn_cancel = QPushButton("출력", self.toolbar)
        self.btn_saveFile = QPushButton("저장", self.toolbar)

        # 버튼방식
        button_style = """
                    QPushButton {
                        background-color: #f0f0f0;
                        border: 1px solid #c0c0c0;
                        border-radius: 5px;
                        padding: 5px 15px;
                        color: #333;
                        font-size: 14px;
                    }
                    QPushButton:hover {
                        background-color: #e0e0e0;
                        border: 1px solid #b0b0b0;
                    }
                    QPushButton:pressed {
                        background-color: #d0d0d0;
                        border: 1px solid #a0a0a0;
                    }
                """
        self.btn_cancel.setStyleSheet(button_style)
        self.btn_saveFile.setStyleSheet(button_style)

        # 매개버튼의위치및크기
        self.btn_saveFile.setGeometry(10, 10, 80, 30)  # 매개변수예x, y, width, height
        self.btn_cancel.setGeometry(100, 10, 80, 30)

        # 도구의가능
        self.toolbar.setVisible(False)

        # 연결버튼정보 
        self.btn_cancel.clicked.connect(self.slt_cancel)
        self.btn_saveFile.clicked.connect(self.slt_saveFile)

    def hideToolBar(self):
        # 도구의
        self.toolbar.setVisible(False)

    def slt_cancel(self):
        self.start_point = None
        self.end_point = None
        self.selecting = False
        self.selection_rect = None
        self.hideToolBar()
        self.toolbar_show = False
        self.update()

    def slt_saveFile(self):
        send_json = {
            "Operation": "confirm",
            "Boxes": [
                {
                    "Left": self.selection_rect.left(),
                    "Top": self.selection_rect.top(),
                    "Right": self.selection_rect.right(),
                    "Bottom": self.selection_rect.bottom(),
                    "Msg": "",
                }
            ],
        }
        logger.debug("selection confirm payload=%s", send_json)
        self.send_message(json.dumps(send_json))
        self.selecting = False

    def mousePressEvent(self, event):
        if event.button() == Qt.LeftButton:
            if not self.selecting:
                self.start_point = event.pos()
                self.selecting = True

    def mouseMoveEvent(self, event):
        if self.selecting and (not self.toolbar_show):
            self.end_point = event.pos()
            self.selection_rect = QRect(self.start_point, self.end_point).normalized()
            self.update()  # Trigger a repaint

    def mouseReleaseEvent(self, event):
        if event.button() == Qt.LeftButton and self.selecting and (not self.toolbar_show):
            toolbar_position = self.selection_rect.bottomLeft()
            toolbar_position.setY(toolbar_position.y() + 3)
            self.toolbar.move(toolbar_position)
            self.toolbar.setVisible(True)
            self.toolbar_show = True

    def paintEvent(self, event):
        painter = QPainter(self)
        rect = self.rect()
        painter.drawPixmap(rect, self.background)

        semi_transparent_red = QColor(255, 0, 0, 80)  # 색상, alpha로100()
        painter.fillRect(rect, semi_transparent_red)

        # If the user is selecting a region, draw the rectangle
        if self.selection_rect:
            painter.setPen(QPen(QColor(255, 0, 0), 2, Qt.SolidLine))
            # 저장현재painter의상태
            painter.save()

            # 로선택
            painter.setClipRect(self.selection_rect)

            # 제어선택의 pixmap, 으로제거색상의결과
            painter.drawPixmap(self.selection_rect, self.background, self.selection_rect)

            # 복사painter의상태
            painter.restore()

            # 후제어선택의가장자리
            painter.setPen(QPen(QColor(255, 0, 0), 2, Qt.SolidLine))
            painter.drawRect(self.selection_rect)

    def send_message(self, send_string):
        # 에서개방법법중트리거정보 , 전송메시지
        self.message_signal.emit(send_string)


class HoverHintWidget(QWidget):
    def __init__(self, mode):
        super().__init__()
        self.setWindowTitle("HoverHintForm")

        self.mode = mode
        # 로그위치, 창위치상태
        self.is_at_bottom_right = False
        self.init_ui()

    def init_ui(self):
        try:
            # 창크기및위치(왼쪽위역할)
            self.setGeometry(0, 0, 300, 200)

            # 없음가장자리창, 방법제어결과
            self.setWindowFlags(Qt.FramelessWindowHint | Qt.WindowStaysOnTopHint)
            self.setAttribute(Qt.WA_TranslucentBackground, True)
        except Exception as e:
            logger.exception("HoverHintWidget 초기화 오류: %s", e)

    def paintEvent(self, event):
        painter = QPainter(self)

        def paint_text(
            painter,
            text,
            center_x,
            center_y,
            font_size=16,
            font="Arial",
            color=QColor(0, 0, 0),
            box_flag=False,
        ):
            # 문자및색상
            painter.setPen(color)
            painter.setFont(QFont(font, font_size))
            text_height = painter.fontMetrics().height()
            text_width = painter.fontMetrics().width(text)
            x = center_x - text_width // 2
            y = center_y - painter.fontMetrics().ascent() // 2
            painter.drawText(x, y, text)

            if box_flag:
                padding = 5  # 에서사추가일, 아니오텍스트
                rect_x = x - padding
                rect_y = y - text_height + padding
                rect_width = text_width + 2 * padding
                rect_height = text_height
                painter.setBrush(Qt.NoBrush)
                painter.drawRect(rect_x, rect_y, rect_width, rect_height)

        # 색상
        painter.setBrush(QColor(255, 255, 255, 200))
        painter.setPen(Qt.NoPen)
        painter.drawRect(self.rect())

        if self.mode == "CV":
            # 문자및색상
            paint_text(painter=painter, text="ALT", center_x=100, center_y=80, box_flag=True)
            paint_text(painter=painter, text="CTRL", center_x=100, center_y=150, box_flag=True)
            paint_text(painter=painter, text="선택 가능", center_x=200, center_y=80)
            paint_text(painter=painter, text="스크린샷선택", center_x=200, center_y=150)
        else:
            # 문자및색상
            paint_text(
                painter=painter,
                text="CTRL + 클릭",
                center_x=100,
                center_y=80,
                box_flag=True,
            )
            paint_text(painter=painter, text="ESC", center_x=100, center_y=150, box_flag=True)
            paint_text(painter=painter, text="선택", center_x=200, center_y=80)
            paint_text(painter=painter, text="출력", center_x=200, center_y=150)

    def enterEvent(self, event):
        # 가져오기화면크기
        screen_geometry = QApplication.desktop().availableGeometry()
        screen_width = screen_geometry.width()
        screen_height = screen_geometry.height()

        if self.is_at_bottom_right:
            x, y = 0, 0
        else:
            # 창까지오른쪽아래역할
            x = screen_width - self.width()
            y = screen_height - self.height()

        # 까지목록 위치
        self.move(x, y)

        # 로그위치
        self.is_at_bottom_right = not self.is_at_bottom_right

        event.accept()


class ConsoleApp(QMainWindow):
    def __init__(self, socket_port):
        super().__init__()

        # 생성 QUdpSocket
        self.udp_socket = QUdpSocket(self)
        self.sender_port = None
        self.sender_host = None
        if not self.udp_socket.bind(QHostAddress.Any, int(socket_port)):  # 지정까지단말 11001
            logger.error("UDP 소켓 바인딩에 실패했습니다. port=%s", socket_port)
            return

        # 연결 readyRead 정보 까지데이터
        self.udp_socket.readyRead.connect(self.read_datagrams)
        logger.info("UDP 서버가 시작되었습니다. port=%s", socket_port)

        self.highlight_form = HighlightForm()
        self.highlight_form.message_signal.connect(self.handle_message)
        self.highlight_form.showFullScreen()
        self.ctrl_form = None
        self.hint_form = None

        self.timer = QTimer(self)
        self.timer.timeout.connect(self.read_datagrams)
        self.timer.start(500)  # 매 500 초호출일 read_datagrams

        logger.debug("ConsoleApp started has_pending=%s", self.udp_socket.hasPendingDatagrams())

    def read_datagrams(self):
        """관리수신까지의 UDP 데이터패키지"""
        if self.udp_socket.hasPendingDatagrams():
            # 가져오기데이터패키지크기
            datagram_size = self.udp_socket.pendingDatagramSize()
            # 가져오기데이터패키지
            datagram = self.udp_socket.readDatagram(datagram_size)
            if datagram:
                data, sender_host, sender_port = datagram
                message = data.decode("utf-8")  # 를문자데이터해제코드로문자열
                logger.debug("received from %s:%s: %s", sender_host, sender_port, message)
                self.sender_port = sender_port
                self.sender_host = sender_host
                message_dict = json.loads(message)
                if message_dict["Operation"] == "start":
                    self.highlight_form.update_mode(message_dict["Type"])
                    if message_dict["Type"] == "CV":
                        if not self.hint_form:
                            self.hint_form = HoverHintWidget("CV")
                            self.hint_form.show()
                            self.hint_form.raise_()
                            pass
                        else:
                            self.hint_form.show()
                            self.hint_form.raise_()
                    if message_dict["Type"] == "hide":
                        self.hint_form.hide()
                    if message_dict["Type"] == "CV_ALT":
                        self.highlight_form.activate_screenshot_form()
                        pass
                    elif message_dict["Type"] == "CV_CTRL":
                        if not self.ctrl_form:
                            self.ctrl_form = CtrlWidget()
                            self.ctrl_form.showFullScreen()
                            self.ctrl_form.raise_()
                            self.ctrl_form.message_signal.connect(self.handle_message)
                        else:
                            self.ctrl_form.show()
                            self.ctrl_form.raise_()
                elif message_dict["Operation"] == "picking":
                    if message_dict["Type"] == "invalid":
                        self.highlight_form.handle_invalid()
                    if len(message_dict["Boxes"]) == 1:
                        logger.debug("updating highlight rect")
                        # 일지정할show일아래, 아니오이면아니오
                        self.highlight_form.show()
                        self.highlight_form.showFullScreen()
                        self.highlight_form.update_rect(
                            QRect(
                                message_dict["Boxes"][0]["Left"],
                                message_dict["Boxes"][0]["Top"],
                                message_dict["Boxes"][0]["Right"] - message_dict["Boxes"][0]["Left"],
                                message_dict["Boxes"][0]["Bottom"] - message_dict["Boxes"][0]["Top"],
                            )
                        )
                    else:
                        validate_rects = []
                        for box in message_dict["Boxes"]:
                            validate_rects.append(
                                QRect(
                                    box["Left"],
                                    box["Top"],
                                    box["Right"] - box["Left"],
                                    box["Bottom"] - box["Top"],
                                )
                            )
                        for i in range(3):
                            self.highlight_form.show()
                            time.sleep(0.5)
                            self.highlight_form.hide()
                            time.sleep(0.5)
                        self.highlight_form.show()
                elif message_dict["Operation"] == "initialize":
                    if message_dict["Type"] == "SHIFT":
                        self.highlight_form.initialize()
                        if self.ctrl_form:
                            self.ctrl_form.close()
                            self.ctrl_form = None
                        if self.hint_form:
                            self.hint_form.show()
                            self.hint_form.raise_()
                    if message_dict["Type"] == "ESC":
                        self.highlight_form.initialize()
                        # if self.highlight_form.screenshot_widget:
                        #     self.highlight_form.close_activate_screenshot_form()
                        if self.ctrl_form:
                            self.ctrl_form.hide()
                        if self.hint_form:
                            self.hint_form.hide()
                    if message_dict["Type"] == "Exit":
                        if self.highlight_form.screenshot_widget:
                            self.highlight_form.screenshot_widget.close()
                        self.highlight_form.close()
                        if self.ctrl_form:
                            self.ctrl_form.close()
                        if self.hint_form:
                            self.hint_form.close()
                        self.close()
                elif message_dict["Operation"] == "validate":
                    validate_rects = []
                    for box in message_dict["Boxes"]:
                        validate_rects.append(
                            QRect(
                                box["Left"],
                                box["Top"],
                                box["Right"] - box["Left"],
                                box["Bottom"] - box["Top"],
                            )
                        )
                    self.highlight_form.update_validate_rects(validate_rects)
                    for i in range(3):
                        self.highlight_form.show()
                        time.sleep(0.5)
                        self.highlight_form.hide()
                        time.sleep(0.5)
                    # self.highlight_form.show()

    def handle_message(self, message):
        # 관리 HighlightForm 의메시지
        logger.debug("received message from HighlightForm: %s", message)
        self.udp_socket.writeDatagram(message.encode("utf-8"), QHostAddress("127.0.0.1"), self.sender_port)


if __name__ == "__main__":
    app = QApplication(sys.argv)
    console = ConsoleApp(sys.argv[1])
    # canvas.showFullScreen()  # 전체
    # canvas.hide()  # 
    sys.exit(app.exec_())
