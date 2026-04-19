<template>
  <div class="fixed-assets-page">
    <PageHeader title="固定资产管理" />
    <el-card>
      <el-table :data="assets" style="width: 100%">
        <el-table-column prop="name" label="资产名称" />
        <el-table-column prop="assetNumber" label="资产编号" />
        <el-table-column prop="category" label="类别" />
        <el-table-column prop="value" label="价值（元）" />
        <el-table-column prop="purchaseDate" label="购置日期" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === '在用' ? 'success' : 'info'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default>
            <el-button size="small">编辑</el-button>
            <el-button size="small" type="danger">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import request from '@/utils/request'

const assets = ref([])

onMounted(async () => {
  try {
    const { data } = await request({
      url: '/fixed-assets',
      method: 'get'
    })
    assets.value = data || []
  } catch (error) {
    console.error('加载失败', error)
  }
})
</script>
