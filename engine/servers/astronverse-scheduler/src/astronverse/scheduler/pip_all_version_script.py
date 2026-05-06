import importlib_metadata

# 가져오기모든완료설치의분발송패키지
packages = importlib_metadata.distributions()

# 생성일개딕셔너리저장패키지이름및버전
package_info = {}

for package in packages:
    name = package.metadata.get("Name", None)
    version = package.version
    if not name:
        continue
    print(f"{name}=={version}")