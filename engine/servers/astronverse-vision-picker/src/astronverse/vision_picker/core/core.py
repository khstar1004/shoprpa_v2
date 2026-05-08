import base64
import io
import json
import logging
import os
from abc import ABC, abstractmethod

import cv2
import numpy as np
import pyautogui
import requests
from astronverse.vision_picker.core.cv_match import AnchorMatch
from PIL import Image

current_directory = os.getcwd()
match_filepath = os.path.join(current_directory, "imgs", "match_img.png")
logger = logging.getLogger(__name__)


class IRectHandler(ABC):
    @staticmethod
    @abstractmethod
    def get_foreground_window_rect():
        pass


class IPickCore(ABC):
    @staticmethod
    @abstractmethod
    def get_mouse_position():
        pass

    @staticmethod
    def image_to_base64(img):
        """
        를이미지변환로base64
        :param img: 입력이미지
        :return:
        """
        img_byte_arr = io.BytesIO()
        img.save(img_byte_arr, format="PNG")  # convert to bytes
        img_byte_arr = img_byte_arr.getvalue()  # convert bytes to base64
        return base64.b64encode(img_byte_arr).decode("utf-8")

    @staticmethod
    def get_img(rect, desktop):
        """
        가져오기요소이미지
        :param rect:
        :param desktop:
        :return:
        """
        return desktop.crop(rect)

    @staticmethod
    def json_res(target_img, target_rect, anchor_img, anchor_rect, screen):
        """
        반환선택까지의목록 요소요소의json결과
        :param target_img:
        :param target_rect:
        :param anchor_img:
        :param anchor_rect:
        :return:
        """

        def encode_image(image):
            return IPickCore.image_to_base64(image) if image else ""

        def get_position(rect, index):
            return rect[index] if rect else ""

        res = {
            "version": "1",
            "type": "cv",
            "app": "",
            "path": "",
            "img": {
                "self": encode_image(target_img),
                "parent": encode_image(anchor_img),
            },
            "pos": {
                "self_x": get_position(target_rect, 0),
                "self_y": get_position(target_rect, 1),
                "parent_x": get_position(anchor_rect, 0),
                "parent_y": get_position(anchor_rect, 1),
            },
            "sr": {"screen_w": screen.width, "screen_h": screen.height},
            "picker_type": "ELEMENT",
        }

        if not (target_img or anchor_img or target_rect or anchor_rect):
            return None

        return json.dumps(res, ensure_ascii=False)

    @staticmethod
    def base64_to_image(base64_str):
        if not base64_str:
            return None

        try:
            image_data = base64.b64decode(base64_str)
            image = Image.open(io.BytesIO(image_data))
            return image
        except Exception as e:
            logger.warning("Failed to convert base64 data to image: %s", e)
            return None

    @staticmethod
    def get_url(input_url, remote_addr):
        logger.debug("Fetching picker image from %s%s", remote_addr, input_url)
        try:
            response = requests.get(f"{remote_addr}{input_url}")
            response.raise_for_status()
        except requests.exceptions.RequestException as e:
            raise Exception(f"서버 오류: {e}")
        base64_encoded_data = base64.b64encode(response.content).decode("utf-8")
        return base64_encoded_data

    @staticmethod
    def match_imgs(data, remote_addr, canny_flag=False):
        match = AnchorMatch()

        target = data["img"]["self"]
        anchor = data["img"]["parent"]
        match_similarity = data.get("similarity", 0.60)

        if target.startswith("/api"):
            target = IPickCore.get_url(target, remote_addr)

        target_img = IPickCore.base64_to_image(target)
        center_coords_aim = f"{data['pos']['self_x']},{data['pos']['self_y']}"

        if anchor:
            if anchor.startswith("/api"):
                anchor = IPickCore.get_url(anchor, remote_addr)
            anchor_img = IPickCore.base64_to_image(anchor)
            center_coords_anchor = f"{data['pos']['parent_x']},{data['pos']['parent_y']}"
        else:
            anchor_img = None
            center_coords_anchor = ""

        match_img = pyautogui.screenshot(region=None)
        match_img.save(match_filepath)
        ratio_w = match_img.width / data["sr"]["screen_w"]
        ratio_h = match_img.height / data["sr"]["screen_h"]
        ratio = f"{ratio_w},{ratio_h}"
        match_img = np.array(match_img)
        try:
            _, match_box = match.process_image(
                match_img,
                target_img,
                anchor_img,
                center_coords_aim=center_coords_aim,
                center_coords_anchor=center_coords_anchor,
                canny_flag=canny_flag,
                ratio=ratio,
                match_similarity=match_similarity,
            )
        except cv2.error as e:
            match_box = None
        finally:
            return match_box
