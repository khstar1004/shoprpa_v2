<script setup lang="ts">
defineProps<{
  illustration?: Illustration
}>()

export type Illustration = 'robot1' | 'robot2'
</script>

<template>
  <div class="bg-illustration">
    <div class="workflow-card card-main">
      <span class="node node-a" />
      <span class="node node-b" />
      <span class="node node-c" />
      <span class="line line-a" />
      <span class="line line-b" />
      <span class="line line-c" />
    </div>
    <div class="workflow-card card-side">
      <span class="bar bar-a" />
      <span class="bar bar-b" />
      <span class="bar bar-c" />
    </div>
  </div>
</template>

<style scoped>
.bg-illustration {
  position: absolute;
  top: 0;
  right: 0;
  width: 100%;
  height: 360px;
  overflow: hidden;
  pointer-events: none;
  background:
    radial-gradient(circle at 84% 16%, rgba(45, 212, 191, 0.22), transparent 28%),
    radial-gradient(circle at 72% 4%, rgba(245, 158, 11, 0.12), transparent 18%);
}

.workflow-card {
  position: absolute;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background:
    linear-gradient(rgba(15, 23, 42, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(15, 23, 42, 0.045) 1px, transparent 1px), rgba(255, 255, 255, 0.64);
  background-size:
    28px 28px,
    28px 28px,
    auto;
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.1);
  backdrop-filter: blur(20px);
}

.card-main {
  top: 48px;
  right: 44px;
  width: 360px;
  height: 156px;
  border-radius: 28px;
}

.card-side {
  top: 76px;
  right: 318px;
  width: 170px;
  height: 112px;
  border-radius: 22px;
  transform: rotate(-4deg);
}

.node,
.line,
.bar {
  position: absolute;
  display: block;
}

.node {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background: #fff;
  box-shadow: inset 0 0 0 1px rgba(15, 23, 42, 0.08);
}

.node-a {
  left: 54px;
  top: 56px;
  background: linear-gradient(135deg, #0ea5a8, #2dd4bf);
}

.node-b {
  left: 164px;
  top: 34px;
}

.node-c {
  right: 52px;
  top: 68px;
  background: linear-gradient(135deg, #f59e0b, #facc15);
}

.line {
  height: 2px;
  background: linear-gradient(90deg, rgba(14, 165, 168, 0.12), rgba(14, 165, 168, 0.78));
  transform-origin: left center;
}

.line-a {
  width: 112px;
  left: 94px;
  top: 76px;
  transform: rotate(-13deg);
}

.line-b {
  width: 112px;
  left: 204px;
  top: 56px;
  transform: rotate(21deg);
}

.line-c {
  width: 238px;
  left: 58px;
  bottom: 26px;
}

.bar {
  left: 26px;
  height: 10px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.14);
}

.bar-a {
  top: 30px;
  width: 98px;
}

.bar-b {
  top: 54px;
  width: 120px;
}

.bar-c {
  top: 78px;
  width: 72px;
  background: rgba(14, 165, 168, 0.42);
}
</style>
