<template>
  <div class="admin">
    <PageHeader title="行政OA" />

    <el-tabs v-model="activeTab" type="card" class="admin-tabs">
      <!-- 通知公告 -->
      <el-tab-pane label="通知公告" name="notices">
        <div class="tab-content">
          <div class="toolbar">
            <el-button type="primary" @click="handleCreateNotice">
              <el-icon><Plus /></el-icon>
              发布通知
            </el-button>
            <el-select v-model="noticeType" placeholder="通知类型" clearable>
              <el-option label="全部" value="" />
              <el-option label="重要通知" value="important" />
              <el-option label="一般通知" value="normal" />
              <el-option label="系统公告" value="system" />
            </el-select>
          </div>

          <el-table :data="noticeList" border>
            <el-table-column prop="title" label="标题" width="300">
              <template #default="{ row }">
                <el-link type="primary" @click="handleViewNotice(row)">
                  <span v-if="row.top" class="top-tag">[置顶]</span>
                  {{ row.title }}
                </el-link>
              </template>
            </el-table-column>
            <el-table-column prop="type" label="类型" width="100">
              <template #default="{ row }">
                <el-tag :type="getNoticeTagType(row.type)">
                  {{ row.type }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="publisher" label="发布人" width="100" />
            <el-table-column prop="publishTime" label="发布时间" width="160" sortable />
            <el-table-column prop="readCount" label="已读/总数" width="100">
              <template #default="{ row }">
                {{ row.readCount }}/{{ row.totalCount }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button link type="primary" size="small">查看</el-button>
                <el-button link type="danger" size="small" @click="handleDeleteNotice(row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 会议室管理 -->
      <el-tab-pane label="会议室管理" name="meeting-rooms">
        <div class="tab-content">
          <div class="toolbar">
            <el-button type="primary" @click="handleBookRoom">
              <el-icon><Plus /></el-icon>
              预约会议室
            </el-button>
            <el-date-picker
              v-model="meetingDate"
              type="date"
              placeholder="选择日期"
              value-format="YYYY-MM-DD"
            />
          </div>

          <div class="room-grid">
            <div
              v-for="room in meetingRooms"
              :key="room.id"
              class="room-card"
              :class="{ 'booked': room.isBooked }"
            >
              <div class="room-header">
                <h4>{{ room.name }}</h4>
                <el-tag :type="room.isBooked ? 'danger' : 'success'" size="small">
                  {{ room.isBooked ? '已占用' : '空闲' }}
                </el-tag>
              </div>
              <div class="room-info">
                <div class="info-item">
                  <span class="label">容量：</span>
                  <span>{{ room.capacity }}人</span>
                </div>
                <div class="info-item">
                  <span class="label">设备：</span>
                  <span>{{ room.facilities }}</span>
                </div>
                <div v-if="room.currentBooking" class="current-booking">
                  <span class="label">当前预约：</span>
                  <span>{{ room.currentBooking.user }} ({{ room.currentBooking.time }})</span>
                </div>
              </div>
              <div class="room-actions">
                <el-button type="primary" size="small" @click="handleBookRoom(room)">
                  预约
                </el-button>
                <el-button size="small" @click="handleViewSchedule(room)">
                  日程
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 考勤管理 -->
      <el-tab-pane label="考勤管理" name="attendance">
        <div class="tab-content">
          <div class="toolbar">
            <el-select v-model="attendanceMonth" placeholder="选择月份" style="width: 150px">
              <el-option
                v-for="month in recentMonths"
                :key="month.value"
                :label="month.label"
                :value="month.value"
              />
            </el-select>
            <el-button @click="handleExportAttendance">
              <el-icon><Download /></el-icon>
              导出报表
            </el-button>
          </div>

          <el-table :data="attendanceList" border>
            <el-table-column prop="userName" label="申请人" width="100" />
            <el-table-column prop="attendanceType" label="类型" width="100">
              <template #default="{ row }">
                <el-tag :type="getAttendanceTypeTag(row.attendanceType)">
                  {{ formatAttendanceType(row.attendanceType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="startDate" label="开始日期" width="110" />
            <el-table-column prop="endDate" label="结束日期" width="110" />
            <el-table-column prop="duration" label="时长" width="80">
              <template #default="{ row }">
                {{ row.duration }} {{ row.durationUnit }}
              </template>
            </el-table-column>
            <el-table-column prop="reason" label="事由" show-overflow-tooltip />
            <el-table-column prop="approvalStatus" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusTag(row.approvalStatus)">
                  {{ formatStatus(row.approvalStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="approverName" label="审批人" width="100" />
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleViewAttendance(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 用户管理 -->
      <el-tab-pane label="用户管理" name="users">
        <div class="tab-content">
          <Suspense>
            <UserManagement />
          </Suspense>
        </div>
      </el-tab-pane>

      <!-- 角色管理 -->
      <el-tab-pane label="角色管理" name="roles">
        <div class="tab-content">
          <Suspense>
            <RoleManagement />
          </Suspense>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 发布通知对话框 -->
    <el-dialog v-model="noticeDialogVisible" title="发布通知" width="700px">
      <el-form :model="noticeForm" label-width="100px">
        <el-form-item label="通知标题" required>
          <el-input v-model="noticeForm.title" placeholder="请输入通知标题" />
        </el-form-item>

        <el-form-item label="通知类型" required>
          <el-select v-model="noticeForm.type" placeholder="请选择通知类型" style="width: 100%">
            <el-option label="一般通知" value="normal" />
            <el-option label="重要通知" value="important" />
            <el-option label="系统公告" value="system" />
          </el-select>
        </el-form-item>

        <el-form-item label="通知内容" required>
          <el-input
            v-model="noticeForm.content"
            type="textarea"
            :rows="8"
            placeholder="请输入通知内容"
          />
        </el-form-item>

        <el-form-item label="置顶">
          <el-switch v-model="noticeForm.top" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="noticeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitNotice">发布</el-button>
      </template>
    </el-dialog>

    <!-- 会议室预约对话框 -->
    <el-dialog v-model="bookingDialogVisible" title="预约会议室" width="600px">
      <el-form :model="bookingForm" label-width="100px">
        <el-form-item label="会议室">
          <el-input v-model="bookingForm.roomName" disabled />
        </el-form-item>

        <el-form-item label="预约日期" required>
          <el-date-picker
            v-model="bookingForm.date"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="开始时间" required>
          <el-time-picker
            v-model="bookingForm.startTime"
            placeholder="选择开始时间"
            value-format="HH:mm"
            format="HH:mm"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="结束时间" required>
          <el-time-picker
            v-model="bookingForm.endTime"
            placeholder="选择结束时间"
            value-format="HH:mm"
            format="HH:mm"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="会议主题" required>
          <el-input v-model="bookingForm.purpose" placeholder="请输入会议主题" />
        </el-form-item>

        <el-form-item label="参会人员">
          <el-input
            v-model="bookingForm.attendees"
            type="textarea"
            :rows="3"
            placeholder="请输入参会人员，用逗号分隔"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="bookingDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitBooking">预约</el-button>
      </template>
    </el-dialog>

    <!-- 日程查看对话框 -->
    <el-dialog v-model="scheduleDialogVisible" :title="`${bookingForm.roomName} - 日程安排`" width="600px">
      <el-timeline v-if="scheduleList.length > 0">
        <el-timeline-item
          v-for="item in scheduleList"
          :key="item.id"
          :timestamp="`${item.date} ${item.startTime} - ${item.endTime}`"
          placement="top"
        >
          <el-card>
            <h4>{{ item.purpose }}</h4>
            <p style="color: #666; margin: 8px 0;">
              预约人：{{ item.userName }} | 参会人员：{{ item.attendees || '无' }}
            </p>
            <p v-if="item.remark" style="color: #999; margin: 0;">备注：{{ item.remark }}</p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无日程安排" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Download } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import request from '@/utils/request'
import { defineAsyncComponent } from 'vue'

// 异步加载系统管理组件
const UserManagement = defineAsyncComponent(() => import('@/views/system/user/index.vue'))
const RoleManagement = defineAsyncComponent(() => import('@/views/system/role/index.vue'))

const activeTab = ref('notices')
const noticeType = ref('')
const meetingDate = ref(new Date().toISOString().split('T')[0])
const attendanceMonth = ref('2024-04')

const noticeList = ref([])
const meetingRooms = ref([])
const loading = ref(false)

// ==================== 通知对话框 ====================
const noticeDialogVisible = ref(false)
const noticeForm = ref({
  title: '',
  content: '',
  type: 'normal',
  top: false
})

// ==================== 会议室预约对话框 ====================
const bookingDialogVisible = ref(false)
const bookingForm = ref({
  roomId: null,
  roomName: '',
  date: new Date().toISOString().split('T')[0],
  startTime: '',
  endTime: '',
  purpose: '',
  attendees: []
})

// ==================== 日程对话框 ====================
const scheduleDialogVisible = ref(false)
const scheduleList = ref([])

// 加载通知公告
const loadNotices = async () => {
  try {
    const { data } = await request({
      url: '/announcement',
      method: 'get'
    })
    // 后端返回Spring Page结构，数据在content字段
    noticeList.value = data?.content || []
  } catch (error) {
    console.error('加载通知公告失败:', error)
  }
}

// 加载会议室列表
const loadMeetingRooms = async () => {
  try {
    const { data } = await request({
      url: '/meeting-room',
      method: 'get'
    })
    meetingRooms.value = data || []
  } catch (error) {
    console.error('加载会议室失败:', error)
  }
}

onMounted(() => {
  loadNotices()
  loadMeetingRooms()
  loadAttendance()
})

const recentMonths = ref([])
const attendanceList = ref([])

// 加载考勤记录
const loadAttendance = async () => {
  try {
    const { data } = await request({
      url: '/attendance/my',
      method: 'get'
    })
    // 后端返回AttendanceRecord数组
    attendanceList.value = data || []
  } catch (error) {
    console.error('加载考勤记录失败:', error)
  }
}

const getNoticeTagType = (type) => {
  const typeMap = {
    '重要通知': 'danger',
    '一般通知': 'primary',
    '系统公告': 'warning'
  }
  return typeMap[type] || ''
}

const getAttendanceTypeTag = (type) => {
  const typeMap = {
    'LEAVE': 'warning',
    'BUSINESS_TRIP': 'info',
    'OVERTIME': 'success'
  }
  return typeMap[type] || ''
}

const getStatusTag = (status) => {
  const typeMap = {
    'PENDING': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'danger'
  }
  return typeMap[status] || ''
}

const formatAttendanceType = (type) => {
  const typeMap = {
    'LEAVE': '请假',
    'BUSINESS_TRIP': '出差',
    'OVERTIME': '加班'
  }
  return typeMap[type] || type
}

const formatStatus = (status) => {
  const statusMap = {
    'PENDING': '待审批',
    'APPROVED': '已通过',
    'REJECTED': '已拒绝'
  }
  return statusMap[status] || status
}

const handleCreateNotice = () => {
  noticeForm.value = {
    title: '',
    content: '',
    type: 'normal',
    top: false
  }
  noticeDialogVisible.value = true
}

const handleSubmitNotice = async () => {
  if (!noticeForm.value.title) {
    ElMessage.warning('请输入通知标题')
    return
  }
  if (!noticeForm.value.content) {
    ElMessage.warning('请输入通知内容')
    return
  }

  try {
    await request({
      url: '/announcement',
      method: 'post',
      data: noticeForm.value
    })
    ElMessage.success('通知发布成功')
    noticeDialogVisible.value = false
    loadNotices()
  } catch (error) {
    console.error('发布通知失败:', error)
    ElMessage.error('发布失败')
  }
}

const handleViewNotice = async (notice) => {
  try {
    // 标记为已读
    await request({
      url: `/announcement/${notice.id}/read`,
      method: 'post'
    })
    // 显示通知详情
    ElMessageBox.alert(
      `<div style="text-align: left;">
        <h3>${notice.title}</h3>
        <p style="color: #666; margin: 10px 0;">
          类型：${notice.type} | 发布人：${notice.publisher} | 发布时间：${notice.publishTime}
        </p>
        <div style="line-height: 1.6;">
          ${notice.content.replace(/\n/g, '<br>')}
        </div>
      </div>`,
      '通知详情',
      {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '关闭'
      }
    )
    // 刷新列表
    loadNotices()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('查看通知失败:', error)
      ElMessage.error('操作失败')
    }
  }
}

const handleDeleteNotice = async (notice) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除通知"${notice.title}"吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await request({
      url: `/announcement/${notice.id}`,
      method: 'delete'
    })
    // 从列表中移除
    const index = noticeList.value.findIndex(n => n.id === notice.id)
    if (index > -1) {
      noticeList.value.splice(index, 1)
    }
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleBookRoom = (room) => {
  if (room && room.id) {
    bookingForm.value = {
      roomId: room.id,
      roomName: room.name,
      date: new Date().toISOString().split('T')[0],
      startTime: '',
      endTime: '',
      purpose: '',
      attendees: []
    }
  } else {
    bookingForm.value = {
      roomId: null,
      roomName: '',
      date: new Date().toISOString().split('T')[0],
      startTime: '',
      endTime: '',
      purpose: '',
      attendees: []
    }
  }
  bookingDialogVisible.value = true
}

const handleSubmitBooking = async () => {
  if (!bookingForm.value.roomId) {
    ElMessage.warning('请选择会议室')
    return
  }
  if (!bookingForm.value.date) {
    ElMessage.warning('请选择日期')
    return
  }
  if (!bookingForm.value.startTime) {
    ElMessage.warning('请选择开始时间')
    return
  }
  if (!bookingForm.value.endTime) {
    ElMessage.warning('请选择结束时间')
    return
  }
  if (!bookingForm.value.purpose) {
    ElMessage.warning('请输入会议主题')
    return
  }

  try {
    await request({
      url: '/meeting-room/book',
      method: 'post',
      data: bookingForm.value
    })
    ElMessage.success('会议室预约成功')
    bookingDialogVisible.value = false
    loadMeetingRooms()
  } catch (error) {
    console.error('预约会议室失败:', error)
    ElMessage.error('预约失败')
  }
}

const handleViewSchedule = async (room) => {
  try {
    const { data } = await request({
      url: `/meeting-room/${room.id}/schedule`,
      method: 'get',
      params: { date: meetingDate.value }
    })
    scheduleList.value = data || []
    bookingForm.value.roomName = room.name
    scheduleDialogVisible.value = true
  } catch (error) {
    console.error('获取日程失败:', error)
    ElMessage.error('获取日程失败')
  }
}

const handleExportAttendance = async () => {
  if (!attendanceMonth.value) {
    ElMessage.warning('请选择月份')
    return
  }

  try {
    const response = await request({
      url: '/attendance/export',
      method: 'get',
      params: { month: attendanceMonth.value },
      responseType: 'blob'
    })

    // 创建下载链接
    const blob = new Blob([response], { type: 'application/vnd.ms-excel' })
    const link = document.createElement('a')
    link.href = window.URL.createObjectURL(blob)
    link.download = `考勤报表_${attendanceMonth.value}.xlsx`
    link.click()
    window.URL.revokeObjectURL(link.href)

    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出考勤报表失败:', error)
    ElMessage.error('导出失败')
  }
}

const handleViewAttendance = (row) => {
  ElMessageBox.alert(
    `类型：${formatAttendanceType(row.attendanceType)}\n开始日期：${row.startDate}\n结束日期：${row.endDate}\n时长：${row.duration} ${row.durationUnit}\n事由：${row.reason || '无'}\n状态：${formatStatus(row.approvalStatus)}\n审批人：${row.approverName || '未审批'}\n审批意见：${row.approvalComment || '无'}`,
    '考勤详情',
    {
      confirmButtonText: '关闭'
    }
  )
}
</script>

<style scoped lang="scss">
.admin {
  .admin-tabs {
    margin-top: 20px;
    background-color: #fff;
    padding: 20px;
    border-radius: 4px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }

  .tab-content {
    min-height: 400px;

    .toolbar {
      display: flex;
      gap: 10px;
      margin-bottom: 20px;
      flex-wrap: wrap;
    }

    .top-tag {
      color: #f56c6c;
      margin-right: 5px;
    }

    .room-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 20px;

      .room-card {
        border: 1px solid #e4e7ed;
        border-radius: 8px;
        padding: 20px;
        transition: all 0.3s;

        &:hover {
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
        }

        &.booked {
          background-color: #fef0f0;
          border-color: #fbc4c4;
        }

        .room-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 15px;

          h4 {
            margin: 0;
            font-size: 16px;
            color: #333;
          }
        }

        .room-info {
          margin-bottom: 15px;

          .info-item {
            display: flex;
            margin-bottom: 8px;
            font-size: 14px;
            color: #666;

            .label {
              min-width: 60px;
              color: #999;
            }
          }

          .current-booking {
            margin-top: 10px;
            padding-top: 10px;
            border-top: 1px dashed #e4e7ed;
            font-size: 13px;
            color: #f56c6c;
          }
        }

        .room-actions {
          display: flex;
          gap: 10px;
        }
      }
    }
  }
}
</style>
