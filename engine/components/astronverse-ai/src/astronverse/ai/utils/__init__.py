"""Recruitment JD analysis and candidate requirement evaluation utilities."""

from astronverse.actionlib import AtomicFormType, AtomicFormTypeMeta, AtomicLevel, DynamicsItem
from astronverse.actionlib.atomic import atomicMg
from astronverse.ai import InputType, JobWebsitesTypes, RatingSystemTypes
from astronverse.ai.api.llm import chat_prompt
from astronverse.ai.utils.extract import FileExtractor
from astronverse.baseline.logger.logger import logger


class RecruitAI:
    """AI helpers for generating recruitment keyword sets and scoring resumes."""

    keywords_boss = {
        "필요": ["아니오제한", "본Shoprpa으로위", "으로위", ""],
        "검사필요": [
            "아니오제한",
            "시스템본Shoprpa",
            "일검사",
            "211검사",
            "985검사",
            "",
            "QS 100",
            "QS 500",
        ],
        "검증필요": ["아니오제한", "에서검사/", "1-3년", "3-5년", "5-10년", "10년으로위"],
        "년필요": [
            "아니오제한",
            "20-25",
            "25-30",
            "30-35",
            "35-40",
            "40-45",
            "45-50",
            "50으로위",
        ],
    }

    keywords_zhilian = {
        "필요": ["아니오제한", "대으로위", "본Shoprpa으로위", "으로위", "지정"],
        "년필요": ["아니오제한", "20-25", "25-30", "30-35", "35-40", "40으로위", "지정"],
        "검증필요": ["아니오제한", "", "1-3년", "3-5년", "5-10년", "지정"],
        "검사필요": ["아니오제한", "시스템", "985", "211", "일", "외부검사"],
    }

    keywords_dict = {"boss": keywords_boss, "lieping": {}, "zhilian": keywords_zhilian}

    @atomicMg.atomic(
        "RecruitAI",
        inputList=[
            atomicMg.param(
                "job_description",
                formType=AtomicFormTypeMeta(type=AtomicFormType.INPUT_PYTHON_TEXTAREAMODAL_VARIABLE.value),
            ),
            atomicMg.param("model", level=AtomicLevel.ADVANCED, required=False),
        ],
        outputList=[atomicMg.param("recruit_keywords", types="Str")],
    )
    def generate_keywords(
        self,
        job_name: str = "",
        job_description: str = "",
        job_website: JobWebsitesTypes = JobWebsitesTypes.BOSS,
        model: str = "",
    ):
        """Generate structured recruitment keywords for a given job description.

        Args:
            job_name: job title
            job_description: free text JD
            job_website: target job board type
            model: model ID
        Returns:
            Raw model reply string containing keyword groups.
        """
        params = {
            "job_name": job_name,
            "job_description": job_description,
            "keywords_list": self.keywords_dict[job_website.value],
        }
        if model:
            reply = chat_prompt(prompt_type="recruit_keywords", params=params, model=model)
        else:
            reply = chat_prompt(prompt_type="recruit_keywords", params=params)
        logger.info("RecruitAI generate_keywords: {}".format(reply))
        return reply

    @atomicMg.atomic(
        "RecruitAI",
        inputList=[
            atomicMg.param(
                "resume_content",
                dynamics=[
                    DynamicsItem(
                        key="$this.resume_content.show",
                        expression="return $this.resume_input_type.value == '{}'".format(InputType.TEXT.value),
                    )
                ],
            ),
            atomicMg.param(
                "resume_file_path",
                dynamics=[
                    DynamicsItem(
                        key="$this.resume_file_path.show",
                        expression="return $this.resume_input_type.value == '{}'".format(InputType.FILE.value),
                    )
                ],
                formType=AtomicFormTypeMeta(
                    type=AtomicFormType.INPUT_VARIABLE_PYTHON_FILE.value,
                    params={"filters": [], "file_type": "file"},
                ),
            ),
            atomicMg.param(
                "rating_dimensions",
                dynamics=[
                    DynamicsItem(
                        key="$this.rating_dimensions.show",
                        expression="return $this.rating_system.value == '{}'".format(RatingSystemTypes.CUSTOM.value),
                    )
                ],
            ),
            atomicMg.param("model", level=AtomicLevel.ADVANCED, required=False),
        ],
        outputList=[atomicMg.param("recruit_rating", types="Str")],
    )
    def rating_resume(
        self,
        job_name,
        resume_input_type: InputType = InputType.TEXT,
        resume_file_path: str = "",
        resume_content: str = "",
        job_description: str = "",
        rating_system: RatingSystemTypes = RatingSystemTypes.DEFAULT,
        rating_dimensions: str = "",
        model: str = "",
    ):
        """Score a resume against a job description.

        Automatically extracts text if file mode selected.

        Args:
            job_name: Job title
            resume_input_type: Whether resume is provided as text or file
            resume_file_path: Path to resume file
            resume_content: Raw resume text if provided directly
            job_description: JD text
            rating_system: default or custom rating style
            rating_dimensions: custom rating dimensions when custom mode
            model: model id
        Returns:
            Rating result reply string.
        """
        if resume_input_type == InputType.FILE:
            resume_content = FileExtractor(resume_file_path).extract_text()

        if rating_system == RatingSystemTypes.DEFAULT:
            params = {
                "job_name": job_name,
                "job_description": job_description,
                "resume": resume_content,
            }
            prompt_type = "recruit_rating_default"

        else:
            # rating_system == RatingSystemTypes.CUSTOM:
            params = {
                "job_name": job_name,
                "rating_dimensions": rating_dimensions,
                "resume": resume_content,
            }
            prompt_type = "recruit_rating_custom"

        if model:
            reply = chat_prompt(prompt_type=prompt_type, params=params, model=model)
        else:
            reply = chat_prompt(prompt_type=prompt_type, params=params)

        logger.info("RecruitAI rating_resume: {}".format(reply))
        return reply