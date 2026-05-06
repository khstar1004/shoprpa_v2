import os
import platform
from ctypes import *

# 로드DLL
script_dir = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "gbild")
if platform.architecture()[0] == "64bit":
    dll_path = os.path.join(script_dir, "gbild64.dll")
else:
    dll_path = os.path.join(script_dir, "gbild32.dll")
dll = windll.LoadLibrary(dll_path)

# 연결반환값유형
dll.getmodel.restype = c_char_p
dll.getserialnumber.restype = c_char_p
dll.getproductiondate.restype = c_char_p
dll.getfirmwareversion.restype = c_char_p
dll.getclientscreenresolution.restype = c_char_p
dll.readstring.restype = c_char_p
dll.encryptstring.restype = c_char_p
dll.decryptstring.restype = c_char_p
dll.getproductionname.restype = c_char_p


# ================ 준비
# 열기 준비(근거준비순서)
def opendevice(index):
    return dll.opendevice(index)


# 열기 준비(근거준비ID)
def opendevicebyid(vid, pid):
    return dll.opendevicebyid(vid, pid)


# 열기 준비(근거준비경로)
def opendevicebypath(path):
    return dll.opendevicebypath(bytes(path, "utf-8"))


# 조회준비여부연결
def isconnected():
    return dll.isconnected()


# 닫기 준비
def closedevice():
    return dll.closedevice()


# 복사위치준비
def resetdevice():
    return dll.resetdevice()


# ================ 준비정보
# 가져오기 준비유형
def getmodel():
    return dll.getmodel().decode("utf-8")


# 가져오기 준비순서열
def getserialnumber():
    return dll.getserialnumber().decode("utf-8")


# 가져오기 준비제품날짜
def getproductiondate():
    return dll.getproductiondate().decode("utf-8")


# 가져오기 준비파일버전
def getfirmwareversion():
    return dll.getfirmwareversion().decode("utf-8")


# ================ 키보드
# 아래
def presskeybyname(keyn):
    return dll.presskeybyname(bytes(keyn, "utf-8"))


def presskeybyvalue(keyv):
    return dll.presskeybyvalue(keyv)


# 
def releasekeybyname(keyn):
    return dll.releasekeybyname(bytes(keyn, "utf-8"))


def releasekeybyvalue(keyv):
    return dll.releasekeybyvalue(keyv)


# 아래
def pressandreleasekeybyname(keyn):
    return dll.pressandreleasekeybyname(bytes(keyn, "utf-8"))


def pressandreleasekeybyvalue(keyv):
    return dll.pressandreleasekeybyvalue(keyv)


# 키보드상태
def iskeypressedbyname(keyn):
    return dll.iskeypressedbyname(bytes(keyn, "utf-8"))


def iskeypressedbyvalue(keyv):
    return dll.iskeypressedbyvalue(keyv)


# 모든키보드
def releaseallkey():
    return dll.releaseallkey()


# 입력문자열
def inputstring(str):
    return dll.inputstring(bytes(str, "utf-8"))


# 가져오기대지정상태
def getcapslock():
    return dll.getcapslock()


# 가져오기숫자키보드지정상태
def getnumlock():
    return dll.getnumlock()


# 여부분크기
def setcasesensitive(cs):
    return dll.setcasesensitive(cs)


# 지연
def setpresskeydelay(maxd, mind):
    return dll.setpresskeydelay(maxd, mind)


# 입력문자열시간
def setinputstringintervaltime(maxd, mind):
    return dll.setinputstringintervaltime(maxd, mind)


# ================ 마우스
# 아래마우스
def pressmousebutton(mbtn):
    return dll.pressmousebutton(mbtn)


# 마우스
def releasemousebutton(mbtn):
    return dll.releasemousebutton(mbtn)


# 아래마우스
def pressandreleasemousebutton(mbtn):
    return dll.pressandreleasemousebutton(mbtn)


# 마우스상태
def ismousebuttonpressed(mbtn):
    return dll.ismousebuttonpressed(mbtn)


# 모든마우스
def releaseallmousebutton():
    return dll.releaseallmousebutton()


# 마우스
def movemouserelative(x, y):
    return dll.movemouserelative(x, y)


# 마우스까지지정
def movemouseto(x, y):
    return dll.movemouseto(x, y)


# 가져오기마우스현재위치
def getmousex():
    return dll.getmousex()


def getmousey():
    return dll.getmousey()


# 마우스
def movemousewheel(z):
    return dll.movemousewheel(z)


# 마우스지연
def setpressmousebuttondelay(maxd, mind):
    return dll.setpressmousebuttondelay(maxd, mind)


# 마우스지연
def setmousemovementdelay(maxd, mind):
    return dll.setmousemovementdelay(maxd, mind)


# 마우스 속도
def setmousemovementspeed(speedvalue):
    return dll.setmousemovementspeed(speedvalue)


# 마우스 이동 방식
def setmousemovementmode(modevalue):
    return dll.setmousemovementmode(modevalue)


# ================ 기기연결
# 마우스현재위치(지원하지 않음값의마우스)
def setmouseposition(x, y):
    return dll.setmouseposition(x, y)


# 마우스위치(지요소값의마우스)
def setmouseabsoluteposition(x, y):
    return dll.setmouseabsoluteposition(x, y)


# 단말화면분
def setclientscreenresolution(width, height):
    return dll.setclientscreenresolution(width, height)


# 가져오기 단말화면분
def getclientscreenresolution():
    return dll.getclientscreenresolution().decode("utf-8")


# ================ 암호화
# 암호화
def initializedongle():
    return dll.initializedongle()


# 비밀번호
def setreadpassword(writepwd, newpwd):
    return dll.setreadpassword(bytes(writepwd, "utf-8"), bytes(newpwd, "utf-8"))


# 비밀번호
def setwritepassword(oldpwd, newpwd):
    return dll.setwritepassword(bytes(oldpwd, "utf-8"), bytes(newpwd, "utf-8"))


# 에서준비문자열
def readstring(readpwd, addr, count):
    return dll.readstring(bytes(readpwd, "utf-8"), addr, count).decode("utf-8")


# 를문자열입력준비
def writestring(writepwd, str, addr):
    return dll.writestring(bytes(writepwd, "utf-8"), bytes(str, "utf-8"), addr)


# 키
def setcipher(writepwd, cipher):
    return dll.setcipher(bytes(writepwd, "utf-8"), bytes(cipher, "utf-8"))


# 암호화문자열
def encryptstring(str):
    return dll.encryptstring(bytes(str, "utf-8")).decode("utf-8")


# 복호화문자열
def decryptstring(str):
    return dll.decryptstring(bytes(str, "utf-8")).decode("utf-8")


# ================ 제어연결
# 아래버튼
def presspowerbutton():
    return dll.presspowerbutton()


# 버튼
def releasepowerbutton():
    return dll.releasepowerbutton()


# 아래버튼
def pressandreleasepowerbutton():
    return dll.pressandreleasepowerbutton()


# 가져오기 상태
def getpowerstatus():
    return dll.getpowerstatus()


# ================ 준비지정연결
# 수정준비정도
def setspeed(speed):
    return dll.setspeed()


# 수정준비ID
def setdeviceid(vid, pid):
    return dll.setdeviceid(vid, pid)


# 복사준비ID
def restoredeviceid():
    return dll.restoredeviceid()


# 제품품목이름
def setproductname(name):
    return dll.setproductname(bytes(name, "gbk"))


# 가져오기제품품목이름
def getproductionname():
    return dll.getproductionname().decode("gbk")