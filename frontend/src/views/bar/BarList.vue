<template>
  <div class="bar-list">
    <div class="page-header">
      <h2 class="art-heading-h2">附近酒吧</h2>
      <el-button type="primary" @click="$router.push('/bars/register')">
        注册酒吧
      </el-button>
    </div>

    <el-card class="search-card">
      <el-tabs v-model="searchType" @tab-change="handleTabChange">
        <el-tab-pane label="附近搜索" name="nearby">
          <div class="search-form">
            <el-form :inline="true">
              <el-form-item label="搜索半径">
                <el-input-number v-model="searchRadius" :min="1" :max="100" :step="5" style="width: 150px" />
                <span style="margin-left: 8px;">公里</span>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="searchNearby" :loading="loading">
                  搜索附近
                </el-button>
                <el-button @click="getCurrentLocation" :loading="locating">
                  <el-icon>
                    <Location />
                  </el-icon>
                  获取当前位置
                </el-button>
              </el-form-item>
            </el-form>
            <el-text v-if="userLocation" type="success" size="small">
              当前位置：经度 {{ userLocation.longitude.toFixed(6) }}，纬度 {{ userLocation.latitude.toFixed(6) }}
            </el-text>
            <el-text v-else type="info" size="small">
              点击"获取当前位置"按钮获取您的地理位置，然后点击"搜索附近"查找周边酒吧
            </el-text>
          </div>
        </el-tab-pane>

        <el-tab-pane label="按城市搜索" name="city">
          <div class="search-form">
            <el-form :inline="true">
              <el-form-item label="城市名称">
                <el-input v-model="searchCity" placeholder="支持模糊搜索，如：上海、北京" style="width: 250px" clearable
                  @keyup.enter="searchByCity" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="searchByCity" :loading="loading">
                  搜索
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <el-tab-pane label="综合搜索" name="name">
          <div class="search-form">
            <el-form :inline="true">
              <el-form-item label="关键词">
                <el-input v-model="searchName" placeholder="搜索名称/城市/地址/酒类" style="width: 250px" clearable
                  @keyup.enter="searchByName" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="searchByName" :loading="loading">
                  搜索
                </el-button>
              </el-form-item>
            </el-form>
            <el-text type="info" size="small">
              可搜索酒吧名称、城市、地址或主营酒类，如：鸡尾酒、威士忌、黄浦区等
            </el-text>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 高德地图容器 -->
    <el-card class="map-card">
      <template #header>
        <span>地图</span>
      </template>
      <div id="amap-container" class="amap-container"></div>
    </el-card>

    <div v-loading="loading" class="bar-grid">
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
import { searchNearbyBars, searchBarsByCity, searchBarsByName } from '@/api/bar'
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
      searchType: 'nearby',
      searchCity: '',
      searchName: '',
      searchRadius: 10,
      userLocation: null,
      bars: [],
      loading: false,
      locating: false,
      map: null,
      markers: [],
      userMarker: null,
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

    // 清理地图实例
    if (this.map) {
      try {
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
    handleTabChange() {
      // 标签切换时不清空搜索结果，让用户可以看到之前的搜索
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
              this.locating = false
              ElMessage.success('位置获取成功')
              
              // 获取位置后自动搜索附近酒吧
              if (this.searchType === 'nearby') {
                this.searchNearby()
              }

              // 如果地图已经初始化，更新地图中心点和标记
          if (this.map) {
            const center = [this.userLocation.longitude, this.userLocation.latitude]
            this.map.setCenter(center)
            this.map.setZoom(13)
            this.addUserMarker(center)
          }
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

    // 搜索附近的酒吧
    async searchNearby() {
      if (!this.userLocation) {
        ElMessage.warning('请先获取当前位置')
        return
      }

      // #region agent log
      fetch('http://127.0.0.1:7242/ingest/2be73884-0cc8-4c48-bb93-e298599f041c', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ location: 'BarList.vue:searchNearby', message: 'search nearby called', data: { lat: this.userLocation.latitude, lng: this.userLocation.longitude, radius: this.searchRadius }, timestamp: Date.now(), sessionId: 'debug-session', runId: 'nearby-frontend', hypothesisId: 'I' }) }).catch(() => { });
      // #endregion

      this.loading = true
      try {
        const { data } = await searchNearbyBars({
          latitude: this.userLocation.latitude,
          longitude: this.userLocation.longitude,
          radiusKm: this.searchRadius
        })

        // #region agent log
        fetch('http://127.0.0.1:7242/ingest/2be73884-0cc8-4c48-bb93-e298599f041c', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ location: 'BarList.vue:searchNearby:result', message: 'search result', data: { count: data ? data.length : 0, isArray: Array.isArray(data) }, timestamp: Date.now(), sessionId: 'debug-session', runId: 'nearby-frontend', hypothesisId: 'J' }) }).catch(() => { });
        // #endregion

        this.bars = data || []
        if (this.bars.length === 0) {
          ElMessage.info(`附近${this.searchRadius}公里内没有找到酒吧`)
        } else {
          ElMessage.success(`找到${this.bars.length}家附近的酒吧`)
          // 更新地图标记
          this.$nextTick(() => {
            this.updateMapMarkers()
          })
        }
      } catch (error) {
        // #region agent log
        fetch('http://127.0.0.1:7242/ingest/2be73884-0cc8-4c48-bb93-e298599f041c', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ location: 'BarList.vue:searchNearby:error', message: 'search error', data: { errorMsg: error.message, errorType: error.constructor.name }, timestamp: Date.now(), sessionId: 'debug-session', runId: 'nearby-frontend', hypothesisId: 'K' }) }).catch(() => { });
        // #endregion
        ElMessage.error(error.message || '搜索失败')
      } finally {
        this.loading = false
      }
    },

    async searchByCity() {
      if (!this.searchCity.trim()) {
        ElMessage.warning('请输入城市名称')
        return
      }

      this.loading = true
      try {
        const { data } = await searchBarsByCity(this.searchCity)
        this.bars = data || []
        // 更新地图标记
        this.$nextTick(() => {
          this.updateMapMarkers()
        })
      } catch (error) {
        ElMessage.error(error.message || '搜索失败')
      } finally {
        this.loading = false
      }
    },

    async searchByName() {
      if (!this.searchName.trim()) {
        ElMessage.warning('请输入搜索关键词')
        return
      }

      this.loading = true
      try {
        const { data } = await searchBarsByName({ name: this.searchName })
        this.bars = data || []
        // 更新地图标记
        this.$nextTick(() => {
          this.updateMapMarkers()
        })
      } catch (error) {
        ElMessage.error(error.message || '搜索失败')
      } finally {
        this.loading = false
      }
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
      script.src = `https://webapi.amap.com/maps?v=2.0&key=${this.amapKey}&plugin=AMap.Geolocation,AMap.Marker,AMap.CitySearch`

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
      container.style.height = '500px'
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
          } else {
            // 如果没有用户位置，尝试获取IP位置
            this.updateMapCenterByIP()
          }

          // 如果有酒吧数据，添加标记
          if (this.bars.length > 0) {
            this.updateMapMarkers()
          }
        })

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
          }
        })
      })
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
          icon: new AMap.Icon({
            size: new AMap.Size(32, 32),
            image: 'https://webapi.amap.com/theme/v1.3/markers/n/mid.png'
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
          const marker = new AMap.Marker({
            position: [bar.longitude, bar.latitude],
            title: bar.name,
            label: {
              content: bar.name,
              direction: 'right'
            },
            icon: new AMap.Icon({
              size: new AMap.Size(32, 32),
              image: 'https://webapi.amap.com/theme/v1.3/markers/n/mid.png',
              imageOffset: new AMap.Pixel(0, 0)
            })
          })

          // 添加信息窗口 - 包含查看路线按钮
          const infoWindow = new AMap.InfoWindow({
            content: `
              <div style="padding: 10px; min-width: 200px;">
                <h3 style="margin: 0 0 8px 0; font-size: 16px; color: #333;">${bar.name}</h3>
                <p style="margin: 4px 0; color: #666; font-size: 14px;">${bar.address}</p>
                ${bar.distance ? `<p style="margin: 4px 0; color: #409EFF; font-size: 14px;">距离: ${bar.distance.toFixed(2)} 公里</p>` : ''}
                ${bar.avgRating ? `<p style="margin: 4px 0; color: #666; font-size: 14px;">评分: ${bar.avgRating.toFixed(1)}</p>` : ''}
              </div>
            `,
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
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

h2 {
  margin: 0;
}

.search-card {
  margin-bottom: 20px;
}

.search-form {
  padding: 10px 0;
}

.bar-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}

.bar-card {
  cursor: pointer;
  transition: transform 0.2s;
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

.map-card {
  margin-bottom: 20px;
}

.amap-container {
  width: 100% !important;
  height: 500px !important;
  min-height: 500px !important;
  background-color: #f5f5f5;
  display: block !important;
  visibility: visible !important;
  position: relative;
}
</style>
