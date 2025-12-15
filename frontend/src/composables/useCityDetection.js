import { ref } from 'vue'

let cachedCity = ''
let pendingPromise = null

export const useCityDetection = () => {
  const detecting = ref(false)
  const detectedCity = ref(cachedCity)

  const detectCity = async () => {
    if (cachedCity) {
      detectedCity.value = cachedCity
      return cachedCity
    }
    if (pendingPromise) {
      return pendingPromise
    }
    detecting.value = true
    pendingPromise = fetch('https://api.vore.top/api/IPdata?ip=')
      .then((res) => res.json())
      .then((data) => {
        const city =
          data?.ipdata?.info1 ||
          data?.adcode?.c ||
          data?.ipdata?.info2 ||
          ''
        cachedCity = (city || '').replace(/\s+/g, '')
        detectedCity.value = cachedCity
        return cachedCity
      })
      .catch((error) => {
        console.error('自动定位失败', error)
        return ''
      })
      .finally(() => {
        detecting.value = false
        pendingPromise = null
      })
    return pendingPromise
  }

  return {
    detecting,
    detectedCity,
    detectCity
  }
}
