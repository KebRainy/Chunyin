import { ref } from 'vue'

const cachedProvinces = ref(null)
let loadingPromise = null

const normalizeName = (name = '') => {
  return name
    .trim()
    .replace(/(市|地区|特别行政区|自治州|回族自治区|维吾尔自治区|壮族自治区|自治区|省|盟)$/u, '')
    .toLowerCase()
}

export const useChinaRegions = () => {
  const provinces = ref([])
  const loading = ref(false)

  const ensureLoaded = async () => {
    if (cachedProvinces.value) {
      provinces.value = cachedProvinces.value
      return cachedProvinces.value
    }
    if (!loadingPromise) {
      loading.value = true
      loadingPromise = fetch('/cities.json')
        .then((res) => res.json())
        .then((data) => {
          const mapped = (data?.provinces || []).map((province) => ({
            label: province.provinceName,
            value: province.provinceName,
            cities: (province.citys || []).map((city) => ({
              label: city.cityName,
              value: city.cityName
            }))
          }))
          cachedProvinces.value = mapped
          provinces.value = mapped
          return mapped
        })
        .catch((error) => {
          console.error('加载城市列表失败', error)
          return []
        })
        .finally(() => {
          loading.value = false
        })
    }
    const result = await loadingPromise
    provinces.value = result
    return result
  }

  const findCityByName = (name = '') => {
    if (!name || !cachedProvinces.value) return null
    const target = normalizeName(name)
    for (const province of cachedProvinces.value) {
      if (normalizeName(province.value) === target) {
        return {
          province: province.value,
          city: province.value
        }
      }
      const cityMatch = province.cities.find(
        (city) => normalizeName(city.value) === target
      )
      if (cityMatch) {
        return {
          province: province.value,
          city: cityMatch.value
        }
      }
    }
    return null
  }

  const getDefaultCity = () => {
    if (!cachedProvinces.value || cachedProvinces.value.length === 0) return ''
    const firstProvince = cachedProvinces.value[0]
    return firstProvince.cities?.[0]?.value || firstProvince.value
  }

  return {
    provinces,
    loading,
    ensureLoaded,
    findCityByName,
    getDefaultCity
  }
}
