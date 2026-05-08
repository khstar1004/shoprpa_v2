<script setup lang="ts">
const props = defineProps<{
  title: string
  subTitle: string
  actionText?: string
}>()

const emit = defineEmits<{
  (e: 'action'): void
}>()
</script>

<template>
  <div
    class="banner w-full h-[244px] short:h-[190px] flex justify-between items-center"
  >
    <div class="flex flex-col items-start justify-center pl-6">
      <div class="banner-title">
        {{ props.title }}
      </div>

      <div class="banner-sub-title text-[rgba(0,0,0,0.65)] dark:text-white">
        {{ props.subTitle }}
      </div>

      <a-button
        v-if="actionText"
        type="primary"
        size="large"
        class="banner-button"
        @click="emit('action')"
      >
        {{ actionText }}
      </a-button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.banner {
  position: relative;
  overflow: hidden;
  border-radius: 0 0 24px 0;

  &::after {
    content: '';
    position: absolute;
    right: 44px;
    top: 44px;
    width: 320px;
    height: 142px;
    border-radius: 28px;
    background:
      linear-gradient(rgba(15, 23, 42, 0.06) 1px, transparent 1px),
      linear-gradient(90deg, rgba(15, 23, 42, 0.06) 1px, transparent 1px),
      linear-gradient(135deg, rgba(255, 255, 255, 0.72), rgba(255, 255, 255, 0.28));
    background-size:
      28px 28px,
      28px 28px,
      auto;
    border: 1px solid rgba(15, 23, 42, 0.08);
    box-shadow: 0 22px 54px rgba(15, 23, 42, 0.08);
  }

  .banner-title {
    position: relative;
    z-index: 1;
    font-size: 34px;
    font-weight: 760;
    line-height: 45px;
    margin-bottom: 8px;
    color: #0f172a;
    letter-spacing: 0;
  }

  .banner-sub-title {
    position: relative;
    z-index: 1;
    font-size: 15px;
    line-height: 24px;
    max-width: 560px;
  }

  .banner-button {
    position: relative;
    z-index: 1;
    margin-top: 24px;
    background: linear-gradient(135deg, #0f766e 0%, #0ea5a8 100%);
    box-shadow: 0 12px 28px rgba(14, 165, 168, 0.24);
  }
}
</style>
