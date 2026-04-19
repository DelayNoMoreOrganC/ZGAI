<template>
  <div class="statistics">
    <PageHeader title="统计报表">
      <template #extra>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          @change="handleDateChange"
        />
        <el-button @click="handleExportExcel">
          <el-icon><Download /></el-icon>
          导出Excel
        </el-button>
        <el-button @click="handleExportPDF">
          <el-icon><Download /></el-icon>
          导出PDF
        </el-button>
      </template>
    </PageHeader>

    <!-- 统计卡片 -->
    <div class="stats-cards">
      <div v-for="stat in statsCards" :key="stat.key" class="stat-card">
        <div class="stat-icon" :style="{ backgroundColor: stat.color }">
          <span>{{ stat.icon }}</span>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
          <div class="stat-trend" :class="stat.trendClass">
            <el-icon><CaretTop v-if="stat.trend === 'up'" /><CaretBottom v-else /></el-icon>
            {{ stat.trendValue }}
          </div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-grid">
      <!-- 案件数量趋势 -->
      <div class="chart-card">
        <div class="chart-header">
          <h4>案件数量趋势</h4>
          <el-radio-group v-model="caseTrendPeriod" size="small" @change="updateCaseTrendChart">
            <el-radio-button label="month">月度</el-radio-button>
            <el-radio-button label="quarter">季度</el-radio-button>
            <el-radio-button label="year">年度</el-radio-button>
          </el-radio-group>
        </div>
        <div ref="caseTrendChartRef" class="chart-container"></div>
      </div>

      <!-- 案件类型分布 -->
      <div class="chart-card">
        <div class="chart-header">
          <h4>案件类型分布</h4>
          <el-switch v-model="caseTypePie" @change="updateCaseTypeChart">
            <template #active-text>饼图</template>
            <template #inactive-text>柱状图</template>
          </el-switch>
        </div>
        <div ref="caseTypeChartRef" class="chart-container"></div>
      </div>

      <!-- 收费统计 -->
      <div class="chart-card">
        <div class="chart-header">
          <h4>收费统计</h4>
          <el-radio-group v-model="feeType" size="small" @change="updateFeeChart">
            <el-radio-button label="income">收入</el-radio-button>
            <el-radio-button label="pending">待收</el-radio-button>
            <el-radio-button label="all">全部</el-radio-button>
          </el-radio-group>
        </div>
        <div ref="feeChartRef" class="chart-container"></div>
      </div>

      <!-- 律师业绩排名 -->
      <div class="chart-card full-width">
        <div class="chart-header">
          <h4>律师业绩排名</h4>
          <el-radio-group v-model="performanceMetric" size="small" @change="updatePerformanceChart">
            <el-radio-button label="caseCount">案件数</el-radio-button>
            <el-radio-button label="fee">收费</el-radio-button>
            <el-radio-button label="closeRate">结案率</el-radio-button>
          </el-radio-group>
        </div>
        <div ref="performanceChartRef" class="chart-container wide"></div>
      </div>

      <!-- 案件胜诉率 -->
      <div class="chart-card">
        <div class="chart-header">
          <h4>案件胜诉率</h4>
        </div>
        <div ref="winRateChartRef" class="chart-container"></div>
      </div>

      <!-- 收款率统计 -->
      <div class="chart-card">
        <div class="chart-header">
          <h4>收款率统计</h4>
        </div>
        <div ref="collectionRateChartRef" class="chart-container"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, CaretTop, CaretBottom } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import * as echarts from 'echarts'
import { getStatsCards, getCaseTrend, getCaseTypeDistribution } from '@/api/statistics'

const dateRange = ref([])
const caseTrendPeriod = ref('month')
const caseTypePie = ref(true)
const feeType = ref('income')
const performanceMetric = ref('caseCount')

// 图表实例
const charts = {}

// 图表DOM引用
const caseTrendChartRef = ref(null)
const caseTypeChartRef = ref(null)
const feeChartRef = ref(null)
const performanceChartRef = ref(null)
const winRateChartRef = ref(null)
const collectionRateChartRef = ref(null)

// 统计卡片数据
const statsCards = ref([])

// 获取统计卡片数据
const fetchStatsCards = async () => {
  try {
    const res = await getStatsCards({ page: 1, size: 10 })
    if (res.success) {
      const data = res.data
      statsCards.value = [
        {
          key: 'totalCases',
          label: '案件总数',
          value: data.totalCases?.toString() || '0',
          icon: '⚖️',
          color: '#1890ff',
          trend: 'up',
          trendValue: data.totalCasesTrend || '0%',
          trendClass: 'trend-up'
        },
        {
          key: 'activeCases',
          label: '进行中',
          value: data.activeCases?.toString() || '0',
          icon: '📋',
          color: '#52c41a',
          trend: 'up',
          trendValue: data.activeCasesTrend || '0%',
          trendClass: 'trend-up'
        },
        {
          key: 'closedCases',
          label: '已结案',
          value: data.closedCases?.toString() || '0',
          icon: '✅',
          color: '#67c23a',
          trend: 'down',
          trendValue: data.closedCasesTrend || '0%',
          trendClass: 'trend-down'
        },
        {
          key: 'totalIncome',
          label: '总收入(万)',
          value: data.totalIncome?.toString() || '0',
          icon: '💰',
          color: '#faad14',
          trend: 'up',
          trendValue: data.totalIncomeTrend || '0%',
          trendClass: 'trend-up'
        }
      ]
    }
  } catch (error) {
    ElMessage.error('获取统计数据失败')
    console.error(error)
  }
}

// 初始化图表
const initCharts = () => {
  charts.caseTrend = echarts.init(caseTrendChartRef.value)
  charts.caseType = echarts.init(caseTypeChartRef.value)
  charts.fee = echarts.init(feeChartRef.value)
  charts.performance = echarts.init(performanceChartRef.value)
  charts.winRate = echarts.init(winRateChartRef.value)
  charts.collectionRate = echarts.init(collectionRateChartRef.value)

  updateAllCharts()
}

// 更新所有图表
const updateAllCharts = () => {
  updateCaseTrendChart()
  updateCaseTypeChart()
  updateFeeChart()
  updatePerformanceChart()
  updateWinRateChart()
  updateCollectionRateChart()
}

// 案件数量趋势图
const updateCaseTrendChart = () => {
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['新增案件', '结案案件']
    },
    xAxis: {
      type: 'category',
      data: ['1月', '2月', '3月', '4月', '5月', '6月']
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '新增案件',
        type: 'line',
        data: [120, 132, 101, 134, 90, 230],
        smooth: true,
        itemStyle: { color: '#1890ff' }
      },
      {
        name: '结案案件',
        type: 'line',
        data: [220, 182, 191, 234, 290, 330],
        smooth: true,
        itemStyle: { color: '#52c41a' }
      }
    ]
  }
  charts.caseTrend.setOption(option)
}

// 案件类型分布图
const updateCaseTypeChart = () => {
  const data = [
    { value: 335, name: '民事' },
    { value: 310, name: '商事' },
    { value: 234, name: '刑事' },
    { value: 135, name: '行政' },
    { value: 148, name: '仲裁' },
    { value: 72, name: '非诉' }
  ]

  const option = caseTypePie.value ? {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        name: '案件类型',
        type: 'pie',
        radius: '50%',
        data: data,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  } : {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    xAxis: {
      type: 'category',
      data: data.map(item => item.name)
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '案件数量',
        type: 'bar',
        data: data.map(item => item.value),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#83bff6' },
            { offset: 0.5, color: '#188df0' },
            { offset: 1, color: '#188df0' }
          ])
        }
      }
    ]
  }
  charts.caseType.setOption(option)
}

// 收费统计图
const updateFeeChart = () => {
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['已收', '待收']
    },
    xAxis: {
      type: 'category',
      data: ['1月', '2月', '3月', '4月', '5月', '6月']
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        formatter: '{value}万'
      }
    },
    series: [
      {
        name: '已收',
        type: 'bar',
        stack: 'total',
        data: [320, 302, 301, 334, 390, 330],
        itemStyle: { color: '#67c23a' }
      },
      {
        name: '待收',
        type: 'bar',
        stack: 'total',
        data: [120, 132, 101, 134, 90, 230],
        itemStyle: { color: '#e6a23c' }
      }
    ]
  }
  charts.fee.setOption(option)
}

// 律师业绩排名图
const updatePerformanceChart = () => {
  const data = [
    { name: '张律师', value: 156 },
    { name: '李律师', value: 142 },
    { name: '王律师', value: 128 },
    { name: '赵律师', value: 98 },
    { name: '钱律师', value: 87 },
    { name: '孙律师', value: 76 },
    { name: '周律师', value: 65 },
    { name: '吴律师', value: 54 }
  ]

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'value'
    },
    yAxis: {
      type: 'category',
      data: data.map(item => item.name)
    },
    series: [
      {
        name: '案件数',
        type: 'bar',
        data: data.map(item => item.value),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#1890ff' },
            { offset: 1, color: '#70a1ff' }
          ])
        },
        label: {
          show: true,
          position: 'right'
        }
      }
    ]
  }
  charts.performance.setOption(option)
}

// 案件胜诉率图
const updateWinRateChart = () => {
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c}% ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        name: '胜诉率',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        label: {
          show: true,
          formatter: '{b}: {d}%'
        },
        data: [
          { value: 65, name: '胜诉', itemStyle: { color: '#52c41a' } },
          { value: 20, name: '部分胜诉', itemStyle: { color: '#1890ff' } },
          { value: 10, name: '败诉', itemStyle: { color: '#f56c6c' } },
          { value: 5, name: '其他', itemStyle: { color: '#909399' } }
        ]
      }
    ]
  }
  charts.winRate.setOption(option)
}

// 收款率统计图
const updateCollectionRateChart = () => {
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['应收', '实收', '收款率']
    },
    xAxis: {
      type: 'category',
      data: ['1月', '2月', '3月', '4月', '5月', '6月']
    },
    yAxis: [
      {
        type: 'value',
        name: '金额(万)',
        position: 'left'
      },
      {
        type: 'value',
        name: '收款率(%)',
        position: 'right',
        axisLabel: {
          formatter: '{value}%'
        }
      }
    ],
    series: [
      {
        name: '应收',
        type: 'bar',
        data: [400, 432, 401, 434, 490, 560],
        itemStyle: { color: '#909399' }
      },
      {
        name: '实收',
        type: 'bar',
        data: [320, 302, 301, 334, 390, 430],
        itemStyle: { color: '#67c23a' }
      },
      {
        name: '收款率',
        type: 'line',
        yAxisIndex: 1,
        data: [80, 70, 75, 77, 80, 77],
        itemStyle: { color: '#faad14' }
      }
    ]
  }
  charts.collectionRate.setOption(option)
}

// 日期范围变化
const handleDateChange = (dates) => {
  // 根据日期范围重新加载数据
  if (dates && dates.length === 2) {
    dateRange.value = dates
    loadStatisticsData()
  }
}

// 导出Excel
const handleExportExcel = async () => {
  try {
    ElMessage.info('正在生成Excel报表...')

    const response = await request({
      url: '/statistics/export/excel',
      method: 'post',
      data: {
        startDate: dateRange.value?.[0],
        endDate: dateRange.value?.[1]
      },
      responseType: 'blob'
    })

    // 创建下载链接
    const url = window.URL.createObjectURL(new Blob([response]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `案件统计报表_${new Date().getTime()}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)

    ElMessage.success('Excel报表导出成功')
  } catch (error) {
    console.error('导出Excel失败:', error)
    ElMessage.error('导出Excel失败')
  }
}

// 导出PDF
const handleExportPDF = async () => {
  try {
    ElMessage.info('正在生成PDF报表...')

    const response = await request({
      url: '/statistics/export/pdf',
      method: 'post',
      data: {
        startDate: dateRange.value?.[0],
        endDate: dateRange.value?.[1]
      },
      responseType: 'blob'
    })

    // 创建下载链接
    const url = window.URL.createObjectURL(new Blob([response]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `案件统计报表_${new Date().getTime()}.pdf`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)

    ElMessage.success('PDF报表导出成功')
  } catch (error) {
    console.error('导出PDF失败:', error)
    ElMessage.error('导出PDF失败')
  }
}

// 响应式处理
const handleResize = () => {
  Object.values(charts).forEach(chart => chart && chart.resize())
}

onMounted(async () => {
  await fetchStatsCards()
  nextTick(() => {
    initCharts()
    window.addEventListener('resize', handleResize)
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  Object.values(charts).forEach(chart => chart && chart.dispose())
})
</script>

<style scoped lang="scss">
.statistics {
  .stats-cards {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 20px;
    margin-bottom: 20px;

    .stat-card {
      display: flex;
      align-items: center;
      gap: 20px;
      padding: 20px;
      background-color: #fff;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
      transition: transform 0.3s;

      &:hover {
        transform: translateY(-4px);
      }

      .stat-icon {
        width: 60px;
        height: 60px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 28px;
      }

      .stat-content {
        flex: 1;

        .stat-value {
          font-size: 28px;
          font-weight: bold;
          color: #333;
          margin-bottom: 5px;
        }

        .stat-label {
          font-size: 14px;
          color: #666;
          margin-bottom: 8px;
        }

        .stat-trend {
          display: flex;
          align-items: center;
          gap: 4px;
          font-size: 12px;

          &.trend-up {
            color: #52c41a;
          }

          &.trend-down {
            color: #f56c6c;
          }
        }
      }
    }
  }

  .charts-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 20px;

    .chart-card {
      background-color: #fff;
      padding: 20px;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

      &.full-width {
        grid-column: 1 / -1;
      }

      .chart-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 20px;

        h4 {
          margin: 0;
          font-size: 16px;
          font-weight: 500;
          color: #333;
          border-left: 4px solid #1890ff;
          padding-left: 12px;
        }
      }

      .chart-container {
        height: 300px;

        &.wide {
          height: 400px;
        }
      }
    }
  }
}
</style>
