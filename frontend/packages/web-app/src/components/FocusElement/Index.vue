<script lang="ts" setup>
/**
 * This component is used to reset the focus of the input element.
 * 개컴포넌트사용재입력요소의.
 * : 의요소브라우저저장, Chromium 의bug, 예다중의, 가능완료메모리, 으로필요재.
 * 수정컴포넌트출력일개방법법외부부서호출, 방법법이름예resetFocus
 */

// const focusInput = ref(null)

// function resetFocus() {
//   if (focusInput.value) {
//     focusInput.value.focus()
//     focusInput.value.blur()
//   }
// }
</script>

<template>
  <div style="position: fixed; top: -100px;">
    <input ref="focusInput" type="text" style="visibility: hidden; width: 0px; height: 0px; ">
  </div>
</template>
