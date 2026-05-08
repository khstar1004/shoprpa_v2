import os

from openpyxl import Workbook


def write_to_excel(dst_file, dst_file_name, header_dict, json_data):
    """
    를에서ai연결가져오기까지의데이터입력까지본excel중
    :param dst_file: 본excel경로(폴더)
    :param dst_file_name: 본excel이름
    :param header_dict: excel의일행제목, 형식{"payer_name": "발송", ...}
    :param json_data: 입력할까지excel중의json데이터, 가득아니오, 형식지정로[{`발송`: "ShopRPA 운영팀", ...}, {`발송`: "ShopRPA 운영팀", ...}, ...]
    :return: 본excel경로
    """
    if not os.path.splitext(dst_file_name)[1]:
        dst_file_name += ".xlsx"

    # 확인목록 폴더존재함
    if not os.path.exists(dst_file):
        os.makedirs(dst_file)

    # 생성의파일 경로
    full_file_path = os.path.join(dst_file, dst_file_name)
    wb = Workbook()
    ws = wb.active
    if ws is None:
        raise ValueError("불가가져오기 테이블")
    cols = []

    # 사용지정값테이블
    for title in header_dict.values():
        if title not in cols:
            cols.append(title)
    ws.append(cols)  # 제목
    # 값
    for line in json_data:
        row_data = []
        for title in cols:
            # 가져오기매일행title값의value값
            value = line.get(title)
            if isinstance(value, list):
                value = ",".join(value)
            row_data.append(value)
        ws.append(row_data)

    wb.save(full_file_path)
    return full_file_path


def generate_src_files(src_file, file_type="image"):
    """
    완료목록파일목록
    """
    files = []
    if os.path.exists(src_file):
        if os.path.isdir(src_file):
            for file in os.listdir(src_file):
                if file_type == "image":
                    if os.path.splitext(file)[1].lower() in (
                        ".jpg",
                        ".jpeg",
                        ".png",
                        ".bmp",
                    ):
                        files.append(os.path.join(src_file, file))
                else:
                    if file.startswith("~$"):
                        continue
                    file_path = os.path.join(src_file, file)
                    if os.path.isfile(file_path):
                        files.append(file_path)
        else:
            if file_type == "image":
                if os.path.splitext(src_file)[1].lower() in (
                    ".jpg",
                    ".jpeg",
                    ".png",
                    ".bmp",
                ):
                    files.append(src_file)
            else:
                files.append(src_file)
    return files
