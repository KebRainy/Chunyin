<template>
  <div class="bar-list">
    <div class="page-header">
      <h2>附近酒吧</h2>
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
                <el-input-number 
                  v-model="searchRadius" 
                  :min="1" 
                  :max="100" 
                  :step="5"
                  style="width: 150px"
                />
                <span style="margin-left: 8px;">公里</span>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="searchNearby" :loading="loading">
                  搜索附近
                </el-button>
                <el-button @click="getCurrentLocation" :loading="locating">
                  <el-icon><Location /></el-icon>
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
                <el-input 
                  v-model="searchCity" 
                  placeholder="支持模糊搜索，如：上海、北京"
                  style="width: 250px"
                  clearable
                  @keyup.enter="searchByCity"
                />
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
                <el-input 
                  v-model="searchName" 
                  placeholder="搜索名称/城市/地址/酒类"
                  style="width: 250px"
                  clearable
                  @keyup.enter="searchByName"
                />
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

    <div v-loading="loading" class="bar-grid">
      <el-empty v-if="bars.length === 0 && !loading" description="暂无酒吧信息" />
      
      <el-card 
        v-for="bar in bars" 
        :key="bar.id" 
        class="bar-card"
        shadow="hover"
        @click="goToDetail(bar.id)"
      >
        <div class="bar-info">
          <h3 class="bar-name">{{ bar.name }}</h3>
          <div class="bar-rating">
            <el-rate 
              v-model="bar.avgRating" 
              disabled 
              show-score 
              :colors="['#99A9BF', '#F7BA2A', '#FF9900']"
            />
            <span class="review-count">({{ bar.reviewCount }}条评价)</span>
          </div>
          <p class="bar-address">
            <el-icon><LocationFilled /></el-icon>
            {{ bar.address }}
          </p>
          <p v-if="bar.distance" class="bar-distance">
            距离: {{ (bar.distance).toFixed(2) }} 公里
          </p>
          <p v-if="bar.mainBeverages" class="bar-beverages">
            主营: {{ bar.mainBeverages }}
          </p>
          <div v-if="bar.openingTime && bar.closingTime" class="bar-time">
            <el-icon><Clock /></el-icon>
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
      locating: false
    }
  },
  mounted() {
    // 页面加载时自动获取位置
    this.getCurrentLocation()
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
      fetch('http://127.0.0.1:7242/ingest/2be73884-0cc8-4c48-bb93-e298599f041c',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'BarList.vue:searchNearby',message:'search nearby called',data:{lat:this.userLocation.latitude,lng:this.userLocation.longitude,radius:this.searchRadius},timestamp:Date.now(),sessionId:'debug-session',runId:'nearby-frontend',hypothesisId:'I'})}).catch(()=>{});
      // #endregion

      this.loading = true
      try {
        const { data } = await searchNearbyBars({
          latitude: this.userLocation.latitude,
          longitude: this.userLocation.longitude,
          radiusKm: this.searchRadius
        })
        
        // #region agent log
        fetch('http://127.0.0.1:7242/ingest/2be73884-0cc8-4c48-bb93-e298599f041c',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'BarList.vue:searchNearby:result',message:'search result',data:{count:data?data.length:0,isArray:Array.isArray(data)},timestamp:Date.now(),sessionId:'debug-session',runId:'nearby-frontend',hypothesisId:'J'})}).catch(()=>{});
        // #endregion
        
        this.bars = data || []
        if (this.bars.length === 0) {
          ElMessage.info(`附近${this.searchRadius}公里内没有找到酒吧`)
        } else {
          ElMessage.success(`找到${this.bars.length}家附近的酒吧`)
        }
      } catch (error) {
        // #region agent log
        fetch('http://127.0.0.1:7242/ingest/2be73884-0cc8-4c48-bb93-e298599f041c',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'BarList.vue:searchNearby:error',message:'search error',data:{errorMsg:error.message,errorType:error.constructor.name},timestamp:Date.now(),sessionId:'debug-session',runId:'nearby-frontend',hypothesisId:'K'})}).catch(()=>{});
        // #endregion
        console.error('附近搜索失败:', error)
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
      } catch (error) {
        console.error('按城市搜索失败:', error)
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
      } catch (error) {
        console.error('搜索失败:', error)
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
    }
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
</style>

