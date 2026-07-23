<template>
  <div class="case-archive-tab">
    <CaseArchiveWorkflow :case-id="caseData.id" @completed="$emit('refresh')" />
  </div>
</template>

<script setup>
import CaseArchiveWorkflow from '@/components/CaseArchiveWorkflow.vue'

defineProps({ caseData: { type: Object, required: true } })
defineEmits(['refresh'])
</script>

<style scoped>
.case-archive-tab { padding: 24px; }
@media (max-width: 760px) { .case-archive-tab { padding: 16px; } }
</style>
