"""Prompt templates for role-play and question generation in chat flows."""

PROMPT_ROLE_PLAY = """
    당신은 {role} 역할입니다. 사용자의 질문에 역할에 맞는 답변을 제공하세요.
    
    1. {role}이 알 수 있는 범위에서 명확하게 답변하세요.
    2. 역할 밖의 내용은 추측하지 말고 필요한 정보를 요청하세요.
    
"""

PROMPT_GENERATE_QUESTION = """
    아래 텍스트를 바탕으로 핵심 확인 질문 3개를 생성하고, 문자열 목록 형태로만 반환하세요.
    
    # Demo
    <text>
    ShopRPA는 반복 업무를 워크플로우로 만들고 실행하는 데스크톱 자동화 프로그램입니다.
    사용자는 화면 조작, 브라우저 작업, 파일 처리, API 연동을 자동화할 수 있습니다.
    서버와 클라이언트를 연결하면 팀 단위 실행 관리와 로그 확인도 가능합니다.
    </text>
    
    <return>
    ['어떤 반복 업무를 자동화할 수 있나요?', '서버와 클라이언트는 어떻게 연결되나요?', '실행 결과는 어디에서 확인하나요?']
    
    
    <text>
    {text}
    </text>
    
    <return>

"""
