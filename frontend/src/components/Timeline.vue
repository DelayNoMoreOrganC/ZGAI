<template>
  <div class="custom-timeline">
    <el-timeline>
      <el-timeline-item
        v-for="(item, index) in items"
        :key="index"
        :timestamp="item.timestamp"
        :color="item.color"
        :size="item.size || 'normal'"
        :icon="item.icon"
      >
        <div class="timeline-content">
          <div class="timeline-header">
            <span class="title">{{ item.title }}</span>
            <el-tag v-if="item.status" :type="getStatusType(item.status)" size="small">
              {{ item.status }}
            </el-tag>
          </div>
          <div v-if="item.content" class="timeline-body">
            {{ item.content }}
          </div>
          <div v-if="item.attachments && item.attachments.length > 0" class="timeline-attachments">
            <el-tag
              v-for="(file, fileIndex) in item.attachments"
              :key="fileIndex"
              size="small"
              class="attachment-tag"
            >
              📎 {{ file.name }}
            </el-tag>
          </div>
          <div v-if="item.actions" class="timeline-actions">
            <slot name="actions" :item="item" :index="index"></slot>
          </div>
        </div>
      </el-timeline-item>
    </el-timeline>
  </div>
</template>

<script setup>
defineProps({
  items: {
    type: Array,
    required: true
  }
})

const getStatusType = (status) => {
  const typeMap = {
    success: 'success',
    completed: 'success',
    pending: 'warning',
    processing: 'primary',
    error: 'danger',
    failed: 'danger'
  }
  return typeMap[status] || 'info'
}
</script>

<style scoped lang="scss">
.custom-timeline {
  padding: 20px;

  .timeline-content {
    .timeline-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;

      .title {
        font-weight: 500;
        font-size: 14px;
        color: #333;
      }
    }

    .timeline-body {
      color: #666;
      font-size: 14px;
      line-height: 1.6;
      margin-bottom: 8px;
    }

    .timeline-attachments {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin-top: 8px;

      .attachment-tag {
        cursor: pointer;

        &:hover {
          opacity: 0.8;
        }
      }
    }

    .timeline-actions {
      margin-top: 8px;
      display: flex;
      gap: 8px;
    }
  }
}
</style>
