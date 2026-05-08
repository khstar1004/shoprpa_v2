<script setup lang="ts">
import { Auth } from '@rpa/components/auth'

const ENV = import.meta.env

function loginSuccess() {
  location.replace(`/admin`)
}
</script>

<template>
  <Auth.LoginPage platform="admin" :auth-type="ENV.VITE_AUTH_TYPE" :edition="ENV.VITE_EDITION" @finish="loginSuccess" />
</template>
