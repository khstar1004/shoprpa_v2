import type { BrowserWindow } from 'electron'
import { app, dialog, Menu, Tray } from 'electron'

import { APP_ICON_PATH } from './config'

let tray: Tray
let scheduling_mode = false

export function createTray(win: BrowserWindow) {
  const contextMenu = Menu.buildFromTemplate([
    {
      label: '열기',
      click: () => {
        win.show()
        if (win.isMinimized())
          win.restore()
        win.focus()
      },
    },
    {
      label: '숨기기',
      click: () => {
        win.hide()
      },
    },
    {
      label: '종료',
      click: () => {
        app.quit()
      },
    },
  ])
  tray = new Tray(APP_ICON_PATH)
  tray.setContextMenu(contextMenu)
  tray.on('click', () => {
    if (!win)
      return
    if (scheduling_mode)
      return
    if (!win.isVisible()) {
      win.show()
      win.focus()
    }
    else if (win.isMinimized()) {
      win.restore()
      win.focus()
    }
    else {
      win.minimize()
    }
  })
  tray.setToolTip(app.name)
}

export function changeTray(win: BrowserWindow, mode: 'scheduling' | 'normal', status?: 'idle' | 'busy') {
  let options: Electron.MenuItemConstructorOptions[] = []
  scheduling_mode = mode === 'scheduling'
  if (mode === 'normal') {
    options = [
      {
        label: '열기',
        click: () => {
          win.show()
          if (win.isMinimized())
            win.restore()
          win.focus()
        },
      },
      {
        label: '숨기기',
        click: () => {
          win.hide()
        },
      },
      {
        label: '종료',
        click: () => {
          app.quit()
        },
      },
    ]
  }
  if (mode === 'scheduling') {
    options = [
      {
        label: status === 'busy' ? '스케줄링 모드(실행 중)' : '스케줄링 모드(대기 중)',
        click: () => {},
      },
    ]
    if (status === 'busy') {
      options.push({
        label: '현재 작업 중지',
        click: () => {
          dialog.showMessageBox(win, {
            type: 'warning',
            title: '작업 중지',
            message: '현재 실행 중인 작업을 중지하시겠습니까?',
            buttons: ['중지', '취소'],
            defaultId: 0,
            cancelId: 1,
            noLink: true,
          }).then((result) => {
            if (result.response === 0) {
              changeTray(win, 'scheduling', 'idle')
              win.webContents.send('stop_task', {})
            }
          })
        },
      })
    }
    options = options.concat([
      {
        label: '스케줄링 모드 종료',
        click: () => {
          dialog.showMessageBox(win, {
            type: 'warning',
            title: '스케줄링 모드 종료',
            message: '종료하면 이 PC에서 예정된 스케줄 작업을 더 이상 실행하지 않습니다. 계속하시겠습니까?',
            buttons: ['종료', '취소'],
            defaultId: 0,
            cancelId: 1,
            noLink: true,
          }).then((result) => {
            if (result.response === 0) {
              changeTray(win, 'normal')// 수정변수메뉴
              win.webContents.send('exit_scheduling_mode', {}) // 출력스케줄링방식알림
              win.show() // 
            }
          })
        },
      },
      {
        label: 'shoprpa 종료',
        click: () => {
          app.quit()
        },
      },
    ])
  }
  const contextMenu = Menu.buildFromTemplate(options)
  tray.setContextMenu(contextMenu)
}
