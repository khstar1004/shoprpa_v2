import json
import os

from astronverse.executor.error import *
from astronverse.executor.flow.syntax.ast import CodeLine
from astronverse.executor.flow.syntax.lexer import Lexer
from astronverse.executor.flow.syntax.parser import Parser
from astronverse.executor.utils.utils import str_to_list_if_possible


class Flow:
    def __init__(self, svc):
        self.svc = svc

    def gen_component(self, path: str, project_id, mode: str, version: str):
        os.makedirs(path, exist_ok=True)
        component_list = self.svc.storage.component_list(project_id, mode, version)
        if component_list:
            for c in component_list:
                component_id = c.get("componentId")
                component_name = c.get("componentId")
                version = c.get("version")
                requirement = self._requirement_display(component_id, "", version)

                component_path = os.path.join(path, "c{}".format(component_id))
                main_params = []
                self.gen_code(
                    path=component_path, project_id=component_id, mode="", version=version, main_params=main_params
                )
                self.svc.add_component_info(
                    project_id,
                    component_id,
                    component_name,
                    version,
                    requirement,
                    "c{}.{}".format(component_id, "main.py"),
                    main_params,
                )

    def gen_code(
        self,
        path: str,
        project_id: str,
        mode: str,
        version: str,
        process_id: str = "",
        line=0,
        end_line=0,
        main_params=None,
    ):
        if main_params is None:
            main_params = []
        os.makedirs(path, exist_ok=True)
        package = path.rstrip("/").split("/")[-1]

        # 1. 가져오기전역 변수
        global_var = self._global_display(project_id, mode, version)
        requirement = self._requirement_display(project_id, mode, version)
        project_info = self.svc.storage.project_info(project_id=project_id, mode=mode, version=version)
        if project_info:
            project_name = project_info.get("name", "봇")
            project_icon = project_info.get("iconUrl", "")
        else:
            project_name = "봇"
            project_icon = ""
        self.svc.add_project_info(
            project_id, mode, version, project_name, requirement, self.svc.conf.gateway_port, global_var, project_icon
        )

        # 2. 완료프로세스닫기데이터
        process_list = self.svc.storage.process_list(project_id=project_id, mode=mode, version=version)
        if len(process_list) == 0:
            raise BaseException(PROCESS_ACCESS_ERROR_FORMAT, "데이터예외 {}".format(project_id))

        process_index = 1
        module_index = 1
        has_main_entry = False
        for process in process_list:
            name = process.get("name")
            category = process.get("resourceCategory")
            resource_id = str(process.get("resourceId", ""))
            file_name = ""

            # 여부예입력
            is_main_process = False
            if process_id:
                if resource_id == str(process_id):
                    is_main_process = True
                    file_name = "main.py"
            else:
                if name == self.svc.conf.main_process_name:
                    is_main_process = True
                    file_name = "main.py"

            # 완료python코드
            if category == "process":
                # 가져오기이름
                if not file_name:
                    file_name = "process{}.py".format(process_index)
                process_index += 1

                # 가져오기매개변수
                param_list = self.svc.storage.param_list(
                    project_id=project_id, mode=mode, version=version, process_id=resource_id
                )
                for p in param_list:
                    param = self.svc.param.parse_param(
                        {
                            "value": str_to_list_if_possible(p.get("varValue")),
                            "types": p.get("varType"),
                            "name": p.get("varName"),
                        }
                    )
                    p["varValue"] = param.show_value()

                # 데이터
                self.svc.add_process_info(project_id, resource_id, category, name, file_name, param_list)

                # 입력python
                if is_main_process:
                    res, map_res = self._flow_display(
                        project_id, mode, version, resource_id, name, start_line=line, end_line=end_line
                    )
                else:
                    res, map_res = self._flow_display(project_id, mode, version, resource_id, name)
                with open(os.path.join(path, file_name), "w", encoding="utf-8") as file:
                    file.write(res)
                    pass
                with open(os.path.join(path, file_name.replace(".py", ".map")), "w", encoding="utf-8") as file:
                    file.write(map_res)
                    pass
            elif category == "module":
                # 가져오기이름
                if not file_name:
                    file_name = "module{}.py".format(module_index)
                module_index += 1

                # 가져오기매개변수
                param_list = self.svc.storage.param_list(
                    project_id=project_id, mode=mode, version=version, module_id=resource_id
                )
                for p in param_list:
                    param = self.svc.param.parse_param(
                        {
                            "value": str_to_list_if_possible(p.get("varValue")),
                            "types": p.get("varType"),
                            "name": p.get("varName"),
                        }
                    )
                    p["varValue"] = param.show_value()

                # 데이터
                self.svc.add_process_info(project_id, resource_id, category, name, file_name, param_list)

                # 입력python
                res = self._module_display(project_id, mode, version, resource_id, name)
                with open(os.path.join(path, file_name), "w", encoding="utf-8") as file:
                    file.write(res)
                    pass
            else:
                raise NotImplementedError()

            if is_main_process and isinstance(main_params, list) and len(main_params) == 0:
                has_main_entry = True
                main_params.extend(param_list)

        if not has_main_entry:
            raise BaseException(PROCESS_ACCESS_ERROR_FORMAT, "데이터예외 {}".format(project_id))

        # 2.1 완료가능컴포넌트
        smart_index = 1
        for smart_key, smart_info in self.svc.ast_globals_dict[project_id].smart_component_info.items():
            file_name = "smart{}.py".format(smart_index)
            smart_index += 1
            with open(os.path.join(path, file_name), "w", encoding="utf-8") as file:
                res = self._smart_component_display(
                    project_id, mode, version, smart_info.smart_id, smart_info.smart_version
                )
                if res:
                    self.svc.update_smart_component(project_id, smart_key, file_name, res.get("smartType"))
                    file.write(res.get("smartCode"))

        # 3. 완료project.py
        tpl_path = os.path.join(os.path.dirname(__file__), "tpl", "package.tpl")
        with open(tpl_path, encoding="utf-8") as tpl_file:
            tpl_content = tpl_file.read()

        global_code = ""
        for k, v in global_var.items():
            global_code += f"gv[{k!r}] = {v}\n"
        tpl_content = tpl_content.replace("{{GLOBAL}}", global_code)
        tpl_content = tpl_content.replace("{{PACKAGE_PATH}}", repr(os.path.join(path, "package.json")))
        package_py_content = tpl_content.replace("{{PACKAGE}}", package)
        with open(os.path.join(path, "package.py"), "w", encoding="utf-8") as file:
            file.write(package_py_content)

        # 4. 완료package.json
        res = json.dumps(
            self.svc.ast_globals_dict[project_id],
            default=lambda o: o.__json__() if hasattr(o, "__json__") else None,
            ensure_ascii=False,
            indent=4,
        )
        with open(os.path.join(path, "package.json"), "w", encoding="utf-8") as file:
            file.write(res)

        # 5. 완료__init__.py(디렉터리성공로패키지, 지요소가져오기)
        init_py_path = os.path.join(path, "__init__.py")
        if not os.path.exists(init_py_path):
            with open(init_py_path, "w", encoding="utf-8") as file:
                file.write("")

    def _requirement_display(self, project_id: str, mode: str, version: str):
        """
        현재패키지의
        """

        requirement = dict()
        res = self.svc.storage.pip_list(project_id=project_id, mode=mode, version=version)
        for i in res:
            pack_name = i.get("packageName")
            pack_version = i.get("packageVersion")
            pack_mirror = i.get("mirror")
            if pack_name not in requirement:
                requirement[pack_name] = {
                    "package_name": pack_name,
                    "package_version": pack_version,
                    "package_mirror": pack_mirror,
                }
        return requirement

    def _global_display(self, project_id: str, mode: str, version: str):
        """
        현재패키지의방문전역 변수
        """
        global_list = self.svc.storage.global_list(project_id=project_id, mode=mode, version=version)
        global_var = {}
        for g in global_list:
            param = self.svc.param.parse_param(
                {
                    "value": str_to_list_if_possible(g.get("varValue")),
                    "types": g.get("varType"),
                    "name": g.get("varName"),
                }
            )
            global_var[g["varName"]] = param.show_value()
        return global_var

    def _module_display(self, project_id: str, mode: str, version: str, module_id: str, module_name) -> str:
        """
        모듈완료 python모듈
        """
        # 가져오기모듈데이터
        module_code = self.svc.storage.module_detail(
            project_id=project_id, mode=mode, version=version, module_id=module_id
        )

        # 내용열기 
        if "rpahelper" in module_code:
            # 코드
            module_code = module_code.replace("rpahelper", "astronverse.workflowlib")
        # 내용결과

        return module_code

    def _smart_component_display(
        self, project_id: str, mode: str, version: str, smart_id: str, smart_version: str
    ) -> str:
        return self.svc.storage.smart_component_detail(
            project_id=project_id, smart_id=smart_id, smart_version=smart_version, mode=mode, version=version
        )

    def _inject_params_to_module(self, module_code: str, param_list: list) -> str:
        """
        를구성 매개변수비고입력까지Python모듈코드중
        에서 def main(args): 데이터열기 추가입력매개변수 
        에서데이터결과추가출력매개변수 돌아가기
        """
        import re

        # 분입력매개변수및출력매개변수
        input_params = [p for p in param_list if p.get("varDirection") == 0]
        output_params = [p for p in param_list if p.get("varDirection") == 1]

        # 조회 def main(args): 의위치
        main_pattern = r"(def\s+main\s*\(\s*args\s*\)\s*(?:->.*?)?\s*:)"
        main_match = re.search(main_pattern, module_code)

        if not main_match:
            # 결과가있음까지 main 데이터, 직선연결반환기존코드
            return module_code

        # 계획 main 데이터의(통신일반예4개공백)
        indent = self.svc.conf.indentation

        # 완료입력매개변수 코드(복사사용 svc.param.parse_param)
        input_code_lines = []
        for p in input_params:
            var_name = p.get("varName")
            param = self.svc.param.parse_param(
                {
                    "value": str_to_list_if_possible(p.get("varValue")),
                    "types": p.get("varType"),
                    "name": var_name,
                }
            )
            input_code_lines.append(f'{indent}{var_name} = args.get("{var_name}", {param.show_value()})')

        # 완료출력매개변수 코드(출력매개변수 필요)
        for p in output_params:
            var_name = p.get("varName")
            param = self.svc.param.parse_param(
                {
                    "value": str_to_list_if_possible(p.get("varValue")),
                    "types": p.get("varType"),
                    "name": var_name,
                }
            )
            input_code_lines.append(f'{indent}{var_name} = args.get("{var_name}", {param.show_value()})')

        if input_code_lines:
            input_code_lines.append(f"{indent}# --- 구성 매개변수 결과 ---")
            input_code_lines.append("")

        # 에서 main 데이터지정후삽입입력매개변수 코드
        input_code = "\n".join(input_code_lines)
        main_end_pos = main_match.end()

        # 삽입입력매개변수 코드
        new_code = module_code[:main_end_pos] + "\n" + input_code + module_code[main_end_pos:]

        # 결과가있음출력매개변수, 필요에서데이터돌아가기
        if output_params:
            # 완료출력매개변수 돌아가기코드
            output_code_lines = [f"{indent}# --- 출력매개변수 돌아가기 ---"]
            for p in output_params:
                var_name = p.get("varName")
                output_code_lines.append(f'{indent}args["{var_name}"] = {var_name}')

            output_code = "\n" + "\n".join(output_code_lines)

            # 까지 return 또는데이터, 에서전삽입출력매개변수 돌아가기코드
            # 단일관리: 에서후일개 return 전삽입(결과가있음의)
            return_pattern = r"(\n)([ \t]*)(return\b.*?)(\n|$)"
            return_matches = list(re.finditer(return_pattern, new_code))

            if return_matches:
                # 에서후일개 return 전삽입
                last_return = return_matches[-1]
                insert_pos = last_return.start()
                new_code = new_code[:insert_pos] + output_code + new_code[insert_pos:]
            else:
                # 있음 return, 에서코드추가
                new_code = new_code.rstrip() + "\n" + output_code + "\n"

        return new_code

    def _flow_display(
        self, project_id: str, mode: str, version: str, process_id: str, process_name: str, start_line=0, end_line=0
    ):
        """
        프로세스완료 프로세스 하위 프로세스
        """

        # 1. 가져오기프로세스데이터
        flow_list = self.svc.storage.process_detail(
            project_id=project_id, mode=mode, version=version, process_id=process_id
        )
        line = 0
        new_flow_list = []
        process_meta = []
        for k, v in enumerate(flow_list):
            line = line + 1
            if v.get("disabled"):
                continue
            if start_line > 0 and line < start_line:
                continue
            if end_line > 0 and line > end_line:
                continue
            v.update(
                {
                    "__line__": line,
                    "__process_id__": process_id,
                }
            )
            if v.get("breakpoint"):
                # 프로세스설명의
                self.svc.add_breakpoint(project_id, process_id, line)
            process_meta.append([line, v.get("id"), v.get("alias", v.get("title", "")), v.get("key")])
            new_flow_list.append(v)

        self.svc.add_process_meta(project_id, process_id, process_meta)

        # 2. 파싱
        lexer = Lexer(flow_list=new_flow_list)
        parser = Parser(lexer=lexer)
        program = parser.parse_program()
        if len(parser.errors) > 0:
            raise BaseException(
                SYNTAX_ERROR_FORMAT.format(" ".join(parser.errors)), "문법 오류: {}".format(parser.errors)
            )
        self.svc.ast_curr_info = {
            "__project_id__": project_id,
            "__mode__": mode,
            "__version__": version,
            "__process_id__": process_id,
            "__process_name__": process_name,
        }
        result = program.display(svc=self.svc, tab_num=0)
        code_lines = []
        map_list = []
        for i, code_line in enumerate(result):
            if isinstance(code_line, CodeLine):
                indent = str(self.svc.conf.indentation * code_line.tab_num)
                code_lines.append(indent + code_line.code)
                if code_line.line > 0:
                    map_list.append("{}:{}".format(i + 1, code_line.line))
        return "\n".join(code_lines), ",".join(map_list)