# ArchivesView.vue - 医生界面，查看患者健康档案的组件

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import request from '@/utils/request'

const router = useRouter()
const goBack = () => router.push('/doctor')

const archives = ref([])
const loading = ref(false)

const showChartDialog = ref(false)
const currentPatientName = ref('')
const chartLoading = ref(false)
const chartRef = ref(null)
let chartInstance = null

// 弹窗顶部的三个详细指标
const indicators = ref({
  bloodPressure: '--/--',
  heartRate: '--',
  bloodSugar: '--'
})

const fetchArchives = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/doctor/patients')
    if (res.data.code === 200) {
      archives.value = res.data.data
    } else {
      ElMessage.error(res.data.message || '获取档案失败')
    }
  } catch (error) {
    console.error('获取档案异常:', error)
    ElMessage.error('网络异常')
  } finally {
    loading.value = false
  }
}

const viewDetails = async (elderId, name) => {
  currentPatientName.value = name
  showChartDialog.value = true // 先把弹窗拉出来
  chartLoading.value = true
  
  try {
    const res = await request.get(`/api/health/dashboard?elderId=${elderId}`)
    if (res.data.code === 200) {
      const { trend, latest } = res.data.data
      
      // 1. 赋值顶部指标
      indicators.value = {
        bloodPressure: latest.bloodPressure || '--/--',
        heartRate: latest.heartRate || '--',
        bloodSugar: latest.bloodSugar || '--'
      }
      
      // 2. 等待弹窗DOM渲染完毕后，画折线图
      setTimeout(() => {
        renderChart(trend)
      }, 150) // 等待 150 毫秒
      
    } else {
      ElMessage.error(res.data.message || '获取健康数据失败')
    }
  } catch (error) {
    console.error('代码执行或网络异常详细信息:', error)
    ElMessage.error('系统异常，请按F12查看控制台')
  } finally {
    chartLoading.value = false
  }
}

const renderChart = (trend) => {
  if (!chartRef.value) return
  if (chartInstance != null) {
    chartInstance.dispose()
  }

  chartInstance = echarts.init(chartRef.value)
  
  chartInstance.setOption({
    color: ['#FF4D4F', '#1890FF', '#52C41A'],
    tooltip: { trigger: 'axis' },
    legend: { data: ['收缩压', '舒张压', '心率'], bottom: 0 },
    grid: { top: 20, left: 40, right: 20, bottom: 40 },
    xAxis: { type: 'category', data: trend.dates || [] },
    yAxis: { type: 'value' },
    series: [
      { name: '收缩压', type: 'line', smooth: true, data: trend.systolic || [] },
      { name: '舒张压', type: 'line', smooth: true, data: trend.diastolic || [] },
      { name: '心率', type: 'line', smooth: true, data: trend.heartRate || [] }
    ]
  })
}

onMounted(() => {
  fetchArchives()
})
</script>

<template>
  <div class="archives-page" v-loading="loading">
    <div class="page-header">
      <div>
        <h1>患者健康档案库</h1>
        <p>管理已确认预约患者的基本信息与健康状态</p>
      </div>
      <el-button type="primary" @click="goBack">返回预约大厅</el-button>
    </div>

    <el-card class="archives-card" shadow="never">
      <el-empty v-if="archives.length === 0" description="您目前暂无已确认的患者档案" />
      
      <div class="archive-row" v-for="item in archives" :key="item.elderId">
        <div class="archive-summary">
          <div class="archive-name">{{ item.name }}</div>
          <div class="archive-condition">
            近期状况：<span :class="{'danger-text': item.condition !== '指标平稳'}">{{ item.condition }}</span>
          </div>
        </div>
        <div class="archive-meta">
          <div>年龄 {{ item.age }} 岁 | 最近更新 {{ item.lastVisit }}</div>
          <el-button 
            type="primary" 
            link 
            style="margin-top: 4px;"
            @click="viewDetails(item.elderId, item.name)"
          >
            查看健康大盘 >
          </el-button>
        </div>
      </div>
    </el-card>

    <el-dialog 
      v-model="showChartDialog" 
      :title="`${currentPatientName} 的健康监测大盘`" 
      width="600px" 
      destroy-on-close
    >
      <div v-loading="chartLoading" class="dialog-content">
        <div class="indicator-row">
          <div class="indicator-box">
            <div class="val text-red">{{ indicators.bloodPressure }}</div>
            <div class="label">血压 (mmHg)</div>
          </div>
          <div class="indicator-box">
            <div class="val text-green">{{ indicators.heartRate }}</div>
            <div class="label">心率 (次/分)</div>
          </div>
          <div class="indicator-box">
            <div class="val text-blue">{{ indicators.bloodSugar }}</div>
            <div class="label">血糖 (mmol/L)</div>
          </div>
        </div>
        <div ref="chartRef" style="width: 100%; height: 280px; margin-top: 20px;"></div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.archives-page {
  min-height: 100vh;
  max-width: 880px;
  margin: 0 auto;
  padding: 24px;
  background: #f0f2f5;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.page-header p {
  margin: 6px 0 0;
  color: #606266;
}

.archives-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
}

.archive-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 0;
  border-bottom: 1px solid #f2f3f5;
}

.archive-row:last-child {
  border-bottom: none;
}

.archive-summary {
  max-width: 60%;
}

.archive-name {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}

.archive-condition {
  margin-top: 8px;
  font-size: 14px;
  color: #606266;
}

.danger-text {
  color: #ff4d4f;
  font-weight: bold;
}

.archive-meta {
  text-align: right;
  font-size: 14px;
  color: #606266;
}

.indicator-row {
  display: flex;
  justify-content: space-around;
  background: #f7f8fa;
  padding: 16px;
  border-radius: 8px;
}

.indicator-box {
  text-align: center;
}

.indicator-box .val {
  font-size: 20px;
  font-weight: bold;
}

.indicator-box .label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.text-red { color: #FF4D4F; }
.text-green { color: #52C41A; }
.text-blue { color: #1890FF; }
</style>
