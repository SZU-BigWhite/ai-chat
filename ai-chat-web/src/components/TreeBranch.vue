<script setup lang="ts">
import { ref } from 'vue'
import type { KnowledgeNode } from '../types'
import AppIcon from './AppIcon.vue'
defineProps<{ node: KnowledgeNode; selectedId: string; depth?: number }>()
const emit = defineEmits<{ select: [id: string] }>()
// 递归组件只负责展示与选择；知识树的业务修改集中在 TreeView。
const collapsed = ref(false)
</script>
<template>
  <li class="tree-branch">
    <div class="branch-line">
      <button
        v-if="node.children.length"
        class="collapse-node"
        :aria-label="`${collapsed ? '展开' : '折叠'} ${node.name}`"
        :aria-expanded="!collapsed"
        @click="collapsed = !collapsed"
      >
        <AppIcon :name="collapsed ? 'ChevronRight' : 'ChevronDown'" :size="14" />
      </button>
      <span v-else class="leaf-joint"></span>
      <button
        class="node-card"
        :class="{ selected: selectedId === node.id, root: !depth }"
        :aria-label="`选择节点 ${node.name}`"
        :aria-pressed="selectedId === node.id"
        @click="emit('select', node.id)"
      >
        <span class="node-symbol">
          <AppIcon
            :name="!depth ? 'Sprout' : node.children.length ? 'FolderTree' : 'Circle'"
            :size="!depth ? 20 : 16"
          />
        </span>
        <span class="node-copy">
          <strong>{{ node.name }}</strong>
          <small>
            {{
              !depth
                ? '根主题'
                : node.children.length
                  ? `${node.children.length} 个子节点`
                  : '叶子节点'
            }}
          </small>
        </span>
        <span v-if="node.status === 'CONFIRMED'" class="node-status confirmed" title="已确认">
          <AppIcon name="Check" :size="14" />
          <span class="sr-only">已确认</span>
        </span>
        <span
          v-else-if="node.status === 'REVIEW'"
          class="node-status review"
          title="父级范围已变更，待复核"
        >
          <AppIcon name="AlertCircle" :size="15" />
          <span class="sr-only">待复核</span>
        </span>
        <AppIcon v-else name="ChevronRight" :size="14" class="node-arrow" />
      </button>
    </div>
    <ul v-if="node.children.length && !collapsed" class="branch-children">
      <TreeBranch
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        :selected-id="selectedId"
        :depth="(depth || 0) + 1"
        @select="emit('select', $event)"
      />
    </ul>
  </li>
</template>
