import tailwindPreset from '@rpa/shared/tokens/tailwind'

/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'selector',
  presets: [tailwindPreset],
  content: [
    './src/**/*.{js,jsx,ts,tsx,vue}',
    './node_modules/@rpa/components/src/**/*.{js,jsx,ts,tsx,vue}',
  ],
  theme: {
    extend: {
      screens: {
        // 추가높음정도의중단점
        short: { raw: '(max-height: 800px)' },
      },
    },
  },
}
