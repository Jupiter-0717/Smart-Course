// src/types/shims-vue.d.ts
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

// src/types/shims-vue.d.ts
declare module 'element-plus' {
  export const ElNotification: (typeof import('element-plus'))['ElNotification']
  export const ElMessageBox: (typeof import('element-plus'))['ElMessageBox']
}
