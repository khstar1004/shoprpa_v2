import { useTranslation } from 'i18next-vue'
import { computed, inject, provide, ref } from 'vue'

import { getBaseURL } from '@/api/http/env'
import { addPyPackageApi, getPyPackageListApi, packageVersion } from '@/api/resource'
import { sseRequest } from '@/api/sse'
import { mirrorList } from '@/components/PythonPackageManagement/config'
import { useProcessStore } from '@/stores/useProcessStore'
import { usePythonPackageStore } from '@/stores/usePythonPackageStore'

// 생성조회위아래문서
export function usePackageCheckContext() {
  const processStore = useProcessStore()
  const pythonPackageStore = usePythonPackageStore()
  const { t } = useTranslation()

  const packages = ref<string[]>([])
  const lackPackages = ref<string[]>([])
  const initialLackPackages = ref<string[]>([])
  const isInstalling = ref(false)
  const isChecking = ref(false)

  async function checkLackPackages() {
    if (!packages.value || packages.value.length === 0) {
      lackPackages.value = []
      initialLackPackages.value = []
      return
    }

    isChecking.value = true
    try {
      const res = await getPyPackageListApi({ robotId: processStore.project.id })
      const installedPackageNames = (res.data || []).map((pkg: any) =>
        pkg.packageName?.toLowerCase(),
      )

      const lack = packages.value.filter((pkg) => {
        const pkgLower = pkg.toLowerCase()
        return !installedPackageNames.includes(pkgLower)
      })
      lackPackages.value = lack

      // 예결과예조회또는 packages 변수, 저장실패의목록
      if (initialLackPackages.value.length === 0 && lack.length > 0) {
        initialLackPackages.value = [...lack]
      }
    }
    catch (error) {
      console.error('조회실패:', error)
    }
    finally {
      isChecking.value = false
    }
  }

  function installPackageAsync(packageName: string): Promise<void> {
    return new Promise((resolve, reject) => {
      const controller = sseRequest.post(
        `${getBaseURL()}/scheduler/pip/install`,
        {
          project_id: processStore.project.id,
          package: packageName,
          version: '',
          mirror: mirrorList[0].value,
        },
        (res) => {
          if (!res)
            return

          let newData
          try {
            newData = JSON.parse(res.data).stdout
          }
          catch (e) {
            newData = res.data
            console.log(e)
          }

          if (newData.includes('stderr')) {
            controller.abort()
            reject(new Error(t('smartComponent.installPackageFailed', { packageName })))
            return
          }

          if (newData.includes('[DONE]')) {
            controller.abort()
            const params = {
              robotId: processStore.project.id,
              packageName,
              packageVersion: '',
              mirror: '',
            }
            packageVersion({ robotId: processStore.project.id, packageName })
              .then((res) => {
                addPyPackageApi({ ...params, packageVersion: res.data.version })
                  .then(() => {
                    pythonPackageStore.updatePythonList()
                    resolve()
                  })
                  .catch(() => resolve())
              })
              .catch(() => resolve())
          }
        },
        (err) => {
          controller.abort()
          reject(err)
        },
      )
    })
  }

  async function installAllPackages() {
    if (lackPackages.value.length === 0 || isInstalling.value) {
      return
    }

    isInstalling.value = true
    const installResults: { success: string[], failed: string[] } = {
      success: [],
      failed: [],
    }

    try {
      for (const pkgName of lackPackages.value) {
        try {
          await installPackageAsync(pkgName)
          installResults.success.push(pkgName)
        }
        catch (error) {
          console.error(`설치 ${pkgName} 실패:`, error)
          installResults.failed.push(pkgName)
        }
      }

      // 설치완료후아니오다시 조회, 보관의실패목록
      // 업데이트실패의패키지목록(제거완료성공설치의)
      lackPackages.value = lackPackages.value.filter(pkg =>
        !installResults.success.includes(pkg),
      )
    }
    catch (error) {
      console.error('설치실패:', error)
    }
    finally {
      isInstalling.value = false
    }
  }

  async function setPackages(pkgs: string[]) {
    packages.value = pkgs
    initialLackPackages.value = []
    await checkLackPackages()
  }

  function reset() {
    packages.value = []
    lackPackages.value = []
    initialLackPackages.value = []
    isInstalling.value = false
    isChecking.value = false
  }

  const hasLackPackages = computed(() => lackPackages.value.length > 0)
  const hasInitialLackPackages = computed(() => initialLackPackages.value.length > 0)
  const isInstallCompleted = computed(() => initialLackPackages.value.length > 0 && lackPackages.value.length === 0)

  return {
    packages,
    lackPackages,
    initialLackPackages,
    isInstalling,
    isChecking,
    hasLackPackages,
    hasInitialLackPackages,
    isInstallCompleted,

    setPackages,
    checkLackPackages,
    installAllPackages,
    reset,
  }
}

export type PackageCheckContext = ReturnType<typeof usePackageCheckContext>

export function providePackageCheckContext(context: PackageCheckContext) {
  provide('packageCheck', context)
}

// 조회비고입력
export function usePackageCheck() {
  const context = inject<PackageCheckContext>('packageCheck')
  if (!context) {
    throw new Error('usePackageCheck must be used within SmartComponent Index component')
  }
  return context
}