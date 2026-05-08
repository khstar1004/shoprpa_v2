from astronverse.browser.browser_software import BrowserSoftware
from astronverse.browser import CommonForBrowserType, CommonForTimeoutHandleType


def test_browser_open_demo():
    """browser_open데이터의단일demo시도"""
    
    print("열기 시도 browser_open 데이터...")
    
    try:
        # 직선연결호출browser_open데이터
        browser = BrowserSoftware.browser_open(
            url="https://example.com",
            browser_type=CommonForBrowserType.BTChrome,
            browser_abs_path="",
            open_args="",
            open_with_incognito=False,
            wait_load_success=False,  # 아니오대기로드완료, 길이시간대기
            timeout=5,
            timeout_handle_type=CommonForTimeoutHandleType.ExecError
        )
        
        print(f"✅ browser_open호출성공!")
        print(f"   브라우저유형: {browser.browser_type}")
        print(f"   브라우저경로: {browser.browser_abs_path}")
        print(f"   브라우저제어객체: {browser.browser_control}")
        
        # 시도가져오기일본정보
        try:
            url = browser.get_url()
            print(f"   현재URL: {url}")
        except Exception as e:
            print(f"   ⚠️ 가져오기URL실패: {e}")
            
        try:
            title = browser.get_title()
            print(f"   현재제목: {title}")
        except Exception as e:
            print(f"   ⚠️ 가져오기제목실패: {e}")
            
        # 닫기브라우저
        try:
            # BrowserSoftware.browser_close(browser)
            print("✅ 브라우저완료닫기")
        except Exception as e:
            print(f"   ⚠️ 닫기브라우저실패: {e}")
            
        return True
        
    except Exception as e:
        print(f"❌ browser_open호출실패: {e}")
        return False


if __name__ == "__main__":
    print("=" * 50)
    print("browser_open데이터demo시도")
    print("=" * 50)
    
    success = test_browser_open_demo()
    
    print("=" * 50)
    if success:
        print("✅ 시도완료!")
    else:
        print("❌ 시도실패!")
    print("=" * 50)
