"""Contract analysis and clause extraction utilities."""

import ast

from astronverse.actionlib import AtomicFormType, AtomicFormTypeMeta, AtomicLevel, DynamicsItem
from astronverse.actionlib.atomic import atomicMg
from astronverse.actionlib.types import PATH
from astronverse.ai import InputType
from astronverse.ai.api.llm import chat_prompt
from astronverse.ai.prompt.contract import CONTRACT_FACTOR_DICT
from astronverse.ai.utils.extract import FileExtractor


class ContractAI:
    """AI helpers for extracting contract clauses and key factors."""

    @staticmethod
    @atomicMg.atomic(
        "ContractAI",
        inputList=[
            atomicMg.param(
                "contract_path",
                dynamics=[
                    DynamicsItem(
                        key="$this.contract_path.show",
                        expression="return $this.contract_type.value == '{}'".format(InputType.FILE.value),
                    )
                ],
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"filters": [], "file_type": "file"},
                ),
            ),
            atomicMg.param(
                "contract_content",
                dynamics=[
                    DynamicsItem(
                        key="$this.contract_content.show",
                        expression="return $this.contract_type.value == '{}'".format(InputType.TEXT.value),
                    )
                ],
            ),
            atomicMg.param(
                "custom_factors",
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.FACTOR_ELEMENT.value,
                    params={
                        "code": 3,
                        "options": [
                            "합치기이름",
                            "합치기번호",
                            "합치기주문날짜",
                            "합치기열기 날짜",
                            "합치기결과날짜",
                            "합치기의",
                            "의수",
                            "단일가격",
                            "",
                            "금액",
                            "합치기금액",
                            "방식",
                            "방법",
                            "방법",
                            "방법열기사용자행",
                            "방법행계정",
                            "방법열기사용자행",
                            "방법행계정",
                        ],
                    },
                ),
            ),
            atomicMg.param(
                "contract_validate",
                formType=AtomicFormTypeMeta(type=AtomicFormType.MODALBUTTON.value, params={"loading": False}),
                required=False,
            ),
            atomicMg.param("model", level=AtomicLevel.ADVANCED, required=False),
        ],
        outputList=[atomicMg.param("factor_result", types="Dict")],
    )
    def get_factors(
        contract_type: InputType = InputType.TEXT,
        contract_path: PATH = "",
        contract_content: str = "",
        custom_factors: str = "",
        contract_validate: str = "",
        model: str = "",
    ):
        """Extract specified factors from a contract file or text content."""
        if contract_type == InputType.FILE:
            contract_content = FileExtractor(contract_path).extract_text()

        try:
            custom_factors = ast.literal_eval(custom_factors)
        except:
            raise ValueError("custom_factors 형식오류, 확인하세요")
        preset_factors = custom_factors.get("preset", [])  # type: ignore
        custom_factors = custom_factors.get("custom", [])  # type: ignore

        factors = []
        for factor in preset_factors:
            factors.append({"필요이름": factor, "필요설명": CONTRACT_FACTOR_DICT[factor]})
        for factor in custom_factors:
            if isinstance(factor, dict):
                factors.append(
                    {
                        "필요이름": factor.get("name", ""),
                        "필요설명": factor.get("desc", ""),
                        "필요": factor.get("example", ""),
                    }
                )
            elif isinstance(factor, (list, tuple)) and len(factor) >= 1:
                factors.append(
                    {
                        "필요이름": factor[0],
                        "필요설명": factor[1] if len(factor) > 1 else "",
                        "필요": factor[2] if len(factor) > 2 else "",
                    }
                )

        params = {"factors": str(factors), "parsed_content": contract_content}
        if model:
            reply = chat_prompt(prompt_type="contract", params=params, model=model)
        else:
            reply = chat_prompt(prompt_type="contract", params=params)

        return reply