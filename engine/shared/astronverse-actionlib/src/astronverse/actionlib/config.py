import os

from astronverse.baseline.config.config import load_config


class Config:
    """가져오기매칭"""

    data: dict = {}

    def __init__(self):
        script_dir = os.path.dirname(os.path.abspath(__file__))
        self.set_config_file(os.path.join(script_dir, "config.yaml"))

    def set_config_file(self, url, file_type="yaml"):
        data = load_config(url, file_type)
        if not data or not isinstance(data, dict):
            return

        # 병합매칭데이터
        for key, val in data.items():
            if key in self.data:
                self.data[key].update(val)
            else:
                self.data[key] = val

    def get(self, *args):
        data = self.data
        for key in args:
            if data is None:
                break
            data = data.get(key, None)
        return data


config = Config()