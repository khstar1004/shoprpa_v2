import copy
import os
import time

import cv2
import numpy as np
from PIL import Image
from astronverse.vision_picker.logger import logger

# def ocr_image(image, flag):
#     """
#     실행OCR가져오기텍스트
#     """
#     uniOCR = universalOcr(APP_ID, API_KEY, API_SECRET)
#     _, buffer = cv2.imencode('.jpg', image)
#     text, boxes = uniOCR.get_result(buffer, flag)
#     return text, boxes


class ImageDetector:
    """
    사용이미지관리및목록 감지의유형, 사용OpenCV라이브러리.

    속성:
        img_path (str): 필요관리의이미지파일 경로.
        original_img (np.ndarray): 기존이미지.
        gray_img (np.ndarray): 정도이미지.
    """

    def __init__(self, img: Image = None):
        """ImageDetector유형.

        :param img_path: 필요관리의이미지의파일 경로.
        """
        if img:
            self.original_img, self.gray_img = self.get_image(img)

    @staticmethod
    def get_image(img: Image = None):
        """
        가져오기 반환이미지정도버전.

        :param img: 이미지
        :return: 기존이미지및정도이미지.
        """
        if img is None:
            raise ValueError("No image provided.")

        try:
            # 를 PIL 이미지변환로 NumPy 배열
            img_np = np.array(img)
            image = cv2.cvtColor(img_np, cv2.COLOR_RGB2BGR)

            # 변환로정도이미지
            gray_pic = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
            return image, gray_pic
        except Exception as e:
            logger.info(f"Error converting image to grayscale: {e}")
            return None, None

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

        closed = cv2.dilate(thresh, (3, 3), iterations=3)
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

        # 변환로 numpy 배열으로추가계획
        boxes_array = np.array(boxes, dtype=np.float32)
        # 계획
        areas = boxes_array[:, 2] * boxes_array[:, 3]
        # 너비정도정렬(보관기존있음)
        order = np.argsort(boxes_array[:, 2])

        keep_boxes = []
        suppressed = np.zeros(len(boxes), dtype=bool)

        iteration = 0
        iou_calc_count = 0

        # 에서후전(에서대까지소)
        for idx in range(len(order) - 1, -1, -1):
            i = order[idx]
            if suppressed[i]:
                continue

            # 보관현재
            keep_boxes.append(boxes[i])
            iteration += 1

            # 가져오기현재의
            x1, y1, w1, h1 = boxes_array[i]
            x1_max, y1_max = x1 + w1, y1 + h1
            area1 = areas[i]

            # 량조회필요제어의
            for jdx in range(idx):
                j = order[jdx]
                if suppressed[j]:
                    continue

                # 빠름빈: 결과가개전체아니오, 건너뛰기IoU계획
                x2, y2, w2, h2 = boxes_array[j]
                x2_max, y2_max = x2 + w2, y2 + h2

                # 조회여부있음재
                if x1_max <= x2 or x2_max <= x1 or y1_max <= y2 or y2_max <= y1:
                    continue  # 아니오, 건너뛰기

                # 계획IoU(내부)
                iou_calc_count += 1
                inter_x1 = max(x1, x2)
                inter_y1 = max(y1, y2)
                inter_x2 = min(x1_max, x2_max)
                inter_y2 = min(y1_max, y2_max)

                inter_area = (inter_x2 - inter_x1) * (inter_y2 - inter_y1)

                if inter_area > 0:
                    area2 = areas[j]
                    union_area = area1 + area2 - inter_area
                    iou = inter_area / union_area

                    if iou >= iou_threshold or iou < 0.0003:
                        suppressed[j] = True

        return keep_boxes

    def draw_dashed_rectangle(self, top_left, bottom_right, color, thickness, dash_length=5):
        x1, y1 = top_left
        x2, y2 = bottom_right
        for x in range(x1, x2, 2 * dash_length):
            cv2.line(self.output_img, (x, y1), (min(x + dash_length, x2), y1), color, thickness)
            cv2.line(self.output_img, (x, y2), (min(x + dash_length, x2), y2), color, thickness)
        for y in range(y1, y2, 2 * dash_length):
            cv2.line(self.output_img, (x1, y), (x1, min(y + dash_length, y2)), color, thickness)
            cv2.line(self.output_img, (x2, y), (x2, min(y + dash_length, y2)), color, thickness)

    def preprocess_stage(self, gradient, is_adaptive):
        if is_adaptive:
            thresh = self.apply_adaptive_threshold(gradient)
        else:
            thresh = self.apply_threshold_and_blur(gradient)

        closed = self.apply_morphology(thresh)

        contours = self.fill_hole(closed)

        contours, _ = cv2.findContours(contours, cv2.RETR_TREE, cv2.CHAIN_APPROX_TC89_KCOS)
        return contours

    # def qcolor_to_bgr(self, qcolor):
    #     # 가져오기 RGB 색상분량
    #     r = qcolor.red()
    #     g = qcolor.green()
    #     b = qcolor.blue()
    #     # 반환 BGR 색상원그룹
    #     return (b, g, r)

    def detect_objects(self, dash_color, line_width):
        """
        감지이미지중의객체, 반환있음감지까지의객체의기존이미지및가장자리목록.

        :return: 있음감지까지의객체의기존이미지및가장자리목록.
                 매개가장자리으로 ((왼쪽위역할x, 왼쪽위역할y), (오른쪽아래역할x, 오른쪽아래역할y)) 의형식테이블.
        """

        # 정도이미지행높이관리
        # self.img = img
        # self.gray_img = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        # self.original_img = o_image

        start_time = time.time()

        # 높이및
        blurred = cv2.GaussianBlur(self.gray_img, (3, 3), 0)

        kernel = np.array([[-1, -1, -1], [-1, 9, -1], [-1, -1, -1]])
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
        selected_boxes = self.apply_nms(all_boxes)

        boxes_with_coordinates = []

        if dash_color is None:
            dash_color = "#00FF00"
        dash_color = dash_color.lstrip("#")

        dash_color = tuple(int(dash_color[i : i + 2], 16) for i in (0, 2, 4))

        for box in selected_boxes:
            x, y, w, h = box
            boxes_with_coordinates.append(box)
            self.draw_dashed_rectangle((x, y), (x + w, y + h), dash_color, line_width, 5)

        selected_boxes = selected_boxes
        # + self.detect_ocr_text(line_width)

        end_time = time.time()
        total_time = end_time - start_time
        logger.info(f"detect_objects 시: {total_time:.3f}초")
        return self.output_img, selected_boxes

    # def detect_ocr_text(self, line_width):
    #     ocr_bboxes = ocr_image(self.original_img, True)[1]
    #     bboxes = []
    #     for ocr_bbox in ocr_bboxes:
    #         x, y = ocr_bbox[0], ocr_bbox[1]
    #         w = ocr_bbox[4] - x
    #         h = ocr_bbox[5] - y
    #         bboxes.append((x, y, w, h))
    #     selected_boxes = self.apply_nms(bboxes)
    #     boxes_with_coordinates = []
    #
    #     dash_color = "#FF0000"
    #     dash_color = dash_color.lstrip('#')
    #
    #     dash_color = tuple(int(dash_color[i:i + 2], 16) for i in (0, 2, 4))
    #     for box in selected_boxes:
    #         x, y, w, h = box
    #         boxes_with_coordinates.append(box)
    #         self.draw_dashed_rectangle((x, y), (x + w, y + h), dash_color, line_width, 5)
    #
    #     return selected_boxes

    def show_or_save_image(self, save_path: str = None, show_image: bool = True):
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
                raise Exception(f"이미지 저장 중 오류: {e}")


if __name__ == "__main__":
    img_path = r"C:\\Users\\zyfan9\\Desktop\\test image\\q10.png"
    detector = ImageDetector(img_path)
    boxes = detector.detect_objects(None, None)
    # logger.info("=====> boxes:> ", boxes)
    detector.show_or_save_image("C:\\Users\\zyfan9\\Desktop\\show_image")
