"""Prompt templates for role-play and question generation in chat flows."""

PROMPT_ROLE_PLAY = """
    일이름{role}, 를으로사용자의출력제목, 필요: 
    
    1. 요청 돌아가기{role}알림내부의제목, 결과가초과출력{role}가능돌아가기의제목, 요청 돌아가기아니오알림.
    2. 요청 {role}의및, 알림행돌아가기.
    
"""

PROMPT_GENERATE_QUESTION = """
    를근거위서술까지의텍스트내용, 완료삼개단일의제목, 으로목록의방식반환.
    
    # Demo
    <text>
    를요청위, .소, 열기출력문, 돌아가기결과.있음본, 이를요청위, 로대, 관리, 대대.
    이를, 일위의., 완료완료Shoprpa.에서출력, 예압축에서완료오행아래.
    빈압축오년후, 가져오기 경로경과, 완료빈완료대, 경로위후완료, 에서높이완료이팔, 에서완료삼.
    </text>
    
    <return>
    ['예완료빈?', '의삼예?', '빈에서작업서비스?']
    
    
    <text>
    {text}
    </text>
    
    <return>

"""