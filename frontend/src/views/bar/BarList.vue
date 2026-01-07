<template>
  <div class="bar-list">
    <div class="map-wrapper">
      <div id="amap-container" class="amap-container"></div>

      <div class="map-overlay map-overlay-left">
        <div class="search-row">
          <el-input
            v-model="searchKeyword"
            class="search-input"
            placeholder="综合搜索：名称/城市/地址/酒类"
            clearable
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-button type="primary" :loading="loading" @click="handleSearch">搜索</el-button>
        </div>
        <div class="search-meta">
          <el-text size="small" type="info">中心：{{ formatLngLat(searchCenter) }}</el-text>
          <el-text size="small" type="info">半径：{{ searchRadius.toFixed(1) }} km</el-text>
        </div>
      </div>

      <div class="map-overlay map-overlay-right">
        <el-button type="primary" @click="$router.push('/bars/register')">注册酒吧</el-button>
        <el-button @click="getCurrentLocation" :loading="locating">
          <el-icon><Location /></el-icon>
          获取位置
        </el-button>
      </div>
    </div>

    <div v-loading="loading" class="bar-grid bar-grid-below-map">
      <el-empty v-if="bars.length === 0 && !loading" description="暂无酒吧信息" />

      <el-card v-for="bar in bars" :key="bar.id" class="bar-card" shadow="hover" @click="goToDetail(bar.id)">
        <div class="bar-info">
          <h3 class="bar-name">{{ bar.name }}</h3>
          <div class="bar-rating">
            <el-rate v-model="bar.avgRating" disabled show-score :colors="['#99A9BF', '#F7BA2A', '#FF9900']" />
            <span class="review-count">({{ bar.reviewCount }}条评价)</span>
          </div>
          <p class="bar-address">
            <el-icon>
              <LocationFilled />
            </el-icon>
            {{ bar.address }}
          </p>
          <p v-if="bar.distance" class="bar-distance">
            距离: {{ (bar.distance).toFixed(2) }} 公里
          </p>
          <p v-if="bar.mainBeverages" class="bar-beverages">
            主营: {{ bar.mainBeverages }}
          </p>
          <div v-if="bar.openingTime && bar.closingTime" class="bar-time">
            <el-icon>
              <Clock />
            </el-icon>
            {{ formatTime(bar.openingTime) }} - {{ formatTime(bar.closingTime) }}
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script>
import { searchNearbyBars, searchBarsByName } from '@/api/bar'
import { LocationFilled, Clock, Location } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'BarList',
  components: {
    LocationFilled,
    Clock,
    Location
  },
  data() {
    return {
      searchKeyword: '',
      searchRadius: 10,
      userLocation: null,
      searchCenter: null,
      bars: [],
      loading: false,
      locating: false,
      map: null,
      markers: [],
      userMarker: null,
      viewportAutoSearchTimer: null,
      // 高德地图配置 - Web端JS API只需要key
      amapKey: '7a2b2596584bc3c9e8f37903befb61f7'
    }
  },
  mounted() {
    // 页面加载时自动获取位置
    this.getCurrentLocation()
    // 加载高德地图API - 延迟一下确保DOM已渲染
    this.$nextTick(() => {
      setTimeout(() => {
        this.loadAmapScript()
      }, 300)
    })
  },
  beforeUnmount() {
    if (this.viewportAutoSearchTimer) {
      clearTimeout(this.viewportAutoSearchTimer)
      this.viewportAutoSearchTimer = null
    }

    // 清理地图实例
    if (this.map) {
      try {
        this.map.off('moveend', this.handleViewportChanged)
        this.map.off('zoomend', this.handleViewportChanged)
        this.map.destroy()
      } catch (e) {
        // 忽略错误
      }
      this.map = null
    }

    // 清理标记
    if (this.userMarker) {
      try {
        this.userMarker.setMap(null)
      } catch (e) {
        // 忽略错误
      }
      this.userMarker = null
    }

    this.markers.forEach(marker => {
      try {
        marker.setMap(null)
      } catch (e) {
        // 忽略错误
      }
    })
    this.markers = []
  },
  methods: {
    formatLngLat(center) {
      if (!center) {
        return '—'
      }
      const lng = Number(center.longitude)
      const lat = Number(center.latitude)
      if (Number.isNaN(lng) || Number.isNaN(lat)) {
        return '—'
      }
      return `${lng.toFixed(5)}, ${lat.toFixed(5)}`
    },

    // 获取当前位置
    getCurrentLocation() {
      if (!navigator.geolocation) {
        ElMessage.error('您的浏览器不支持地理定位功能')
        return
      }

      this.locating = true
      navigator.geolocation.getCurrentPosition(
        (position) => {
          this.userLocation = {
                latitude: position.coords.latitude,
                longitude: position.coords.longitude
              }
              this.searchCenter = { ...this.userLocation }
              this.locating = false
              ElMessage.success('位置获取成功')

              // 如果地图已经初始化，更新地图中心点和标记
          if (this.map) {
            const center = [this.userLocation.longitude, this.userLocation.latitude]
            this.map.setCenter(center)
            this.map.setZoom(13)
            this.addUserMarker(center)
            this.handleViewportChanged()
          }

          this.handleSearch()
        },
        (error) => {
          this.locating = false
          let errorMsg = '获取位置失败'
          switch (error.code) {
            case error.PERMISSION_DENIED:
              errorMsg = '您拒绝了位置权限请求'
              break
            case error.POSITION_UNAVAILABLE:
              errorMsg = '位置信息不可用'
              break
            case error.TIMEOUT:
              errorMsg = '获取位置超时'
              break
          }
          ElMessage.error(errorMsg)
        },
        {
          enableHighAccuracy: true,
          timeout: 10000,
          maximumAge: 0
        }
      )
    },

    async handleSearch() {
      const keyword = (this.searchKeyword || '').trim()
      if (keyword) {
        this.tryMoveMapToCity(keyword)
        return this.searchByKeyword(keyword)
      }
      return this.searchNearby()
    },

    // 搜索附近的酒吧
    async searchNearby(options = {}) {
      if (!this.searchCenter) {
        ElMessage.warning('请先获取位置或移动地图选择中心点')
        return
      }
      const silent = !!options.silent

      this.loading = true
      try {
        const { data } = await searchNearbyBars({
          latitude: this.searchCenter.latitude,
          longitude: this.searchCenter.longitude,
          radiusKm: this.searchRadius
        })

        this.bars = data || []
        if (!silent && this.bars.length === 0) {
          ElMessage.info(`附近${this.searchRadius}公里内没有找到酒吧`)
        } else if (!silent) {
          ElMessage.success(`找到${this.bars.length}家附近的酒吧`)
          // 更新地图标记
          this.$nextTick(() => {
            this.updateMapMarkers()
          })
        }
      } catch (error) {
        if (!silent) {
          ElMessage.error(error.message || '搜索失败')
        }
      } finally {
        this.loading = false
      }
    },

    async searchByKeyword(keyword) {
      this.loading = true
      try {
        const { data } = await searchBarsByName({ name: keyword })
        this.bars = data || []
        this.$nextTick(() => {
          this.updateMapMarkers()
        })
      } catch (error) {
        ElMessage.error(error.message || '搜索失败')
      } finally {
        this.loading = false
      }
    },

    async tryMoveMapToCity(keyword) {
      if (!this.map || !window.AMap || !window.AMap.Geocoder) {
        return
      }
      if (!keyword || keyword.length > 12) {
        return
      }
      const normalized = keyword.endsWith('市') ? keyword : `${keyword}市`

      await Promise.race([new Promise((resolve) => {
        try {
          const geocoder = new window.AMap.Geocoder({})
          geocoder.getLocation(normalized, (status, result) => {
            if (status === 'complete' && result && result.geocodes && result.geocodes.length > 0) {
              const geocode = result.geocodes[0]
              const level = geocode.level
              if (level === '省' || level === '市' || level === '区县' || level === '区') {
                const location = geocode.location
                if (location) {
                  this.map.setCenter([location.lng, location.lat])
                  this.map.setZoom(11)
                  this.handleViewportChanged()
                }
              }
            }
            resolve()
          })
        } catch (e) {
          resolve()
        }
      }), new Promise((resolve) => setTimeout(resolve, 800))])
    },

    goToDetail(barId) {
      this.$router.push(`/bars/${barId}`)
    },

    formatTime(time) {
      if (!time) return ''
      // time 格式为 HH:mm:ss，我们只需要 HH:mm
      return time.substring(0, 5)
    },

    // 加载高德地图API - 参考map-show.html的简单方式
    loadAmapScript() {
      // 检查容器是否存在
      const container = document.getElementById('amap-container')
      if (!container) {
        setTimeout(() => this.loadAmapScript(), 100)
        return
      }

      // 如果已经加载过，直接初始化
      if (window.AMap && window.AMap.Map) {
        setTimeout(() => {
          this.initMap()
        }, 100)
        return
      }

      // 检查是否正在加载
      if (document.querySelector(`script[src*="webapi.amap.com"]`)) {
        // 等待加载完成
        const checkInterval = setInterval(() => {
          if (window.AMap && window.AMap.Map) {
            clearInterval(checkInterval)
            setTimeout(() => {
              this.initMap()
            }, 100)
          }
        }, 100)
        // 设置超时，避免无限等待
        setTimeout(() => {
          clearInterval(checkInterval)
        }, 10000)
        return
      }

      // 创建script标签加载高德地图API - 参考map-show.html的方式
      const script = document.createElement('script')
      script.type = 'text/javascript'
      script.async = true
      // 只加载基础插件，不加载路线规划（避免key平台不匹配问题）
      script.src = `https://webapi.amap.com/maps?v=2.0&key=${this.amapKey}&plugin=AMap.Geolocation,AMap.Marker,AMap.CitySearch,AMap.Geocoder`

      script.onload = () => {
        // API加载完成后初始化地图 - 延迟一下确保DOM完全准备好
        setTimeout(() => {
          if (window.AMap && window.AMap.Map) {
            this.initMap()
          } else {
            ElMessage.error('高德地图API加载失败')
          }
        }, 300)
      }

      script.onerror = () => {
        ElMessage.error('高德地图脚本加载失败，请检查网络连接或key配置')
      }

      document.head.appendChild(script)
    },

    // 初始化地图 - 完全按照map-show.html的简单方式
    initMap() {
      // 检查AMap对象是否存在
      if (!window.AMap || !window.AMap.Map) {
        return
      }

      // 检查容器是否存在
      const container = document.getElementById('amap-container')
      if (!container) {
        return
      }

      // 确保容器有高度和宽度
      container.style.height = '100%'
      container.style.width = '100%'
      container.style.display = 'block'
      container.style.visibility = 'visible'

      // 如果地图已经初始化，先销毁
      if (this.map) {
        try {
          this.map.destroy()
        } catch (e) {
          // 忽略销毁错误
        }
        this.map = null
      }

      // 确定地图中心点 - 先使用默认位置，确保地图能显示
      let mapCenter = [116.397428, 39.90923] // 默认北京天安门
      let mapZoom = 11

      // 如果有用户位置，使用用户位置
      if (this.userLocation && this.userLocation.longitude && this.userLocation.latitude) {
        mapCenter = [this.userLocation.longitude, this.userLocation.latitude]
        mapZoom = 13
      }

      // 创建地图实例 - 完全按照map-show.html的方式
      try {
        this.map = new AMap.Map('amap-container', {
          viewMode: '2D', // 默认使用2D模式
          zoom: mapZoom, // 地图级别
          center: mapCenter // 地图中心点
        })

        // 地图加载完成后，优先使用用户位置，否则使用IP位置
        this.map.on('complete', () => {
          // 优先使用用户位置
          if (this.userLocation && this.userLocation.longitude && this.userLocation.latitude) {
            const center = [this.userLocation.longitude, this.userLocation.latitude]
            this.map.setCenter(center)
            this.map.setZoom(13)
            this.addUserMarker(center)
            this.handleViewportChanged()
          } else {
            // 如果没有用户位置，尝试获取IP位置
            this.updateMapCenterByIP()
          }

          // 如果有酒吧数据，添加标记
          if (this.bars.length > 0) {
            this.updateMapMarkers()
          }
        })

        this.map.on('moveend', this.handleViewportChanged)
        this.map.on('zoomend', this.handleViewportChanged)

      } catch (error) {
        ElMessage.error('地图初始化失败：' + (error.message || '未知错误'))
      }
    },

    // 通过IP更新地图中心点（仅在用户位置不可用时使用）
    updateMapCenterByIP() {
      if (!window.AMap || !this.map) {
        return
      }

      // 如果已经有用户位置，不使用IP定位
      if (this.userLocation && this.userLocation.longitude && this.userLocation.latitude) {
        return
      }

      // 使用IP定位（CitySearch）
      AMap.plugin('AMap.CitySearch', () => {
        const citySearch = new AMap.CitySearch()
        citySearch.getLocalCity((status, result) => {
          if (status === 'complete' && result.bounds) {
            const center = result.bounds.getCenter()
            this.map.setCenter([center.lng, center.lat])
            this.map.setZoom(13)
            // 添加用户位置标记
            this.addUserMarker([center.lng, center.lat])
            this.handleViewportChanged()
          }
        })
      })
    },

    handleViewportChanged() {
      if (!this.map) {
        return
      }

      const center = this.map.getCenter && this.map.getCenter()
      if (center && typeof center.getLng === 'function' && typeof center.getLat === 'function') {
        this.searchCenter = {
          longitude: center.getLng(),
          latitude: center.getLat()
        }
      }

      const computedRadius = this.computeViewportRadiusKm()
      if (computedRadius && computedRadius > 0) {
        this.searchRadius = computedRadius
      }

      const keyword = (this.searchKeyword || '').trim()
      if (keyword) {
        return
      }
      this.scheduleViewportAutoSearch()
    },

    scheduleViewportAutoSearch() {
      if (this.viewportAutoSearchTimer) {
        clearTimeout(this.viewportAutoSearchTimer)
        this.viewportAutoSearchTimer = null
      }
      this.viewportAutoSearchTimer = setTimeout(() => {
        this.viewportAutoSearchTimer = null
        this.searchNearby({ silent: true })
      }, 450)
    },

    computeViewportRadiusKm() {
      if (!this.map || !this.map.getBounds || !this.map.getCenter) {
        return null
      }
      try {
        const bounds = this.map.getBounds()
        const center = this.map.getCenter()
        if (!bounds || !center) {
          return null
        }

        const ne = bounds.getNorthEast && bounds.getNorthEast()
        if (!ne) {
          return null
        }

        const km = this.haversineKm(center.getLat(), center.getLng(), ne.getLat(), ne.getLng())
        if (!km || Number.isNaN(km)) {
          return null
        }

        return Math.max(0.8, km * 0.9)
      } catch (e) {
        return null
      }
    },

    haversineKm(lat1, lon1, lat2, lon2) {
      const toRad = (deg) => (deg * Math.PI) / 180
      const R = 6371
      const dLat = toRad(lat2 - lat1)
      const dLon = toRad(lon2 - lon1)
      const a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2)
      const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
      return R * c
    },

    // 添加用户位置标记
    addUserMarker(position) {
      if (!this.map || !position) {
        return
      }

      // 清除旧标记
      if (this.userMarker) {
        try {
          this.userMarker.setMap(null)
        } catch (e) {
          // 忽略错误
        }
      }

      try {
        this.userMarker = new AMap.Marker({
          position: position,
          title: '我的位置',
          anchor: 'bottom-center',
          icon: new AMap.Icon({
            size: new AMap.Size(34, 34),
            image: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_b.png',
            imageSize: new AMap.Size(34, 34)
          })
        })
        this.userMarker.setMap(this.map)
      } catch (error) {
        // 忽略标记添加错误
      }
    },


    // 更新地图标记
    updateMapMarkers() {
      if (!this.map || !this.bars || this.bars.length === 0) {
        return
      }

      // 清除旧标记
      this.markers.forEach(marker => marker.setMap(null))
      this.markers = []

      // 添加酒吧标记
      this.bars.forEach(bar => {
        if (bar.longitude && bar.latitude) {
          const router = this.$router
          const marker = new AMap.Marker({
            position: [bar.longitude, bar.latitude],
            title: bar.name,
            anchor: 'bottom-center',
            label: {
              content: bar.name,
              direction: 'right'
            },
            icon: new AMap.Icon({
              size: new AMap.Size(44, 44),
              image: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_r.png',
              imageSize: new AMap.Size(44, 44),
              imageOffset: new AMap.Pixel(0, 0)
            })
          })

          // 添加信息窗口 - 包含查看路线按钮
          const content = document.createElement('div')
          content.className = 'bar-infowindow'
          content.innerHTML = `
              <div class="bar-infowindow__title">${bar.name}</div>
              <div class="bar-infowindow__row">${bar.address || ''}</div>
              ${bar.distance ? `<div class="bar-infowindow__row bar-infowindow__distance">距离: ${bar.distance.toFixed(2)} 公里</div>` : ''}
              ${bar.avgRating ? `<div class="bar-infowindow__row">评分: ${bar.avgRating.toFixed(1)}</div>` : ''}
              <a class="bar-infowindow__link" href="/bars/${bar.id}">查看酒吧详情</a>
            `
          const link = content.querySelector('.bar-infowindow__link')
          if (link && router) {
            link.addEventListener('click', (e) => {
              e.preventDefault()
              router.push(`/bars/${bar.id}`)
            })
          }
          const infoWindow = new AMap.InfoWindow({
            content,
            offset: new AMap.Pixel(0, -30)
          })

          marker.on('click', () => {
            infoWindow.open(this.map, marker.getPosition())
          })

          marker.setMap(this.map)
          this.markers.push(marker)
        }
      })

      // 调整地图视野以包含所有标记
      if (this.markers.length > 0) {
        try {
          const allMarkers = [...this.markers]
          if (this.userMarker) {
            allMarkers.push(this.userMarker)
          }
          this.map.setFitView(allMarkers, false, [50, 50, 50, 50])
        } catch (error) {
          // 忽略视野调整错误
        }
      }
    },

  }
}
</script>

<style scoped>
.bar-list {
  width: 100%;
}

.map-wrapper {
  position: relative;
  width: 100%;
  height: calc(100vh - 72px);
  min-height: 560px;
  background: #f5f5f5;
  border-radius: 18px;
  overflow: hidden;
}

.map-overlay {
  position: absolute;
  top: 16px;
  z-index: 10;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(236, 239, 245, 0.9);
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.12);
  border-radius: 14px;
  padding: 12px;
  backdrop-filter: blur(10px);
}

.map-overlay-left {
  left: 16px;
  width: min(360px, calc(100% - 32px));
}

.map-overlay-right {
  right: 16px;
  display: flex;
  gap: 10px;
  align-items: center;
}

.search-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.search-row :deep(.el-input) {
  flex: 1;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 12px;
}

.search-meta {
  margin-top: 8px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.bar-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}

.bar-grid-below-map {
  padding: 18px 20px 20px;
}

.bar-card {
  cursor: pointer;
  will-change: transform;
  transition: transform var(--motion-normal, 220ms) var(--motion-ease, ease);
}

.bar-card:hover {
  transform: translateY(-5px);
}

.bar-info {
  padding: 10px;
}

.bar-name {
  margin: 0 0 10px 0;
  font-size: 18px;
  color: #333;
}

.bar-rating {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.review-count {
  margin-left: 10px;
  color: #999;
  font-size: 14px;
}

.bar-address {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #666;
  font-size: 14px;
  margin: 8px 0;
}

.bar-distance {
  color: #409EFF;
  font-size: 14px;
  margin: 8px 0;
}

.bar-beverages {
  color: #666;
  font-size: 14px;
  margin: 8px 0;
}

.bar-time {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #999;
  font-size: 14px;
  margin-top: 8px;
}

.amap-container {
  width: 100% !important;
  height: 100% !important;
  min-height: 100% !important;
  background-color: #f5f5f5;
  display: block !important;
  visibility: visible !important;
  position: relative;
}

.bar-infowindow {
  padding: 10px 12px;
  min-width: 220px;
  font-size: 13px;
  color: #1f2d3d;
}

.bar-infowindow__title {
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 6px;
}

.bar-infowindow__row {
  margin: 3px 0;
  color: #606266;
}

.bar-infowindow__distance {
  color: #409eff;
}

.bar-infowindow__link {
  display: inline-block;
  margin-top: 8px;
  color: #409eff;
  font-weight: 600;
  text-decoration: none;
}

.bar-infowindow__link:hover {
  text-decoration: underline;
}

@media (max-width: 768px) {
  .map-wrapper {
    height: calc(100vh - 64px);
    border-radius: 0;
  }

  .map-overlay-right {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }
}
</style>
