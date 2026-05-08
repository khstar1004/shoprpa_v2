/// <reference types="vite/client" />
/// <reference types="vite-plugin-svg4vue/client" />
/// <reference types="@rpa/shared/platform" />

interface ViteTypeOptions {
  // 추가행코드, 가능으로를 ImportMetaEnv 의유형로격식방식,
  // 아니오허용있음지원하지 않는의키값완료.
  // strictImportMetaEnv: unknown
}

interface ImportMetaEnv {
  readonly VITE_SENTRY_DSN: string
  readonly VITE_SERVICE_HOST: string
  // 변경다중변수...
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

declare const __VUE_VERSION__: string
declare const __PINIA_VERSION__: string
