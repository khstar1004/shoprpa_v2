"""로완료내용, 가능으로삭제"""

from astronverse.actionlib.report import IReport, report
from astronverse.workflowlib.report import print

logger: IReport = report

print = print


class Helper:
    def __init__(self, **kwargs):
        self.kwargs = kwargs

    def params(self):
        return self.kwargs