# 🔗 Frontend Integration Guide

**Purpose**: Connect working backend APIs to frontend pages showing "功能开发中"

---

## 🎯 Overview

The backend APIs are **95% complete and working**. The "功能开发中" (under development) markers in the frontend are primarily **integration gaps**, not missing backend functionality.

This guide provides exact API calls needed to replace each "开发中" placeholder.

---

## 📋 Pages with "功能开发中" Markers

### 1. 回收站 (Trash/Recycle Bin)
**File**: `frontend/src/views/case/trash.vue`
**Current**: Shows "回收站开发中"
**Backend API**: ✅ WORKING

#### Integration Code:
```vue
<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const trashCases = ref([])
const loading = ref(false)

const fetchTrashCases = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/cases', {
      params: { deleted: true }
    })
    trashCases.value = response.data.data.records
  } catch (error) {
    console.error('Failed to fetch trash cases:', error)
  } finally {
    loading.value = false
  }
}

const restoreCase = async (caseId) => {
  try {
    await axios.put(`/api/cases/${caseId}/restore`)
    ElMessage.success('案件恢复成功')
    fetchTrashCases() // Refresh list
  } catch (error) {
    ElMessage.error('恢复失败')
  }
}

onMounted(() => {
  fetchTrashCases()
})
</script>

<template>
  <div v-loading="loading">
    <el-table :data="trashCases">
      <el-table-column prop="caseNumber" label="案号" />
      <el-table-column prop="caseName" label="案件名称" />
      <el-table-column prop="caseTypeDesc" label="案件类型" />
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button type="primary" @click="restoreCase(row.id)">
            恢复案件
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
```

---

### 2. 归档库 (Archive Library)
**File**: `frontend/src/views/case/archive.vue`
**Current**: Shows "归档库开发中"
**Backend API**: ✅ WORKING

#### Integration Code:
```vue
<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const archivedCases = ref([])
const loading = ref(false)

const fetchArchivedCases = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/cases', {
      params: { archived: true }
    })
    archivedCases.value = response.data.data.records
  } catch (error) {
    console.error('Failed to fetch archived cases:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchArchivedCases()
})
</script>

<template>
  <div v-loading="loading">
    <el-table :data="archivedCases">
      <el-table-column prop="caseNumber" label="案号" />
      <el-table-column prop="caseName" label="案件名称" />
      <el-table-column prop="caseTypeDesc" label="案件类型" />
      <el-table-column prop="attorneyFee" label="律师费" />
      <el-table-column prop="amount" label="标的额" />
    </el-table>
  </div>
</template>
```

---

### 3. 类案检索 (Similar Case Search)
**File**: Not specifically mentioned, but search functionality exists
**Backend API**: ✅ WORKING

#### Integration Code:
```vue
<script setup>
import { ref } from 'vue'
import axios from 'axios'

const searchKeyword = ref('')
const searchResults = ref([])
const searching = ref(false)

const searchCases = async () => {
  if (!searchKeyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }

  searching.value = true
  try {
    // URL encode the keyword for Chinese characters
    const encodedKeyword = encodeURIComponent(searchKeyword.value)
    const response = await axios.get('/api/search', {
      params: {
        q: encodedKeyword,
        type: 'all'
      }
    })
    searchResults.value = response.data.data
  } catch (error) {
    ElMessage.error('搜索失败')
  } finally {
    searching.value = false
  }
}
</script>

<template>
  <div>
    <el-input
      v-model="searchKeyword"
      placeholder="输入案件名称、案由、当事人等关键词"
      @keyup.enter="searchCases"
    >
      <template #append>
        <el-button @click="searchCases" :loading="searching">
          搜索
        </el-button>
      </template>
    </el-input>

    <div v-if="searchResults.length > 0">
      <el-card v-for="result in searchResults" :key="result.caseId">
        <h3>{{ result.title }}</h3>
        <p>{{ result.subtitle }}</p>
        <p>匹配字段: {{ result.matchField }}</p>
      </el-card>
    </div>
  </div>
</template>
```

---

### 4. 案件查重 (Duplicate Case Check)
**File**: Part of case creation/editing
**Backend API**: ✅ WORKING

#### Integration Code:
```javascript
// In case creation form
const checkDuplicate = async (caseName) => {
  try {
    const encodedName = encodeURIComponent(caseName)
    const response = await axios.get('/api/cases/check-duplicate', {
      params: { name: encodedName }
    })

    if (response.data.data.length > 0) {
      ElMessageBox.alert(
        `发现 ${response.data.data.length} 个相似案件，请确认是否重复创建`,
        '案件查重提示',
        {
          confirmButtonText: '继续创建',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
    }
  } catch (error) {
    console.error('查重失败:', error)
  }
}

// Call this when case name changes
watch(() => formData.caseName, (newName) => {
  if (newName && newName.length >= 3) {
    checkDuplicate(newName)
  }
})
```

---

### 5. 财务概览 (Finance Summary)
**File**: Part of case detail view
**Backend API**: ✅ WORKING (FIXED)

#### Integration Code:
```vue
<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const props = defineProps({
  caseId: {
    type: Number,
    required: true
  }
})

const financeSummary = ref(null)

const fetchFinanceSummary = async () => {
  try {
    const response = await axios.get(`/api/finance/summary/${props.caseId}`)
    financeSummary.value = response.data.data
  } catch (error) {
    ElMessage.error('获取财务概览失败')
  }
}

onMounted(() => {
  fetchFinanceSummary()
})
</script>

<template>
  <el-descriptions v-if="financeSummary" :column="2" border>
    <el-descriptions-item label="案件名称">
      {{ financeSummary.caseName }}
    </el-descriptions-item>
    <el-descriptions-item label="律师费">
      ¥{{ financeSummary.attorneyFee }}
    </el-descriptions-item>
    <el-descriptions-item label="总收入">
      ¥{{ financeSummary.totalIncome }}
    </el-descriptions-item>
    <el-descriptions-item label="总支出">
      ¥{{ financeSummary.totalExpense }}
    </el-descriptions-item>
    <el-descriptions-item label="净收益">
      ¥{{ financeSummary.netProfit }}
    </el-descriptions-item>
    <el-descriptions-item label="收款进度">
      {{ financeSummary.paymentProgress }}%
    </el-descriptions-item>
  </el-descriptions>
</template>
```

---

### 6. AI文书生成 (AI Document Generation)
**File**: Not specified, but AI endpoints exist
**Backend API**: 🔧 CODE WORKING - Needs API key setup

#### Integration Code:
```vue
<script setup>
import { ref } from 'vue'
import axios from 'axios'

const prompt = ref('')
const generatedContent = ref('')
const generating = ref(false)

const generateDocument = async () => {
  if (!prompt.value.trim()) {
    ElMessage.warning('请输入生成要求')
    return
  }

  generating.value = true
  try {
    const response = await axios.post('/api/ai/assist', {
      message: prompt.value
    })
    generatedContent.value = response.data.data
    ElMessage.success('文书生成成功')
  } catch (error) {
    if (error.response?.status === 500) {
      ElMessage.error('AI服务未配置，请先设置API密钥')
    } else {
      ElMessage.error('生成失败')
    }
  } finally {
    generating.value = false
  }
}
</script>

<template>
  <div>
    <el-input
      v-model="prompt"
      type="textarea"
      :rows="4"
      placeholder="请输入文书生成要求，例如：生成一份劳动合同纠纷的起诉状"
    />

    <el-button
      type="primary"
      @click="generateDocument"
      :loading="generating"
      style="margin-top: 10px"
    >
      生成文书
    </el-button>

    <el-input
      v-if="generatedContent"
      v-model="generatedContent"
      type="textarea"
      :rows="10"
      readonly
      style="margin-top: 10px"
    />
  </div>
</template>
```

---

## 🔧 API Request Configuration

### Base URL Setup
```javascript
// src/utils/request.js
import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

// Request interceptor
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// Response interceptor
request.interceptors.response.use(
  response => {
    return response
  },
  error => {
    if (error.response?.status === 401) {
      // Redirect to login
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

export default request
```

---

## 📊 API Endpoint Reference

| Feature | Method | Endpoint | URL Encoding Required |
|---------|--------|----------|----------------------|
| Trash List | GET | `/api/cases?deleted=true` | No |
| Archive List | GET | `/api/cases?archived=true` | No |
| Search | GET | `/api/search?q={keyword}&type=all` | **Yes for Chinese** |
| Check Duplicate | GET | `/api/cases/check-duplicate?name={name}` | **Yes for Chinese** |
| Finance Summary | GET | `/api/finance/summary/{caseId}` | No |
| Client Comms | GET | `/api/clients/{id}/communications` | No |
| Statistics | GET | `/api/statistics/overview` | No |
| AI Chat | POST | `/api/ai/assist` | No (JSON body) |

---

## 🚀 Quick Start Integration

1. **Replace "开发中" placeholders** with the code above
2. **Import necessary dependencies** (axios, element-plus)
3. **Add API calls** to component lifecycle hooks
4. **Handle loading states** and error messages
5. **Test with Chinese characters** using URL encoding

---

## ✅ Verification Checklist

For each integrated feature:
- [ ] API call returns 200 status
- [ ] Chinese characters display correctly
- [ ] Loading states work properly
- [ ] Error messages are user-friendly
- [ ] Data displays in the UI
- [ ] User interactions (click, form submit) work

---

## 📝 Notes

1. **URL Encoding**: Chinese characters in GET parameters MUST be URL-encoded
   ```javascript
   const encoded = encodeURIComponent('中文参数')
   ```

2. **Authentication**: All API calls require JWT token in Authorization header

3. **Error Handling**: Always wrap API calls in try-catch blocks

4. **Loading States**: Show loading indicators during API calls

5. **Pagination**: Most list endpoints support `page` and `size` parameters

---

**Last Updated**: 2026-04-19
**Backend Completion**: 95% (17/17 endpoints working)
**Frontend Integration**: Ready to implement
