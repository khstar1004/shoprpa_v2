import dayjs from 'dayjs'

import { WEEK_MAP_INDAYJS } from '@/views/Home/config/task'

/**
 * 가져오기매일 h 시 m 분 s 초 의아래일있음실행시간
 */
export function getEverydayExecuteTime(h: number, m: number, s: number = 0) {
  const t = dayjs().hour(h).minute(m).second(s).format('YYYY-MM-DD HH:mm:ss')
  if (dayjs(t).isBefore(dayjs())) {
    return dayjs(t).add(1, 'day').format('YYYY-MM-DD HH:mm:ss')
  }
  return t
}

/**
 * 가져오기매 h 개시간, m 분, s 초 의아래일있음실행시간
 */
export function getEveryHoursExecuteTime(h: number, m: number, s: number = 0) {
  return dayjs().add(h, 'hour').add(m, 'minute').add(s, 'second').format('YYYY-MM-DD HH:mm:ss')
}

/**
 * 매 m 분,  s초의아래일있음실행시간
 */
export function getEveryMinutesExecuteTime(m: number, s: number = 0) {
  return dayjs().add(m, 'minute').add(s, 'second').format('YYYY-MM-DD HH:mm:ss')
}

/**
 * 매주의 weeks 의 h 시 m 분 s 초 의아래일있음실행시간
 */
export function getEveryWeeksExecuteTime(weeks: number[], h: number, m: number, s: number = 0) {
  if (weeks.length === 0) {
    return ''
  }
  const now = dayjs()
  let nextTime = null
  const sortedWeeks = weeks.map(week => WEEK_MAP_INDAYJS[week]).sort((a, b) => a - b) // 변환로 dayjs 의데이터
  for (const week of sortedWeeks) {
    const t = now.day(week).hour(h).minute(m).second(s)
    if (t.isAfter(now)) {
      nextTime = t
      break
    }
  }
  if (!nextTime) {
    nextTime = now.add(1, 'week').day(sortedWeeks[0]).hour(h).minute(m).second(s)
  }
  return nextTime.format('YYYY-MM-DD HH:mm:ss')
}

/**
 * 에서 months 월 매주의 weeks 의 h 시 m 분 s 초 의아래일있음실행시간
 */
export function getEveryMonthsExecuteTime(months: number, weeks: number[], h: number, m: number, s: number = 0) {
  if (weeks.length === 0 || months < 1 || months > 12)
    return ''
  const sortedWeeks = weeks.map(week => WEEK_MAP_INDAYJS[week]).sort((a, b) => a - b)
  const currentMonth = dayjs().month() + 1
  if (currentMonth > months) {
    const date = findFirstDayOfMonthByWeek(months, sortedWeeks).add(1, 'year')
    return date.hour(h).minute(m).second(s).format('YYYY-MM-DD HH:mm:ss')
  }
  else if (currentMonth === months) {
    return getEveryWeeksExecuteTime(weeks, h, m, s)
  }
  else {
    const date = findFirstDayOfMonthByWeek(months, sortedWeeks)
    return date.hour(h).minute(m).second(s).format('YYYY-MM-DD HH:mm:ss')
  }
}

function findFirstDayOfMonthByWeek(month: number, weekDays: number[]): dayjs.Dayjs {
  let date = dayjs().month(month - 1).date(1)
  while (!weekDays.includes(date.day())) {
    date = date.add(1, 'day')
  }
  return date
}

export function getDurationText(seconds: number) {
  let durationText = ''
  const hour = Math.floor(seconds / 3600)
  const minute = Math.floor((seconds - hour * 3600) / 60)
  const second = Math.floor(seconds - hour * 3600 - minute * 60)
  durationText = `${hour > 0 ? `${hour}h` : ''}${minute > 0 ? `${minute}m` : ''}${second}s`
  return durationText
}
