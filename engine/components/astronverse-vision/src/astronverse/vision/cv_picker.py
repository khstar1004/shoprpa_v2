import copy
import os
import time

import cv2
import numpy as np


class ImageDetector:
    """
    사용이미지관리및목록 감지의유형, 사용OpenCV라이브러리.

    속성:
        img_path (str): 필요관리의이미지파일 경로.
        original_img (np.ndarray): 기존이미지.
        gray_img (np.ndarray): 정도이미지.
    """

    def __init__(self, img_path: str = ""):
        """ImageDetector유형.

        :param img_path: 필요관리의이미지의파일 경로.
        """
        if img_path:
            self.img_path = img_path
            self.original_img, self.gray_img = self.get_image(img_path)

    @staticmethod
    def get_image(img_path: str) -> tuple[np.ndarray, np.ndarray]:
        """
        가져오기 반환이미지정도버전.

        :param img_path: 이미지의파일 경로.
        :return: 기존이미지및정도이미지.
        """

        img = cv2.imread(img_path)
        gray_pic = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        return img, gray_pic

    def get_image_from_gradio(self, img):
        self.original_img = img
        self.gray_img = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

    @staticmethod
    def compute_canny_edge(grey: np.ndarray) -> np.ndarray:
        """
        계획이미지의Canny정도.
        Sobel, Canny가능감지까지변경의모듈분

        :param blurred: 경과높이의이미지.
        :return: 이값Canny정도이미지, 0또는255.
        """
        edges = cv2.Canny(grey, 100, 150)
        # 100,200사용색상가장자리

        return edges

    @staticmethod
    def compute_sobel_gradient(blurred: np.ndarray) -> np.ndarray:
        """
        계획이미지의Sobel정도.

        :param blurred: 경과높이의이미지.
        :return: Sobel정도이미지.
        """

        grad_x = cv2.Sobel(blurred, ddepth=cv2.CV_32F, dx=1, dy=0)
        grad_y = cv2.Sobel(blurred, ddepth=cv2.CV_32F, dx=0, dy=1)
        gradient = cv2.subtract(grad_x, grad_y)
        gradient = cv2.convertScaleAbs(gradient)
        return gradient

    @staticmethod
    def apply_threshold_and_blur(gradient: np.ndarray) -> np.ndarray:
        """
        이미지정도사용값관리및관리.
        필요sobel행값관리

        :param gradient: 정도이미지.
        :return: 경과값관리및관리의이미지.
        """

        # 값유형가장자리의가능감지, 대75불가감지까지
        _, thresh = cv2.threshold(gradient, 75, 255, cv2.THRESH_BINARY)

        return thresh

    @staticmethod
    def apply_adaptive_threshold(edge: np.ndarray) -> np.ndarray:
        """
        값선택

        본위있음사용……
        """
        thresh = cv2.adaptiveThreshold(edge, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 11, 2)

        return thresh

    @staticmethod
    def apply_morphology(thresh: np.ndarray) -> np.ndarray:
        """
        이미지사용변수.

        :param thresh: 값관리후의이미지.
        :return: 변수후의이미지.
        """

        kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (3, 3))
        closed = cv2.dilate(thresh, kernel, iterations=3)
        return closed

    @staticmethod
    def compute_iou(box1: list[int], box2: list[int]) -> float:
        """
        계획개가장자리의(IoU).

        :param box1: 일개가장자리의및(x, y, 너비정도, 높이정도).
        :param box2: 이개가장자리의및.
        :return: 개가장자리의(IoU).
        """

        x1min, y1min = box1[0], box1[1]
        x1max, y1max = box1[0] + box1[2], box1[1] + box1[3]
        s1 = box1[2] * box1[3]
        x2min, y2min = box2[0], box2[1]
        x2max, y2max = box2[0] + box2[2], box2[1] + box2[3]
        s2 = box2[2] * box2[3]

        xmin = np.maximum(x1min, x2min)
        ymin = np.maximum(y1min, y2min)
        xmax = np.minimum(x1max, x2max)
        ymax = np.minimum(y1max, y2max)
        inter_h = np.maximum(ymax - ymin, 0.0)
        inter_w = np.maximum(xmax - xmin, 0.0)

        intersection = inter_h * inter_w
        union = s1 + s2 - intersection
        iou = intersection / union

        return iou

    def fill_hole(self, masker):
        _, mask = cv2.threshold(masker, 30, 255, cv2.THRESH_BINARY)
        contours, hierarchy = cv2.findContours(mask, cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)

        areas = [cv2.contourArea(contour) for contour in contours]

        # fill in small holes
        for idx, area in enumerate(areas):
            # only fill in contours which have area smaller than 400
            if area < 200:
                x, y, w, h = cv2.boundingRect(contours[idx])
                mask[y : y + h, x : x + w] = 255
        return mask

    @staticmethod
    def apply_nms(boxes: list[list[int]], iou_threshold: float = 0.3) -> list[list[int]]:
        """
        가장자리사용대값제어(NMS).

        :param boxes: 가장자리목록.
        :param iou_threshold: IoU값, 사용지정여부제어.
        :return: 경과NMS관리후의가장자리목록.
        """
        if not boxes:
            return []

        # 정렬법으로가장자리의너비정도로순서정렬열
        boxes = sorted(boxes, key=lambda x: x[2], reverse=False)
        print(boxes)
        keep_boxes = []

        while boxes:
            base_box = boxes.pop()
            keep_boxes.append(base_box)

            for box in boxes[:]:
                # 합치기의값대의가장자리행선택
                iou = ImageDetector.compute_iou(base_box, box)
                if iou > 0 and (iou >= iou_threshold or iou < 0.0003):
                    boxes.remove(box)

        return keep_boxes

    def draw_dashed_rectangle(self, top_left, bottom_right, color, thickness, dash_length=5):
        x1, y1 = top_left
        x2, y2 = bottom_right
        for x in range(x1, x2, 2 * dash_length):
            cv2.line(
                self.output_img,
                (x, y1),
                (min(x + dash_length, x2), y1),
                color,
                thickness,
            )
            cv2.line(
                self.output_img,
                (x, y2),
                (min(x + dash_length, x2), y2),
                color,
                thickness,
            )
        for y in range(y1, y2, 2 * dash_length):
            cv2.line(
                self.output_img,
                (x1, y),
                (x1, min(y + dash_length, y2)),
                color,
                thickness,
            )
            cv2.line(
                self.output_img,
                (x2, y),
                (x2, min(y + dash_length, y2)),
                color,
                thickness,
            )

    def preprocess_stage(self, gradient, is_adaptive):
        if is_adaptive:
            thresh = self.apply_adaptive_threshold(gradient)
        else:
            thresh = self.apply_threshold_and_blur(gradient)

        closed = self.apply_morphology(thresh)
        contours = self.fill_hole(closed)
        contours, _ = cv2.findContours(contours, cv2.RETR_TREE, cv2.CHAIN_APPROX_TC89_KCOS)

        return contours

    def detect_objects(self, dash_color, line_width):
        """
        감지이미지중의객체, 반환있음감지까지의객체의기존이미지및가장자리목록.

        :return: 있음감지까지의객체의기존이미지및가장자리목록.
                 매개가장자리으로 ((왼쪽위역할x, 왼쪽위역할y), (오른쪽아래역할x, 오른쪽아래역할y)) 의형식테이블.
        """

        start_time = time.time()
        blurred = cv2.GaussianBlur(self.gray_img, (3, 3), 0)
        kernel = np.array([[-1, -1, -1], [-1, 9, -1], [-1, -1, -1]])
        kernel1 = np.array([[0, -1, 0], [-1, 5, -1], [0, -1, 0]])
        sharpened = cv2.filter2D(blurred, -1, kernel)
        # 계획가장자리정도
        canny_gradient = self.compute_canny_edge(sharpened)
        sobel_gradient = self.compute_sobel_gradient(sharpened)

        # Step1 전감지, 선택
        _, fore_g = cv2.threshold(canny_gradient, 127, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
        kernel = np.ones((3, 3), np.uint8)
        # 데이터
        fore_g = cv2.dilate(fore_g, kernel, iterations=2)
        _, fore_markers = cv2.connectedComponents(fore_g)
        fore_markers = fore_markers.astype(np.uint8)
        fore_contours, _ = cv2.findContours(fore_markers.copy(), cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)

        # Step2 Sobel감지일반가장자리
        sobel_contours = self.preprocess_stage(sobel_gradient, False)

        # Step3 Canny감지내용
        canny_contours = self.preprocess_stage(canny_gradient, False)

        # 가장자리선택
        fore_boxes = [
            (x, y, w, h)
            for x, y, w, h in (cv2.boundingRect(contour) for contour in fore_contours)
            if (w * h) > 50
            and (h / w) < 10
            and (w * h) / (self.original_img.shape[0] * self.original_img.shape[1]) < 0.2
        ]

        sobel_boxes = [
            (x, y, w, h)
            for x, y, w, h in (cv2.boundingRect(contour) for contour in sobel_contours)
            if (w * h) > 50
            and (h / w) < 10
            and (w * h) / (self.original_img.shape[0] * self.original_img.shape[1]) < 0.2
        ]

        canny_boxes = [
            (x, y, w, h)
            for x, y, w, h in (cv2.boundingRect(contour) for contour in canny_contours)
            if ((w * h) > 20 and (w * h) <= 50)
            or ((w * h) > 200 and (w * h) <= 350)
            and (h / w) < 10
            and (w * h) / (self.original_img.shape[0] * self.original_img.shape[1]) < 0.2
        ]

        self.output_img = copy.deepcopy(self.original_img)
        # 가장자리합치기&대값제어
        all_boxes = fore_boxes + sobel_boxes
        all_boxes = [list(box) for box in all_boxes]
        selected_boxes = self.apply_nms(all_boxes)

        boxes_with_coordinates = []

        if dash_color is None:
            dash_color = "#00FF00"
        dash_color = dash_color.lstrip("#")

        dash_color = tuple(int(dash_color[i : i + 2], 16) for i in (0, 2, 4))

        # dash_color = self.qcolor_to_bgr(dash_color)

        for box in selected_boxes:
            x, y, w, h = box
            boxes_with_coordinates.append(box)
            self.draw_dashed_rectangle((x, y), (x + w, y + h), dash_color, line_width, 5)
        selected_boxes = selected_boxes
        # + self.detect_ocr_text(line_width)

        end_time = time.time()
        print(end_time - start_time)
        return self.output_img, selected_boxes

    def show_or_save_image(self, save_path: str = "", show_image: bool = True):
        """
        또는저장감지까지객체의이미지.

        :param save_path: (선택 가능) 이미지저장의파일 경로.에서 show_image 로 False 시사용.
        :param show_image: 제어예이미지예저장까지파일.로 True, 이미지.
        """
        if show_image:
            cv2.imwrite("output.png", self.output_img)
            cv2.imshow("Detected Objects", self.output_img)
            cv2.waitKey(0)
            cv2.destroyAllWindows()

        else:
            if save_path is None:
                raise ValueError(" save_path 매개변수으로저장이미지.")

            if not os.path.isdir(os.path.dirname(save_path)):
                raise Exception(f"오류: 경로 '{save_path}' 없음또는찾을 수 없습니다.")

            try:
                cv2.imwrite(save_path, self.original_img)
            except Exception as e:
                raise Exception(f"저장이미지시발송오류: {e}")