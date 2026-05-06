import { defineComponent, onMounted, ref, watch } from 'vue'
import '@rpa/components/icon'

export const Icon = defineComponent({
  name: 'RIcon',
  props: {
    // 아이콘이름
    name: { type: String, required: true },
    // 테이블의 svg 이미지너비높음, 1em, 있음값의형식매개svg요소의너비정도/높음정도가져오기값.예결과아이콘1:1, 생성경과값, 직선연결width, height
    size: { type: String },
    // 테이블의svg이미지너비정도, 1em, 단계높음size
    width: { type: String },
    // 테이블의svg이미지높음정도, 1em, 단계높음size
    height: { type: String },
    // 테이블여부로 svg 추가변환(변환로없음제한), 로false.
    spin: { type: Boolean, default: false },
    // 테이블의svg이미지중필요변수색상의색상.있음값의형식매개svg요소의stroke/fill 가져오기값.
    color: { type: String },
    // 테이블의 svg 이미지중필요변수색상의 stroke 색상, 단계높음 color .
    stroke: { type: String },
    // 테이블의 svg 이미지중필요변수색상의 fill 색상, 단계높음 color .
    fill: { type: String },
    // 아이콘, 지정아이콘아니오저장에서시사용
    defaultName: { type: String, default: 'atom-default' },
  },
  emits: ['click'],
  setup(props, { emit }) {
    const currentIconName = ref(props.name)

    function checkIcon() {
      if (!props.name)
        return
      if (!document.getElementById(props.name)) {
        currentIconName.value = props.defaultName
      }
      else {
        currentIconName.value = props.name
      }
    }

    watch(() => props.name, () => {
      currentIconName.value = props.name
      checkIcon()
    })

    onMounted(() => {
      // 지연조회, 확인 SVG sprite 완료로드
      setTimeout(() => {
        checkIcon()
      }, 100)
    })

    return () => (
      <svg
        name={props.name}
        class={['r-icon inline-block', { 'animate-spin': props.spin }]}
        style={props.color ? { color: props.color } : {}}
        width={props.width || props.size || '1em'}
        height={props.height || props.size || '1em'}
        fill={props.fill}
        stroke={props.stroke}
        onClick={() => emit('click')}
      >
        <use href={`#${currentIconName.value}`}></use>
      </svg>
    )
  },
})