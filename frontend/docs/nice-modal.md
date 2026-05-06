# Vue Nice Modal

**[@ebay/nice-modal-react](https://github.com/eBay/nice-modal-react) Vue 버전.**

vue-nice-modal 예일개도구라이브러리,가능으로를 Vue.js 의 modal 컴포넌트변환로항목 Promise 의 API.

## 사용방법방식

### 1. Provider 패키지항목사용

```html
<!-- App.vue -->
<template>
  <NiceModalProvider>
    <router-view />
  </NiceModalProvider>
</template>

<script setup>
  import { NiceModal } from '@rpa/components'
  const NiceModalProvider = NiceModal.Provider
</script>
```

### 2. 생성항목항목컴포넌트

```html
<!-- my-modal.vue -->
<template>
  <van-dialog
    show-cancel-button
    :value="modal.visible"
    :close-on-click-overlay="false"
    :title="title"
    :message="content"
    @closed="modal.remove"
    @confirm="handleConfirm"
    @cancel="handleCancel"
  />
</template>

<script setup>
  import { NiceModal } from '@rpa/components'

  const modal = NiceModal.useModal()
  defineProps(['title', 'content'])

  const handleCancel = () => {
    modal.reject('cancel')
    modal.hide()
  }

  const handleConfirm = () => {
    modal.resolve('confirm')
    modal.hide()
  }
</script>
```

> 가능및작업항목 UI 라이브러리매칭합치기사용, 예 antdv

```html
<!-- my-drawer.vue -->
<template>
  <a-drawer v-bind="NiceModal.antdDrawer(modal)">xxx</a-drawer>
</template>
```

```html
<!-- my-modal.vue -->
<a-modal v-bind="NiceModal.antdModal(modal)">xxx</a-modal>
```

> 항목후사용 NiceModal.create 생성항목항목높음항목컴포넌트

```js
// my-modal.js
import { NiceModal } from '@rpa/components'

import _MyModal from './my-modal.vue'

export const MyModal = NiceModal.create(_MyModal)
```

### 3. 사용항목항목

#### 3.1 항목사용법 - 직선연결사용컴포넌트

```js
async function showModal() {
  try {
    const res = await NiceModal.show(MyModal, {
      title: '제목',
      content: '내용',
    })
    console.log('결과:', res)
  }
  catch (error) {
    console.log('가져오기항목:', error)
  }
}
```

#### 3.2 항목방식사용법 - 통신경과 ID 항목사용완료항목의항목항목

> 가능항목항목항목위아래문서

```html
<template>
  <MyModal id="my-modal" />
</template>

<script setup>
  const showModal = async () => {
    try {
      const res = await NiceModal.show('my-modal', {
        title: '제목',
        content: '내용',
      })
      console.log('결과:', res)
    } catch (error) {
      console.log('가져오기항목:', error)
    }
  }
</script>
```

#### 3.3 Hook 사용법 - 사용 useModal 그룹합치기방식 API

```js
const modal = NiceModal.useModal(MyModal)

async function showModal() {
  try {
    const res = await modal.show({
      title: '제목',
      content: '내용',
    })
    console.log('결과:', res)
  }
  catch (error) {
    console.log('가져오기항목:', error)
  }
}
```

#### 3.4 회원가입사용법 - 통신경과회원가입후사용 ID 호출

```js
// 항목회원가입항목항목
NiceModal.register('register-modal', MyModal)

async function showModal() {
  try {
    const res = await NiceModal.show('register-modal', {
      title: '제목',
      content: '내용',
    })
    console.log('결과:', res)
  }
  catch (error) {
    console.log('가져오기항목:', error)
  }
}
```

## API 매개항목

### 컴포넌트

#### `NiceModal.Provider`

항목항목내용기기컴포넌트, 필요패키지항목에서항목사용항목외부항목.

#### `NiceModal.create(Component)`

높음항목컴포넌트, 사용항목생성항목항목컴포넌트.

### 방법법

#### `show(modalId, args?)`

항목항목항목, 지원항목입력매개변수.

- `modalId`: 항목항목 ID 또는컴포넌트
- `args`: 항목항목항목의매개변수
- 반환: Promise

#### `hide(modalId)`

항목항목항목.

- `modalId`: 항목항목 ID 또는컴포넌트
- 반환: Promise

#### `remove(modalId)`

에서 DOM 중제거항목항목.

- `modalId`: 항목항목 ID 또는컴포넌트

#### `register(id, component, props?)`

회원가입항목항목컴포넌트.

- `id`: 항목항목 ID
- `component`: 항목항목컴포넌트
- `props`: 항목 props

#### `unregister(id)`

비고판매항목항목컴포넌트.

- `id`: 항목항목 ID

### Hook

#### `useModal(modal?, args?)`

반환값:

- `id`: 항목항목 ID
- `args`: 항목항목매개변수
- `visible`: 가능항목상태
- `show(args?)`: 항목항목항목
- `hide()`: 항목항목항목
- `remove()`: 제거항목항목
- `resolve(value)`: 파싱항목항목 Promise
- `reject(reason)`: 항목항목항목 Promise
- `resolveHide(value)`: 파싱항목 Promise