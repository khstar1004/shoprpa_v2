<script lang="ts" setup>
import { BorderOuterOutlined, CloseOutlined, RedoOutlined } from '@ant-design/icons-vue'
import { NiceModal } from '@rpa/components'
import { Image } from 'ant-design-vue'
import { throttle } from 'lodash-es'
import type { Ref } from 'vue'
import { computed, h, onUnmounted, ref, watch } from 'vue'

import { isBase64Image, trimBase64Header } from '@/utils/common'

import { getImageURL } from '@/api/http/env'
import { useCvPickStore } from '@/stores/useCvPickStore'
import { useCvStore } from '@/stores/useCvStore'
import AtomSlider from '@/views/Arrange/components/atomForm/AtomSlider.vue'
import { useCvPick } from '@/views/Arrange/components/cvPick/hooks/useCvPick.ts'
import { useCvPickForm } from '@/views/Arrange/components/cvPick/hooks/useCvPickForm.ts'

const { entry, groupId } = defineProps({
  entry: { // 선택입력입구, 'group'-분그룹증가추가 저장그리고계속 atomFormBtn-원자 기능매칭테이블단일선택버튼, 저장 edit-입력입구, 저장, 버튼문서문자표시로"저장"
    type: String,
    default: 'group',
  },
  groupId: { // 분그룹id
    type: String,
    default: '',
  },
})

const cvPickStore = useCvPickStore()
const cvStore = useCvStore()
const useCv = useCvPick()
const modal = NiceModal.useModal()

function handleCancel() {
  // 다시 선택경과중아니오허용닫기팝업
  if (cvPickStore.isPicking)
    return

  modal.hide()
  cvStore.resetCurrentItem()
}

/** 테이블단일데이터 start */
const { formOption, formRef, rules, setFormOption, validateForm } = useCvPickForm()
/** 테이블단일데이터 end */

const saveLoading = ref(false)
function setSaveLoading(flag: boolean) {
  saveLoading.value = flag
}

function getElementData(isSave: boolean) {
  const eleData = JSON.parse(cvStore.currentCvItem.elementData)

  const selfImg = cvStore.currentCvItem.imageUrl
  const parentImg = cvStore.currentCvItem.parentImageUrl
  // 저장시만약예base64이미지, 이면아니오저장
  const img = isSave
    ? {
        self: isBase64Image(selfImg) ? '' : selfImg,
        parent: isBase64Image(parentImg) ? '' : parentImg,
      }
    : {
        self: isBase64Image(selfImg) ? trimBase64Header(selfImg) : selfImg,
        parent: isBase64Image(parentImg) ? trimBase64Header(parentImg) : parentImg,
      }

  const elementData = {
    ...eleData, // 요소데이터
    img,
  }
  return elementData
}

// 저장요소
const save = throttle((isContinue = false) => {
  validateForm(() => {
    setSaveLoading(true)
    cvStore.saveCvItem({
      ...cvStore.currentCvItem,
      name: formOption.value.pickName,
      elementData: JSON.stringify(getElementData(true)),
    }, groupId).then(() => {
      setSaveLoading(false)
      // 저장닫기팝업
      modal.hide()
      cvStore.resetCurrentItem()
      // 계속열기시작선택
      if (isContinue)
        useCv.pick({ entry, groupId })
    }, () => {
      setSaveLoading(false)
    }).catch(() => {
      setSaveLoading(false)
    })
  })
}, 1500, { leading: true, trailing: false })

// 검증요소
const similarity: Ref<RPA.AtomDisplayItem> = ref({ value: 0.95, key: '', title: '' })
const validate = throttle(() => {
  validateForm(() => {
    const eleData = getElementData(false)
    useCv.check({ ...eleData, similarity: similarity.value.value })
  })
}, 1500, { leading: true, trailing: false })

// 선택점
const pickAnchor = throttle(() => {
  validateForm(() => {
    useCv.pickAnchor({ ...cvStore.currentCvItem, elementData: { ...getElementData(false), similarity: similarity.value.value } })
  })
}, 1500, { leading: true, trailing: false })

// 다시 선택
const rePick = throttle(() => {
  useCv.rePick(cvStore.currentCvItem)
}, 1500, { leading: true, trailing: false })

const defaultAnchor = ref(false)

//  useCvStore.currentElement.name 의변수
watch(() => cvStore.currentCvItem, (newVal) => {
  if (newVal) {
    setFormOption({ pickName: newVal.name })
    const eleData = JSON.parse(newVal.elementData)
    defaultAnchor.value = eleData.defaultAnchor
  }
}, { immediate: true })

const disabled = computed(() => {
  return cvPickStore.isPicking || cvPickStore.isChecking
})

onUnmounted(() => {
  cvStore.resetCurrentItem()
})
</script>

<template>
  <a-modal
    v-bind="NiceModal.antdModal(modal)"
    destroy-on-close
    :width="530"
    :z-index="1040"
    :title="$t('cvEdit')"
    class="modal-ant-operation"
    :keyboard="false"
    :mask-closable="false"
  >
    <template #closeIcon>
      <CloseOutlined :class="[disabled ? 'opacity-50' : '']" @click.stop="handleCancel" />
    </template>
    <template #footer>
      <a-button key="back" :disabled="disabled" @click="handleCancel">
        {{ $t("cancel") }}
      </a-button>
      <a-button v-if="entry === 'group'" key="save" type="primary" :disabled="disabled" :loading="saveLoading" @click="save(true)">
        {{ $t("saveAndContinue") }}
      </a-button>
      <a-button key="finish" type="primary" :disabled="disabled" :loading="saveLoading" @click="save(false)">
        {{ $t('done') }}
      </a-button>
    </template>
    <div class="cv-pick-wrapper">
      <a-form
        ref="formRef"
        class="form-wrapper"
        :rules="rules"
        :model="formOption"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 14 }"
        label-align="left"
      >
        <a-form-item :label="$t('elementName')" name="pickName">
          <a-input v-model:value="formOption.pickName" :maxlength="32" :placeholder="$t('enterElementName')" />
        </a-form-item>
      </a-form>
      <div class="cv-pick-inner">
        <p v-if="cvStore.currentCvItem?.parentImageUrl" class="cv-pick-tip">
          {{ $t('cvPick.multiElementTip') }}
        </p>
        <a-row class="cv-pick-content">
          <a-col :span="15" class="cv-imgs flex">
            <span v-if="cvStore.currentCvItem?.parentImageUrl" class="cv-img-item anchor-img relative">
              <Image v-if="!defaultAnchor" :title="$t('fullSizeImage')" :src="getImageURL(cvStore.currentCvItem.parentImageUrl)" />
              <span v-else class="anchor flex items-center justify-center" @click="pickAnchor">
                <rpa-icon name="anchor-point" color="#F39D09" class="cursor-pointer" size="48" />
              </span>
              <span class="anchor-tip absolute inline-block" @click="pickAnchor">{{ $t('cvPick.setAnchor') }}</span>
            </span>
            <span class="cv-img-item cv-img" :class="{ 'cv-img-item-fullw': !cvStore.currentCvItem?.parentImageUrl }">
              <Image v-if="cvStore.currentCvItem?.imageUrl" :title="$t('fullSizeImage')" :src="getImageURL(cvStore.currentCvItem.imageUrl)" />
            </span>
          </a-col>
          <a-col :span="9" class="cv-buttons flex items-center">
            <span>
              <a-button
                size="small"
                :icon="h(RedoOutlined)"
                :loading="cvPickStore.isPicking"
                :disabled="disabled"
                class="font-size-12 inline-flex-center"
                @click="rePick"
              >
                {{ $t("rePickupElement") }}
              </a-button>
              <a-button class="font-size-12 inline-flex-center" :icon="h(BorderOuterOutlined)" size="small" :loading="cvPickStore.isChecking" :disabled="cvPickStore.isPicking" @click="validate">
                {{ $t("validateElement") }}
              </a-button>
              <span class="text-left validate-slider">
                {{ $t('cvPick.similarityCheck') }}
                <AtomSlider class="cursor-pointer mr-1" :render-data="similarity" />
              </span>
            </span>
          </a-col>
        </a-row>
      </div>
    </div>
  </a-modal>
</template>

<style scoped lang="scss">
:deep(.ant-modal .ant-modal-content) {
  padding: 20px 12px;
}
.cv-pick {
  &-content {
    height: 150px;
  }
  &-inner {
    padding: 10px;
  }
  &-tip {
    font-size: 12px;
    color: #999;
  }
}
.cv-imgs {
  height: 100%;
  .anchor {
    width: 100%;
    height: 100%;
    border-radius: 4px;
    border: 1px solid #f8f8f8;
    cursor: pointer;
    position: relative !important;
  }
  .anchor-tip {
    width: 100%;
    background-color: #fff;
    font-size: 12px;
    height: 24px;
    line-height: 24px;
    text-align: center;
    bottom: 0;
    cursor: pointer;
    &:hover {
      color: #1890ff;
    }
  }
  .cv-img-item {
    height: 100%;
    background: #f5f5f5;
    margin-right: 10px;
    border-radius: 4px;
    overflow: hidden;
    &:first-child {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 30%;
    }
    &:last-child {
      width: 70%;
    }
    &-fullw {
      width: 100% !important;
    }
  }
}

.cv-buttons {
  text-align: center;
  :deep(.anticon) {
    font-size: 12px;
  }
}
.validate-slider {
  margin-top: 12px;
  font-size: 12px;
  display: inline-block;
}
:deep(.validate-slider .atom-slider .ant-slider) {
  width: 80px;
}
:deep(.cv-buttons .ant-btn:first-child) {
  margin-bottom: 10px !important;
}
:deep(.cv-buttons .ant-btn) {
  display: flex;
  align-items: center;
}
:deep(.ant-image) {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  overflow: hidden;
}
:deep(.ant-image .ant-image-img) {
  width: auto;
  height: auto;
  max-height: 150px;
  // max-width: 150px;
}
</style>
