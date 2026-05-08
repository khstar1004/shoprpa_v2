<script setup lang="ts">
import { Carousel } from 'ant-design-vue'
import type { CarouselRef } from 'ant-design-vue/es/carousel'
import { ref, useTemplateRef } from 'vue'

// import { useTheme } from '@rpa/components'
import { illustrationList } from '@/constants/launch'

// const { colorTheme } = useTheme()

const carouselRef = useTemplateRef<CarouselRef>('carouselRef')
const current = ref(0)

// 에서 illustrationList 중기기일그룹
const randomIllustrationGroup
  = illustrationList[Math.floor(Math.random() * illustrationList.length)]

function onChange(idx: number) {
  current.value = idx
}

function onSwitch(idx: number) {
  current.value = idx
  carouselRef.value?.goTo(idx)
}
</script>

<template>
  <div class="launch-carousel">
    <Carousel
      ref="carouselRef"
      :after-change="onChange"
      autoplay
      :dots="false"
      effect="fade"
      class="w-full"
    >
      <div
        v-for="(item, index) in randomIllustrationGroup"
        :key="index"
        class="launch-panel"
      >
        <div class="panel-grid">
          <span class="node node-primary" />
          <span class="node node-muted" />
          <span class="node node-amber" />
          <span class="flow flow-one" />
          <span class="flow flow-two" />
          <span class="flow flow-three" />
        </div>
        <div class="panel-copy">
          <strong>{{ item.text }}</strong>
          <span>{{ item.desc }}</span>
        </div>
      </div>
    </Carousel>

    <div class="mt-4 flex items-center justify-center gap-2">
      <span
        v-for="(_, index) in randomIllustrationGroup"
        :key="index"
        class="w-2 h-2 rounded cursor-pointer bg-[rgba(255,255,255,0.28)]"
        :class="{ '!bg-primary': index === current }"
        @click="onSwitch(index)"
      />
    </div>

    <div class="mt-5 text-base leading-[22px] font-semibold text-white">
      {{ randomIllustrationGroup?.[current]?.text }}
    </div>
    <div class="mt-[6px] max-w-[360px] text-center text-sm leading-[22px] text-white/70">
      {{ randomIllustrationGroup?.[current]?.desc }}
    </div>
    <slot name="footer" />
  </div>
</template>

<style lang="scss" scoped>
.launch-carousel {
  width: min(420px, calc(100vw - 48px));
  display: flex;
  flex-direction: column;
  align-items: center;
}

.launch-panel {
  height: 238px;
  padding: 22px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 8px;
  background: linear-gradient(145deg, rgba(22, 30, 40, 0.94), rgba(9, 14, 20, 0.94));
  box-shadow: 0 24px 72px rgba(0, 0, 0, 0.32);
}

.panel-grid {
  position: relative;
  height: 128px;
  border-radius: 8px;
  background:
    linear-gradient(rgba(255, 255, 255, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.045) 1px, transparent 1px);
  background-size: 34px 34px;
}

.node,
.flow {
  position: absolute;
  display: block;
}

.node {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.16);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.14);
}

.node-primary {
  left: 54px;
  top: 44px;
  background: linear-gradient(135deg, #0ea5a8, #2dd4bf);
}

.node-muted {
  left: 168px;
  top: 22px;
}

.node-amber {
  right: 50px;
  top: 58px;
  background: linear-gradient(135deg, #f59e0b, #facc15);
}

.flow {
  height: 2px;
  background: linear-gradient(90deg, rgba(45, 212, 191, 0.05), rgba(45, 212, 191, 0.85));
  transform-origin: left center;
}

.flow-one {
  width: 120px;
  left: 94px;
  top: 64px;
  transform: rotate(-13deg);
}

.flow-two {
  width: 112px;
  left: 206px;
  top: 44px;
  transform: rotate(23deg);
}

.flow-three {
  width: 210px;
  left: 64px;
  bottom: 22px;
}

.panel-copy {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: white;

  strong {
    font-size: 16px;
    line-height: 22px;
  }

  span {
    font-size: 13px;
    line-height: 20px;
    color: rgba(255, 255, 255, 0.66);
  }
}
</style>
