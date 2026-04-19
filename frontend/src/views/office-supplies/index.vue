<template>
  <div class="office-supplies-page">
    <PageHeader title="办公用品管理" />
    <el-card>
      <el-table :data="supplies" style="width: 100%">
        <el-table-column prop="name" label="用品名称" />
        <el-table-column prop="category" label="类别" />
        <el-table-column prop="quantity" label="库存数量" />
        <el-table-column prop="unit" label="单位" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.quantity > 10 ? 'success' : 'warning'">
              {{ row.quantity > 10 ? '充足' : '不足' }}
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

const supplies = ref([])

onMounted(async () => {
  try {
    const { data } = await request({
      url: '/office-supplies',
      method: 'get'
    })
    supplies.value = data || []
  } catch (error) {
    console.error('加载失败', error)
  }
})
</script>
