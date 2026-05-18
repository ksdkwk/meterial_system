<template>
  <div class="dashboard-layout" :class="{ 'density-compact': preferenceForm.uiDensity === 'compact' }">
    <aside class="sidebar">
      <div class="logo">智慧工地物料配送系统</div>
      <nav class="menu">
        <!-- 首页：所有角色可见 -->
        <div class="menu-section">
          <a class="menu-item" :class="{ active: currentPage === 'home' }" @click="setPage('home')">📊 首页</a>
        </div>

        <!-- 用户管理：仅管理人员可见 -->
        <div v-if="isManager" class="menu-section">
          <a class="menu-item" :class="{ active: currentPage === 'users' }" @click="setPage('users')">👥 用户管理</a>
        </div>

        <!-- 物料信息：所有角色可见，但施工人员仅看前两项 -->
        <div class="menu-section">
          <!-- 父菜单（物料信息） -->
          <div class="menu-item menu-parent" @click="toggleMenu('materials')">
            <span>📦 物料信息</span>
            <span class="arrow" :class="{ expand: expandedMenus.materials }">▾</span>
          </div>
          <!-- 子菜单 -->
          <div class="submenu" v-show="expandedMenus.materials">
            <div
              class="submenu-item"
              :class="{ active: currentPage === 'materialBase' }"
              @click.stop="setPage('materialBase')"
            >
              物料基础信息管理
            </div>
            <div
              class="submenu-item"
              :class="{ active: currentPage === 'materialDemand' }"
              @click.stop="setPage('materialDemand')"
            >
              物料需求
            </div>
            <div
              v-if="isManager"
              class="submenu-item"
              :class="{ active: currentPage === 'materialDemandApproval' }"
              @click.stop="setPage('materialDemandApproval')"
            >
              物料需求审批
            </div>
            <div
              v-if="isManager"
              class="submenu-item"
              :class="{ active: currentPage === 'materialDemandApprovalRecord' }"
              @click.stop="setPage('materialDemandApprovalRecord')"
            >
              物料需求审批记录
            </div>
            <!-- 以下三项施工人员不可见 -->
            <template v-if="!isWorker">
              <div
                class="submenu-item"
                :class="{ active: currentPage === 'allocationPlan' }"
                @click.stop="setPage('allocationPlan')"
              >
                物料方案分配信息
              </div>
              <div
                class="submenu-item"
                :class="{ active: currentPage === 'transportRecord' }"
                @click.stop="setPage('transportRecord')"
              >
                物料运输信息
              </div>
            </template>
          </div>
        </div>

        <!-- 施工班组信息管理：所有角色可见 -->
        <div class="menu-section">
          <a class="menu-item" :class="{ active: currentPage === 'teamManagement' }" @click="setPage('teamManagement')">🏗️ 施工班组信息管理</a>
        </div>

        <!-- 物料供应商信息管理：管理人员和仓库管理员可见 -->
        <div v-if="!isWorker" class="menu-section">
          <a class="menu-item" :class="{ active: currentPage === 'supplierManagement' }" @click="setPage('supplierManagement')">🏪 物料供应商信息管理</a>
        </div>

        <!-- 工地运输车辆信息管理：管理人员和仓库管理员可见 -->
        <div v-if="!isWorker" class="menu-section">
          <a class="menu-item" :class="{ active: currentPage === 'vehicleManagement' }" @click="setPage('vehicleManagement')">🚗 工地运输车辆信息管理</a>
        </div>

        <!-- 库存：所有角色可见 -->
        <div class="menu-section">
          <div class="menu-item menu-parent" @click="toggleMenu('inventory')">
            📈 库存
            <span class="arrow" :class="{ expand: expandedMenus.inventory }">▼</span>
          </div>
          <div v-if="expandedMenus.inventory" class="submenu">
            <div class="submenu-item" :class="{ active: currentPage === 'inventoryTotal' }" @click.stop="setPage('inventoryTotal')">库存总量</div>
            <div class="submenu-item" :class="{ active: currentPage === 'inventoryLog' }" @click.stop="setPage('inventoryLog')">库存变动日志</div>
            <div class="submenu-item" :class="{ active: currentPage === 'inventoryReport' }" @click.stop="setPage('inventoryReport')">统计分析报表</div>
            <div class="submenu-item" :class="{ active: currentPage === 'inventoryAiAnalysis' }" @click.stop="setPage('inventoryAiAnalysis')">AI库存分析</div>
          </div>
        </div>

        <!-- 辅助信息模块：所有角色可见 -->
        <div class="menu-section">
          <div class="menu-item menu-parent" @click="toggleMenu('auxiliary')">
            🤖 辅助信息模块
            <span class="arrow" :class="{ expand: expandedMenus.auxiliary }">▼</span>
          </div>
          <div v-if="expandedMenus.auxiliary" class="submenu">
            <div
              class="submenu-item"
              :class="{ active: currentPage === 'aiRecognitionLog' }"
              @click.stop="setPage('aiRecognitionLog')"
            >
              AI识别
            </div>
            <div
              v-if="isManager"
              class="submenu-item"
              :class="{ active: currentPage === 'operationLog' }"
              @click.stop="setPage('operationLog')"
            >
              系统操作日志
            </div>
          </div>
        </div>

        <!-- 消息：功能入口隐藏（保留代码） -->
        <div v-if="messageFeatureVisible" class="menu-section">
          <a class="menu-item" :class="{ active: currentPage === 'messageHome' }" @click="setPage('messageHome')">💬 消息</a>
        </div>

        <!-- 信箱：功能入口隐藏（保留代码） -->
        <div v-if="messageFeatureVisible" class="menu-section">
          <div class="menu-item menu-parent" @click="toggleMenu('messages')">
            ✉️ 信箱
            <span class="arrow" :class="{ expand: expandedMenus.messages }">▼</span>
          </div>
          <div v-if="expandedMenus.messages" class="submenu">
            <div class="submenu-item" :class="{ active: currentPage === 'messageCompose' }" @click.stop="setPage('messageCompose')">写信</div>
            <div class="submenu-item" :class="{ active: currentPage === 'messageInbox' }" @click.stop="setPage('messageInbox')">收信</div>
            <div class="submenu-item" :class="{ active: currentPage === 'messageSent' }" @click.stop="setPage('messageSent')">已发送</div>
            <div class="submenu-item" :class="{ active: currentPage === 'messageDeleted' }" @click.stop="setPage('messageDeleted')">已删除</div>
            <div class="submenu-item" :class="{ active: currentPage === 'messageDraft' }" @click.stop="setPage('messageDraft')">草稿</div>
          </div>
        </div>

        <!-- 个人设置：所有角色可见（放在导航最下面） -->
        <div class="menu-section">
          <div class="menu-item menu-parent" @click="toggleMenu('personalSettings')">
            ⚙️ 个人设置
            <span class="arrow" :class="{ expand: expandedMenus.personalSettings }">▼</span>
          </div>
          <div v-if="expandedMenus.personalSettings" class="submenu">
            <div class="submenu-item" :class="{ active: currentPage === 'settingsGeneral' }" @click.stop="setPage('settingsGeneral')">常规设置</div>
            <div class="submenu-item" :class="{ active: currentPage === 'settingsSecurity' }" @click.stop="setPage('settingsSecurity')">安全中心</div>
          </div>
        </div>
      </nav>
    </aside>

    <section class="main">
      <header class="topbar">
        <div class="crumb">仪表板 / {{ currentPageLabel }}</div>
        <div class="user-area">
          <span>{{ user?.realName || '管理员' }}</span>
          <button class="logout-btn" @click="$emit('logout')">退出</button>
        </div>
      </header>

      <main class="content">
        <!-- 首页仪表盘 -->
        <template v-if="currentPage === 'home'">
          <h2 class="title">仪表板</h2>

          <div class="cards">
            <div class="card">
              <div class="dot blue"></div>
              <div>
                <div class="num">{{ usersCount }}</div>
                <div class="label">总用户数</div>
              </div>
            </div>
            <div class="card">
              <div class="dot green"></div>
              <div>
                <div class="num">{{ activeCount }}</div>
                <div class="label">今日活跃</div>
              </div>
            </div>
            <div class="card">
              <div class="dot yellow"></div>
              <div>
                <div class="num">{{ pendingTasks }}</div>
                <div class="label">未完成单</div>
              </div>
            </div>
            <div class="card">
              <div class="dot red"></div>
              <div>
                <div class="num">{{ pendingItems }}</div>
                <div class="label">待处理项</div>
              </div>
            </div>
          </div>

          <section class="welcome">
            <h3>欢迎回来，{{ user?.realName || '管理员' }}！</h3>
            <p>今天是 2026 年 3 月 11 日，祝你工作顺利！</p>
          </section>
        </template>

        <!-- 用户管理页面 -->
        <UserManagementPage v-else-if="currentPage === 'users'" />
        <!-- 消息一级菜单页面（即时通信） -->
        <section v-else-if="currentPage === 'messageHome'" class="chat-shell">
          <aside class="chat-aside">
            <div class="chat-aside-title">会话</div>
            <div class="chat-search-wrap">
              <input
                v-model.trim="chatSearchKeyword"
                class="chat-search-input"
                type="text"
                placeholder="搜索会话：姓名 / 用户ID / 最近消息"
                @keydown.enter.prevent
              />
              <div ref="chatQuickMenuRef" class="chat-plus-wrap">
                <button class="chat-plus-btn" type="button" @click.stop="toggleChatQuickMenu">+</button>
                <div v-if="showChatQuickMenu" class="chat-quick-menu" @click.stop>
                  <button class="chat-quick-item" type="button" @click="handleChatQuickAction('add-friend')">添加好友</button>
                  <button class="chat-quick-item" type="button" @click="handleChatQuickAction('create-group')">创建群聊</button>
                </div>
              </div>
            </div>
            <div v-if="messageLoading" class="chat-empty">加载中...</div>
            <div v-else-if="chatSessions.length === 0" class="chat-empty">暂无会话</div>
            <div v-else-if="filteredChatSessions.length === 0" class="chat-empty">未找到匹配会话</div>
            <div
              v-for="session in filteredChatSessions"
              :key="session.userId"
              class="chat-session-item"
              :class="{ active: selectedChatUserId === session.userId }"
              @click="openChat(session)"
            >
              <div class="chat-session-top">
                <span class="chat-session-name">{{ session.displayName }}</span>
                <span class="chat-session-time">{{ formatShortTime(session.lastTime) }}</span>
              </div>
              <div class="chat-session-last">{{ session.lastContent || '（暂无内容）' }}</div>
              <span v-if="session.unreadCount > 0" class="chat-unread-dot">{{ session.unreadCount }}</span>
            </div>
          </aside>

          <div class="chat-main">
            <template v-if="selectedChat">
              <header class="chat-main-header">
                <div class="chat-main-name">{{ selectedChat.displayName }}</div>
                <div class="chat-main-id">账号：{{ selectedChat.userId }}</div>
              </header>

              <div class="chat-history">
                <div v-if="chatHistory.length === 0" class="chat-empty">暂无历史记录</div>
                <div
                  v-for="msg in chatHistory"
                  :key="msg.messageId"
                  class="chat-row"
                  :class="{ mine: msg.senderId === currentUserId }"
                >
                  <div class="chat-meta">
                    {{ msg.senderId === currentUserId ? '我' : (msg.senderName || msg.senderId) }}
                    · {{ formatDateTime(msg.sendTime) }}
                  </div>
                  <div class="chat-bubble" v-safe-html="msg.content"></div>
                </div>
              </div>

              <footer class="chat-input-wrap">
                <textarea
                  v-model="chatInput"
                  class="chat-input"
                  placeholder="请输入消息内容，Ctrl+Enter 发送"
                  @keydown.ctrl.enter.prevent="sendChatMessage"
                />
                <div class="chat-input-actions">
                  <span v-if="chatError" class="chat-error">{{ chatError }}</span>
                  <button class="mail-btn primary" type="button" :disabled="chatSending" @click="sendChatMessage">
                    {{ chatSending ? '发送中...' : '发送' }}
                  </button>
                </div>
              </footer>
            </template>

            <div v-else class="chat-placeholder">请选择左侧联系人开始沟通</div>
          </div>
        </section>
        <!-- 个人设置-常规设置 -->
        <section v-else-if="currentPage === 'settingsGeneral'" class="settings-wrap">
          <h3>常规设置</h3>

          <div class="settings-grid">
            <section class="settings-card">
              <h4>个人基础信息</h4>
              <div class="settings-row">
                <label>姓名</label>
                <input :value="profileInfo.realName" type="text" readonly />
              </div>
              <div class="settings-row">
                <label>唯一ID</label>
                <input :value="profileInfo.userId" type="text" readonly />
              </div>
              <div class="settings-row">
                <label>账号</label>
                <input :value="profileInfo.username" type="text" readonly />
              </div>
              <div class="settings-row">
                <label>角色</label>
                <input :value="profileInfo.roleName" type="text" readonly />
              </div>
            </section>

            <section class="settings-card">
              <h4>显示与个人偏好</h4>
              <div class="settings-row">
                <label>字体大小</label>
                <select v-model="preferenceForm.fontSize">
                  <option value="13">小（13px）</option>
                  <option value="14">标准（14px）</option>
                  <option value="16">中（16px）</option>
                  <option value="18">大（18px）</option>
                </select>
              </div>
              <div class="settings-row">
                <label>界面密度</label>
                <select v-model="preferenceForm.uiDensity">
                  <option value="comfortable">舒适</option>
                  <option value="compact">紧凑</option>
                </select>
              </div>
              <div class="settings-actions">
                <button class="mail-btn primary" type="button" @click="saveUserPreferences">保存偏好</button>
                <button class="mail-btn" type="button" @click="resetUserPreferences">恢复默认</button>
                <span v-if="preferenceTip" class="settings-tip">{{ preferenceTip }}</span>
              </div>
            </section>
          </div>
        </section>
        <!-- 个人设置-安全中心 -->
        <section v-else-if="currentPage === 'settingsSecurity'" class="settings-wrap">
          <h3>安全中心</h3>
          <div class="settings-grid">
            <section class="settings-card">
              <h4>已绑定账号信息</h4>
              <div class="settings-row">
                <label>账号</label>
                <input :value="securityBindingInfo.username" type="text" readonly />
              </div>
              <div class="settings-row">
                <label>邮箱</label>
                <input :value="securityBindingInfo.userMail" type="text" readonly />
                <button class="mail-btn" type="button" @click="openBindingModal('email')">修改</button>
              </div>
              <div class="settings-row">
                <label>手机号</label>
                <input :value="securityBindingInfo.phone" type="text" readonly />
                <button class="mail-btn" type="button" @click="openBindingModal('phone')">修改</button>
              </div>
              <p class="settings-tip">提示：未绑定项会显示“未绑定”，可后续在账号安全流程中补充。</p>
            </section>
          </div>
        </section>
        <!-- 消息-写信 -->
        <section v-else-if="currentPage === 'messageCompose'" class="mail-compose">
          <div class="mail-actions">
            <button class="mail-btn primary" type="button" @click="sendMail">✈ 发送</button>
            <button class="mail-btn">预 览</button>
            <button class="mail-btn">存草稿</button>
            <button class="mail-btn" type="button" @click="openAttachmentPicker">📎 上传附件</button>
            <button class="mail-btn">取 消</button>
            <input
              ref="attachmentInputRef"
              class="hidden-file-input"
              type="file"
              multiple
              @change="handleAttachmentChange"
            />
          </div>

          <div v-if="composeAttachments.length > 0" class="attach-list">
            <div class="attach-title">已选附件：</div>
            <div class="attach-items">
              <div v-for="(file, idx) in composeAttachments" :key="`${file.name}-${idx}`" class="attach-item">
                <span class="attach-name">{{ file.name }}</span>
                <span class="attach-size">({{ formatFileSize(file.size) }})</span>
                <button class="attach-remove" type="button" @click="removeAttachment(idx)">移除</button>
              </div>
            </div>
          </div>

          <div class="mail-line">
            <label>收件人：</label>
            <input v-model="composeForm.to" type="text" placeholder="请输入收件人账号或用户ID" />
          </div>
          <div class="mail-line">
            <label>主　题：</label>
            <input v-model="composeForm.subject" type="text" placeholder="请输入主题" />
          </div>

          <div class="mail-toolbar">
            <span>↶</span><span>↷</span><span>B</span><span>I</span><span>U</span>
            <span>•</span><span>1.</span><span>🔗</span><span>🙂</span><span>🖼</span>
          </div>

          <textarea
            v-model="composeForm.content"
            class="mail-editor"
            placeholder="请输入邮件内容..."
          />

          <div v-if="sendResult" class="mail-tip">{{ sendResult }}</div>
        </section>
        <!-- 消息-收信 -->
        <section v-else-if="currentPage === 'messageInbox'" class="welcome">
          <h3>收信</h3>
          <p v-if="messageLoading">加载中...</p>
          <p v-else-if="messageError">{{ messageError }}</p>
          <div v-else class="msg-list">
            <div v-if="inboxMessages.length === 0" class="msg-empty">暂无消息</div>
            <div v-for="msg in inboxMessages" :key="msg.messageId" class="msg-item" :class="{ unread: !msg.read }" @click="markMessageRead(msg)">
              <div class="msg-head">
                <span>来自：{{ msg.senderName || msg.senderId }}</span>
                <span>{{ formatDateTime(msg.sendTime) }}</span>
              </div>
              <div class="msg-title" v-safe-html="msg.subject || '（无主题）'"></div>
              <div class="msg-content" v-safe-html="msg.content"></div>
            </div>
          </div>
        </section>
        <!-- 消息-已发送 -->
        <section v-else-if="currentPage === 'messageSent'" class="welcome">
          <h3>已发送</h3>
          <div class="msg-list">
            <div v-if="sentMessages.length === 0" class="msg-empty">暂无已发送消息</div>
            <div v-for="msg in sentMessages" :key="msg.messageId" class="msg-item">
              <div class="msg-head">
                <span>发送给：{{ msg.receiverName || msg.receiverId }}</span>
                <span>{{ formatDateTime(msg.sendTime) }}</span>
              </div>
              <div class="msg-title" v-safe-html="msg.subject || '（无主题）'"></div>
              <div class="msg-content" v-safe-html="msg.content"></div>
            </div>
          </div>
        </section>
        <!-- 消息-已删除 -->
        <section v-else-if="currentPage === 'messageDeleted'" class="welcome">
          <h3>已删除</h3>
          <p>已删除消息列表（建设中）。</p>
        </section>
        <!-- 消息-草稿 -->
        <section v-else-if="currentPage === 'messageDraft'" class="welcome">
          <h3>草稿</h3>
          <p>草稿箱（建设中）。</p>
        </section>
        <!-- 物料基础信息管理页面 -->
        <MaterialBasePage v-else-if="currentPage === 'materialBase'" />
        <!-- 物料需求页面 -->
        <MaterialDemandPage v-else-if="currentPage === 'materialDemand'" />
        <!-- 物料需求审批页面（管理人员） -->
        <MaterialDemandPage v-else-if="currentPage === 'materialDemandApproval'" />
        <!-- 物料需求审批记录页面（管理人员） -->
        <MaterialDemandApprovalRecordPage v-else-if="currentPage === 'materialDemandApprovalRecord'" />
        <!-- 物料方案分配信息页面 -->
        <AllocationPlanPage v-else-if="currentPage === 'allocationPlan'" />
        <!-- 物料运输信息页面 -->
        <TransportRecordPage v-else-if="currentPage === 'transportRecord'" />
        <!-- 施工班组信息管理页面 -->
        <TeamManagementPage v-else-if="currentPage === 'teamManagement'" />
        <!-- 物料供应商信息管理页面 -->
        <SupplierManagementPage v-else-if="currentPage === 'supplierManagement'" />
        <!-- 工地运输车辆信息管理页面 -->
        <VehicleManagementPage v-else-if="currentPage === 'vehicleManagement'" />
        <!-- 库存总量页面 -->
        <InventoryTotalPage v-else-if="currentPage === 'inventoryTotal'" />
        <!-- 库存变动日志页面 -->
        <InventoryLogPage v-else-if="currentPage === 'inventoryLog'" />
        <!-- 统计分析报表页面 -->
        <section v-else-if="currentPage === 'inventoryReport'" class="report-wrap">
          <h3>统计分析报表</h3>

          <div class="report-filters">
            <select v-model="reportFilters.granularity" class="query-select">
              <option value="day">按日</option>
              <option value="week">按周</option>
              <option value="month">按月</option>
              <option value="year">按年</option>
            </select>
            <select v-model="reportFilters.materialId" class="query-select">
              <option value="">全部物料</option>
              <option v-for="m in reportMaterialOptions" :key="m.materialId" :value="m.materialId">{{ m.materialName || m.materialId }}</option>
            </select>
            <select v-model="reportFilters.storageLocation" class="query-select">
              <option value="">全部库位</option>
              <option v-for="loc in reportLocationOptions" :key="loc" :value="loc">{{ loc }}</option>
            </select>
            <button class="primary-btn" type="button" @click="loadInventoryReportData">刷新数据</button>
          </div>

          <div v-if="reportLoading" class="loading">报表数据加载中...</div>
          <div v-else-if="reportError" class="error-box">❌ {{ reportError }}</div>
          <div v-else class="report-scroll-area">
            <section class="report-card">
              <h4>一、库存量变化趋势图</h4>
              <p class="report-desc">按 {{ reportFilters.granularity === 'day' ? '日' : reportFilters.granularity === 'week' ? '周' : reportFilters.granularity === 'month' ? '月' : '年' }} 维度统计期初、入库、出库、期末库存。</p>
              <div v-if="trendChartData" class="trend-chart-wrap">
                <div class="trend-chart-legend">
                  <span><i class="legend-chip inbound"></i>入库（柱）</span>
                  <span><i class="legend-chip outbound"></i>出库（柱）</span>
                  <span><i class="legend-chip line"></i>期末库存（折线）</span>
                </div>
                <div class="trend-chart-scroll">
                  <svg :width="trendChartData.width" :height="trendChartData.height" class="trend-chart-svg" role="img" aria-label="库存趋势混合图">
                    <line
                      v-for="tick in trendChartData.yTicks"
                      :key="`grid-${tick.value}`"
                      :x1="trendChartData.padding.left"
                      :x2="trendChartData.width - trendChartData.padding.right"
                      :y1="tick.y"
                      :y2="tick.y"
                      class="trend-grid-line"
                    />

                    <text
                      v-for="tick in trendChartData.yTicks"
                      :key="`label-${tick.value}`"
                      :x="trendChartData.padding.left - 8"
                      :y="tick.y + 4"
                      text-anchor="end"
                      class="trend-axis-label"
                    >
                      {{ tick.value }}
                    </text>

                    <g v-for="item in trendChartData.items" :key="item.bucket">
                      <rect
                        :x="item.inboundX"
                        :y="item.inboundY"
                        :width="item.barWidth"
                        :height="item.inboundHeight"
                        class="trend-bar inbound"
                      />
                      <rect
                        :x="item.outboundX"
                        :y="item.outboundY"
                        :width="item.barWidth"
                        :height="item.outboundHeight"
                        class="trend-bar outbound"
                      />
                    </g>

                    <polyline
                      :points="trendChartData.linePoints"
                      class="trend-line"
                    />

                    <circle
                      v-for="item in trendChartData.items"
                      :key="`point-${item.bucket}`"
                      :cx="item.centerX"
                      :cy="item.lineY"
                      r="3"
                      class="trend-line-point"
                    />

                    <text
                      v-for="item in trendChartData.items"
                      :key="`x-${item.bucket}`"
                      :x="item.centerX"
                      :y="trendChartData.height - 10"
                      text-anchor="middle"
                      class="trend-axis-label"
                    >
                      {{ item.bucket }}
                    </text>
                  </svg>
                </div>
              </div>
              <div class="report-table-wrap">
                <table class="report-table">
                  <thead>
                    <tr>
                      <th>时间段</th>
                      <th>期初库存</th>
                      <th>入库总量</th>
                      <th>出库总量</th>
                      <th>期末库存</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="row in trendRows" :key="row.bucket">
                      <td>{{ row.bucket }}</td>
                      <td>{{ row.periodStart }}</td>
                      <td>{{ row.inbound }}</td>
                      <td>{{ row.outbound }}</td>
                      <td>{{ row.periodEnd }}</td>
                    </tr>
                    <tr v-if="trendRows.length === 0">
                      <td colspan="5" class="empty">暂无趋势数据</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>

            <section class="report-card">
              <h4>二、库存周转率分析</h4>
              <p class="report-desc">库存周转率 = 统计周期内物料出库总金额 / 统计周期内平均库存金额</p>
              <div class="metric-grid">
                <div class="metric-item"><span>出库总金额</span><strong>¥ {{ outboundAmount.toFixed(2) }}</strong></div>
                <div class="metric-item"><span>平均库存金额</span><strong>¥ {{ averageInventoryAmount.toFixed(2) }}</strong></div>
                <div class="metric-item"><span>库存周转率</span><strong>{{ turnoverRate.toFixed(2) }}</strong></div>
              </div>
            </section>

            <section class="report-card">
              <h4>三、缺货率统计</h4>
              <div class="metric-grid">
                <div class="metric-item"><span>库存预警缺货率</span><strong>{{ (inventoryWarningShortageRate * 100).toFixed(2) }}%</strong></div>
                <div class="metric-item"><span>需求满足缺货率</span><strong>{{ (demandSatisfyShortageRate * 100).toFixed(2) }}%</strong></div>
                <div class="metric-item"><span>综合缺货率</span><strong>{{ (overallShortageRate * 100).toFixed(2) }}%</strong></div>
              </div>
            </section>

            <section class="report-card pie-row">
              <div class="pie-block">
                <h4>当前库存数量占比（饼图）</h4>
                <div class="pie" :style="{ background: currentInventoryPieGradient }"></div>
                <ul class="pie-legend">
                  <li v-for="p in currentInventoryPieParts" :key="`cur-${p.name}`">
                    <span class="pie-dot" :style="{ background: p.color }"></span>{{ p.name }}：{{ p.percent.toFixed(2) }}%
                  </li>
                </ul>
              </div>
              <div class="pie-block">
                <h4>库存历史变动数量占比（饼图）</h4>
                <div class="pie" :style="{ background: historyChangePieGradient }"></div>
                <ul class="pie-legend">
                  <li v-for="p in historyChangePieParts" :key="`his-${p.name}`">
                    <span class="pie-dot" :style="{ background: p.color }"></span>{{ p.name }}：{{ p.percent.toFixed(2) }}%
                  </li>
                </ul>
              </div>
            </section>
          </div>
        </section>
        <!-- AI库存分析页面 -->
        <section v-else-if="currentPage === 'inventoryAiAnalysis'" class="ai-analysis-wrap">
          <h3>AI库存分析</h3>

          <div class="ai-toolbar">
            <select v-model="aiFilters.storageLocation" class="query-select">
              <option value="">全部库位</option>
              <option v-for="loc in reportLocationOptions" :key="`ai-${loc}`" :value="loc">{{ loc }}</option>
            </select>
            <select v-model.number="aiFilters.lookbackDays" class="query-select">
              <option :value="7">近7天</option>
              <option :value="14">近14天</option>
              <option :value="30">近30天</option>
              <option :value="60">近60天</option>
              <option :value="90">近90天</option>
            </select>
            <select v-model.number="aiFilters.forecastDays" class="query-select">
              <option :value="7">预测7天</option>
              <option :value="14">预测14天</option>
              <option :value="30">预测30天</option>
            </select>
            <button class="primary-btn" type="button" @click="runPersistedAiAnalysis">生成并入库AI分析结果</button>
            <button class="primary-btn" type="button" @click="loadInventoryReportData">刷新分析</button>
          </div>

          <div v-if="aiPersistTip" class="report-tip">{{ aiPersistTip }}</div>
          <div class="ai-config-check" :class="{ ok: aiConfigCheck.baseUrlConfigured && aiConfigCheck.deepseekApiConfigured }">
            <span v-if="aiConfigLoading">AI配置自检中...</span>
            <template v-else>
              <span>BASE_URL：{{ aiConfigCheck.baseUrlConfigured ? '已读取' : '未读取' }}（{{ aiConfigCheck.baseUrlMasked || '未读取到' }}）</span>
              <span>DEEPSEEK_API：{{ aiConfigCheck.deepseekApiConfigured ? '已读取' : '未读取' }}（{{ aiConfigCheck.deepseekApiMasked || '未读取到' }}）</span>
            </template>
          </div>
          <div v-if="aiPersistError" class="error-box">❌ {{ aiPersistError }}</div>

          <div v-if="reportLoading" class="loading">AI分析数据加载中...</div>
          <div v-else-if="reportError" class="error-box">❌ {{ reportError }}</div>
          <div v-else class="ai-scroll-area">
            <div class="ai-content-wide">
              <div class="ai-overview-grid">
                <div class="ai-overview-card">
                  <span>AI库存综合评分</span>
                  <strong>{{ aiSummary.overallScore.toFixed(1) }}</strong>
                  <em>分值越高风险越低</em>
                </div>
                <div class="ai-overview-card">
                  <span>高风险物料数</span>
                  <strong>{{ aiSummary.highRiskCount }}</strong>
                  <em>建议优先处理TOP风险项</em>
                </div>
                <div class="ai-overview-card">
                  <span>预测需求总量（{{ aiFilters.forecastDays }}天）</span>
                  <strong>{{ aiSummary.totalForecast.toFixed(2) }}</strong>
                  <em>基于历史出库节奏估算</em>
                </div>
                <div class="ai-overview-card">
                  <span>建议补货总量</span>
                  <strong>{{ aiSummary.totalSuggested.toFixed(2) }}</strong>
                  <em>综合需求+安全库存+待满足需求</em>
                </div>
              </div>

              <section class="ai-panel">
                <h4>AI分析入库记录（tb_ai_analysis_result）</h4>
                <div v-if="aiPersistLoading" class="loading">入库记录加载中...</div>
                <div v-else class="report-table-wrap">
                  <table class="report-table">
                    <thead>
                      <tr>
                        <th>分析ID</th>
                        <th>类型</th>
                        <th>统计范围</th>
                        <th>核心指标</th>
                        <th>状态</th>
                        <th>完成时间</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="row in aiPersistedRecords" :key="`persist-${row.analysisId}`">
                        <td>
                          <button class="ai-link" type="button" @click="openAiRawResult(row)">{{ row.analysisId }}</button>
                        </td>
                        <td>{{ aiAnalysisTypeText(row.analysisType) }}</td>
                        <td>{{ row.startTime ? String(row.startTime).replace('T', ' ').slice(0, 16) : '—' }} ~ {{ row.endTime ? String(row.endTime).replace('T', ' ').slice(0, 16) : '—' }}</td>
                        <td>{{ row.coreIndicator == null ? '—' : Number(row.coreIndicator).toFixed(4) }}</td>
                        <td>{{ Number(row.analysisStatus) === 1 ? '完成' : '失败' }}</td>
                        <td>{{ row.finishTime ? String(row.finishTime).replace('T', ' ').slice(0, 16) : '—' }}</td>
                      </tr>
                      <tr v-if="aiPersistedRecords.length === 0">
                        <td colspan="6" class="empty">暂无AI入库记录，请先点击“生成并入库AI分析结果”</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </section>

              <section class="ai-panel">
              <h4>一、需求预测与告警评分</h4>
              <div class="ai-panel-actions">
                <button
                  class="primary-btn"
                  type="button"
                  :disabled="aiSubmittingDraft || aiTopSuggestions.length === 0"
                  @click="generateAiReplenishDrafts"
                >
                  {{ aiSubmittingDraft ? '生成中...' : '一键生成补货单（草稿）' }}
                </button>
                <span v-if="aiDraftTip" class="ai-draft-tip">{{ aiDraftTip }}</span>
              </div>
              <div class="report-table-wrap">
                <table class="report-table">
                  <thead>
                    <tr>
                      <th>物料</th>
                      <th>近{{ aiFilters.lookbackDays }}天趋势</th>
                      <th>当前库存</th>
                      <th>日均出库</th>
                      <th>{{ aiFilters.forecastDays }}天预测需求</th>
                      <th>待满足需求</th>
                      <th>建议补货</th>
                      <th>预计可用天数</th>
                      <th>告警评分</th>
                      <th>风险等级</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="row in aiAnalysisRows" :key="`ai-row-${row.materialId}`">
                      <td>{{ row.materialName }}</td>
                      <td>
                        <svg class="ai-trend-mini" viewBox="0 0 120 34" role="img" :aria-label="`${row.materialName}趋势`">
                          <polyline :points="row.sparklinePoints" class="ai-trend-line" />
                        </svg>
                      </td>
                      <td>{{ row.currentStock.toFixed(2) }}</td>
                      <td>{{ row.dailyOutboundAvg.toFixed(2) }}</td>
                      <td>{{ row.forecastDemand.toFixed(2) }}</td>
                      <td>{{ row.pendingDemand.toFixed(2) }}</td>
                      <td>{{ row.suggestedReplenish.toFixed(2) }}</td>
                      <td>{{ formatAiRunoutDays(row.runoutDays) }}</td>
                      <td>{{ row.alertScore.toFixed(1) }}</td>
                      <td>
                        <span class="ai-risk-tag" :class="`risk-${row.riskLevelCode}`">{{ row.riskLevel }}</span>
                      </td>
                    </tr>
                    <tr v-if="aiAnalysisRows.length === 0">
                      <td colspan="10" class="empty">暂无可分析的库存数据</td>
                    </tr>
                  </tbody>
                </table>
              </div>
              </section>

              <section class="ai-panel">
              <h4>二、风险热力分层</h4>
              <div class="ai-heat-grid">
                <div class="ai-heat-col high">
                  <div class="ai-heat-head">高风险（{{ aiRiskBuckets.high.length }}）</div>
                  <div v-for="item in aiRiskBuckets.high" :key="`heat-h-${item.materialId}`" class="ai-heat-item">
                    <span>{{ item.materialName }}</span>
                    <b>{{ item.alertScore.toFixed(1) }}</b>
                  </div>
                  <div v-if="aiRiskBuckets.high.length === 0" class="report-empty">无</div>
                </div>
                <div class="ai-heat-col medium">
                  <div class="ai-heat-head">中风险（{{ aiRiskBuckets.medium.length }}）</div>
                  <div v-for="item in aiRiskBuckets.medium" :key="`heat-m-${item.materialId}`" class="ai-heat-item">
                    <span>{{ item.materialName }}</span>
                    <b>{{ item.alertScore.toFixed(1) }}</b>
                  </div>
                  <div v-if="aiRiskBuckets.medium.length === 0" class="report-empty">无</div>
                </div>
                <div class="ai-heat-col low">
                  <div class="ai-heat-head">低风险（{{ aiRiskBuckets.low.length }}）</div>
                  <div v-for="item in aiRiskBuckets.low" :key="`heat-l-${item.materialId}`" class="ai-heat-item">
                    <span>{{ item.materialName }}</span>
                    <b>{{ item.alertScore.toFixed(1) }}</b>
                  </div>
                  <div v-if="aiRiskBuckets.low.length === 0" class="report-empty">无</div>
                </div>
              </div>
              </section>

              <section class="ai-panel">
              <h4>三、AI补货建议（TOP 5）</h4>
              <div class="ai-suggest-list">
                <div v-for="item in aiTopSuggestions" :key="`suggest-${item.materialId}`" class="ai-suggest-item">
                  <div>
                    <div class="ai-suggest-title">{{ item.materialName }}（{{ item.materialId }}）</div>
                    <div class="ai-suggest-desc">{{ item.aiComment }}</div>
                  </div>
                  <div class="ai-suggest-metrics">
                    <span>建议补货：<b>{{ item.suggestedReplenish.toFixed(2) }}</b></span>
                    <span>告警评分：<b>{{ item.alertScore.toFixed(1) }}</b></span>
                  </div>
                </div>
                <div v-if="aiTopSuggestions.length === 0" class="report-empty">当前无补货建议。</div>
              </div>
              </section>

              <section class="ai-panel">
              <h4>四、AI库存综合分析结论</h4>
              <ul class="ai-conclusion-list">
                <li v-for="(line, idx) in aiConclusions" :key="`conclusion-${idx}`">{{ line }}</li>
              </ul>
              </section>
            </div>
          </div>
        </section>
        <!-- AI识别记录表页面 -->
        <AIRecognitionLogPage v-else-if="currentPage === 'aiRecognitionLog'" />
        <!-- 系统操作日志页面 -->
        <OperationLogPage v-else-if="currentPage === 'operationLog'" />
      </main>

      <div v-if="bindingModal.visible" class="binding-modal-mask" @click.self="closeBindingModal">
        <div class="binding-modal">
          <h4>修改{{ bindingModal.type === 'email' ? '邮箱' : '手机号' }}</h4>

          <div v-if="bindingModal.step === 'verifyCurrent'" class="binding-step">
            <p class="binding-desc">当前已绑定{{ bindingModal.type === 'email' ? '邮箱' : '手机号' }}，请先完成旧绑定验证。</p>
            <div class="settings-row">
              <label>验证码</label>
              <input
                v-model.trim="bindingModal.currentCode"
                :class="{ 'binding-input-error': bindingModal.currentCodeError }"
                type="text"
                placeholder="请输入旧绑定验证码"
                @input="bindingModal.currentCodeError = false"
              />
              <button class="mail-btn" type="button" :disabled="bindingModal.sending || bindingModal.currentCountdown > 0" @click="sendCurrentBindingCode">
                {{ bindingModal.currentCountdown > 0 ? `${bindingModal.currentCountdown}s后重发` : (bindingModal.sending ? '发送中...' : '发送验证码') }}
              </button>
            </div>
            <p v-if="bindingModal.currentCodeError" class="binding-error-tip">旧绑定验证码格式错误，请输入6位数字。</p>
            <div class="settings-actions">
              <button class="mail-btn primary" type="button" @click="verifyCurrentBindingCode">验证并下一步</button>
              <button class="mail-btn" type="button" @click="closeBindingModal">取消</button>
            </div>
          </div>

          <div v-else class="binding-step">
            <p class="binding-desc">请输入新的{{ bindingModal.type === 'email' ? '邮箱' : '手机号' }}，并完成验证码校验。</p>
            <div class="settings-row">
              <label>新{{ bindingModal.type === 'email' ? '邮箱' : '手机号' }}</label>
              <input
                v-model.trim="bindingModal.newValue"
                :class="{ 'binding-input-error': bindingModal.newValueError }"
                type="text"
                :placeholder="bindingModal.type === 'email' ? '请输入新邮箱' : '请输入新手机号'"
                @input="bindingModal.newValueError = false"
              />
            </div>
            <p v-if="bindingModal.newValueError" class="binding-error-tip">{{ bindingModal.type === 'email' ? '新邮箱格式不正确。' : '新手机号格式不正确，应为11位手机号。' }}</p>
            <div class="settings-row">
              <label>验证码</label>
              <input
                v-model.trim="bindingModal.newCode"
                :class="{ 'binding-input-error': bindingModal.newCodeError }"
                type="text"
                placeholder="请输入新绑定验证码"
                @input="bindingModal.newCodeError = false"
              />
              <button class="mail-btn" type="button" :disabled="bindingModal.sending || bindingModal.newCountdown > 0" @click="sendNewBindingCode">
                {{ bindingModal.newCountdown > 0 ? `${bindingModal.newCountdown}s后重发` : (bindingModal.sending ? '发送中...' : '发送验证码') }}
              </button>
            </div>
            <p v-if="bindingModal.newCodeError" class="binding-error-tip">新绑定验证码格式错误，请输入6位数字。</p>
            <div class="settings-actions">
              <button class="mail-btn primary" type="button" @click="submitBindingUpdate">确认修改</button>
              <button class="mail-btn" type="button" @click="closeBindingModal">取消</button>
            </div>
          </div>

          <p v-if="bindingModal.tip" class="settings-tip">{{ bindingModal.tip }}</p>
        </div>
      </div>

      <div v-if="aiRawVisible" class="ai-raw-mask" @click.self="closeAiRawResult">
        <div class="ai-raw-modal">
          <div class="ai-raw-header">
            <div>
              <h4>AI分析原始JSON</h4>
              <p class="ai-raw-sub">{{ aiRawTitle }}</p>
            </div>
            <button class="plain-btn" type="button" @click="closeAiRawResult">关闭</button>
          </div>
          <div v-if="aiRawLoading" class="loading">加载中...</div>
          <div v-else-if="aiRawError" class="error-box">❌ {{ aiRawError }}</div>
          <pre v-else class="ai-raw-content">{{ aiRawContent }}</pre>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watchEffect } from 'vue'
import request from '@/utils/request'
import UserManagementPage from './UserManagementPage.vue'
import MaterialBasePage from './MaterialBasePage.vue'
import MaterialDemandPage from './MaterialDemandPage.vue'
import MaterialDemandApprovalRecordPage from './MaterialDemandApprovalRecordPage.vue'
import AllocationPlanPage from './AllocationPlanPage.vue'
import TransportRecordPage from './TransportRecordPage.vue'
import TeamManagementPage from './TeamManagementPage.vue'
import SupplierManagementPage from './SupplierManagementPage.vue'
import VehicleManagementPage from './VehicleManagementPage.vue'
import InventoryTotalPage from './InventoryTotalPage.vue'
import InventoryLogPage from './InventoryLogPage.vue'
import AIRecognitionLogPage from './AIRecognitionLogPage.vue'
import OperationLogPage from './OperationLogPage.vue'

const props = defineProps({
  user: {
    type: Object,
    default: () => ({})
  }
})

defineEmits(['logout'])

// 角色判断（兼容多种管理员角色码/名称）
const roleCode = computed(() => String(props.user?.role?.roleCode || props.user?.roleCode || props.user?.rolePermission || '').trim().toUpperCase())
const roleName = computed(() => String(props.user?.role?.roleName || '').trim())
const permissions = computed(() => Array.isArray(props.user?.permissions) ? props.user.permissions : [])

const ADMIN_ROLE_CODES = ['MANAGER', 'ADMIN', 'SYSTEM_ADMIN', 'SUPER_ADMIN']
const ADMIN_ROLE_NAMES = ['管理员', '系统管理员', '超级管理员']
const USER_MANAGE_PERMS = ['user:manage', 'user:update', 'sys:user:edit', 'sys:user:update']

const isManager = computed(() => {
  if (ADMIN_ROLE_CODES.includes(roleCode.value)) return true
  if (ADMIN_ROLE_NAMES.includes(roleName.value)) return true
  return USER_MANAGE_PERMS.some(p => permissions.value.includes(p))
})

const isWorker = computed(() => ['WORKER', 'USER_WORKER'].includes(roleCode.value))
const isWarehouse = computed(() => ['WAREHOUSE', 'WAREHOUSE_ADMIN', 'USER_WAREHOUSE'].includes(roleCode.value))
const messageFeatureVisible = false

// 当前页面
const currentPage = ref('home')
const setPage = (page) => {
  if (!messageFeatureVisible && String(page).startsWith('message')) {
    currentPage.value = 'home'
    return
  }
  currentPage.value = page
  if (String(page).startsWith('message')) {
    loadInbox()
    loadSent()
  }
  if (page === 'settingsSecurity') {
    loadSecurityBindings()
  }
  if (page === 'inventoryReport' || page === 'inventoryAiAnalysis') {
    loadInventoryReportData()
    if (page === 'inventoryAiAnalysis') {
      loadAiPersistedResults()
      loadAiConfigCheck()
    }
  }
}

// 防止无权限角色进入用户管理页
watchEffect(() => {
  if (!messageFeatureVisible && String(currentPage.value).startsWith('message')) {
    currentPage.value = 'home'
  }
  if (currentPage.value === 'users' && !isManager.value) {
    currentPage.value = 'home'
  }
  if (currentPage.value === 'allocationPlan' && isWorker.value) {
    currentPage.value = 'home'
  }
  if (currentPage.value === 'materialDemandApproval' && !isManager.value) {
    currentPage.value = 'home'
  }
  if (currentPage.value === 'materialDemandApprovalRecord' && !isManager.value) {
    currentPage.value = 'home'
  }
  if (currentPage.value === 'transportRecord' && isWorker.value) {
    currentPage.value = 'home'
  }
  if (currentPage.value === 'supplierManagement' && isWorker.value) {
    currentPage.value = 'home'
  }
  if (currentPage.value === 'vehicleManagement' && isWorker.value) {
    currentPage.value = 'home'
  }
  if (currentPage.value === 'operationLog' && !isManager.value) {
    currentPage.value = 'home'
  }
})

const currentPageLabel = computed(() => {
  const map = {
    home: '首页',
    users: '用户管理',
    messageHome: '消息',
    settingsGeneral: '常规设置',
    settingsSecurity: '安全中心',
    messageCompose: '写信',
    messageInbox: '收信',
    messageSent: '已发送',
    messageDeleted: '已删除',
    messageDraft: '草稿',
    materialBase: '物料基础信息管理',
    materialDemand: '物料需求',
    materialDemandApproval: '物料需求审批',
    materialDemandApprovalRecord: '物料需求审批记录',
    allocationPlan: '物料方案分配信息',
    transportRecord: '物料运输信息',
    teamManagement: '施工班组信息管理',
    supplierManagement: '物料供应商信息管理',
    vehicleManagement: '工地运输车辆信息管理',
    inventoryTotal: '库存总量',
    inventoryLog: '库存变动日志',
    inventoryReport: '统计分析报表',
    inventoryAiAnalysis: 'AI库存分析',
    aiRecognitionLog: 'AI识别',
    operationLog: '系统操作日志'
  }
  return map[currentPage.value] || '首页'
})

const usersCount = ref(1234)
const activeCount = ref(856)
const pendingTasks = ref(12)
const pendingItems = ref(5)

const composeForm = ref({
  to: '',
  subject: '',
  content: ''
})
const sendResult = ref('')
const inboxMessages = ref([])
const sentMessages = ref([])
const messageLoading = ref(false)
const messageError = ref('')
const selectedChatUserId = ref('')
const chatSearchKeyword = ref('')
const showChatQuickMenu = ref(false)
const chatQuickMenuRef = ref(null)
const chatInput = ref('')
const chatError = ref('')
const chatSending = ref(false)
const preferenceTip = ref('')
const preferenceForm = ref({
  fontSize: '14',
  uiDensity: 'comfortable'
})
const securityBindingState = ref({
  username: props.user?.username || '',
  userMail: props.user?.userMail || '',
  phone: props.user?.phone || ''
})
const bindingModal = ref({
  visible: false,
  type: 'email',
  step: 'verifyCurrent',
  currentCode: '',
  currentCodeError: false,
  currentCountdown: 0,
  newValue: '',
  newValueError: false,
  newCode: '',
  newCodeError: false,
  newCountdown: 0,
  sending: false,
  tip: ''
})
const attachmentInputRef = ref(null)
const composeAttachments = ref([])
let messageStream = null
const reportLoading = ref(false)
const reportError = ref('')
const reportFilters = ref({
  granularity: 'month',
  materialId: '',
  storageLocation: ''
})
const reportInventories = ref([])
const reportLogs = ref([])
const reportMaterials = ref([])
const reportDemands = ref([])
const aiFilters = ref({
  storageLocation: '',
  lookbackDays: 30,
  forecastDays: 14
})
const aiPersistLoading = ref(false)
const aiPersistError = ref('')
const aiPersistTip = ref('')
const aiPersistedRecords = ref([])
const aiRawVisible = ref(false)
const aiRawLoading = ref(false)
const aiRawError = ref('')
const aiRawTitle = ref('')
const aiRawContent = ref('')
const aiConfigLoading = ref(false)
const aiConfigCheck = ref({
  baseUrlConfigured: false,
  deepseekApiConfigured: false,
  baseUrlMasked: '未读取到',
  deepseekApiMasked: '未读取到'
})
const aiSubmittingDraft = ref(false)
const aiDraftTip = ref('')

const PIE_COLORS = ['#3f77e8', '#4caf50', '#ff9800', '#9c27b0', '#00acc1', '#f44336', '#8bc34a', '#ff5722']
let currentCodeTimer = null
let newCodeTimer = null

const currentUserId = computed(() => String(props.user?.userId || '').trim())
const profileInfo = computed(() => ({
  realName: props.user?.realName || '未设置',
  userId: props.user?.userId || '未设置',
  username: props.user?.username || '未设置',
  roleName: props.user?.role?.roleName || props.user?.role?.roleCode || '未设置'
}))

const securityBindingInfo = computed(() => ({
  username: securityBindingState.value.username || props.user?.username || '未绑定',
  userMail: securityBindingState.value.userMail || '未绑定',
  phone: securityBindingState.value.phone || '未绑定'
}))

const REGEXP_LOCAL_EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const REGEXP_LOCAL_PHONE = /^1\d{10}$/
const REGEXP_CODE_SIX = /^\d{6}$/

const persistUserInfoToLocal = () => {
  try {
    const raw = localStorage.getItem('userInfo')
    if (!raw) return
    const user = JSON.parse(raw)
    user.userMail = securityBindingState.value.userMail || ''
    user.phone = securityBindingState.value.phone || ''
    localStorage.setItem('userInfo', JSON.stringify(user))
  } catch (e) {
    // ignore parse error
  }
}

const loadSecurityBindings = async () => {
  try {
    const res = await request.get('/users/security/bindings')
    if (res.code === 200 && res.data) {
      securityBindingState.value = {
        username: res.data.username || props.user?.username || '',
        userMail: res.data.userMail || '',
        phone: res.data.phone || ''
      }
      persistUserInfoToLocal()
    }
  } catch (e) {
    // ignore load error
  }
}

const clearBindingCountdownTimers = () => {
  if (currentCodeTimer) {
    clearInterval(currentCodeTimer)
    currentCodeTimer = null
  }
  if (newCodeTimer) {
    clearInterval(newCodeTimer)
    newCodeTimer = null
  }
}

const startCurrentCountdown = () => {
  bindingModal.value.currentCountdown = 60
  if (currentCodeTimer) {
    clearInterval(currentCodeTimer)
  }
  currentCodeTimer = setInterval(() => {
    if (bindingModal.value.currentCountdown <= 1) {
      bindingModal.value.currentCountdown = 0
      clearInterval(currentCodeTimer)
      currentCodeTimer = null
      return
    }
    bindingModal.value.currentCountdown -= 1
  }, 1000)
}

const startNewCountdown = () => {
  bindingModal.value.newCountdown = 60
  if (newCodeTimer) {
    clearInterval(newCodeTimer)
  }
  newCodeTimer = setInterval(() => {
    if (bindingModal.value.newCountdown <= 1) {
      bindingModal.value.newCountdown = 0
      clearInterval(newCodeTimer)
      newCodeTimer = null
      return
    }
    bindingModal.value.newCountdown -= 1
  }, 1000)
}

const openBindingModal = (type) => {
  const isEmail = type === 'email'
  const hasCurrentBinding = isEmail
    ? !!(securityBindingState.value.userMail || '').trim()
    : !!(securityBindingState.value.phone || '').trim()

  bindingModal.value = {
    visible: true,
    type,
    step: hasCurrentBinding ? 'verifyCurrent' : 'editNew',
    currentCode: '',
    currentCodeError: false,
    currentCountdown: 0,
    newValue: '',
    newValueError: false,
    newCode: '',
    newCodeError: false,
    newCountdown: 0,
    sending: false,
    tip: hasCurrentBinding
      ? '请先验证当前绑定信息。'
      : '当前未绑定，可直接进入新绑定验证。'
  }
}

const closeBindingModal = () => {
  clearBindingCountdownTimers()
  bindingModal.value.currentCountdown = 0
  bindingModal.value.newCountdown = 0
  bindingModal.value.visible = false
}

const sendCurrentBindingCode = async () => {
  if (bindingModal.value.currentCountdown > 0) {
    return
  }
  bindingModal.value.sending = true
  bindingModal.value.tip = ''
  bindingModal.value.currentCodeError = false
  try {
    const res = await request.post('/users/security/send-current-code', null, {
      params: { type: bindingModal.value.type }
    })
    if (res.code === 200) {
      startCurrentCountdown()
    }
    bindingModal.value.tip = res.code === 200 ? (res.data || '验证码已发送') : (res.msg || '发送失败')
  } catch (e) {
    bindingModal.value.tip = e?.response?.data?.msg || '发送失败'
  } finally {
    bindingModal.value.sending = false
  }
}

const verifyCurrentBindingCode = async () => {
  const code = (bindingModal.value.currentCode || '').trim()
  bindingModal.value.currentCodeError = false
  if (!REGEXP_CODE_SIX.test(code)) {
    bindingModal.value.currentCodeError = true
    bindingModal.value.tip = '请输入6位验证码'
    return
  }
  try {
    const res = await request.post('/users/security/verify-current-code', null, {
      params: {
        type: bindingModal.value.type,
        code
      }
    })
    if (res.code === 200) {
      bindingModal.value.step = 'editNew'
      bindingModal.value.currentCode = ''
      bindingModal.value.tip = '旧绑定验证通过，请继续填写新绑定信息。'
      return
    }
    bindingModal.value.tip = res.msg || '验证失败'
  } catch (e) {
    bindingModal.value.tip = e?.response?.data?.msg || '验证失败'
  }
}

const sendNewBindingCode = async () => {
  const newValue = (bindingModal.value.newValue || '').trim()
  bindingModal.value.newValueError = false
  if (bindingModal.value.type === 'email' && !REGEXP_LOCAL_EMAIL.test(newValue)) {
    bindingModal.value.newValueError = true
    bindingModal.value.tip = '新邮箱格式不正确'
    return
  }
  if (bindingModal.value.type === 'phone' && !REGEXP_LOCAL_PHONE.test(newValue)) {
    bindingModal.value.newValueError = true
    bindingModal.value.tip = '新手机号格式不正确'
    return
  }

  if (bindingModal.value.newCountdown > 0) {
    return
  }

  bindingModal.value.sending = true
  bindingModal.value.tip = ''
  try {
    const res = await request.post('/users/security/send-new-code', null, {
      params: {
        type: bindingModal.value.type,
        target: newValue
      }
    })
    if (res.code === 200) {
      startNewCountdown()
    }
    bindingModal.value.tip = res.code === 200 ? (res.data || '验证码已发送') : (res.msg || '发送失败')
  } catch (e) {
    bindingModal.value.tip = e?.response?.data?.msg || '发送失败'
  } finally {
    bindingModal.value.sending = false
  }
}

const submitBindingUpdate = async () => {
  const newValue = (bindingModal.value.newValue || '').trim()
  const newCode = (bindingModal.value.newCode || '').trim()

  bindingModal.value.newValueError = false
  bindingModal.value.newCodeError = false

  if (bindingModal.value.type === 'email' && !REGEXP_LOCAL_EMAIL.test(newValue)) {
    bindingModal.value.newValueError = true
    bindingModal.value.tip = '新邮箱格式不正确'
    return
  }
  if (bindingModal.value.type === 'phone' && !REGEXP_LOCAL_PHONE.test(newValue)) {
    bindingModal.value.newValueError = true
    bindingModal.value.tip = '新手机号格式不正确'
    return
  }
  if (!REGEXP_CODE_SIX.test(newCode)) {
    bindingModal.value.newCodeError = true
    bindingModal.value.tip = '请输入6位验证码'
    return
  }

  try {
    const res = await request.post('/users/security/update-binding', null, {
      params: {
        type: bindingModal.value.type,
        target: newValue,
        code: newCode
      }
    })
    if (res.code === 200) {
      bindingModal.value.tip = '修改成功'
      await loadSecurityBindings()
      setTimeout(() => {
        closeBindingModal()
      }, 600)
      return
    }
    bindingModal.value.tip = res.msg || '修改失败'
  } catch (e) {
    bindingModal.value.tip = e?.response?.data?.msg || '修改失败'
  }
}

const getPreferenceStorageKey = () => `ui-preferences:${currentUserId.value || 'anonymous'}`

const applyUserPreferences = () => {
  const size = Number(preferenceForm.value.fontSize || 14)
  const safeSize = Number.isFinite(size) ? Math.min(18, Math.max(13, size)) : 14
  document.documentElement.style.setProperty('--app-user-font-size', `${safeSize}px`)
}

const loadUserPreferences = () => {
  try {
    const raw = localStorage.getItem(getPreferenceStorageKey())
    if (!raw) return
    const parsed = JSON.parse(raw)
    preferenceForm.value = {
      fontSize: String(parsed?.fontSize || '14'),
      uiDensity: parsed?.uiDensity === 'compact' ? 'compact' : 'comfortable'
    }
  } catch (e) {
    // ignore parse error
  }
}

const saveUserPreferences = () => {
  localStorage.setItem(getPreferenceStorageKey(), JSON.stringify(preferenceForm.value))
  preferenceTip.value = '已保存'
  setTimeout(() => {
    preferenceTip.value = ''
  }, 1200)
}

const resetUserPreferences = () => {
  preferenceForm.value = {
    fontSize: '14',
    uiDensity: 'comfortable'
  }
  saveUserPreferences()
}

const formatDateTime = (v) => {
  if (!v) return '—'
  return String(v).replace('T', ' ').substring(0, 19)
}

const formatShortTime = (v) => {
  if (!v) return ''
  const text = formatDateTime(v)
  return text.length >= 16 ? text.substring(5, 16) : text
}

const getMessageTs = (v) => {
  const ts = Date.parse(v)
  return Number.isNaN(ts) ? 0 : ts
}

const toNum = (v, d = 0) => {
  const n = Number(v)
  return Number.isFinite(n) ? n : d
}

const formatBucket = (timeValue, granularity) => {
  const d = new Date(timeValue)
  if (Number.isNaN(d.getTime())) return ''
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  if (granularity === 'day') return `${y}-${m}-${day}`
  if (granularity === 'month') return `${y}-${m}`
  if (granularity === 'year') return String(y)
  const oneJan = new Date(d.getFullYear(), 0, 1)
  const week = Math.ceil((((d - oneJan) / 86400000) + oneJan.getDay() + 1) / 7)
  return `${y}-W${String(week).padStart(2, '0')}`
}

const reportInventoryMap = computed(() => {
  const map = new Map()
  reportInventories.value.forEach((i) => {
    if (i?.inventoryId) {
      map.set(i.inventoryId, i)
    }
  })
  return map
})

const reportMaterialMap = computed(() => {
  const map = new Map()
  reportMaterials.value.forEach((m) => {
    if (m?.materialId) {
      map.set(m.materialId, m)
    }
  })
  return map
})

const reportMaterialOptions = computed(() => {
  return reportMaterials.value.map((m) => ({ materialId: m.materialId, materialName: m.materialName || m.materialId }))
})

const reportLocationOptions = computed(() => {
  const set = new Set()
  reportInventories.value.forEach((i) => {
    if (i?.storageLocation) set.add(i.storageLocation)
  })
  return Array.from(set)
})

const filteredReportInventories = computed(() => {
  return reportInventories.value.filter((i) => {
    if (reportFilters.value.materialId && i.materialId !== reportFilters.value.materialId) return false
    if (reportFilters.value.storageLocation && i.storageLocation !== reportFilters.value.storageLocation) return false
    return true
  })
})

const filteredReportLogs = computed(() => {
  return reportLogs.value
    .filter((l) => l?.logStatus == null || Number(l.logStatus) === 1)
    .filter((l) => {
      if (reportFilters.value.materialId && l.materialId !== reportFilters.value.materialId) return false
      if (reportFilters.value.storageLocation) {
        const inv = reportInventoryMap.value.get(l.inventoryId)
        if (!inv || inv.storageLocation !== reportFilters.value.storageLocation) return false
      }
      return true
    })
})

const trendRows = computed(() => {
  const rows = []
  const bucketMap = new Map()
  const sorted = [...filteredReportLogs.value].sort((a, b) => getMessageTs(a.operateTime) - getMessageTs(b.operateTime))
  sorted.forEach((log) => {
    const bucket = formatBucket(log.operateTime, reportFilters.value.granularity)
    if (!bucket) return

    if (!bucketMap.has(bucket)) {
      bucketMap.set(bucket, { inbound: 0, outbound: 0, entities: new Map() })
    }
    const bucketObj = bucketMap.get(bucket)
    const qty = Math.abs(toNum(log.changeQuantity, 0))
    if (Number(log.changeType) === 2) bucketObj.outbound += qty
    else bucketObj.inbound += qty

    const entityKey = `${log.inventoryId || ''}_${log.materialId || ''}`
    if (!bucketObj.entities.has(entityKey)) {
      bucketObj.entities.set(entityKey, {
        start: toNum(log.beforeQuantity, 0),
        end: toNum(log.afterQuantity, 0)
      })
    } else {
      const entity = bucketObj.entities.get(entityKey)
      entity.end = toNum(log.afterQuantity, entity.end)
    }
  })

  Array.from(bucketMap.entries())
    .sort((a, b) => a[0].localeCompare(b[0]))
    .forEach(([bucket, data]) => {
      let periodStart = 0
      let periodEnd = 0
      data.entities.forEach((entity) => {
        periodStart += toNum(entity.start)
        periodEnd += toNum(entity.end)
      })
      rows.push({
        bucket,
        periodStart: Number(periodStart.toFixed(2)),
        inbound: Number(data.inbound.toFixed(2)),
        outbound: Number(data.outbound.toFixed(2)),
        periodEnd: Number(periodEnd.toFixed(2))
      })
    })

  return rows
})

const trendChartData = computed(() => {
  const rows = trendRows.value
  if (!rows.length) return null

  const padding = { top: 20, right: 24, bottom: 38, left: 56 }
  const chartHeight = 260
  const rowWidth = 64
  const gap = 14
  const minPlotWidth = 860
  const dynamicPlotWidth = rows.length * (rowWidth + gap)
  const plotWidth = Math.max(minPlotWidth, dynamicPlotWidth)
  const width = padding.left + padding.right + plotWidth
  const height = padding.top + padding.bottom + chartHeight

  const maxValue = Math.max(
    1,
    ...rows.map((r) => Math.max(
      toNum(r.inbound),
      toNum(r.outbound),
      toNum(r.periodEnd)
    ))
  )

  const toY = (v) => padding.top + chartHeight - (toNum(v) / maxValue) * chartHeight
  const yTicks = [0, 0.25, 0.5, 0.75, 1].map((ratio) => {
    const value = Number((maxValue * ratio).toFixed(0))
    return {
      value,
      y: toY(value)
    }
  })

  const slot = plotWidth / rows.length
  const barWidth = Math.max(8, Math.min(20, slot * 0.22))
  const items = rows.map((r, idx) => {
    const centerX = padding.left + (idx + 0.5) * slot
    const inboundY = toY(r.inbound)
    const outboundY = toY(r.outbound)
    const lineY = toY(r.periodEnd)
    return {
      bucket: r.bucket,
      centerX,
      barWidth,
      inboundX: centerX - barWidth - 3,
      outboundX: centerX + 3,
      inboundY,
      outboundY,
      inboundHeight: Math.max(0, padding.top + chartHeight - inboundY),
      outboundHeight: Math.max(0, padding.top + chartHeight - outboundY),
      lineY
    }
  })

  const linePoints = items.map((i) => `${i.centerX},${i.lineY}`).join(' ')
  return {
    width,
    height,
    padding,
    yTicks,
    items,
    linePoints
  }
})

const averageUnitPrice = computed(() => {
  const arr = filteredReportInventories.value
  if (!arr.length) return 0
  let sumValue = 0
  let sumQty = 0
  arr.forEach((i) => {
    const mat = reportMaterialMap.value.get(i.materialId)
    const unitPrice = toNum(mat?.unitPrice, 0)
    const qty = toNum(i.stockQuantity, 0)
    sumValue += qty * unitPrice
    sumQty += qty
  })
  return sumQty > 0 ? sumValue / sumQty : 0
})

const outboundAmount = computed(() => {
  return filteredReportLogs.value.reduce((sum, log) => {
    if (Number(log.changeType) !== 2) return sum
    const mat = reportMaterialMap.value.get(log.materialId)
    const unitPrice = toNum(mat?.unitPrice, 0)
    return sum + Math.abs(toNum(log.changeQuantity, 0)) * unitPrice
  }, 0)
})

const averageInventoryAmount = computed(() => {
  if (!trendRows.value.length) return 0
  const first = trendRows.value[0]
  const last = trendRows.value[trendRows.value.length - 1]
  const avgQty = (toNum(first.periodStart) + toNum(last.periodEnd)) / 2
  return avgQty * averageUnitPrice.value
})

const turnoverRate = computed(() => {
  if (averageInventoryAmount.value <= 0) return 0
  return outboundAmount.value / averageInventoryAmount.value
})

const inventoryWarningShortageRate = computed(() => {
  const logs = filteredReportLogs.value
  if (!logs.length) return 0
  const allDays = new Set()
  const shortageDays = new Set()
  logs.forEach((l) => {
    const day = formatBucket(l.operateTime, 'day')
    if (!day) return
    allDays.add(day)
    const mat = reportMaterialMap.value.get(l.materialId)
    const minThreshold = toNum(mat?.minThreshold, 0)
    if (toNum(l.afterQuantity, 0) < minThreshold) {
      shortageDays.add(day)
    }
  })
  if (!allDays.size) return 0
  return shortageDays.size / allDays.size
})

const demandSatisfyShortageRate = computed(() => {
  const arr = reportDemands.value.filter((d) => {
    if (reportFilters.value.materialId && d.materialId !== reportFilters.value.materialId) return false
    return true
  })
  if (!arr.length) return 0
  const completed = arr.filter((d) => Number(d.demandStatus) === 4).length
  return 1 - (completed / arr.length)
})

const overallShortageRate = computed(() => (inventoryWarningShortageRate.value + demandSatisfyShortageRate.value) / 2)

const makePieParts = (rawItems) => {
  const total = rawItems.reduce((s, i) => s + toNum(i.value, 0), 0)
  if (total <= 0) return []
  return rawItems
    .filter((i) => toNum(i.value, 0) > 0)
    .map((i, idx) => ({
      name: i.name,
      value: toNum(i.value, 0),
      percent: (toNum(i.value, 0) / total) * 100,
      color: PIE_COLORS[idx % PIE_COLORS.length]
    }))
}

const currentInventoryPieParts = computed(() => {
  const group = new Map()
  filteredReportInventories.value.forEach((inv) => {
    const mat = reportMaterialMap.value.get(inv.materialId)
    const key = mat?.materialName || inv.materialName || inv.materialId || '未知物料'
    group.set(key, (group.get(key) || 0) + toNum(inv.stockQuantity, 0))
  })
  return makePieParts(Array.from(group.entries()).map(([name, value]) => ({ name, value })))
})

const historyChangePieParts = computed(() => {
  const map = new Map([
    ['入库', 0],
    ['出库', 0],
    ['盘点调整', 0],
    ['退货入库', 0]
  ])
  filteredReportLogs.value.forEach((log) => {
    const type = Number(log.changeType)
    const qty = Math.abs(toNum(log.changeQuantity, 0))
    if (type === 1) map.set('入库', map.get('入库') + qty)
    else if (type === 2) map.set('出库', map.get('出库') + qty)
    else if (type === 3) map.set('盘点调整', map.get('盘点调整') + qty)
    else if (type === 4) map.set('退货入库', map.get('退货入库') + qty)
  })
  return makePieParts(Array.from(map.entries()).map(([name, value]) => ({ name, value })))
})

const buildPieGradient = (parts) => {
  if (!parts.length) {
    return 'conic-gradient(#e5ebf3 0deg 360deg)'
  }
  let acc = 0
  const segs = parts.map((p) => {
    const start = acc
    acc += (p.percent / 100) * 360
    return `${p.color} ${start.toFixed(2)}deg ${acc.toFixed(2)}deg`
  })
  return `conic-gradient(${segs.join(', ')})`
}

const currentInventoryPieGradient = computed(() => buildPieGradient(currentInventoryPieParts.value))
const historyChangePieGradient = computed(() => buildPieGradient(historyChangePieParts.value))

const loadInventoryReportData = async () => {
  reportLoading.value = true
  reportError.value = ''
  try {
    const [inventoryRes, logRes, materialRes, demandRes] = await Promise.all([
      request.get('/inventories/list', { params: { pageNum: 1, pageSize: 1000 } }),
      request.get('/inventory-logs/list', { params: { pageNum: 1, pageSize: 2000 } }),
      request.get('/materials/list'),
      request.get('/material-demands/list', { params: { pageNum: 1, pageSize: 1000 } })
    ])

    const inventoryPage = inventoryRes?.data || {}
    const logPage = logRes?.data || {}
    const demandPage = demandRes?.data || {}

    reportInventories.value = Array.isArray(inventoryPage.records) ? inventoryPage.records : []
    reportLogs.value = Array.isArray(logPage.records) ? logPage.records : []
    reportMaterials.value = Array.isArray(materialRes?.data) ? materialRes.data : []
    reportDemands.value = Array.isArray(demandPage.records) ? demandPage.records : []
  } catch (e) {
    reportError.value = e?.response?.data?.msg || '统计分析数据加载失败'
  } finally {
    reportLoading.value = false
  }
}

const loadAiPersistedResults = async () => {
  aiPersistLoading.value = true
  aiPersistError.value = ''
  try {
    const res = await request.get('/ai-analysis/inventory/list', {
      params: {
        pageNum: 1,
        pageSize: 30
      }
    })
    if (res.code === 200) {
      const page = res.data || {}
      aiPersistedRecords.value = Array.isArray(page.records) ? page.records : []
    } else {
      aiPersistError.value = res.msg || '加载AI入库记录失败'
    }
  } catch (e) {
    aiPersistError.value = e?.response?.data?.msg || '加载AI入库记录失败'
  } finally {
    aiPersistLoading.value = false
  }
}

const loadAiConfigCheck = async () => {
  aiConfigLoading.value = true
  try {
    const res = await request.get('/ai-analysis/inventory/config-check')
    if (res.code === 200 && res.data) {
      aiConfigCheck.value = {
        baseUrlConfigured: !!res.data.baseUrlConfigured,
        deepseekApiConfigured: !!res.data.deepseekApiConfigured,
        baseUrlMasked: res.data.baseUrlMasked || '未读取到',
        deepseekApiMasked: res.data.deepseekApiMasked || '未读取到'
      }
      return
    }
  } catch (e) {
    // ignore
  } finally {
    aiConfigLoading.value = false
  }

  aiConfigCheck.value = {
    baseUrlConfigured: false,
    deepseekApiConfigured: false,
    baseUrlMasked: '读取失败',
    deepseekApiMasked: '读取失败'
  }
}

const runPersistedAiAnalysis = async () => {
  aiPersistTip.value = ''
  aiPersistError.value = ''
  try {
    const now = new Date()
    const end = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:00`
    const startDate = new Date(now.getTime() - Number(aiFilters.value.lookbackDays || 30) * 24 * 60 * 60 * 1000)
    const start = `${startDate.getFullYear()}-${String(startDate.getMonth() + 1).padStart(2, '0')}-${String(startDate.getDate()).padStart(2, '0')} ${String(startDate.getHours()).padStart(2, '0')}:${String(startDate.getMinutes()).padStart(2, '0')}:00`

    const body = {
      createUserId: currentUserId.value,
      timeGranularity: 1,
      startTime: start,
      endTime: end,
      storageLocation: aiFilters.value.storageLocation || null,
      stockoutCalcType: 2
    }
    const res = await request.post('/ai-analysis/inventory/run', body, {
      timeout: 120000
    })
    if (res.code === 200) {
      const records = Array.isArray(res?.data?.records) ? res.data.records : []
      aiPersistTip.value = `AI分析完成并入库 ${records.length} 条记录。`
      await loadAiPersistedResults()
    } else {
      aiPersistError.value = res.msg || 'AI分析执行失败'
    }
  } catch (e) {
    if (e?.code === 'ECONNABORTED') {
      aiPersistError.value = 'AI分析超时，请稍后重试（建议等待10-30秒）'
      return
    }
    aiPersistError.value = e?.response?.data?.msg || 'AI分析执行失败'
  }
}

const aiAnalysisTypeText = (type) => {
  if (Number(type) === 1) return '库存趋势'
  if (Number(type) === 2) return '周转率'
  if (Number(type) === 3) return '缺货率'
  return '未知'
}

const formatAiRawJson = (raw) => {
  if (raw == null || raw === '') return '暂无原始结果'
  if (typeof raw === 'string') {
    const trimmed = raw.trim()
    if (!trimmed) return '暂无原始结果'
    try {
      return JSON.stringify(JSON.parse(trimmed), null, 2)
    } catch (e) {
      return trimmed
    }
  }
  try {
    return JSON.stringify(raw, null, 2)
  } catch (e) {
    return String(raw)
  }
}

const openAiRawResult = async (row) => {
  if (!row?.analysisId) return
  aiRawVisible.value = true
  aiRawLoading.value = true
  aiRawError.value = ''
  aiRawContent.value = ''
  aiRawTitle.value = `分析ID：${row.analysisId}`
  try {
    const res = await request.get('/ai-analysis/inventory/result', {
      params: { analysisId: row.analysisId }
    })
    if (res.code === 200) {
      const payload = res.data || {}
      aiRawContent.value = formatAiRawJson(payload.analysisResult)
    } else {
      aiRawError.value = res.msg || '加载原始结果失败'
    }
  } catch (e) {
    aiRawError.value = e?.response?.data?.msg || '加载原始结果失败'
  } finally {
    aiRawLoading.value = false
  }
}

const closeAiRawResult = () => {
  aiRawVisible.value = false
  aiRawLoading.value = false
  aiRawError.value = ''
  aiRawTitle.value = ''
  aiRawContent.value = ''
}

const aiFilteredInventories = computed(() => {
  return reportInventories.value.filter((inv) => {
    if (!aiFilters.value.storageLocation) return true
    return inv.storageLocation === aiFilters.value.storageLocation
  })
})

const aiFilteredLogs = computed(() => {
  return reportLogs.value
    .filter((l) => l?.logStatus == null || Number(l.logStatus) === 1)
    .filter((l) => {
      if (!aiFilters.value.storageLocation) return true
      const inv = reportInventoryMap.value.get(l.inventoryId)
      return !!inv && inv.storageLocation === aiFilters.value.storageLocation
    })
})

const aiRelevantMaterialIds = computed(() => {
  const set = new Set()
  reportMaterials.value.forEach((m) => m?.materialId && set.add(m.materialId))
  aiFilteredInventories.value.forEach((i) => i?.materialId && set.add(i.materialId))
  aiFilteredLogs.value.forEach((l) => l?.materialId && set.add(l.materialId))
  reportDemands.value.forEach((d) => d?.materialId && set.add(d.materialId))
  return Array.from(set)
})

const aiLatestLogTime = computed(() => {
  let maxTs = 0
  aiFilteredLogs.value.forEach((l) => {
    const ts = getMessageTs(l?.operateTime)
    if (ts > maxTs) maxTs = ts
  })
  return maxTs || Date.now()
})

const buildDailyOutboundSeries = (materialId, lookbackDays, fromTs) => {
  const endDate = new Date(fromTs)
  endDate.setHours(0, 0, 0, 0)
  const startTs = endDate.getTime() - (lookbackDays - 1) * 24 * 60 * 60 * 1000

  const dayMap = new Map()
  aiFilteredLogs.value.forEach((log) => {
    if (log.materialId !== materialId) return
    if (Number(log.changeType) !== 2) return
    const ts = getMessageTs(log.operateTime)
    if (!ts || ts < startTs || ts > endDate.getTime() + 24 * 60 * 60 * 1000 - 1) return
    const day = formatBucket(log.operateTime, 'day')
    dayMap.set(day, (dayMap.get(day) || 0) + Math.abs(toNum(log.changeQuantity, 0)))
  })

  const values = []
  for (let i = 0; i < lookbackDays; i += 1) {
    const d = new Date(startTs + i * 24 * 60 * 60 * 1000)
    const dayKey = formatBucket(d, 'day')
    values.push(toNum(dayMap.get(dayKey), 0))
  }
  return values
}

const toSparklinePoints = (values) => {
  const width = 120
  const height = 34
  const paddingX = 4
  const paddingY = 3
  if (!Array.isArray(values) || values.length === 0) {
    return `${paddingX},${height - paddingY} ${width - paddingX},${height - paddingY}`
  }
  const max = Math.max(1, ...values.map(v => toNum(v, 0)))
  const step = values.length > 1 ? (width - paddingX * 2) / (values.length - 1) : 0
  return values
    .map((v, idx) => {
      const x = paddingX + idx * step
      const y = height - paddingY - ((toNum(v, 0) / max) * (height - paddingY * 2))
      return `${x.toFixed(2)},${y.toFixed(2)}`
    })
    .join(' ')
}

const aiAnalysisRows = computed(() => {
  const lookbackDays = Math.max(1, toNum(aiFilters.value.lookbackDays, 30))
  const forecastDays = Math.max(1, toNum(aiFilters.value.forecastDays, 14))
  const latestTs = aiLatestLogTime.value

  const inventoryByMaterial = new Map()
  aiFilteredInventories.value.forEach((inv) => {
    inventoryByMaterial.set(inv.materialId, (inventoryByMaterial.get(inv.materialId) || 0) + toNum(inv.stockQuantity, 0))
  })

  const pendingDemandByMaterial = new Map()
  reportDemands.value.forEach((d) => {
    if (!d?.materialId) return
    if (Number(d.demandStatus) === 4) return
    pendingDemandByMaterial.set(d.materialId, (pendingDemandByMaterial.get(d.materialId) || 0) + toNum(d.demandQuantity, 0))
  })

  const rows = aiRelevantMaterialIds.value.map((materialId) => {
    const mat = reportMaterialMap.value.get(materialId) || {}
    const materialName = mat.materialName || materialId
    const currentStock = toNum(inventoryByMaterial.get(materialId), 0)
    const minThreshold = toNum(mat.minThreshold, 0)
    const pendingDemand = toNum(pendingDemandByMaterial.get(materialId), 0)

    const outboundSeries = buildDailyOutboundSeries(materialId, lookbackDays, latestTs)
    const totalOutbound = outboundSeries.reduce((s, v) => s + v, 0)
    const dailyOutboundAvg = totalOutbound / lookbackDays

    const variance = outboundSeries.reduce((s, v) => s + Math.pow(v - dailyOutboundAvg, 2), 0) / lookbackDays
    const stdDev = Math.sqrt(variance)
    const volatilityRatio = dailyOutboundAvg > 0 ? stdDev / dailyOutboundAvg : 0
    const volatilityRisk = Math.min(1, volatilityRatio / 1.5)

    const forecastDemand = dailyOutboundAvg * forecastDays
    const safetyStock = dailyOutboundAvg * 7
    const targetStock = forecastDemand + safetyStock + pendingDemand
    const suggestedReplenish = Math.max(0, targetStock - currentStock)
    const runoutDays = dailyOutboundAvg > 0 ? currentStock / dailyOutboundAvg : Number.POSITIVE_INFINITY

    const gapRatio = targetStock > 0 ? suggestedReplenish / targetStock : 0
    const thresholdRisk = minThreshold > 0 && currentStock < minThreshold
      ? Math.min(1, (minThreshold - currentStock) / minThreshold)
      : 0
    const demandPressure = targetStock > 0 ? Math.min(1, pendingDemand / targetStock) : 0

    const alertScore = Math.min(
      100,
      gapRatio * 55 + demandPressure * 20 + volatilityRisk * 15 + thresholdRisk * 10
    )

    let riskLevel = '低'
    let riskLevelCode = 'low'
    if (alertScore >= 75) {
      riskLevel = '高'
      riskLevelCode = 'high'
    } else if (alertScore >= 45) {
      riskLevel = '中'
      riskLevelCode = 'medium'
    }

    let aiComment = '库存状态稳定。'
    if (suggestedReplenish > 0 && alertScore >= 75) {
      aiComment = `短期存在明显缺口，建议优先补货 ${suggestedReplenish.toFixed(2)}。`
    } else if (suggestedReplenish > 0) {
      aiComment = `建议按需补货 ${suggestedReplenish.toFixed(2)}，并持续观察波动。`
    } else if (dailyOutboundAvg <= 0.01) {
      aiComment = '近期出库较低，可暂缓补货。'
    }

    return {
      materialId,
      materialName,
      outboundSeries,
      sparklinePoints: toSparklinePoints(outboundSeries),
      currentStock,
      dailyOutboundAvg,
      forecastDemand,
      pendingDemand,
      suggestedReplenish,
      runoutDays,
      alertScore,
      riskLevel,
      riskLevelCode,
      aiComment
    }
  })

  return rows
    .filter((r) => r.currentStock > 0 || r.dailyOutboundAvg > 0 || r.pendingDemand > 0)
    .sort((a, b) => b.alertScore - a.alertScore)
})

const aiTopSuggestions = computed(() => {
  return aiAnalysisRows.value
    .filter((r) => r.suggestedReplenish > 0)
    .slice(0, 5)
})

const aiSummary = computed(() => {
  if (!aiAnalysisRows.value.length) {
    return {
      overallScore: 100,
      highRiskCount: 0,
      totalForecast: 0,
      totalSuggested: 0
    }
  }
  const totalForecast = aiAnalysisRows.value.reduce((s, r) => s + r.forecastDemand, 0)
  const totalSuggested = aiAnalysisRows.value.reduce((s, r) => s + r.suggestedReplenish, 0)
  const highRiskCount = aiAnalysisRows.value.filter((r) => r.riskLevelCode === 'high').length
  const avgAlert = aiAnalysisRows.value.reduce((s, r) => s + r.alertScore, 0) / aiAnalysisRows.value.length
  const overallScore = Math.max(0, 100 - avgAlert)
  return {
    overallScore,
    highRiskCount,
    totalForecast,
    totalSuggested
  }
})

const aiRiskBuckets = computed(() => {
  return {
    high: aiAnalysisRows.value.filter((r) => r.riskLevelCode === 'high').slice(0, 8),
    medium: aiAnalysisRows.value.filter((r) => r.riskLevelCode === 'medium').slice(0, 8),
    low: aiAnalysisRows.value.filter((r) => r.riskLevelCode === 'low').slice(0, 8)
  }
})

const formatDraftUseTime = (daysLater = 1) => {
  const d = new Date(Date.now() + daysLater * 24 * 60 * 60 * 1000)
  d.setMinutes(0, 0, 0)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day}T${hh}:${mm}`
}

const generateAiReplenishDrafts = async () => {
  if (aiSubmittingDraft.value) return
  if (!currentUserId.value) {
    aiDraftTip.value = '未识别当前用户，无法生成草稿。'
    return
  }

  const targets = aiTopSuggestions.value
    .filter((r) => r.suggestedReplenish > 0)
    .slice(0, 5)
  if (!targets.length) {
    aiDraftTip.value = '当前没有可生成的补货建议。'
    return
  }

  aiSubmittingDraft.value = true
  aiDraftTip.value = ''
  let success = 0
  let failed = 0

  try {
    for (const item of targets) {
      const payload = {
        demandUserId: currentUserId.value,
        materialId: item.materialId,
        demandQuantity: Math.max(1, Number(item.suggestedReplenish.toFixed(0))),
        useTime: formatDraftUseTime(1),
        sitePhotoUrl: null
      }
      try {
        const res = await request.post('/material-demands', payload)
        if (res?.code === 200) success += 1
        else failed += 1
      } catch (e) {
        failed += 1
      }
    }
    aiDraftTip.value = `补货单草稿生成完成：成功 ${success} 条，失败 ${failed} 条。`
    if (success > 0) {
      await loadInventoryReportData()
    }
  } finally {
    aiSubmittingDraft.value = false
  }
}

const aiConclusions = computed(() => {
  const rows = aiAnalysisRows.value
  if (!rows.length) {
    return ['当前没有可用于AI分析的库存与出库数据，请先录入库存或变动日志。']
  }
  const top = rows[0]
  const highRiskCount = rows.filter((r) => r.riskLevelCode === 'high').length
  const mediumRiskCount = rows.filter((r) => r.riskLevelCode === 'medium').length
  const lines = [
    `当前纳入分析物料 ${rows.length} 项，高风险 ${highRiskCount} 项，中风险 ${mediumRiskCount} 项。`,
    `最高风险物料为 ${top.materialName}（评分 ${top.alertScore.toFixed(1)}），建议补货 ${top.suggestedReplenish.toFixed(2)}。`,
    `未来 ${aiFilters.value.forecastDays} 天预测总需求为 ${aiSummary.value.totalForecast.toFixed(2)}，建议总补货量 ${aiSummary.value.totalSuggested.toFixed(2)}。`
  ]
  if (aiSummary.value.overallScore >= 80) {
    lines.push('库存整体健康度较好，可按周例行巡检。')
  } else if (aiSummary.value.overallScore >= 60) {
    lines.push('库存存在一定压力，建议关注高频出库物料并滚动补货。')
  } else {
    lines.push('库存风险偏高，建议立即执行高风险物料补货并提高安全库存阈值。')
  }
  return lines
})

const formatAiRunoutDays = (days) => {
  if (!Number.isFinite(days)) return '∞'
  if (days > 999) return '999+'
  return `${days.toFixed(1)}天`
}

const chatSessions = computed(() => {
  const me = currentUserId.value
  if (!me) return []

  const map = new Map()
  const upsert = (msg, fromInbox) => {
    const isMine = msg?.senderId === me
    const peerId = isMine ? msg?.receiverId : msg?.senderId
    const peerName = isMine ? (msg?.receiverName || msg?.receiverId) : (msg?.senderName || msg?.senderId)
    if (!peerId) return

    const ts = getMessageTs(msg?.sendTime)
    if (!map.has(peerId)) {
      map.set(peerId, {
        userId: peerId,
        displayName: peerName || peerId,
        lastTime: msg?.sendTime || '',
        lastTs: ts,
        lastContent: msg?.content || '',
        unreadCount: 0
      })
    } else {
      const old = map.get(peerId)
      if (ts >= old.lastTs) {
        old.lastTs = ts
        old.lastTime = msg?.sendTime || old.lastTime
        old.lastContent = msg?.content || old.lastContent
      }
      if (peerName) {
        old.displayName = peerName
      }
    }

    if (fromInbox && msg?.senderId === peerId && !msg?.read) {
      map.get(peerId).unreadCount += 1
    }
  }

  inboxMessages.value.forEach(msg => upsert(msg, true))
  sentMessages.value.forEach(msg => upsert(msg, false))

  return Array.from(map.values()).sort((a, b) => b.lastTs - a.lastTs)
})

const selectedChat = computed(() => {
  if (!selectedChatUserId.value) return null
  return chatSessions.value.find(s => s.userId === selectedChatUserId.value) || null
})

const filteredChatSessions = computed(() => {
  const keyword = (chatSearchKeyword.value || '').trim().toLowerCase()
  if (!keyword) return chatSessions.value

  return chatSessions.value.filter((session) => {
    const displayName = String(session?.displayName || '').toLowerCase()
    const userId = String(session?.userId || '').toLowerCase()
    const lastContent = String(session?.lastContent || '').toLowerCase()
    return displayName.includes(keyword) || userId.includes(keyword) || lastContent.includes(keyword)
  })
})

const chatHistory = computed(() => {
  const me = currentUserId.value
  const peer = selectedChatUserId.value
  if (!me || !peer) return []

  const all = [
    ...inboxMessages.value.filter(m => m?.senderId === peer || m?.receiverId === peer),
    ...sentMessages.value.filter(m => m?.senderId === peer || m?.receiverId === peer)
  ]

  const dedup = new Map()
  all.forEach((msg) => {
    if (!msg?.messageId) return
    dedup.set(msg.messageId, msg)
  })

  return Array.from(dedup.values()).sort((a, b) => {
    const ta = getMessageTs(a?.sendTime)
    const tb = getMessageTs(b?.sendTime)
    if (ta !== tb) return ta - tb
    return String(a?.messageId || '').localeCompare(String(b?.messageId || ''))
  })
})

const openAttachmentPicker = () => {
  attachmentInputRef.value?.click()
}

const handleAttachmentChange = (e) => {
  const files = Array.from(e?.target?.files || [])
  if (!files.length) return
  composeAttachments.value = [...composeAttachments.value, ...files]
  if (attachmentInputRef.value) {
    attachmentInputRef.value.value = ''
  }
}

const removeAttachment = (idx) => {
  composeAttachments.value.splice(idx, 1)
}

const formatFileSize = (size) => {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(2)} MB`
}

const loadInbox = async () => {
  if (!currentUserId.value) return
  messageLoading.value = true
  messageError.value = ''
  try {
    const res = await request.get('/messages/inbox', { params: { userId: currentUserId.value } })
    if (res.code === 200) {
      inboxMessages.value = Array.isArray(res.data) ? res.data : []
    } else {
      messageError.value = res.msg || '收信加载失败'
    }
  } catch (e) {
    messageError.value = e?.response?.data?.msg || '收信加载失败'
  } finally {
    messageLoading.value = false
  }
}

const loadSent = async () => {
  if (!currentUserId.value) return
  try {
    const res = await request.get('/messages/sent', { params: { userId: currentUserId.value } })
    if (res.code === 200) {
      sentMessages.value = Array.isArray(res.data) ? res.data : []
    }
  } catch (e) {
    console.error('加载已发送消息失败', e)
  }
}

const sendMail = async () => {
  sendResult.value = ''
  const to = (composeForm.value.to || '').trim()
  const content = (composeForm.value.content || '').trim()
  if (!to) {
    sendResult.value = '请输入收件人账号或用户ID'
    return
  }
  if (!content) {
    sendResult.value = '请输入邮件内容'
    return
  }
  try {
    const res = await request.post('/messages/send', {
      receiver: to,
      subject: composeForm.value.subject || '',
      content
    })
    if (res.code === 200) {
      sendResult.value = '发送成功'
      composeForm.value.subject = ''
      composeForm.value.content = ''
      composeAttachments.value = []
      await loadSent()
      return
    }
    sendResult.value = res.msg || '发送失败'
  } catch (e) {
    sendResult.value = e?.response?.data?.msg || '发送失败'
  }
}

const openChat = async (session) => {
  if (!session?.userId) return
  selectedChatUserId.value = session.userId
  chatError.value = ''
  const unread = inboxMessages.value.filter(m => m?.senderId === session.userId && !m?.read)
  if (unread.length) {
    await Promise.all(unread.map(msg => markMessageRead(msg)))
  }
}

const toggleChatQuickMenu = () => {
  showChatQuickMenu.value = !showChatQuickMenu.value
}

const handleChatQuickAction = (action) => {
  showChatQuickMenu.value = false
  if (action === 'add-friend') {
    const keyword = window.prompt('请输入好友姓名或用户ID：', '')
    if (!keyword || !keyword.trim()) return
    chatSearchKeyword.value = keyword.trim()
    return
  }
  if (action === 'create-group') {
    window.alert('创建群聊功能建设中')
  }
}

const handleGlobalClickForChatQuickMenu = (event) => {
  const root = chatQuickMenuRef.value
  if (!root) return
  if (!root.contains(event.target)) {
    showChatQuickMenu.value = false
  }
}

const sendChatMessage = async () => {
  if (!selectedChatUserId.value) {
    chatError.value = '请先选择联系人'
    return
  }
  const content = (chatInput.value || '').trim()
  if (!content) {
    chatError.value = '请输入消息内容'
    return
  }

  chatSending.value = true
  chatError.value = ''
  try {
    const res = await request.post('/messages/send', {
      receiver: selectedChatUserId.value,
      subject: '',
      content
    })
    if (res.code === 200) {
      const created = res.data
      if (created?.messageId) {
        sentMessages.value = [created, ...sentMessages.value.filter(m => m.messageId !== created.messageId)]
      } else {
        await loadSent()
      }
      chatInput.value = ''
      return
    }
    chatError.value = res.msg || '发送失败'
  } catch (e) {
    chatError.value = e?.response?.data?.msg || '发送失败'
  } finally {
    chatSending.value = false
  }
}

const markMessageRead = async (msg) => {
  if (!msg || msg.read) return
  try {
    const res = await request.post('/messages/read', null, { params: { messageId: msg.messageId } })
    if (res.code === 200) {
      msg.read = true
    }
  } catch (e) {
    console.error('标记已读失败', e)
  }
}

const connectMessageStream = () => {
  if (!currentUserId.value || messageStream) return
  const token = localStorage.getItem('accessToken') || ''
  if (!token) return
  messageStream = new EventSource(`http://localhost:8081/messages/stream?access_token=${encodeURIComponent(token)}`)
  const handler = (event) => {
    try {
      if (!event?.data || event.data === 'ok') return
      const msg = JSON.parse(event.data)
      if (msg?.receiverId === currentUserId.value) {
        inboxMessages.value = [msg, ...inboxMessages.value.filter(m => m.messageId !== msg.messageId)]
      }
      if (msg?.senderId === currentUserId.value) {
        sentMessages.value = [msg, ...sentMessages.value.filter(m => m.messageId !== msg.messageId)]
      }
    } catch (e) {
      // ignore parse error
    }
  }
  messageStream.addEventListener('message', handler)
}

const closeMessageStream = () => {
  if (messageStream) {
    messageStream.close()
    messageStream = null
  }
}

const expandedMenus = ref({
  materials: false,
  inventory: false,
  auxiliary: false,
  messages: false,
  personalSettings: false
})

const toggleMenu = (menuName) => {
  expandedMenus.value[menuName] = !expandedMenus.value[menuName]
}

watchEffect(() => {
  if (currentPage.value !== 'messageHome') return
  const sessions = chatSessions.value
  if (!sessions.length) {
    selectedChatUserId.value = ''
    return
  }
  const exists = sessions.some(s => s.userId === selectedChatUserId.value)
  if (!exists) {
    selectedChatUserId.value = sessions[0].userId
  }
})

watchEffect(() => {
  applyUserPreferences()
})

onMounted(async () => {
  loadUserPreferences()
  await loadSecurityBindings()
  document.addEventListener('click', handleGlobalClickForChatQuickMenu)
  try {
    const response = await request.get('/stats/users-count')
    if (response.code === 200) {
      usersCount.value = response.data || 0
    }
  } catch (error) {
    console.error('获取用户数失败：', error)
  }

  connectMessageStream()
  await Promise.all([loadInbox(), loadSent()])
})

onUnmounted(() => {
  clearBindingCountdownTimers()
  document.removeEventListener('click', handleGlobalClickForChatQuickMenu)
  closeMessageStream()
})
</script>

<style scoped>
.dashboard-layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 220px 1fr;
  background: #eef2f7;
  font-size: var(--app-user-font-size, 14px);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.dashboard-layout.density-compact .menu-item {
  padding-top: 8px;
  padding-bottom: 8px;
}

.dashboard-layout.density-compact .submenu-item {
  padding-top: 6px;
  padding-bottom: 6px;
}

.dashboard-layout.density-compact .content {
  padding: 12px;
}

.sidebar {
  background: #233a59;
  color: #c6d5e6;
  padding: 16px 12px;
  overflow-y: auto;
  max-height: 100vh;
}

.logo {
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 16px;
}

.menu {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.menu-section {
  margin-bottom: 4px;
}

.menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #a9bfd8;
  text-decoration: none;
  font-size: 13px;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.menu-item:hover {
  background: #2f5f99;
  color: #fff;
}

.menu-item.active {
  background: #2f5f99;
  color: #fff;
  font-weight: 600;
}

.menu-parent {
  cursor: pointer;
}

.arrow {
  font-size: 10px;
  transition: transform 0.2s;
  margin-left: auto;
}

.arrow.expand {
  transform: rotate(180deg);
}

.submenu {
  display: flex;
  flex-direction: column;
  gap: 0;
  background: #1a2740;
  border-radius: 4px;
  margin-top: 4px;
  margin-bottom: 4px;
  overflow: hidden;
}

.submenu-item {
  color: #9cb3d1;
  text-decoration: none;
  font-size: 12px;
  padding: 8px 12px 8px 28px;
  display: block;
  transition: all 0.2s;
  cursor: pointer;
}

.submenu-item:hover {
  background: #2f5f99;
  color: #fff;
}

.submenu-item.active {
  background: #2f5f99;
  color: #fff;
  font-weight: 600;
}

.main {
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.topbar {
  height: 56px;
  border-bottom: 1px solid #dbe3ec;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 18px;
}

.crumb {
  color: #8ba0b4;
  font-size: 12px;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #5b6b7e;
}

.logout-btn {
  border: 1px solid #ccd6e2;
  background: #fff;
  border-radius: 4px;
  padding: 4px 10px;
  cursor: pointer;
}

.content {
  padding: 18px;
  min-width: 0;
  overflow: auto;
}

.title {
  margin: 0 0 12px;
  color: #243547;
}

.cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(180px, 1fr));
  gap: 10px;
}

.card {
  background: #fff;
  border: 1px solid #dde5ef;
  border-radius: 6px;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
}

.blue { background: #4fa3ff; }
.green { background: #7bc043; }
.yellow { background: #f0b53a; }
.red { background: #ef6b6b; }

.num {
  font-size: 18px;
  font-weight: 700;
  color: #2a3f55;
}

.label {
  color: #8799ab;
  font-size: 12px;
}

.welcome {
  margin-top: 12px;
  background: #fff;
  border: 1px solid #dde5ef;
  border-radius: 6px;
  padding: 14px;
}

.welcome h3 {
  margin: 0 0 6px;
  color: #2a3f55;
}

.welcome p {
  margin: 0;
  color: #7d90a4;
  font-size: 13px;
}

.settings-wrap {
  margin-top: 12px;
  background: #fff;
  border: 1px solid #dde5ef;
  border-radius: 6px;
  padding: 14px;
}

.settings-wrap h3 {
  margin: 0 0 10px;
  color: #2a3f55;
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(260px, 1fr));
  gap: 12px;
}

.settings-card {
  border: 1px solid #dde5ef;
  border-radius: 6px;
  padding: 12px;
  background: #fbfdff;
}

.settings-card h4 {
  margin: 0 0 10px;
  color: #3a516a;
  font-size: 14px;
}

.settings-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.settings-row label {
  width: 70px;
  color: #5a6f87;
  font-size: 13px;
}

.settings-row input,
.settings-row select {
  flex: 1;
  height: 34px;
  border: 1px solid #d7e0eb;
  border-radius: 6px;
  background: #fff;
  padding: 0 10px;
  color: #334d67;
}

.settings-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.settings-tip {
  color: #4b6580;
  font-size: 12px;
}

.binding-modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.28);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.binding-modal {
  width: min(560px, calc(100vw - 32px));
  background: #fff;
  border-radius: 10px;
  border: 1px solid #dde5ef;
  padding: 14px;
  box-shadow: 0 12px 30px rgba(24, 44, 74, 0.22);
}

.binding-modal h4 {
  margin: 0 0 10px;
  color: #314a63;
  font-size: 16px;
}

.binding-step {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.binding-desc {
  color: #5f748d;
  font-size: 13px;
  margin: 0;
}

.binding-input-error {
  border-color: #ef5350 !important;
  background: #fff6f6 !important;
}

.binding-error-tip {
  color: #e64b4b;
  font-size: 12px;
  margin: -2px 0 2px 74px;
}

.chat-shell {
  margin-top: 12px;
  background: #fff;
  border: 1px solid #dde5ef;
  border-radius: 6px;
  min-height: 680px;
  display: grid;
  grid-template-columns: 280px 1fr;
  overflow: hidden;
}

.chat-aside {
  border-right: 1px solid #e3eaf3;
  background: #f6f9fc;
  padding: 10px;
  overflow: auto;
}

.chat-aside-title {
  font-size: 13px;
  color: #4d6279;
  font-weight: 600;
  margin-bottom: 8px;
}

.chat-search-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
}

.chat-search-input {
  flex: 1;
  height: 30px;
  border: 1px solid #d4deea;
  border-radius: 6px;
  background: #fff;
  color: #2f445a;
  padding: 0 10px;
  font-size: 12px;
}

.chat-search-input:focus {
  border-color: #8bb6ff;
  outline: none;
  box-shadow: 0 0 0 2px rgba(63, 119, 232, 0.12);
}

.chat-plus-wrap {
  position: relative;
}

.chat-plus-btn {
  width: 30px;
  height: 30px;
  border: 1px solid #c9d5e5;
  border-radius: 6px;
  background: #fff;
  color: #445a71;
  font-size: 20px;
  line-height: 24px;
  cursor: pointer;
}

.chat-plus-btn:hover {
  background: #edf3fb;
}

.chat-quick-menu {
  position: absolute;
  right: 0;
  top: 34px;
  min-width: 110px;
  padding: 4px;
  border: 1px solid #d7e0eb;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 20px rgba(25, 44, 72, 0.12);
  z-index: 10;
}

.chat-quick-item {
  width: 100%;
  height: 30px;
  border: none;
  background: transparent;
  text-align: left;
  padding: 0 8px;
  border-radius: 6px;
  color: #445a71;
  font-size: 12px;
  cursor: pointer;
}

.chat-quick-item:hover {
  background: #edf3fb;
}

.chat-session-item {
  position: relative;
  padding: 10px;
  border: 1px solid transparent;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.chat-session-item:hover {
  background: #edf3fb;
}

.chat-session-item.active {
  background: #e9f1ff;
  border-color: #b8d1ff;
}

.chat-session-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.chat-session-name {
  color: #2f445a;
  font-weight: 600;
  font-size: 13px;
}

.chat-session-time {
  color: #8aa0b8;
  font-size: 12px;
  white-space: nowrap;
}

.chat-session-last {
  margin-top: 4px;
  color: #667f99;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 230px;
}

.chat-unread-dot {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  min-width: 18px;
  height: 18px;
  border-radius: 10px;
  background: #ef5350;
  color: #fff;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
  padding: 0 4px;
}

.chat-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-main-header {
  height: 64px;
  border-bottom: 1px solid #e3eaf3;
  padding: 0 16px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  background: #fff;
}

.chat-main-name {
  color: #2f445a;
  font-size: 15px;
  font-weight: 600;
}

.chat-main-id {
  color: #8aa0b8;
  font-size: 12px;
  margin-top: 2px;
}

.chat-history {
  flex: 1;
  overflow: auto;
  padding: 14px 16px;
  background: #f4f7fb;
}

.chat-row {
  margin-bottom: 10px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.chat-row.mine {
  align-items: flex-end;
}

.chat-meta {
  color: #869bb2;
  font-size: 12px;
  margin-bottom: 4px;
}

.chat-bubble {
  max-width: min(70%, 720px);
  border-radius: 8px;
  padding: 8px 10px;
  background: #fff;
  border: 1px solid #dbe5f0;
  color: #2f445a;
  font-size: 13px;
  white-space: pre-wrap;
  word-break: break-word;
}

.chat-row.mine .chat-bubble {
  background: #e8f0ff;
  border-color: #bdd3ff;
}

.chat-input-wrap {
  border-top: 1px solid #e3eaf3;
  background: #fff;
  padding: 10px 12px;
}

.chat-input {
  width: 100%;
  min-height: 92px;
  max-height: 220px;
  resize: vertical;
  border: 1px solid #d7e0eb;
  border-radius: 6px;
  padding: 10px;
  color: #2f445a;
  font-size: 13px;
  line-height: 1.6;
}

.chat-input-actions {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chat-error {
  color: #e64b4b;
  font-size: 12px;
}

.chat-placeholder,
.chat-empty {
  color: #7d90a4;
  font-size: 13px;
}

.chat-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f6f9fc;
}

.mail-compose {
  background: #fff;
  border: 1px solid #dde5ef;
  border-radius: 6px;
  padding: 12px;
}

.mail-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.hidden-file-input {
  display: none;
}

.attach-list {
  border: 1px solid #d7e0eb;
  border-radius: 4px;
  background: #fbfdff;
  padding: 8px 10px;
  margin-bottom: 10px;
}

.attach-title {
  color: #556b84;
  font-size: 12px;
  margin-bottom: 6px;
}

.attach-items {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.attach-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #3f5873;
  font-size: 12px;
}

.attach-name {
  max-width: 360px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attach-size {
  color: #8aa0b8;
}

.attach-remove {
  border: none;
  background: transparent;
  color: #e64b4b;
  cursor: pointer;
  font-size: 12px;
}

.mail-btn {
  height: 30px;
  padding: 0 12px;
  border: 1px solid #cfd8e3;
  background: #fff;
  border-radius: 4px;
  cursor: pointer;
  color: #445a71;
}

.mail-btn.primary {
  background: #3f77e8;
  border-color: #3f77e8;
  color: #fff;
}

.mail-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.mail-line label {
  width: 56px;
  color: #50657f;
  font-size: 13px;
}

.mail-line input {
  flex: 1;
  height: 36px;
  border: 1px solid #d7e0eb;
  border-radius: 4px;
  padding: 0 10px;
  color: #2f445a;
}

.mail-toolbar {
  height: 36px;
  border: 1px solid #d7e0eb;
  border-radius: 4px 4px 0 0;
  border-bottom: none;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 10px;
  color: #647a93;
  background: #fbfdff;
}

.mail-editor {
  width: 100%;
  min-height: 480px;
  resize: vertical;
  border: 1px solid #d7e0eb;
  border-radius: 0 0 4px 4px;
  padding: 12px;
  color: #2f445a;
  font-size: 14px;
  line-height: 1.6;
}

.mail-tip {
  margin-top: 8px;
  color: #4b6580;
  font-size: 13px;
}

.msg-list {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.msg-empty {
  color: #7d90a4;
  font-size: 13px;
}

.msg-item {
  border: 1px solid #dde5ef;
  border-radius: 6px;
  padding: 10px 12px;
  background: #fff;
  cursor: pointer;
}

.msg-item.unread {
  border-color: #8db6ff;
  background: #f7fbff;
}

.msg-head {
  display: flex;
  justify-content: space-between;
  color: #7d90a4;
  font-size: 12px;
  margin-bottom: 4px;
}

.msg-title {
  color: #314a63;
  font-weight: 600;
  margin-bottom: 4px;
}

.msg-content {
  color: #4d6279;
  font-size: 13px;
  white-space: pre-wrap;
}

.report-section {
  margin-top: 12px;
  background: #fff;
  border: 1px solid #dde5ef;
  border-radius: 8px;
  padding: 14px;
  min-height: 640px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.report-filters {
  display: grid;
  grid-template-columns: repeat(4, minmax(180px, 1fr));
  gap: 10px;
  padding: 10px;
  border: 1px solid #e3eaf3;
  border-radius: 8px;
  background: #f9fbfe;
}

.report-filter-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.report-filter-item label {
  color: #5f748d;
  font-size: 12px;
}

.report-filter-item select,
.report-filter-item input {
  height: 34px;
  border: 1px solid #d7e0eb;
  border-radius: 6px;
  background: #fff;
  padding: 0 10px;
  color: #334d67;
}

.report-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
  min-width: 980px;
}

.report-card {
  border: 1px solid #e1e8f1;
  border-radius: 8px;
  padding: 12px;
  background: #fff;
}

.report-card h4 {
  margin: 0 0 10px;
  color: #2f445a;
  font-size: 15px;
}

.report-scroll {
  overflow: auto;
  border: 1px solid #e8eef6;
  border-radius: 8px;
}

.report-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 820px;
}

.report-table th,
.report-table td {
  border-bottom: 1px solid #edf2f8;
  padding: 8px 10px;
  text-align: center;
  color: #425a72;
  font-size: 12px;
}

.report-table th {
  background: #f6f9fc;
  color: #3b536d;
  font-weight: 600;
  position: sticky;
  top: 0;
}

.report-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(220px, 1fr));
  gap: 10px;
}

.report-summary-card {
  border: 1px solid #e3eaf3;
  border-radius: 8px;
  padding: 10px;
  background: #fbfdff;
}

.report-summary-title {
  color: #5f748d;
  font-size: 12px;
}

.report-summary-value {
  margin-top: 4px;
  color: #2f445a;
  font-size: 24px;
  font-weight: 700;
}

.report-summary-tip {
  margin-top: 4px;
  color: #7a8fa6;
  font-size: 12px;
}

.report-shortage-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(220px, 1fr));
  gap: 10px;
}

.report-shortage-item {
  border: 1px solid #e3eaf3;
  border-radius: 8px;
  padding: 10px;
  background: #fff;
}

.report-shortage-name {
  color: #5f748d;
  font-size: 12px;
}

.report-shortage-value {
  margin-top: 6px;
  color: #e57373;
  font-size: 22px;
  font-weight: 700;
}

.report-pie-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(320px, 1fr));
  gap: 12px;
}

.report-pie-card {
  border: 1px solid #e3eaf3;
  border-radius: 8px;
  padding: 10px;
  background: #fbfdff;
}

.report-pie-title {
  color: #3b536d;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 10px;
}

.report-pie-body {
  display: flex;
  align-items: center;
  gap: 14px;
}

.pie-chart {
  width: 180px;
  height: 180px;
  border-radius: 50%;
  border: 10px solid #fff;
  box-shadow: inset 0 0 0 1px #e8eef6;
}

.pie-legend {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.pie-legend-item {
  display: grid;
  grid-template-columns: 12px 1fr auto;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #4d6279;
}

.pie-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.report-error {
  color: #e64b4b;
  font-size: 12px;
}

.report-tip {
  margin: 8px 0 10px;
  color: #2d7a31;
  font-size: 13px;
}

.ai-config-check {
  margin: 8px 0 10px;
  padding: 8px 10px;
  border: 1px solid #ffd591;
  background: #fff7e6;
  border-radius: 6px;
  color: #8a5a00;
  font-size: 13px;
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
}

.ai-config-check.ok {
  border-color: #b7eb8f;
  background: #f6ffed;
  color: #237804;
}

.report-loading,
.report-empty {
  color: #7d90a4;
  font-size: 13px;
}

.report-wrap {
  margin-top: 12px;
  background: #fff;
  border: 1px solid #dde5ef;
  border-radius: 8px;
  padding: 14px;
  min-height: 640px;
}

.report-wrap h3 {
  margin: 0 0 12px;
  color: #2a3f55;
}

.query-select {
  height: 34px;
  border: 1px solid #d7e0eb;
  border-radius: 6px;
  background: #fff;
  padding: 0 10px;
  color: #334d67;
}

.primary-btn {
  height: 34px;
  border: 1px solid #3f77e8;
  border-radius: 6px;
  background: #3f77e8;
  color: #fff;
  padding: 0 14px;
  cursor: pointer;
}

.primary-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading {
  color: #5f748d;
  font-size: 13px;
}

.error-box {
  color: #e64b4b;
  font-size: 13px;
}

.report-scroll-area {
  overflow: auto;
  max-height: calc(100vh - 210px);
  padding-right: 4px;
}

.report-desc {
  margin: 0 0 10px;
  color: #6a7f97;
  font-size: 12px;
}

.report-table-wrap {
  overflow: auto;
  border: 1px solid #e8eef6;
  border-radius: 8px;
}

.trend-chart-wrap {
  border: 1px solid #e8eef6;
  border-radius: 8px;
  background: #fbfdff;
  padding: 10px;
  margin-bottom: 10px;
}

.trend-chart-legend {
  display: flex;
  align-items: center;
  gap: 14px;
  color: #5f748d;
  font-size: 12px;
  margin-bottom: 8px;
}

.trend-chart-legend span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.legend-chip {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 3px;
}

.legend-chip.inbound {
  background: #6fa8ff;
}

.legend-chip.outbound {
  background: #ffb86a;
}

.legend-chip.line {
  width: 14px;
  height: 2px;
  border-radius: 0;
  background: #3f77e8;
}

.trend-chart-scroll {
  overflow-x: auto;
  overflow-y: hidden;
}

.trend-chart-svg {
  display: block;
}

.trend-grid-line {
  stroke: #e7edf6;
  stroke-width: 1;
}

.trend-axis-label {
  fill: #7b8fa6;
  font-size: 11px;
}

.trend-bar.inbound {
  fill: #6fa8ff;
}

.trend-bar.outbound {
  fill: #ffb86a;
}

.trend-line {
  fill: none;
  stroke: #3f77e8;
  stroke-width: 2;
}

.trend-line-point {
  fill: #3f77e8;
}

.empty {
  color: #8aa0b8;
  text-align: center;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(220px, 1fr));
  gap: 10px;
}

.metric-item {
  border: 1px solid #e3eaf3;
  border-radius: 8px;
  padding: 10px;
  background: #fbfdff;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.metric-item span {
  color: #5f748d;
  font-size: 12px;
}

.metric-item strong {
  color: #2f445a;
  font-size: 22px;
}

.pie-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(320px, 1fr));
  gap: 12px;
}

.pie-block {
  border: 1px solid #e3eaf3;
  border-radius: 8px;
  padding: 10px;
  background: #fbfdff;
}

.pie-block h4 {
  margin: 0 0 10px;
}

.pie {
  width: 180px;
  height: 180px;
  border-radius: 50%;
  border: 10px solid #fff;
  box-shadow: inset 0 0 0 1px #e8eef6;
  margin-bottom: 10px;
}

.pie-legend {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.pie-legend li {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #4d6279;
  font-size: 12px;
}

.ai-analysis-wrap {
  margin-top: 12px;
  background: #fff;
  border: 1px solid #dde5ef;
  border-radius: 8px;
  padding: 14px;
}

.ai-analysis-wrap h3 {
  margin: 0 0 12px;
  color: #2a3f55;
}

.ai-scroll-area {
  overflow: auto;
  max-height: calc(100vh - 210px);
  padding-right: 4px;
}

.ai-content-wide {
  min-width: 1100px;
}

.ai-toolbar {
  display: grid;
  grid-template-columns: repeat(4, minmax(170px, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.ai-overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(180px, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.ai-overview-card {
  border: 1px solid #e2e9f2;
  border-radius: 8px;
  padding: 10px;
  background: #fbfdff;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ai-overview-card span {
  color: #5f748d;
  font-size: 12px;
}

.ai-overview-card strong {
  color: #2f445a;
  font-size: 24px;
  line-height: 1.1;
}

.ai-overview-card em {
  color: #7c90a6;
  font-size: 12px;
  font-style: normal;
}

.ai-panel {
  border: 1px solid #e3eaf3;
  border-radius: 8px;
  padding: 10px;
  background: #fff;
  margin-bottom: 12px;
}

.ai-panel h4 {
  margin: 0 0 10px;
  color: #2f445a;
}

.ai-link {
  border: none;
  background: transparent;
  color: #3f77e8;
  cursor: pointer;
  padding: 0;
  font-size: 13px;
  text-decoration: underline;
}

.ai-raw-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.ai-raw-modal {
  width: min(960px, calc(100vw - 32px));
  max-height: calc(100vh - 120px);
  overflow: hidden;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.22);
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ai-raw-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.ai-raw-header h4 {
  margin: 0;
  color: #2f445a;
}

.ai-raw-sub {
  margin: 4px 0 0;
  color: #6b7f97;
  font-size: 12px;
}

.ai-raw-content {
  margin: 0;
  padding: 12px;
  background: #0f172a;
  color: #e2e8f0;
  border-radius: 8px;
  font-size: 12px;
  line-height: 1.6;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  flex: 1;
}

.ai-panel-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.ai-draft-tip {
  color: #4d6279;
  font-size: 12px;
}

.ai-trend-mini {
  width: 120px;
  height: 34px;
  display: block;
}

.ai-trend-line {
  fill: none;
  stroke: #3f77e8;
  stroke-width: 1.8;
}

.ai-heat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(220px, 1fr));
  gap: 10px;
}

.ai-heat-col {
  border: 1px solid #e3eaf3;
  border-radius: 8px;
  padding: 10px;
  background: #fbfdff;
}

.ai-heat-col.high {
  background: linear-gradient(180deg, #fff5f5 0%, #fff 100%);
}

.ai-heat-col.medium {
  background: linear-gradient(180deg, #fffaf1 0%, #fff 100%);
}

.ai-heat-col.low {
  background: linear-gradient(180deg, #f4fbf6 0%, #fff 100%);
}

.ai-heat-head {
  color: #2f445a;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
}

.ai-heat-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  border-radius: 6px;
  background: #fff;
  border: 1px solid #e8eef6;
  padding: 6px 8px;
  margin-bottom: 6px;
  color: #4b6179;
  font-size: 12px;
}

.ai-heat-item b {
  color: #2f445a;
}

.ai-risk-tag {
  display: inline-block;
  min-width: 38px;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 18px;
}

.ai-risk-tag.risk-low {
  background: #e8f5e9;
  color: #2e7d32;
}

.ai-risk-tag.risk-medium {
  background: #fff3e0;
  color: #ef6c00;
}

.ai-risk-tag.risk-high {
  background: #ffebee;
  color: #c62828;
}

.ai-suggest-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ai-suggest-item {
  border: 1px solid #e3eaf3;
  border-radius: 8px;
  padding: 10px;
  background: #fbfdff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.ai-suggest-title {
  color: #2f445a;
  font-weight: 600;
  margin-bottom: 4px;
}

.ai-suggest-desc {
  color: #6a7f97;
  font-size: 12px;
}

.ai-suggest-metrics {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #4b6179;
  font-size: 12px;
  white-space: nowrap;
}

.ai-conclusion-list {
  margin: 0;
  padding-left: 18px;
  color: #4b6179;
  font-size: 13px;
  line-height: 1.7;
}
</style>
