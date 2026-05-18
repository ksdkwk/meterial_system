<template>
  <RouterView />
</template>

<script setup>
</script>

<style>
:root {
  color-scheme: light;
  --sys-bg: #ffffff;
  --sys-surface: #ffffff;
  --sys-surface-muted: #ffffff;
  --sys-text: #243547;
  --sys-text-secondary: #4b5f77;
  --sys-text-muted: #5f738e;
  --sys-nav-text: #3e5a78;
  --sys-nav-text-strong: #1f3f60;
  --sys-sidebar: #ffffff;
  --sys-sidebar-hover: #f3f7fc;
  --sys-submenu: #ffffff;
  --auth-bg: #ffffff;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: var(--sys-bg);
  color: var(--sys-text);
}

/* 业务页允许整页滚动，避免内容被全局 overflow:hidden 裁剪 */
#app:has(.dashboard-layout) {
  overflow: auto;
}

/* 认证页全屏居中容器 */
#app:has(.auth-card) {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: var(--auth-bg);
}

/* 主业务界面常用背景统一跟随系统主题 */
.dashboard-layout {
  background: var(--sys-bg) !important;
}

.sidebar {
  background: var(--sys-sidebar) !important;
}

.submenu {
  background: var(--sys-submenu) !important;
}

.menu-item:hover,
.menu-item.active,
.submenu-item:hover,
.submenu-item.active {
  background: var(--sys-sidebar-hover) !important;
}

.sidebar .menu-item,
.sidebar .submenu-item {
  color: var(--sys-nav-text) !important;
  font-weight: 600;
}

.sidebar .logo {
  color: var(--sys-nav-text-strong) !important;
  font-weight: 800 !important;
  font-size: 13px !important;
  line-height: 1.3;
  letter-spacing: 0;
}

.sidebar .menu-item.active,
.sidebar .submenu-item.active,
.sidebar .menu-item:hover,
.sidebar .submenu-item:hover {
  color: var(--sys-nav-text-strong) !important;
}

.crumb,
.label,
.welcome p,
.sub,
.row,
.footer,
.forgot-page-name,
.forgot-links a {
  color: var(--sys-text-muted) !important;
}

.user-area,
.title,
.welcome h3,
.page-title,
.forgot-title,
.forgot-system-name {
  color: var(--sys-text-secondary) !important;
}

.main {
  background: #ffffff !important;
}

.topbar,
.content,
.card,
.welcome,
.inventory-log-page,
.ai-log-page,
.op-log-page,
.review-log-page,
.login-card,
.forgot-card {
  background: var(--sys-surface) !important;
}

.logout-btn {
  background: var(--sys-surface) !important;
}

.forgot-page,
.forgot-topbar,
.forgot-main {
  background: #ffffff !important;
}
</style>