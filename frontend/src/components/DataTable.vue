<template>
  <div class="data-table">
    <el-table
      :data="tableData"
      v-loading="loading"
      element-loading-text="加载中..."
      element-loading-background="rgba(255, 255, 255, 0.9)"
      :element-loading-spinner="loadingSpinner"
      border
      stripe
      :height="height"
      :empty-text="emptyText"
      @selection-change="handleSelectionChange"
    >
      <!-- 空状态插槽 -->
      <template #empty>
        <div class="table-empty">
          <el-empty :description="emptyText" :image-size="emptyImageSize">
            <template v-if="showEmptyAction" #default>
              <el-button type="primary" @click="handleEmptyAction">{{ emptyActionText }}</el-button>
            </template>
          </el-empty>
        </div>
      </template>

      <el-table-column v-if="showSelection" type="selection" width="55" />
      <el-table-column
        v-if="showIndex"
        type="index"
        label="序号"
        width="60"
        :index="indexMethod"
      />
      <slot></slot>
      <el-table-column v-if="showActions" label="操作" :width="actionWidth" fixed="right">
        <template #default="{ row }">
          <slot name="actions" :row="row"></slot>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="showPagination"
      :current-page="currentPage"
      :page-size="pageSize"
      :page-sizes="[10, 20, 50, 100]"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      class="pagination"
      :background="true"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  tableData: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  total: {
    type: Number,
    default: 0
  },
  currentPage: {
    type: Number,
    default: 1
  },
  pageSize: {
    type: Number,
    default: 20
  },
  height: {
    type: [String, Number],
    default: 'auto'
  },
  showSelection: {
    type: Boolean,
    default: false
  },
  showIndex: {
    type: Boolean,
    default: true
  },
  showActions: {
    type: Boolean,
    default: true
  },
  actionWidth: {
    type: Number,
    default: 200
  },
  showPagination: {
    type: Boolean,
    default: true
  },
  loadingText: {
    type: String,
    default: '加载中...'
  },
  loadingSpinner: {
    type: String,
    default: 'el-icon-loading'
  },
  emptyText: {
    type: String,
    default: '暂无数据'
  },
  emptyImageSize: {
    type: Number,
    default: 100
  },
  showEmptyAction: {
    type: Boolean,
    default: false
  },
  emptyActionText: {
    type: String,
    default: '立即创建'
  }
})

const emit = defineEmits(['selection-change', 'size-change', 'current-change', 'empty-action'])

const indexMethod = (index) => {
  return (props.currentPage - 1) * props.pageSize + index + 1
}

const handleSelectionChange = (selection) => {
  emit('selection-change', selection)
}

const handleSizeChange = (size) => {
  emit('size-change', size)
}

const handleCurrentChange = (page) => {
  emit('current-change', page)
}

const handleEmptyAction = () => {
  emit('empty-action')
}
</script>

<style scoped lang="scss">
.data-table {
  background-color: #fff;
  padding: 20px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

  // 增强的loading状态
  :deep(.el-loading-mask) {
    border-radius: 4px;
    background-color: rgba(255, 255, 255, 0.9);
    backdrop-filter: blur(2px);

    .el-loading-spinner {
      .el-loading-text {
        color: #409eff;
        font-size: 14px;
        margin-top: 10px;
      }
    }
  }

  // 空状态样式优化
  .table-empty {
    padding: 40px 0;

    :deep(.el-empty) {
      .el-empty__description {
        color: #909399;
        font-size: 14px;
        margin-top: 16px;
      }
    }
  }

  // 分页样式优化
  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
    padding: 10px 0;

    :deep(.el-pagination) {
      .el-pagination__total {
        font-weight: normal;
        color: #606266;
      }

      .btn-prev,
      .btn-next,
      .el-pager li {
        background-color: #fff;
        border: 1px solid #dcdfe6;
        color: #606266;

        &:hover {
          color: #409eff;
          border-color: #409eff;
        }

        &.active {
          background-color: #409eff;
          border-color: #409eff;
          color: #fff;
        }
      }
    }
  }

  // 表格样式优化
  :deep(.el-table) {
    .el-table__empty-block {
      min-height: 200px;
      display: flex;
      align-items: center;
      justify-content: center;

      .el-table__empty-text {
        color: #909399;
        font-size: 14px;
      }
    }

    // 斑马纹优化
    &.el-table--striped {
      .el-table__body tr.el-table__row--striped td {
        background-color: #fafafa;
      }
    }

    // 边框优化
    &.el-table--border {
      .el-table__cell {
        border-color: #ebeef5;
      }
    }
  }
}
</style>
