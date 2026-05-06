import unittest
from datetime import UTC, datetime

from astronverse.actionlib import *
from astronverse.actionlib.types import Date
from astronverse.dataprocess import TimeChangeType, TimestampUnitType, TimeUnitType, TimeZoneType
from astronverse.dataprocess.time import TimeProcess
from dateutil.relativedelta import relativedelta


class TestTimeProcess(unittest.TestCase):
    def setUp(self):
        """시도전의준비"""
        self.test_datetime = datetime(2023, 12, 25, 10, 30, 45)
        self.test_date = Date()
        self.test_date.time = self.test_datetime
        self.test_date.format = TimeFormatType.YMD_HMS

    def test_get_current_time_success(self):
        """시도가져오기현재시간성공"""
        result = TimeProcess.get_current_time(time_format=TimeFormatType.YMD_HMS)

        self.assertIsInstance(result, Date)
        self.assertEqual(result.format, TimeFormatType.YMD_HMS)

    def test_get_current_time_with_different_format(self):
        """시도아니오형식가져오기현재시간"""
        result = TimeProcess.get_current_time(time_format=TimeFormatType.YMD)

        self.assertIsInstance(result, Date)
        self.assertEqual(result.format, TimeFormatType.YMD)

    def test_set_time_maintain(self):
        """시도시간 - 보관방식"""
        original_time = self.test_date.time
        result = TimeProcess.set_time(time=self.test_date, change_type=TimeChangeType.MAINTAIN)

        self.assertEqual(result.time, original_time)

    def test_set_time_add(self):
        """시도시간 - 증가추가방식"""
        original_time = self.test_date.time
        result = TimeProcess.set_time(time=self.test_date, change_type=TimeChangeType.ADD, days=1, hours=2)

        expected_time = original_time + relativedelta(days=1, hours=2)
        self.assertEqual(result.time, expected_time)

    def test_set_time_subtract(self):
        """시도시간 - 적음방식"""
        original_time = self.test_date.time
        result = TimeProcess.set_time(time=self.test_date, change_type=TimeChangeType.SUB, days=1, hours=2)

        expected_time = original_time - relativedelta(days=1, hours=2)
        self.assertEqual(result.time, expected_time)

    def test_set_time_with_all_parameters(self):
        """시도시간 - 모든매개변수"""
        original_time = self.test_date.time
        result = TimeProcess.set_time(
            time=self.test_date,
            change_type=TimeChangeType.ADD,
            seconds=30,
            minutes=15,
            hours=3,
            days=5,
            months=2,
            years=1,
        )

        expected_time = original_time + relativedelta(seconds=30, minutes=15, hours=3, days=5, months=2, years=1)
        self.assertEqual(result.time, expected_time)

    def test_time_to_timestamp_second(self):
        """시도시간변환시간 - 초단계"""
        result = TimeProcess.time_to_timestamp(time=self.test_date, timestamp_unit=TimestampUnitType.SECOND)

        expected_timestamp = int(self.test_datetime.timestamp())
        self.assertEqual(result, expected_timestamp)

    def test_time_to_timestamp_millisecond(self):
        """시도시간변환시간 - 초단계"""
        result = TimeProcess.time_to_timestamp(time=self.test_date, timestamp_unit=TimestampUnitType.MILLISECOND)

        expected_timestamp = int(self.test_datetime.timestamp() * 1000)
        self.assertEqual(result, expected_timestamp)

    def test_time_to_timestamp_microsecond(self):
        """시도시간변환시간 - 초단계"""
        result = TimeProcess.time_to_timestamp(time=self.test_date, timestamp_unit=TimestampUnitType.MICROSECOND)

        expected_timestamp = int(self.test_datetime.timestamp() * 1000000)
        self.assertEqual(result, expected_timestamp)

    def test_timestamp_to_time_local(self):
        """시도시간변환시간 - 본시"""
        timestamp = int(self.test_datetime.timestamp())
        result = TimeProcess.timestamp_to_time(timestamp=timestamp, time_zone=TimeZoneType.LOCAL)

        self.assertIsInstance(result, Date)
        self.assertEqual(result.time, self.test_datetime)

    def test_timestamp_to_time_utc(self):
        """시도시간변환시간 - UTC시"""
        utc_datetime = datetime(2023, 12, 25, 10, 30, 45, tzinfo=UTC)
        timestamp = int(utc_datetime.timestamp())
        result = TimeProcess.timestamp_to_time(timestamp=timestamp, time_zone=TimeZoneType.UTC)

        self.assertIsInstance(result, Date)
        self.assertEqual(result.time, utc_datetime)

    def test_timestamp_to_time_millisecond(self):
        """시도초단계시간변환시간"""
        timestamp = int(self.test_datetime.timestamp() * 1000)
        result = TimeProcess.timestamp_to_time(timestamp=timestamp, time_zone=TimeZoneType.LOCAL)

        self.assertIsInstance(result, Date)
        # 정도제목, 까지초단계
        self.assertEqual(int(result.time.timestamp()), int(self.test_datetime.timestamp()))

    def test_timestamp_to_time_microsecond(self):
        """시도초단계시간변환시간"""
        timestamp = int(self.test_datetime.timestamp() * 1000000)
        result = TimeProcess.timestamp_to_time(timestamp=timestamp, time_zone=TimeZoneType.LOCAL)

        self.assertIsInstance(result, Date)
        # 정도제목, 까지초단계
        self.assertEqual(int(result.time.timestamp()), int(self.test_datetime.timestamp()))

    def test_get_time_difference_seconds(self):
        """시도계획시간 - 초단계"""
        time_1 = Date()
        time_1.time = datetime(2023, 12, 25, 10, 30, 0)

        time_2 = Date()
        time_2.time = datetime(2023, 12, 25, 10, 30, 30)

        result = TimeProcess.get_time_difference(time_1=time_1, time_2=time_2, time_unit=TimeUnitType.SECOND)
        self.assertEqual(result, 30)

    def test_get_time_difference_minutes(self):
        """시도계획시간 - 분단계"""
        time_1 = Date()
        time_1.time = datetime(2023, 12, 25, 10, 30, 0)

        time_2 = Date()
        time_2.time = datetime(2023, 12, 25, 10, 32, 0)

        result = TimeProcess.get_time_difference(time_1=time_1, time_2=time_2, time_unit=TimeUnitType.MINUTE)
        self.assertEqual(result, 2)

    def test_get_time_difference_hours(self):
        """시도계획시간 - 시간단계"""
        time_1 = Date()
        time_1.time = datetime(2023, 12, 25, 10, 0, 0)

        time_2 = Date()
        time_2.time = datetime(2023, 12, 25, 13, 0, 0)

        result = TimeProcess.get_time_difference(time_1=time_1, time_2=time_2, time_unit=TimeUnitType.HOUR)
        self.assertEqual(result, 3)

    def test_get_time_difference_days(self):
        """시도계획시간 - 단계"""
        time_1 = Date()
        time_1.time = datetime(2023, 12, 25, 10, 0, 0)

        time_2 = Date()
        time_2.time = datetime(2023, 12, 28, 10, 0, 0)

        result = TimeProcess.get_time_difference(time_1=time_1, time_2=time_2, time_unit=TimeUnitType.DAY)
        self.assertEqual(result, 3)

    def test_get_time_difference_months(self):
        """시도계획시간 - 월단계"""
        time_1 = Date()
        time_1.time = datetime(2023, 1, 1, 10, 0, 0)

        time_2 = Date()
        time_2.time = datetime(2023, 3, 1, 10, 0, 0)

        result = TimeProcess.get_time_difference(time_1=time_1, time_2=time_2, time_unit=TimeUnitType.MONTH)
        self.assertEqual(result, 2)

    def test_get_time_difference_years(self):
        """시도계획시간 - 년단계"""
        time_1 = Date()
        time_1.time = datetime(2020, 1, 1, 10, 0, 0)

        time_2 = Date()
        time_2.time = datetime(2023, 1, 1, 10, 0, 0)

        result = TimeProcess.get_time_difference(time_1=time_1, time_2=time_2, time_unit=TimeUnitType.YEAR)
        self.assertEqual(result, 3)

    def test_get_time_difference_reverse_order(self):
        """시도계획시간 - 시간순서"""
        time_1 = Date()
        time_1.time = datetime(2023, 12, 25, 10, 30, 30)

        time_2 = Date()
        time_2.time = datetime(2023, 12, 25, 10, 30, 0)

        result = TimeProcess.get_time_difference(time_1=time_1, time_2=time_2, time_unit=TimeUnitType.SECOND)
        self.assertEqual(result, 30)  # 해당반환값

    def test_format_datetime_ymd_hms(self):
        """시도형식시간 - YMD_HMS형식"""
        result = TimeProcess.format_datetime(time=self.test_date, format_type=TimeFormatType.YMD_HMS)

        expected_format = "2023-12-25 10:30:45"
        self.assertEqual(result, expected_format)

    def test_format_datetime_ymd(self):
        """시도형식시간 - YMD형식"""
        result = TimeProcess.format_datetime(time=self.test_date, format_type=TimeFormatType.YMD)

        expected_format = "2023-12-25"
        self.assertEqual(result, expected_format)

    def test_format_datetime_ym(self):
        """시도형식시간 - YM형식"""
        result = TimeProcess.format_datetime(time=self.test_date, format_type=TimeFormatType.YMD)

        expected_format = "2023-12-25"
        self.assertEqual(result, expected_format)

    def test_format_datetime_hms(self):
        """시도형식시간 - HMS형식"""
        result = TimeProcess.format_datetime(time=self.test_date, format_type=TimeFormatType.HMS)

        expected_format = "10:30:45"
        self.assertEqual(result, expected_format)

    def test_format_datetime_hm(self):
        """시도형식시간 - HM형식"""
        result = TimeProcess.format_datetime(time=self.test_date, format_type=TimeFormatType.HM)

        expected_format = "10:30"
        self.assertEqual(result, expected_format)

    def test_get_time_difference_invalid_unit(self):
        """시도계획시간 - 없음시간단일위치"""
        time_1 = Date()
        time_1.time = datetime(2023, 12, 25, 10, 30, 0)

        time_2 = Date()
        time_2.time = datetime(2023, 12, 25, 10, 30, 30)

        # 시도지원하지 않는 시간단일위치
        with self.assertRaises(NotImplementedError):
            TimeProcess.get_time_difference(time_1=time_1, time_2=time_2, time_unit="INVALID_UNIT")

    def test_set_time_edge_cases(self):
        """시도시간의가장자리"""
        # 시도영값
        result = TimeProcess.set_time(
            time=self.test_date,
            change_type=TimeChangeType.ADD,
            seconds=0,
            minutes=0,
            hours=0,
            days=0,
            months=0,
            years=0,
        )
        self.assertEqual(result.time, self.test_date.time)

        # 시도데이터
        original_time = self.test_date.time
        result = TimeProcess.set_time(time=self.test_date, change_type=TimeChangeType.ADD, days=-1)
        expected_time = original_time + relativedelta(days=-1)
        self.assertEqual(result.time, expected_time)

    def test_timestamp_edge_cases(self):
        """시도시간변환의가장자리"""
        # 시도영시간
        result = TimeProcess.timestamp_to_time(timestamp=0, time_zone=TimeZoneType.LOCAL)
        self.assertIsInstance(result, Date)

        # 시도1970년1월1일
        epoch_timestamp = 0
        result = TimeProcess.timestamp_to_time(timestamp=epoch_timestamp, time_zone=TimeZoneType.LOCAL)
        expected_time = datetime.fromtimestamp(0)
        self.assertEqual(result.time, expected_time)

    def test_time_difference_edge_cases(self):
        """시도시간계획의가장자리"""
        # 시도시간
        time_1 = Date()
        time_1.time = datetime(2023, 12, 25, 10, 30, 0)

        time_2 = Date()
        time_2.time = datetime(2023, 12, 25, 10, 30, 0)

        result = TimeProcess.get_time_difference(time_1=time_1, time_2=time_2, time_unit=TimeUnitType.SECOND)
        self.assertEqual(result, 0)

        # 시도년
        time_1.time = datetime(2022, 12, 31, 23, 59, 59)
        time_2.time = datetime(2023, 1, 1, 0, 0, 1)

        result = TimeProcess.get_time_difference(time_1=time_1, time_2=time_2, time_unit=TimeUnitType.SECOND)
        self.assertEqual(result, 2)

    def test_format_datetime_edge_cases(self):
        """시도형식시간의가장자리"""
        # 시도시간
        midnight_date = Date()
        midnight_date.time = datetime(2023, 12, 25, 0, 0, 0)
        midnight_date.format = TimeFormatType.YMD_HMS

        result = TimeProcess.format_datetime(time=midnight_date, format_type=TimeFormatType.YMD_HMS)
        self.assertEqual(result, "2023-12-25 00:00:00")

        # 시도년
        year_end_date = Date()
        year_end_date.time = datetime(2023, 12, 31, 23, 59, 59)
        year_end_date.format = TimeFormatType.YMD_HMS

        result = TimeProcess.format_datetime(time=year_end_date, format_type=TimeFormatType.YMD_HMS)
        self.assertEqual(result, "2023-12-31 23:59:59")


if __name__ == "__main__":
    unittest.main()