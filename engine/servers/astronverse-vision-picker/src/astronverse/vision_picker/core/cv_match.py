import math

import cv2
import numpy as np
from astronverse.vision_picker.logger import logger


class AnchorMatch:
    def __init__(self):
        pass

    def draw_dashed_rectangle(self, image, top_left, bottom_right, color, thickness, dash_length=10):
        x1, y1 = top_left
        x2, y2 = bottom_right

        for x in range(x1, x2, 2 * dash_length):
            cv2.line(image, (x, y1), (min(x + dash_length, x2), y1), color, thickness)
            cv2.line(image, (x, y2), (min(x + dash_length, x2), y2), color, thickness)
        for y in range(y1, y2, 2 * dash_length):
            cv2.line(image, (x1, y), (x1, min(y + dash_length, y2)), color, thickness)
            cv2.line(image, (x2, y), (x2, min(y + dash_length, y2)), color, thickness)

    @staticmethod
    def check_if_multiple_elements(image, element, match_similarity):
        if not isinstance(image, np.ndarray):
            image = np.array(image.convert("RGBA"))
        if not isinstance(element, np.ndarray):
            element = np.array(element.convert("RGBA"))
        # Convert to grayscale
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        # gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        if element is not None:
            small_gray = cv2.cvtColor(element, cv2.COLOR_RGB2GRAY)
            h, w = small_gray.shape[:2]

            result = cv2.matchTemplate(gray, small_gray, cv2.TM_CCOEFF_NORMED)

            # 방법법1: 사용통신분
            binary = (result >= match_similarity).astype(np.uint8)
            num_labels, _, _, _ = cv2.connectedComponentsWithStats(binary)
            count = num_labels - 1  # 
        if count == 1:
            return True  # 식별자요소일존재함가능매칭
        else:
            return False  # 식별자요소아니오일또는일치하지 않는까지
        # if count > 1:
        #     return 1
        # elif count < 1:
        #     return -1
        # else:
        #     return 0

    def _limit_roi_bounds(self, roi_top_left: tuple, roi_bottom_right: tuple, image_shape: tuple) -> tuple:
        """
        제한ROI의가장자리값에서이미지내부

        Args:
            roi_top_left: (x, y) 왼쪽위역할
            roi_bottom_right: (x, y) 오른쪽아래역할
            image_shape: 이미지 (height, width, channels)

        Returns:
            (limited_top_left, limited_bottom_right)
        """
        img_height, img_width = image_shape[:2]

        # 제한x
        x1 = max(0, min(roi_top_left[0], img_width - 1))
        x2 = max(0, min(roi_bottom_right[0], img_width - 1))

        # 제한y
        y1 = max(0, min(roi_top_left[1], img_height - 1))
        y2 = max(0, min(roi_bottom_right[1], img_height - 1))

        # 확인오른쪽아래역할대왼쪽위역할
        x2 = max(x2, x1 + 1)
        y2 = max(y2, y1 + 1)

        return (x1, y1), (x2, y2)

    def process_image(
        self,
        image,
        element,
        anchor=None,
        center_coords_aim=None,
        center_coords_anchor=None,
        canny_flag=False,
        ratio=None,
        match_similarity=0.95,
        line_width_match=None,
        dash_color=None,
    ):
        """
        근거까지목록 
        Args:
            image (_type_): 화면스크린샷
            element (_type_): 목록 요소이미지
            anchor (_type_, optional): 요소이미지. Defaults to None.
            center_coords_aim (_type_, optional): 목록 요소. Defaults to None.
            center_coords_anchor (_type_, optional): 요소. Defaults to None.
            ratio (_type_, optional): 화면. Defaults to None.
            line_width_match (_type_, optional): 너비. Defaults to None.
            dash_color (_type_, optional): 색상. Defaults to None.

        Returns:
            _type_: 까지목록 요소의화면스크린샷
        """
        if dash_color is None:
            dash_color = "#00FF00"
        dash_color = dash_color.lstrip("#")
        color_bgr = tuple(int(dash_color[i : i + 2], 16) for i in (0, 2, 4))
        # 목록 원조회의의색상
        roi_color_bgr = tuple(int("ADD8E6"[i : i + 2], 16) for i in (2, 0, 4))
        image = cv2.cvtColor(image, cv2.COLOR_RGB2BGR)
        element = np.array(element)
        element = cv2.cvtColor(element, cv2.COLOR_RGB2BGR)
        if anchor is not None:
            anchor = np.array(anchor)
            anchor = cv2.cvtColor(anchor, cv2.COLOR_RGB2BGR)

        # 가져오기길이너비
        if ratio != "":
            rw, rh = float(ratio.split(",")[0]), float(ratio.split(",")[1])
        else:
            rw, rh = 1, 1

        # 확인및가져오기까지
        logger.info(f"현재화면및기존로{rw},{rh}")
        if center_coords_anchor != "" and anchor is not None:
            # 가져오기 변환
            aim_x, aim_y = map(int, center_coords_aim.split(","))
            anchor_x, anchor_y = map(int, center_coords_anchor.split(","))

            # 계획거리
            dis_x = (aim_x - anchor_x) * rw
            dis_y = (aim_y - anchor_y) * rh
            # dis_x = int((int(center_coords_aim.split(',')[0])-int(center_coords_anchor.split(',')[0]))*rw)
            # dis_y = int((int(center_coords_aim.split(',')[1])-int(center_coords_anchor.split(',')[1]))*rh)

        # 대이미지변환정도이미지
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        if canny_flag:
            gray = cv2.Canny(gray, 50, 250)

        # 결과가원존재함
        if element is not None:
            # 매칭목록 위치,화면대기
            # w, h = int(element.shape[1]), int(element.shape[0])
            # w, h = int(w*rw), int(h*rh)
            w, h = int(element.shape[1] * rw), int(element.shape[0] * rh)
            element = cv2.resize(element, (w, h), interpolation=cv2.INTER_CUBIC)
            small_gray = cv2.cvtColor(element, cv2.COLOR_RGB2GRAY)
            if canny_flag:
                small_gray = cv2.Canny(small_gray, 50, 250)

            if anchor is not None:
                # 필요에서화면일
                anchor_gray = cv2.cvtColor(anchor, cv2.COLOR_RGB2GRAY)
                anchor_threshold = match_similarity
                if canny_flag:
                    anchor_gray = cv2.Canny(anchor_gray, 50, 250)
                    anchor_threshold = 0.6
                aw, ah = int(anchor_gray.shape[1] * rw), int(anchor_gray.shape[0] * rh)

                anchor_gray = cv2.resize(anchor_gray, (aw, ah), interpolation=cv2.INTER_CUBIC)
                anchor_match_res = cv2.matchTemplate(gray, anchor_gray, cv2.TM_CCORR_NORMED)
                _, anchor_max_val, _, anchor_pos = cv2.minMaxLoc(anchor_match_res)

                # TODO 실행의시찾을 수 없습니다완료, 필요관리, 실행의시가져오기 변수의
                logger.info(f"현재목록 요소아니오일또는정보정도낮음, 필요, 정보정도로{anchor_max_val}")
                if anchor_max_val < anchor_threshold:
                    logger.info("화면위아니오저장된 요소또는현재이미지경과낮음가져오기 아니오까지요소")
                    # gr.Info("화면위아니오저장된 요소또는현재이미지경과낮음가져오기 아니오까지요소")

                roi_loc = (anchor_pos[0] + dis_x, anchor_pos[1] + dis_y)
                # 지정원인, 으로계획
                expand_factor = 1 / 5
                # 계획ROI의
                roi_top_left = (
                    math.ceil(roi_loc[0] - (w * expand_factor)),
                    math.ceil(roi_loc[1] - (h * expand_factor)),
                )
                roi_bottom_right = (
                    math.ceil(roi_loc[0] + w * (1 + expand_factor)),
                    math.ceil(roi_loc[1] + h * (1 + expand_factor)),
                )
                logger.info(f"roi_top_left:{roi_top_left},roi_bottom_right:{roi_bottom_right}")
                roi_top_left, roi_bottom_right = self._limit_roi_bounds(roi_top_left, roi_bottom_right, image.shape)
                logger.info(f"roi_top_left:{roi_top_left},roi_bottom_right:{roi_bottom_right}")
                self.draw_dashed_rectangle(image, roi_top_left, roi_bottom_right, roi_color_bgr, line_width_match)
                roi = gray[roi_top_left[1] : roi_bottom_right[1], roi_top_left[0] : roi_bottom_right[0]]

                result_CCORR_top = cv2.matchTemplate(roi, small_gray, cv2.TM_CCORR_NORMED)
                result_CCOEFF_top = cv2.matchTemplate(roi, small_gray, cv2.TM_CCOEFF_NORMED)
                min_rr, max_rr, _, max_loc = cv2.minMaxLoc(result_CCORR_top)
                min_a, max_ccoeff_val, _, max_loc_ccoeff = cv2.minMaxLoc(result_CCOEFF_top)

                logger.info(f"max_val:{max_ccoeff_val}")
                # target_threshold = 0.85
                target_threshold = match_similarity
                if canny_flag:
                    target_threshold = 0.40
                if max_ccoeff_val >= target_threshold:
                    match_box = (roi_top_left[0] + max_loc[0], roi_top_left[1] + max_loc[1], w, h)
                    # self.draw_dashed_rectangle(image, (roi_top_left[0] + max_loc[0], roi_top_left[1] + max_loc[1]),
                    #                            (roi_top_left[0] + max_loc[0] + w, roi_top_left[1] + max_loc[1] + h),
                    #                            color_bgr, line_width_match)
                    logger.info("요소완료에서내부매칭완료")

                else:
                    logger.info("현재화면목록 요소를 찾을 수 없습니다또는발송완료변수")
                    match_box = None

            else:
                target_match_res = cv2.matchTemplate(gray, small_gray, cv2.TM_CCOEFF_NORMED)
                _, target_max_val, _, target_max_loc = cv2.minMaxLoc(target_match_res)
                logger.info(f"target_max_val:{target_max_val}")
                target_threshold = match_similarity
                if canny_flag:
                    target_threshold = 0.40
                if target_max_val >= target_threshold:
                    match_box = (target_max_loc[0], target_max_loc[1], w, h)
                    # self.draw_dashed_rectangle(image, target_max_loc, (target_max_loc[0] + w, target_max_loc[1] + h),
                    #                            color_bgr, line_width_match)
                    logger.info("요소매칭완료")
                else:
                    logger.info("현재화면목록 요소를 찾을 수 없습니다또는발송완료변수")
                    match_box = None
            # 위치발송변수시, ROI 가능아니오패키지목록 .시도전체영역매칭, 명령중위치.
            if match_box is None:
                fallback_res = cv2.matchTemplate(gray, small_gray, cv2.TM_CCOEFF_NORMED)
                _, fallback_max_val, _, fallback_loc = cv2.minMaxLoc(fallback_res)
                fallback_threshold = match_similarity if not canny_flag else 0.40
                if fallback_max_val >= fallback_threshold:
                    match_box = (fallback_loc[0], fallback_loc[1], w, h)

        else:
            return None

        return image, match_box


if __name__ == "__main__":
    # cv_match()
    pass