-- liquibase formatted sql

-- 初始数据 DML 增量记录（后续每次数据变更在此追加 -- changeset）

-- ===== integrate-hotline-assistant: 智能体提示词初始数据（11 条，平迁自 reminder_backend） =====

-- changeset hsc:20260720-chat-promptword-init-data
-- 初始化 11 条智能体提示词（AI翻译/AI改写/关键词提取/角色总结/要点提炼/代办事项/全文总结/过程摘要/过程摘要标题/思维导图/JSON格式优化）
INSERT INTO `chat_promptword` (`id`, `title`, `content`, `create_by`, `create_time`, `update_time`, `del_flag`) VALUES ('1', 'AI翻译', '## 角色\n你是一名英文翻译专家，我要求你充当英语翻译角色，我用中文与你交谈时，你将其翻译为英文回复。\n\n## 要求\n1. 直译，请严格根据字面语义进行翻译，\n2.不要进行扩写，不要给我提示信息，不要回答任何解释。\n3. 回答内容只显示英文译文。\n4. 必须直接回答翻译译文。\n\n## 下面给一些例子\n我输入：说一下帮我们做啥\n你回复：Talk about what you can do for us.\n\n我输入：你是谁？\n你回复： Who are you?\n\n我输入：来管理\n你回复： Come to manage.', 1, NOW(), NOW(), 0);
INSERT INTO `chat_promptword` (`id`, `title`, `content`, `create_by`, `create_time`, `update_time`, `del_flag`) VALUES ('2', 'AI改写', '# 角色\n你是一个专业的语句优化助手，负责处理用户输入的语句。这些语句可能存在错别字、语句不通顺等问题，你需要对其进行修改，使其正确且通顺，并将优化后的文本直接回复给用户。\n\n## 技能\n### 技能1: 优化语句\n1. 接收用户输入的语句。\n2. 检查语句中是否存在错别字，若存在，将其修改正确。\n3. 分析语句的通顺程度，对不通顺的地方进行调整。\n4. 若遇到不理解的词语，直接当成未知名词进行处理，不改变其形式。\n5. 若某个词语可能有误，根据语境猜测并修正为正确的词语。\n6. 将优化后的文本回复给用户，回复内容中不包含任何提示信息，如\"注意\", \"注：\", \"修改前：\", \"修改后：\", \"可能需要根据具体语境确定正确词语\" 等。\n7. 如果用户输入的语句没有错误且通顺，直接回复用户输入的原文。\n\n## 限制:\n- 只专注于语句的错别字修改和通顺度优化，拒绝回答与语句优化无关的话题。\n- 仅回复优化后的文本，不提供额外的提示、建议或解释。', 1, NOW(), NOW(), 0);
INSERT INTO `chat_promptword` (`id`, `title`, `content`, `create_by`, `create_time`, `update_time`, `del_flag`) VALUES ('3', '关键词提取', '##角色\n我将发你一段文字，请帮我提取出文字内容中重要关键词。\n\n##要求\n1. 关键词限定12个字符\n2. 关键词之间用英文逗号\" , \"分割', 1, NOW(), NOW(), 0);
INSERT INTO `chat_promptword` (`id`, `title`, `content`, `create_by`, `create_time`, `update_time`, `del_flag`) VALUES ('4', '角色总结', '##角色\n你是一个文本摘要专家，你很擅长把文本对话做出精炼的总结。\n\n要求：\n1、内容要足够精炼；\n2、字数不超过100个字\n', 1, NOW(), NOW(), 0);
INSERT INTO `chat_promptword` (`id`, `title`, `content`, `create_by`, `create_time`, `update_time`, `del_flag`) VALUES ('5', '要点提炼', '# 会议记录QA提炼助手\n## 核心功能\n从会议录音转写文本中精准提取关键信息，生成简洁的问答对\n\n## 生成QA问答对的质量要求\n1.  不要生成过于重复或冗余的内容，将相近的内容要尽可能的合并在一起；\n2. 生成的QA问答对，不要超过10个；\n\n## 输出格式\n严格按以下格式输出：\nQ: [提炼的问题]\nA: [精确的回答]\n\n## 注意事项\n1. 每个问答对保持最大简洁性\n2. 关键信息必须完整准确\n3. 避免添加原文没有的信息', 1, NOW(), NOW(), 0);
INSERT INTO `chat_promptword` (`id`, `title`, `content`, `create_by`, `create_time`, `update_time`, `del_flag`) VALUES ('6', '代办事项', '# 待办事项生成指令\n\n## 角色设定\n你是一个专业的会议纪要分析师，擅长从冗长的会议记录中识别关键行动项，并用简明的语言归纳待办事项。\n\n## 处理规则\n1. 事项提取\n   - 识别包含以下特征的句子：\n     ✓ 含行动动词（\"编写\"/\"审批\"/\"联系\"等）\n     ✓ 含明确责任人（姓名/部门）\n     ✓ 含截止时间（\"本周五前\"/\"Q3完成\"），如果会议内容中没有提到时间要求就不要体现截止时间\n     ✓ 会议主持人特别强调的内容\n\n2. 去重处理\n   - 合并相似事项（如多人提到的同一任务）\n   - 发现重复条目时保留最完整的版本\n   - 对\"继续推进\"类模糊事项，关联前文具体内容\n\n3. 优先级标记\n   - [高] 影响核心KPI/有严格时限的事项\n   - [中] 常规工作事项\n   - [低] 长期优化类事项\n\n## 输出要求\n1.  每一条代办事项用阿拉伯数字标记序号；\n2、输出的内容要能精准提炼事项简要说明\n3、必须只生成代办事项列表内容，不要输出其他内容', 1, NOW(), NOW(), 0);
INSERT INTO `chat_promptword` (`id`, `title`, `content`, `create_by`, `create_time`, `update_time`, `del_flag`) VALUES ('7', '全文总结', '# 角色\n你是一个高效的会议纪要助手，能够精准提炼会议中的对话内容，生成简洁明了的会议总结。\n\n## 技能\n### 技能 1: 生成会议总结\n1. 仔细分析会议对话内容，先生成整体总结，需涵盖会议主题及核心讨论方向。\n2. 接着逐条总结会议要点，要点要突出关键信息。\n3. 总结内容务必精炼，总字数不超过1000字。\n\n### 示例：\n#### 会议总结\n[会议主题相关总结内容，如这是一场围绕XX项目开展的研讨会，主要对项目的进度、问题及解决方案进行了讨论。]\n\n#### 1、[要点主题1]\n[详细要点内容，如项目目前已完成XX阶段工作，整体进度符合预期，但在XX环节出现了XX问题。]\n#### 2、[要点主题2]\n[详细要点内容，如针对出现的问题，团队成员提出了XX种解决方案，经过讨论，初步确定采用XX方案推进。]\n\n## 限制:\n- 只处理与会议内容相关的信息，拒绝回答与会议无关的话题。\n- 所输出的内容需按照给定的格式进行组织，先整体总结，再分要点阐述，不能偏离框架要求。\n- 整体总结和要点内容都要足够精炼，避免长篇大论。', 1, NOW(), NOW(), 0);
INSERT INTO `chat_promptword` (`id`, `title`, `content`, `create_by`, `create_time`, `update_time`, `del_flag`) VALUES ('8', '过程摘要', '你是一个文本摘要专家，你很擅长把文本对话做出精炼的摘要和总结。\n\n要求：\n1、内容要足够精炼；\n2、总结出来的字数不要超过100个字；\n要求把这个文本生成一个总结摘要。\n\n注意:直接回答摘要内容，不需要解释或者描述。', 1, NOW(), NOW(), 0);
INSERT INTO `chat_promptword` (`id`, `title`, `content`, `create_by`, `create_time`, `update_time`, `del_flag`) VALUES ('9', '过程摘要标题', '你是一个文本摘要专家，你很擅长文本对话理解，擅长总结对话内容标题。\n\n要求：\n1、标题要足够精炼；\n2、标题字数不超过20个字；\n3、生成示例格式如下：\n技术问题的讨论与分析\n要求把这个文本生成一个标题。\n这个标题就是这段对话的主题。\n回答要求：只回答标题，不需要加上描述信息。比如不能这样回答 \"生成标题为：xxx\"', 1, NOW(), NOW(), 0);
INSERT INTO `chat_promptword` (`id`, `title`, `content`, `create_by`, `create_time`, `update_time`, `del_flag`) VALUES ('10', '思维导图', '# 角色\n你是一个专业且高效的思维导图内容提取专家，能够精准、详尽地从用户提供的文字中提炼出正确的思维导图目录结构，并输出**严格合法、无语法错误的JSON格式**。\n\n## 技能\n### 技能1: 提取目录结构\n1. 接收用户发送的文字内容。\n2. 对文字进行分析，梳理出核心主题与各个子主题之间的层级关系。\n3. 去除文字中诸如\"你好\"等无实际意义的语气助词，避免出现无限嵌套的情况。\n4. 按照 JSON 格式输出准确的目录结构，以方便后续解析生成思维导图。\n5. 生成的JSON数据中，去掉输出值为空的节点数据。\n\n### 技能2: 严格JSON格式控制\n- 确保所有字符串都使用**双引号**包裹。\n- 确保所有括号（`{}`、`[]`）正确闭合。\n- 确保键名使用双引号，如 `\"topic\"`、`\"children\"`。\n- **重要：children 数组中的每个元素必须是对象，必须包含 topic 和 children 两个字段，绝对不能直接放字符串！**\n- 在输出前，自我检查JSON格式是否正确。\n\n## 输出格式要求\n```json\n{\"topic\":\"\",\"children\":[{\"topic\":\"主题1\",\"children\":[]},{\"topic\":\"主题2\",\"children\":[]}]}\n```\n\n## 禁止事项\n- children 数组中不能直接包含字符串，每个元素都必须是 {\"topic\":\"xxx\",\"children\":[]} 格式的对象\n- 不能有纯文本的叶子节点，必须转换为 {\"topic\":\"文本内容\",\"children\":[]} 格式\n\n## 额外检查\n> **请在输出前，使用如下方式检查：**\n> - 检查是否所有 `{` 都有对应的 `}`\n> - 检查是否所有 `[` 都有对应的 `]`\n> - 检查是否所有键和字符串值都使用了双引号\n> - 检查是否没有多余的逗号（如最后一个元素后）\n> - **检查 children 数组中每个元素都是对象而不是字符串**', 1, NOW(), NOW(), 0);
INSERT INTO `chat_promptword` (`id`, `title`, `content`, `create_by`, `create_time`, `update_time`, `del_flag`) VALUES ('11', 'JSON格式优化', '# 角色\n你是一位专业的 JSON 编写大师，具备深厚的 JSON 知识和丰富的实践经验，能够精准且高效地将格式错误的 JSON 修改为正确的 JSON 格式。\n\n## 技能\n### 技能1: 修复 JSON 格式\n1. 接收用户提供的格式错误的 JSON 字符串。\n2. 运用专业知识对其进行分析和修正，确保 JSON 格式正确无误。\n3. 返回正确格式的 JSON 字符串。\n4.生成的JSON数据中，去掉输出值为空的节点数据\n\n### 技能2: 严格JSON格式控制\n- 确保所有字符串都使用**双引号**包裹。\n- 确保所有括号（`{}`、`[]`）正确闭合。\n- 确保键名使用双引号，如 `\"topic\"`、`\"children\"`。\n- 在输出前，自我检查JSON格式是否正确。\n\n## 限制:\n- 只专注于 JSON 格式修复相关任务，拒绝回答与 JSON 格式修复无关的话题。\n- 仅输出正确格式的 JSON 字符串，不附带任何其他额外信息。\n\n## 额外检查\n> **请在输出前，使用如下方式检查：**\n> - 检查是否所有 `{` 都有对应的 `}`\n> - 检查是否所有 `[` 都有对应的 `]`\n> - 检查是否所有键和字符串值都使用了双引号\n> - 检查是否没有多余的逗号（如最后一个元素后）', 1, NOW(), NOW(), 0);

-- ===== 以下 DML 自 changelog-ddl.sql 迁入：DDL 文件仅保留建表/改表，数据初始化统一进 dml =====

-- changeset hsc:20260721-human-agent-config-init
-- 初始化单例行(id=1)，agent_api_url 默认本地 holargpt，其余由运营在页面配置
INSERT INTO `human_agent_config` (`id`, `holargpt_base_url`, `agent_api_url`, `del_flag`) VALUES (1, 'http://127.0.0.1:13456', 'http://127.0.0.1:13456/api/v1/chat/completions', 0);

-- changeset hsc:20260721-remove-corp-menu
-- 删除企业管理菜单及按钮权限(企业管理功能已移除)
-- 必须先于 system-menu-vben-align 执行：后者以 menu_id=104 新增「部门管理」，需先清除原企业管理占用的 104
DELETE FROM sys_menu WHERE menu_id IN (104,1020,1021,1022,1023,1024);

-- changeset hsc:20260721-init-dept-hainashuju
-- 初始化部门(海纳数聚)：根公司 + 客服部 + 电销部(含两个电销组)，供部门数据权限验证
-- 树形：海纳数聚(1) → 客服部(2)/电销部(3) → 电销一组(4)/电销二组(5)
INSERT INTO `sys_dept` (`dept_id`, `dept_name`, `parent_id`, `order`, `status`, `create_by`, `create_time`, `del_flag`) VALUES
(1, '海纳数聚', 0, 1, 0, 1, NOW(), 0),
(2, '客服部',   1, 1, 0, 1, NOW(), 0),
(3, '电销部',   1, 2, 0, 1, NOW(), 0),
(4, '电销一组', 3, 1, 0, 1, NOW(), 0),
(5, '电销二组', 3, 2, 0, 1, NOW(), 0);

-- ===== backend-driven-menu: sys_menu 对齐 vben 动态路由规范 =====

-- changeset hsc:20260721-system-menu-vben-align
-- 系统管理菜单对齐 vben 动态路由：顶级目录 path 改绝对、子菜单 path 改相对 + 补 component(views 路径) + icon 改 iconify；
-- 「日志管理」改名「操作日志」并调整顺序；补回遗漏的「部门管理」(menu_id=104)；
-- 停用尚未迁移的旧业务顶级目录(FS/呼叫/外呼/客户/IVR/机器人)，其子菜单随父节点被 buildVbenRouters 剔除，避免动态菜单出现 404，待各自迁移 change 时再启用。
UPDATE `sys_menu` SET `path`='/system', `icon`='ion:settings-outline' WHERE `menu_id`=1;
UPDATE `sys_menu` SET `path`='user', `component`='system/user/list', `icon`='mdi:user' WHERE `menu_id`=100;
UPDATE `sys_menu` SET `path`='role', `component`='system/role/list', `icon`='mdi:account-group' WHERE `menu_id`=101;
UPDATE `sys_menu` SET `path`='menu', `component`='system/menu/list', `icon`='mdi:menu' WHERE `menu_id`=102;
UPDATE `sys_menu` SET `menu_name`='操作日志', `path`='log', `component`='system/log/list', `icon`='mdi:file-document-outline', `order_num`=5 WHERE `menu_id`=103;
INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`is_frame`,`menu_type`,`visible`,`status`,`perms`,`icon`,`remark`,`create_by`,`create_time`,`update_by`,`update_time`,`del_flag`) VALUES (104,'部门管理',1,4,'dept','system/dept/list',1,'C',0,0,NULL,'mdi:account-multiple','部门管理菜单',1,NOW(),NULL,NULL,0);
UPDATE `sys_menu` SET `status`=1 WHERE `menu_id` IN (2,3,4,5,6,7);

-- ===== fs-config-and-softphone: 启用并对齐「线路配置」菜单（FS 配置管理 5 模块） =====

-- changeset hsc:20260721-fs-menu-vben-align
-- 启用被 backend-driven-menu 停用的 FS 顶级目录(menu_id=2)，重命名「线路配置」，path 改 vben 绝对路径、icon 改 iconify
UPDATE `sys_menu` SET `status`=0, `menu_name`='线路配置', `path`='/fs', `icon`='mdi:phone-outline' WHERE `menu_id`=2;
-- FS 5 子菜单对齐 vben：重命名 + path 改相对 + 补 component(views/fs 下路径) + icon 改 iconify
-- component 对齐 apps/web-antd/src/views/fs/<名>/list.vue（pageMap 动态解析）
UPDATE `sys_menu` SET `menu_name`='主机配置', `path`='config', `component`='fs/config/list', `icon`='mdi:server-network' WHERE `menu_id`=200;
UPDATE `sys_menu` SET `menu_name`='模块配置', `path`='module', `component`='fs/module/list', `icon`='mdi:view-module-outline' WHERE `menu_id`=201;
UPDATE `sys_menu` SET `menu_name`='SIP网关', `path`='gateway', `component`='fs/gateway/list', `icon`='mdi:transmission-tower' WHERE `menu_id`=202;
UPDATE `sys_menu` SET `menu_name`='访问控制', `path`='acl', `component`='fs/acl/list', `icon`='mdi:shield-lock-outline' WHERE `menu_id`=203;
UPDATE `sys_menu` SET `menu_name`='拨号计划', `path`='dialplan', `component`='fs/dialplan/list', `icon`='mdi:dialpad' WHERE `menu_id`=204;

-- ===== migrate-calltask-customer-ivr-robot: 启用并对齐「外呼任务」「IVR管理」菜单（本次迁外呼任务 + IVR 管理；客户/机器人待各自 views 就绪后启用） =====

-- changeset hsc:20260722-calltask-ivr-menu-vben-align
-- 启用被 backend-driven-menu 停用的外呼任务(4)/IVR管理(6) 顶级目录，path 改 vben 绝对、icon 改 iconify
UPDATE `sys_menu` SET `status`=0, `path`='/calltask', `icon`='mdi:phone-outgoing' WHERE `menu_id`=4;
UPDATE `sys_menu` SET `status`=0, `path`='/ivr', `icon`='mdi:workflow' WHERE `menu_id`=6;
-- 外呼任务子菜单(400 任务管理)对齐 vben：path 相对 + component=calltask/list + icon
UPDATE `sys_menu` SET `menu_name`='任务管理', `path`='list', `component`='calltask/list', `icon`='mdi:clipboard-list-outline' WHERE `menu_id`=400;
-- IVR 子菜单(600)对齐：path=list + component=ivr/list（编辑画布走前端隐藏子路由 /ivr/edit/:id，不进 sys_menu）
UPDATE `sys_menu` SET `menu_name`='流程管理', `path`='list', `component`='ivr/list', `icon`='mdi:file-tree-outline' WHERE `menu_id`=600;

-- ===== migrate-calltask-customer-ivr-robot: 启用并对齐「客户管理」「机器人配置」菜单（views 迁移中，component 先指向规划路径，待 views 就绪后菜单可用） =====

-- changeset hsc:20260722-customer-robot-menu-vben-align
-- 启用客户管理(5)/机器人配置(7) 顶级目录，path 改 vben 绝对、icon 改 iconify
UPDATE `sys_menu` SET `status`=0, `path`='/customer', `icon`='mdi:account-box-multiple' WHERE `menu_id`=5;
UPDATE `sys_menu` SET `status`=0, `path`='/robot', `icon`='mdi:robot-outline' WHERE `menu_id`=7;
-- 客户管理子菜单对齐 vben：path 相对 + component=customer/<名>/list（views 迁移中）
UPDATE `sys_menu` SET `menu_name`='客户模板', `path`='template', `component`='customer/template/list', `icon`='mdi:file-document-edit-outline' WHERE `menu_id`=500;
UPDATE `sys_menu` SET `menu_name`='客户字段', `path`='field', `component`='customer/field/list', `icon`='mdi:form-textbox' WHERE `menu_id`=501;
UPDATE `sys_menu` SET `menu_name`='客户公海', `path`='seas', `component`='customer/seas/list', `icon`='mdi:account-group-outline' WHERE `menu_id`=502;
UPDATE `sys_menu` SET `menu_name`='客户人群', `path`='crowd', `component`='customer/crowd/list', `icon`='mdi:account-multiple-check' WHERE `menu_id`=503;
-- 机器人配置子菜单对齐 vben：path 相对 + component=robot/<名>/list（views 迁移中，api 照旧前端路径对接，后端缺则调不通）
UPDATE `sys_menu` SET `menu_name`='意图管理', `path`='intent', `component`='robot/intent/list', `icon`='mdi:target' WHERE `menu_id`=700;
UPDATE `sys_menu` SET `menu_name`='大模型引擎管理', `path`='engine', `component`='robot/engine/list', `icon`='mdi:brain' WHERE `menu_id`=701;
UPDATE `sys_menu` SET `menu_name`='知识库管理', `path`='knowledge', `component`='robot/knowledge/index', `icon`='mdi:book-open-variant' WHERE `menu_id`=702;
UPDATE `sys_menu` SET `menu_name`='机器人管理', `path`='index', `component`='robot/index', `icon`='mdi:robot' WHERE `menu_id`=703;

-- ===== fs-config-and-softphone: 「线路配置」按钮权限去专业词化（FS/acl 等用户看不懂的缩写改友好名） =====

-- changeset hsc:20260722-fs-button-rename
-- 线路配置(原 FS 配置管理)按钮权限去专业词化：menu_name/remark 改为用户友好的中文(主机配置/模块配置/SIP网关/访问控制)，
-- 去掉 FS、acl 等终端用户看不懂的缩写；拨号计划本就为中文不改；perms 权限码(system:fs:*/system:acl:*)保持不变，避免影响后端 @PreAuthorize 鉴权。
-- 主机配置(menu_id 1025-1030)
UPDATE `sys_menu` SET `menu_name`='新增主机配置', `remark`='新增主机配置按钮' WHERE `menu_id`=1025;
UPDATE `sys_menu` SET `menu_name`='修改主机配置', `remark`='修改主机配置按钮' WHERE `menu_id`=1026;
UPDATE `sys_menu` SET `menu_name`='主机配置详情', `remark`='主机配置详情按钮' WHERE `menu_id`=1027;
UPDATE `sys_menu` SET `menu_name`='删除主机配置', `remark`='删除主机配置按钮' WHERE `menu_id`=1028;
UPDATE `sys_menu` SET `menu_name`='主机配置列表(分页)', `remark`='主机配置列表(分页)按钮' WHERE `menu_id`=1029;
UPDATE `sys_menu` SET `menu_name`='主机配置列表(不分页)', `remark`='主机配置列表(不分页)按钮' WHERE `menu_id`=1030;
-- 模块配置(menu_id 1031-1036)
UPDATE `sys_menu` SET `menu_name`='新增模块配置', `remark`='新增模块配置按钮' WHERE `menu_id`=1031;
UPDATE `sys_menu` SET `menu_name`='修改模块配置', `remark`='修改模块配置按钮' WHERE `menu_id`=1032;
UPDATE `sys_menu` SET `menu_name`='模块配置详情', `remark`='模块配置详情按钮' WHERE `menu_id`=1033;
UPDATE `sys_menu` SET `menu_name`='删除模块配置', `remark`='删除模块配置按钮' WHERE `menu_id`=1034;
UPDATE `sys_menu` SET `menu_name`='模块配置列表(分页)', `remark`='模块配置列表(分页)按钮' WHERE `menu_id`=1035;
UPDATE `sys_menu` SET `menu_name`='模块配置列表(不分页)', `remark`='模块配置列表(不分页)按钮' WHERE `menu_id`=1036;
-- SIP网关(menu_id 1037-1042)
UPDATE `sys_menu` SET `menu_name`='新增SIP网关', `remark`='新增SIP网关按钮' WHERE `menu_id`=1037;
UPDATE `sys_menu` SET `menu_name`='修改SIP网关', `remark`='修改SIP网关按钮' WHERE `menu_id`=1038;
UPDATE `sys_menu` SET `menu_name`='SIP网关详情', `remark`='SIP网关详情按钮' WHERE `menu_id`=1039;
UPDATE `sys_menu` SET `menu_name`='删除SIP网关', `remark`='删除SIP网关按钮' WHERE `menu_id`=1040;
UPDATE `sys_menu` SET `menu_name`='SIP网关列表(分页)', `remark`='SIP网关列表(分页)按钮' WHERE `menu_id`=1041;
UPDATE `sys_menu` SET `menu_name`='SIP网关列表(不分页)', `remark`='SIP网关列表(不分页)按钮' WHERE `menu_id`=1042;
-- 访问控制(menu_id 1043-1050)：acl→规则组/规则，保留「规则组(table) vs 规则(node)」语义区分，避免重名混淆
UPDATE `sys_menu` SET `menu_name`='新增规则组', `remark`='新增规则组按钮' WHERE `menu_id`=1043;
UPDATE `sys_menu` SET `menu_name`='新增规则', `remark`='新增规则按钮' WHERE `menu_id`=1044;
UPDATE `sys_menu` SET `menu_name`='修改规则组', `remark`='修改规则组按钮' WHERE `menu_id`=1045;
UPDATE `sys_menu` SET `menu_name`='修改规则', `remark`='修改规则按钮' WHERE `menu_id`=1046;
UPDATE `sys_menu` SET `menu_name`='规则详情', `remark`='规则详情按钮' WHERE `menu_id`=1047;
UPDATE `sys_menu` SET `menu_name`='删除规则', `remark`='删除规则按钮' WHERE `menu_id`=1048;
UPDATE `sys_menu` SET `menu_name`='访问控制列表(分页)', `remark`='访问控制列表(分页)按钮' WHERE `menu_id`=1049;
UPDATE `sys_menu` SET `menu_name`='访问控制列表(不分页)', `remark`='访问控制列表(不分页)按钮' WHERE `menu_id`=1050;

-- ===== calltask path 规范化：/calltask → /call-task，修复 mixed 模式 name 大小写不对齐导致的「外呼任务」重复 =====

-- changeset hsc:20260722-calltask-path-kebab
-- 外呼任务(menu_id=4) path 由 /calltask 改 /call-task(kebab-case)：
-- 后端 toRouteName('/call-task')=CallTask、('/call-task/list')=CallTaskList，与前端 calltask.ts 静态路由
-- name(CallTask/CallTaskList)对齐；此前 path=/calltask 生成 Calltask，与前端 CallTask 大小写不一致，
-- mixed 模式按 name 合并失败会导致「外呼任务」连真后端时重复显示。component 不变(views 目录仍为 calltask)。
-- 前端 calltask.ts 父 path 已同步改为 /call-task。
UPDATE `sys_menu` SET `path`='/call-task' WHERE `menu_id`=4;

-- ===== calling 菜单转后端：启用并对齐「呼叫管理」（前端 calling 模块已迁移，后端菜单此前停用、靠前端 calling.ts 静态兜底） =====

-- changeset hsc:20260722-calling-menu-vben-align
-- 启用呼叫管理(menu_id=3，20260721-system-menu-vben-align 曾停用、此前靠前端 calling.ts 静态兜底)并对齐 vben：
-- 顶层 path 改 /calling、icon 改 iconify；10 个子菜单 path 改相对(对齐前端 calling.ts)、补 component(views/calling/*)、icon 改有效 iconify 名。
-- 启用后 mixed 模式按 name(Calling/CallingAgent/...=toRouteName(/calling/agent 等))与前端静态合并，呼叫管理转为后端 SQL 驱动、可按角色控制。
-- 号码池/号码路由图标用 mdi:database-outline / mdi:sign-direction(mdi:phone-multiple、mdi:routing、mdi:route 在 MDI 均不存在，iconify API not_found)。
-- 权限：超管(role_id=1)硬编码全量可见；其他角色需经「角色管理」页面勾选 menu_id=3 与 300-309 后方可见。
UPDATE `sys_menu` SET `status`=0, `path`='/calling', `icon`='ion:call-outline'    WHERE `menu_id`=3;
UPDATE `sys_menu` SET `path`='agent',              `component`='calling/agent/list',            `icon`='mdi:headset'                WHERE `menu_id`=300;
UPDATE `sys_menu` SET `path`='phone-number-route', `component`='calling/phoneNumberRoute/list', `icon`='mdi:sign-direction'         WHERE `menu_id`=301;
UPDATE `sys_menu` SET `path`='sip-number',         `component`='calling/sipNumber/list',        `icon`='mdi:ip-network'             WHERE `menu_id`=302;
UPDATE `sys_menu` SET `path`='phone-pool',         `component`='calling/phonePool/list',        `icon`='mdi:database-outline'       WHERE `menu_id`=303;
UPDATE `sys_menu` SET `path`='phone-number',       `component`='calling/phoneNumber/list',      `icon`='mdi:phone-outline'          WHERE `menu_id`=304;
UPDATE `sys_menu` SET `path`='voice-file',         `component`='calling/voiceFile/list',        `icon`='mdi:file-music-outline'     WHERE `menu_id`=305;
UPDATE `sys_menu` SET `path`='schedule',           `component`='calling/schedule/list',         `icon`='mdi:calendar-clock-outline' WHERE `menu_id`=306;
UPDATE `sys_menu` SET `path`='skill',              `component`='calling/skill/list',            `icon`='mdi:account-group-outline'  WHERE `menu_id`=307;
UPDATE `sys_menu` SET `path`='call-record',        `component`='calling/callRecord/list',       `icon`='mdi:phone-log-outline'      WHERE `menu_id`=308;
UPDATE `sys_menu` SET `path`='engine',             `component`='calling/engine/list',           `icon`='mdi:robot-outline'          WHERE `menu_id`=309;

-- ===== 线路配置图标调整：避免与号码管理等电话图标重复 =====

-- changeset hsc:20260722-fs-menu-icon-router-network
-- 线路配置(menu_id=2)图标由 mdi:phone-outline 改 mdi:router-network：
-- 原图标与号码管理(menu_id=304)等电话图标重复，且「线路配置」(FS 通信线路/网关总配置)用电话图标语义不贴。
-- mdi:router-network(网络线路设备)更贴，且与子菜单(主机 server-network、网关 transmission-tower)区分。
UPDATE `sys_menu` SET `icon`='mdi:router-network' WHERE `menu_id`=2;

-- ===== 重复图标去重：AI引擎、客户公海改用独立图标 =====

-- changeset hsc:20260722-menu-icon-dedup
-- 去重两处完全相同的菜单图标：
-- AI引擎管理(menu_id=309) mdi:robot-outline → mdi:pulse(声波脉冲，ASR/TTS 语音引擎；与机器人配置 menu_id=7 的 robot-outline 去重。
-- 注：曾用 mdi:waveform，该名在 iconify mdi 集中不存在导致菜单无图标，改用老牌图标 mdi:pulse)
-- 客户公海(menu_id=502) mdi:account-group-outline → mdi:water-outline(公"海"，脱离 account 系列；与技能组管理(menu_id=307)的 account-group-outline 去重)
UPDATE `sys_menu` SET `icon`='mdi:pulse' WHERE `menu_id`=309;
UPDATE `sys_menu` SET `icon`='mdi:water-outline' WHERE `menu_id`=502;

-- ===== 补全业务模块菜单按钮权限（F）：migrate 加模块只建 M/C、F(perms)未补，非超管拿不到 perms 被拦 =====

-- changeset hsc:add-category-c-menu splitStatements:true endDelimiter:;
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,`path`,component,is_frame,menu_type,visible,status,perms,icon,remark,create_by,create_time,update_by,update_time,del_flag) VALUES
(800,'类目管理',1,10,'/category',NULL,1,'C',0,0,NULL,'','类目管理（通用分组树）',1,NOW(),NULL,NULL,0);

-- 2) 补全各业务模块 F 按钮权限
-- changeset hsc:add-menu-button-perms splitStatements:false
INSERT INTO sys_menu (menu_id,menu_name,parent_id,order_num,`path`,component,is_frame,menu_type,visible,status,perms,icon,remark,create_by,create_time,update_by,update_time,del_flag) VALUES
-- 部门管理(父104)
(1071,'新增部门',104,1,'#',NULL,1,'F',0,0,'system:dept:add','','新增部门按钮',1,NOW(),NULL,NULL,0),
(1072,'修改部门',104,2,'#',NULL,1,'F',0,0,'system:dept:edit','','修改部门按钮',1,NOW(),NULL,NULL,0),
(1073,'删除部门',104,3,'#',NULL,1,'F',0,0,'system:dept:delete','','删除部门按钮',1,NOW(),NULL,NULL,0),
(1074,'部门详情',104,4,'#',NULL,1,'F',0,0,'system:dept:get','','部门详情按钮',1,NOW(),NULL,NULL,0),
(1075,'部门树',104,5,'#',NULL,1,'F',0,0,'system:dept:tree','','部门树按钮',1,NOW(),NULL,NULL,0),
-- 号码路由(父301)  ⚠️ perms 是 rout（少 e）
(1076,'新增路由',301,1,'#',NULL,1,'F',0,0,'call:rout:add','','新增路由按钮',1,NOW(),NULL,NULL,0),
(1077,'修改路由',301,2,'#',NULL,1,'F',0,0,'call:rout:edit','','修改路由按钮',1,NOW(),NULL,NULL,0),
(1078,'启用路由',301,3,'#',NULL,1,'F',0,0,'call:rout:enable','','启用路由按钮',1,NOW(),NULL,NULL,0),
(1079,'禁用路由',301,4,'#',NULL,1,'F',0,0,'call:rout:disable','','禁用路由按钮',1,NOW(),NULL,NULL,0),
(1080,'删除路由',301,5,'#',NULL,1,'F',0,0,'call:rout:delete','','删除路由按钮',1,NOW(),NULL,NULL,0),
(1081,'路由详情',301,6,'#',NULL,1,'F',0,0,'call:rout:get','','路由详情按钮',1,NOW(),NULL,NULL,0),
(1082,'路由列表',301,7,'#',NULL,1,'F',0,0,'call:rout:page:list','','路由列表按钮',1,NOW(),NULL,NULL,0),
-- SIP号码(父302)  ⚠️ perms 前缀 subscriber，删除用 del
(1083,'新增SIP',302,1,'#',NULL,1,'F',0,0,'system:subscriber:add','','新增SIP按钮',1,NOW(),NULL,NULL,0),
(1084,'批量新增SIP',302,2,'#',NULL,1,'F',0,0,'system:subscriber:batch:add','','批量新增SIP按钮',1,NOW(),NULL,NULL,0),
(1085,'修改SIP',302,3,'#',NULL,1,'F',0,0,'system:subscriber:edit','','修改SIP按钮',1,NOW(),NULL,NULL,0),
(1086,'SIP详情',302,4,'#',NULL,1,'F',0,0,'system:subscriber:get','','SIP详情按钮',1,NOW(),NULL,NULL,0),
(1087,'删除SIP',302,5,'#',NULL,1,'F',0,0,'system:subscriber:del','','删除SIP按钮',1,NOW(),NULL,NULL,0),
(1088,'SIP列表',302,6,'#',NULL,1,'F',0,0,'system:subscriber:page:list','','SIP列表按钮',1,NOW(),NULL,NULL,0),
(1089,'SIP下拉',302,7,'#',NULL,1,'F',0,0,'system:subscriber:select:list','','SIP下拉按钮',1,NOW(),NULL,NULL,0),
-- 号码池(父303)
(1090,'新增号码池',303,1,'#',NULL,1,'F',0,0,'call:display:pool:add','','新增号码池按钮',1,NOW(),NULL,NULL,0),
(1091,'修改号码池',303,2,'#',NULL,1,'F',0,0,'call:display:pool:edit','','修改号码池按钮',1,NOW(),NULL,NULL,0),
(1092,'删除号码池',303,3,'#',NULL,1,'F',0,0,'call:display:pool:delete','','删除号码池按钮',1,NOW(),NULL,NULL,0),
(1093,'号码池详情',303,4,'#',NULL,1,'F',0,0,'call:display:pool:get','','号码池详情按钮',1,NOW(),NULL,NULL,0),
(1094,'号码池列表',303,5,'#',NULL,1,'F',0,0,'call:display:pool:page:list','','号码池列表按钮',1,NOW(),NULL,NULL,0),
-- 号码管理(父304)
(1095,'新增号码',304,1,'#',NULL,1,'F',0,0,'call:display:add','','新增号码按钮',1,NOW(),NULL,NULL,0),
(1096,'修改号码',304,2,'#',NULL,1,'F',0,0,'call:display:edit','','修改号码按钮',1,NOW(),NULL,NULL,0),
(1097,'号码详情',304,3,'#',NULL,1,'F',0,0,'call:display:get','','号码详情按钮',1,NOW(),NULL,NULL,0),
(1098,'删除号码',304,4,'#',NULL,1,'F',0,0,'call:display:delete','','删除号码按钮',1,NOW(),NULL,NULL,0),
(1099,'号码列表',304,5,'#',NULL,1,'F',0,0,'call:display:page:list','','号码列表按钮',1,NOW(),NULL,NULL,0),
-- 语音文件(父305)
(1100,'新增语音',305,1,'#',NULL,1,'F',0,0,'call:voice:file:add','','新增语音按钮',1,NOW(),NULL,NULL,0),
(1101,'修改语音',305,2,'#',NULL,1,'F',0,0,'call:voice:file:edit','','修改语音按钮',1,NOW(),NULL,NULL,0),
(1102,'删除语音',305,3,'#',NULL,1,'F',0,0,'call:voice:file:delete','','删除语音按钮',1,NOW(),NULL,NULL,0),
(1103,'语音详情',305,4,'#',NULL,1,'F',0,0,'call:voice:file:get','','语音详情按钮',1,NOW(),NULL,NULL,0),
(1104,'语音列表',305,5,'#',NULL,1,'F',0,0,'call:voice:file:page:list','','语音列表按钮',1,NOW(),NULL,NULL,0),
-- 日程管理(父306)
(1105,'新增日程',306,1,'#',NULL,1,'F',0,0,'call:schedule:add','','新增日程按钮',1,NOW(),NULL,NULL,0),
(1106,'修改日程',306,2,'#',NULL,1,'F',0,0,'call:schedule:edit','','修改日程按钮',1,NOW(),NULL,NULL,0),
(1107,'删除日程',306,3,'#',NULL,1,'F',0,0,'call:schedule:delete','','删除日程按钮',1,NOW(),NULL,NULL,0),
(1108,'日程详情',306,4,'#',NULL,1,'F',0,0,'call:schedule:get','','日程详情按钮',1,NOW(),NULL,NULL,0),
(1109,'日程列表',306,5,'#',NULL,1,'F',0,0,'call:schedule:page:list','','日程列表按钮',1,NOW(),NULL,NULL,0),
-- 技能组(父307)  ⚠️ 删除复用 edit，无独立 delete
(1110,'新增技能组',307,1,'#',NULL,1,'F',0,0,'call:skill:add','','新增技能组按钮',1,NOW(),NULL,NULL,0),
(1111,'修改技能组',307,2,'#',NULL,1,'F',0,0,'call:skill:edit','','修改/删除技能组按钮',1,NOW(),NULL,NULL,0),
(1112,'技能组详情',307,3,'#',NULL,1,'F',0,0,'call:skill:get','','技能组详情按钮',1,NOW(),NULL,NULL,0),
(1113,'技能组列表',307,4,'#',NULL,1,'F',0,0,'call:skill:page:list','','技能组列表按钮',1,NOW(),NULL,NULL,0),
-- AI引擎(ASR/TTS)(父309)  ⚠️ 区别于机器人大模型引擎(modelConfig)
(1114,'新增引擎',309,1,'#',NULL,1,'F',0,0,'call:engine:add','','新增引擎按钮',1,NOW(),NULL,NULL,0),
(1115,'修改引擎',309,2,'#',NULL,1,'F',0,0,'call:engine:edit','','修改引擎按钮',1,NOW(),NULL,NULL,0),
(1116,'删除引擎',309,3,'#',NULL,1,'F',0,0,'call:engine:delete','','删除引擎按钮',1,NOW(),NULL,NULL,0),
(1117,'引擎详情',309,4,'#',NULL,1,'F',0,0,'call:engine:get','','引擎详情按钮',1,NOW(),NULL,NULL,0),
(1118,'引擎列表',309,5,'#',NULL,1,'F',0,0,'call:engine:page:list','','引擎列表按钮',1,NOW(),NULL,NULL,0),
-- IVR流程(父600)
(1119,'新增流程',600,1,'#',NULL,1,'F',0,0,'call:ivr:add','','新增流程按钮',1,NOW(),NULL,NULL,0),
(1120,'修改流程',600,2,'#',NULL,1,'F',0,0,'call:ivr:edit','','修改流程按钮',1,NOW(),NULL,NULL,0),
(1121,'删除流程',600,3,'#',NULL,1,'F',0,0,'call:ivr:delete','','删除流程按钮',1,NOW(),NULL,NULL,0),
(1122,'流程详情',600,4,'#',NULL,1,'F',0,0,'call:ivr:get','','流程详情按钮',1,NOW(),NULL,NULL,0),
(1123,'流程列表',600,5,'#',NULL,1,'F',0,0,'call:ivr:page:list','','流程列表按钮',1,NOW(),NULL,NULL,0),
(1124,'发布流程',600,6,'#',NULL,1,'F',0,0,'call:ivr:publish','','发布流程按钮',1,NOW(),NULL,NULL,0),
(1125,'下线流程',600,7,'#',NULL,1,'F',0,0,'call:ivr:offline','','下线流程按钮',1,NOW(),NULL,NULL,0),
-- 外呼任务(父400)
(1126,'新增任务',400,1,'#',NULL,1,'F',0,0,'call:task:add','','新增任务按钮',1,NOW(),NULL,NULL,0),
(1127,'修改任务',400,2,'#',NULL,1,'F',0,0,'call:task:edit','','修改任务按钮',1,NOW(),NULL,NULL,0),
(1128,'删除任务',400,3,'#',NULL,1,'F',0,0,'call:task:delete','','删除任务按钮',1,NOW(),NULL,NULL,0),
(1129,'任务详情',400,4,'#',NULL,1,'F',0,0,'call:task:get','','任务详情按钮',1,NOW(),NULL,NULL,0),
(1130,'任务列表',400,5,'#',NULL,1,'F',0,0,'call:task:page:list','','任务列表按钮',1,NOW(),NULL,NULL,0),
(1131,'启动任务',400,6,'#',NULL,1,'F',0,0,'call:task:start','','启动任务按钮',1,NOW(),NULL,NULL,0),
(1132,'暂停任务',400,7,'#',NULL,1,'F',0,0,'call:task:pause','','暂停任务按钮',1,NOW(),NULL,NULL,0),
(1133,'结束任务',400,8,'#',NULL,1,'F',0,0,'call:task:end','','结束任务按钮',1,NOW(),NULL,NULL,0),
(1134,'联系人列表',400,9,'#',NULL,1,'F',0,0,'call:task:contact:list','','联系人列表按钮',1,NOW(),NULL,NULL,0),
(1135,'导入联系人',400,10,'#',NULL,1,'F',0,0,'call:task:contact:import','','导入联系人按钮',1,NOW(),NULL,NULL,0),
-- 客户模板(父500)
(1136,'新增模板',500,1,'#',NULL,1,'F',0,0,'customer:template:add','','新增模板按钮',1,NOW(),NULL,NULL,0),
(1137,'修改模板',500,2,'#',NULL,1,'F',0,0,'customer:template:edit','','修改模板按钮',1,NOW(),NULL,NULL,0),
(1138,'模板详情',500,3,'#',NULL,1,'F',0,0,'customer:template:get','','模板详情按钮',1,NOW(),NULL,NULL,0),
(1139,'删除模板',500,4,'#',NULL,1,'F',0,0,'customer:template:delete','','删除模板按钮',1,NOW(),NULL,NULL,0),
(1140,'模板列表',500,5,'#',NULL,1,'F',0,0,'customer:template:page:list','','模板列表按钮',1,NOW(),NULL,NULL,0),
-- 客户字段(父501)
(1141,'新增字段',501,1,'#',NULL,1,'F',0,0,'customer:field:add','','新增字段按钮',1,NOW(),NULL,NULL,0),
(1142,'修改字段',501,2,'#',NULL,1,'F',0,0,'customer:field:edit','','修改字段按钮',1,NOW(),NULL,NULL,0),
(1143,'字段详情',501,3,'#',NULL,1,'F',0,0,'customer:field:get','','字段详情按钮',1,NOW(),NULL,NULL,0),
(1144,'删除字段',501,4,'#',NULL,1,'F',0,0,'customer:field:delete','','删除字段按钮',1,NOW(),NULL,NULL,0),
(1145,'字段列表',501,5,'#',NULL,1,'F',0,0,'customer:field:page:list','','字段列表按钮',1,NOW(),NULL,NULL,0),
-- 客户公海(父502)
(1146,'新增公海',502,1,'#',NULL,1,'F',0,0,'customer:seas:add','','新增公海按钮',1,NOW(),NULL,NULL,0),
(1147,'修改公海',502,2,'#',NULL,1,'F',0,0,'customer:seas:edit','','修改公海按钮',1,NOW(),NULL,NULL,0),
(1148,'公海详情',502,3,'#',NULL,1,'F',0,0,'customer:seas:get','','公海详情按钮',1,NOW(),NULL,NULL,0),
(1149,'删除公海',502,4,'#',NULL,1,'F',0,0,'customer:seas:delete','','删除公海按钮',1,NOW(),NULL,NULL,0),
(1150,'公海列表',502,5,'#',NULL,1,'F',0,0,'customer:seas:page:list','','公海列表按钮',1,NOW(),NULL,NULL,0),
(1151,'导入公海',502,6,'#',NULL,1,'F',0,0,'customer:seas:import','','导入公海按钮',1,NOW(),NULL,NULL,0),
-- 客户人群(父503)
(1152,'新增人群',503,1,'#',NULL,1,'F',0,0,'customer:crowd:add','','新增人群按钮',1,NOW(),NULL,NULL,0),
(1153,'修改人群',503,2,'#',NULL,1,'F',0,0,'customer:crowd:edit','','修改人群按钮',1,NOW(),NULL,NULL,0),
(1154,'人群详情',503,3,'#',NULL,1,'F',0,0,'customer:crowd:get','','人群详情按钮',1,NOW(),NULL,NULL,0),
(1155,'删除人群',503,4,'#',NULL,1,'F',0,0,'customer:crowd:delete','','删除人群按钮',1,NOW(),NULL,NULL,0),
(1156,'人群列表',503,5,'#',NULL,1,'F',0,0,'customer:crowd:page:list','','人群列表按钮',1,NOW(),NULL,NULL,0),
(1157,'人群客户列表',503,6,'#',NULL,1,'F',0,0,'customer:crowd:customer:page:list','','人群客户列表按钮',1,NOW(),NULL,NULL,0),
-- 大模型引擎(父701)  ⚠️ 区别于呼叫AI引擎(call:engine)
(1158,'大模型列表',701,1,'#',NULL,1,'F',0,0,'system:modelConfig:page:list','','大模型列表按钮',1,NOW(),NULL,NULL,0),
(1159,'大模型详情',701,2,'#',NULL,1,'F',0,0,'system:modelConfig:get','','大模型详情按钮',1,NOW(),NULL,NULL,0),
(1160,'新增大模型',701,3,'#',NULL,1,'F',0,0,'system:modelConfig:add','','新增大模型按钮',1,NOW(),NULL,NULL,0),
(1161,'修改大模型',701,4,'#',NULL,1,'F',0,0,'system:modelConfig:edit','','修改大模型按钮',1,NOW(),NULL,NULL,0),
(1162,'删除大模型',701,5,'#',NULL,1,'F',0,0,'system:modelConfig:delete','','删除大模型按钮',1,NOW(),NULL,NULL,0),
-- 话单/对话(父308)
(1163,'话单列表',308,1,'#',NULL,1,'F',0,0,'system:dialogue:list','','话单列表按钮',1,NOW(),NULL,NULL,0),
(1164,'话单编辑',308,2,'#',NULL,1,'F',0,0,'system:dialogue:edit','','话单编辑按钮',1,NOW(),NULL,NULL,0),
(1165,'删除话单',308,3,'#',NULL,1,'F',0,0,'system:dialogue:delete','','删除话单按钮',1,NOW(),NULL,NULL,0),
(1166,'话单导出',308,4,'#',NULL,1,'F',0,0,'system:dialogue:export','','话单导出按钮',1,NOW(),NULL,NULL,0),
-- 类目管理(父800)  CategoryTree 组件用
(1167,'新增类目',800,1,'#',NULL,1,'F',0,0,'system:category:add','','新增类目按钮',1,NOW(),NULL,NULL,0),
(1168,'修改类目',800,2,'#',NULL,1,'F',0,0,'system:category:edit','','修改类目按钮',1,NOW(),NULL,NULL,0),
(1169,'删除类目',800,3,'#',NULL,1,'F',0,0,'system:category:delete','','删除类目按钮',1,NOW(),NULL,NULL,0),
(1170,'类目详情',800,4,'#',NULL,1,'F',0,0,'system:category:get','','类目详情按钮',1,NOW(),NULL,NULL,0),
(1171,'类目树',800,5,'#',NULL,1,'F',0,0,'system:category:tree:list','','类目树按钮',1,NOW(),NULL,NULL,0);

-- ===== 删除多余的「类目管理」空壳菜单；分组 F 改挂系统管理目录 =====

-- changeset hsc:20260724-category-menu-reorg
-- 删「类目管理」空壳菜单(800，无对应前端页面、点不进去)；其下 5 个分组 F 按钮(1167-1171) 改挂到「系统管理」目录(menu_id=1) 下。
-- 分组(category)接口是通用一套(system:category:* 服务 IVR/技能组/意图/拨号计划)，挂系统管理通用区合理；
-- F 按钮不进导航菜单，用户在系统管理下看不到它们，只在「角色管理」分配树里出现供勾选。
DELETE FROM sys_menu WHERE menu_id = 800;
UPDATE sys_menu SET parent_id = 1 WHERE menu_id IN (1167,1168,1169,1170,1171);

-- ===== sip-registrations-perm: SIP号码注册状态查询按钮(挂 SIP号码管理 parent=302,perms 对齐后端 @PreAuthorize) =====

-- changeset hsc:20260728-sip-registrations-perm
INSERT INTO `sys_menu` VALUES (1172,'SIP注册状态',302,8,'#',NULL,1,'F',0,0,'system:subscriber:registrations','','SIP号码注册状态按钮',1,NOW(),NULL,NULL,0);

-- ===== fs-acl-event-socket-init: 预置 event_socket.auto(ESL ACL)收紧为"只后端连" =====

-- changeset hsc:20260728-fs-acl-event-socket-init
-- event_socket.auto 给 event_socket 模块(8021 ESL)用,控制谁能连后端管理通道。
-- 收紧为 default=deny + 不预置 node:
--   - 后端连 8021 不靠此节点,靠 FsAclXmlCurlHandler 下发时注入的"后端 IP 放行"兜底
--     (最前注入 allow 127.0.0.1 + 后端容器网卡 IP),故 default=deny 不会锁死后端;
--   - 外网/陌生 IP 一律拒,防外人连 8021 控制 FS(窃听/挂断/盗打)。
-- 配套:ACL 写操作走 reloadAcl(重建 ACL 池,非 reloadxml);event_socket.conf.xml apply-inbound-acl=event_socket.auto。
INSERT INTO `fs_acl` (`name`, `default_type`, `list_id`, `del_flag`, `create_time`)
VALUES ('event_socket.auto', 'deny', 0, 0, NOW());

-- ===== fs-config-local-init: 预置本地 FS 主机配置(docker 部署默认连宿主 FS) =====

-- changeset hsc:20260729-fs-config-local-init
-- 预置本地 FS 主机:后端容器经 host.docker.internal 连宿主 FS 的 ESL(8021)。
-- password=qweasdzxc 对齐 event_socket.conf.xml;out_time=8(ESL 连接超时秒)。
-- status 不落库(FsConfig 的 status 标了 @TableField(exist=false),由 FsClient 心跳实时维护),故不插。
-- 已存在同 IP 主机则不插(避免重复)。
INSERT INTO `fs_config` (`name`, `ip`, `port`, `password`, `out_time`, `create_time`, `del_flag`)
SELECT '本地', 'host.docker.internal', 8021, 'qweasdzxc', 10, NOW(), 0 FROM dual
WHERE NOT EXISTS (SELECT 1 FROM `fs_config` WHERE `ip`='host.docker.internal' AND `del_flag`=0);


-- ===== fs-acl-candidate-domains-init: 预置 candidate-allow + domains(换环境只改 CIDR) =====

-- changeset hsc:20260731-fs-acl-candidate-domains-init
-- 预置 WebRTC candidate ACL + 呼入鉴权 domains 两条 list 及默认 node,换部署环境只改 CIDR 即可。
-- 前提:xml_curl.conf.xml bindings 含 acl(否则 fs_acl 不下发,FS 仍读 acl.conf.xml 静态配置)。
-- candidate-allow:internal profile 的 apply-candidate-acl 引用,过滤 WebRTC ICE candidate。
--   放行 loopback 本机网段(18.18.0.0/16,按实际改) + RFC1918 内网(浏览器软电话 candidate)。
-- domains:FS 判定 SIP 入站"是否本域"用,放行 FS 自己 + HX4G 网段(按实际改)。
-- 配套:绑 acl 后 acl.conf.xml 里写死的 candidate-allow/domains 可删(FS 走 fs_acl 动态)。

-- candidate-allow list(default=deny,只放行下列 node)
INSERT INTO `fs_acl` (`name`, `default_type`, `list_id`, `del_flag`, `create_time`)
VALUES ('candidate-allow', 'deny', 0, 0, NOW());
-- candidate-allow nodes(CROSS JOIN 关联 list id,避免同表 INSERT 子查询限制)
INSERT INTO `fs_acl` (`list_id`, `node_type`, `cidr`, `del_flag`, `create_time`)
SELECT t.id, n.node_type, n.cidr, 0, NOW()
FROM (SELECT id FROM fs_acl WHERE name='candidate-allow' AND list_id=0) t
         CROSS JOIN (
    SELECT 'allow' AS node_type, '18.18.0.0/16' AS cidr UNION ALL
    SELECT 'allow', '192.168.0.0/16' UNION ALL
    SELECT 'allow', '10.0.0.0/8' UNION ALL
    SELECT 'allow', '172.16.0.0/12' UNION ALL
    SELECT 'allow', '127.0.0.1/32'
) n;

-- domains list(default=deny,只放行下列 node)
INSERT INTO `fs_acl` (`name`, `default_type`, `list_id`, `del_flag`, `create_time`)
VALUES ('domains', 'deny', 0, 0, NOW());
-- domains nodes
INSERT INTO `fs_acl` (`list_id`, `node_type`, `cidr`, `del_flag`, `create_time`)
SELECT t.id, n.node_type, n.cidr, 0, NOW()
FROM (SELECT id FROM fs_acl WHERE name='domains' AND list_id=0) t
         CROSS JOIN (
    SELECT 'allow' AS node_type, '18.18.18.69/32' AS cidr UNION ALL
    SELECT 'allow', '18.18.19.0/24'
) n;

-- ===== migrate-ai-config: AI 坐席配置预置（单例 id=1，默认 RuntimeConfig） =====

-- changeset hsc:20260803-ai-callbot-config-init
INSERT INTO `ai_callbot_config` (`id`, `config_json`, `del_flag`) VALUES (1, '{"reply_mode":"knowledge_base","asr_engine":"local","tts_engine":"local","welcome_enabled":true,"transfer_enabled":false,"transfer_dtmf_key":"0","transfer_refer_timeout":15,"knowledge_base_limit":100,"knowledge_base_similarity":0.5,"knowledge_base_embedding_weight":0.7,"knowledge_base_using_rerank":true,"knowledge_base_rerank_weight":0.9,"asr_pause_threshold":1.0,"interrupt_min_question_len":3,"vad_mode":2,"vad_speech_threshold_frames":15,"vad_self_suppress_ms":400,"vad_rms_floor":300,"barge_in_require_text":true,"barge_in_max_tail_s":1.2,"barge_in_grace_s":1.0,"endpoint_silence_frames":25,"silence_remind_sec":20,"silence_max_reminds":3,"silence_remind_prompt":"您还在吗，请问有什么可以帮您","silence_hangup_prompt":"感谢您的来电，再见","aliyun_asr_url":"wss://nls-gateway.cn-shanghai.aliyuncs.com/ws/v1","aliyun_tts_voice":"siyue","aliyun_tts_format":"wav","aliyun_tts_sample_rate":16000,"holartts_voice_id":"28","barge_in_backchannel_words":"嗯,嗯嗯,哦,哦哦,啊,是,是的,是啊,对,对的,没错,好的,行,知道,明白,了解"}', 0);

-- ===== migrate-ai-config: 提示词/人工坐席/AI坐席 菜单 + F 按钮权限 =====
-- 注：大模型引擎(701)的 F 按钮已存在(1158-1162 system:modelConfig:*)，本次无需补；
-- promptword/humanAgentConfig 后端虽已平迁，但 sys_menu 的 C 菜单+F 按钮此前缺失，本次一并补齐。

-- changeset hsc:20260803-ai-config-menu
-- 提示词管理(父7=机器人配置) C 菜单 + F 按钮
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`) VALUES
(1200, '提示词管理', 7, 2, 'promptword', 'robot/promptword/list', 1, 'C', 0, 0, NULL, 'mdi:text-box-edit-outline', '提示词管理', 1, NOW(), NULL, NULL, 0),
(1201, '提示词列表', 1200, 1, '#', NULL, 1, 'F', 0, 0, 'system:promptword:page:list', '', '提示词列表按钮', 1, NOW(), NULL, NULL, 0),
(1202, '提示词详情', 1200, 2, '#', NULL, 1, 'F', 0, 0, 'system:promptword:get', '', '提示词详情按钮', 1, NOW(), NULL, NULL, 0),
(1203, '新增提示词', 1200, 3, '#', NULL, 1, 'F', 0, 0, 'system:promptword:add', '', '新增提示词按钮', 1, NOW(), NULL, NULL, 0),
(1204, '修改提示词', 1200, 4, '#', NULL, 1, 'F', 0, 0, 'system:promptword:edit', '', '修改提示词按钮', 1, NOW(), NULL, NULL, 0),
(1205, '删除提示词', 1200, 5, '#', NULL, 1, 'F', 0, 0, 'system:promptword:delete', '', '删除提示词按钮', 1, NOW(), NULL, NULL, 0),
-- 人工坐席配置(父7) C 菜单 + F 按钮（配置坐席AI话术推荐，非知识库）
(1206, '坐席助手配置', 7, 3, 'human-agent', 'robot/humanAgent/index', 1, 'C', 0, 0, NULL, 'mdi:account-voice', '坐席AI话术推荐配置(真人接电话时调智能体推话术)', 1, NOW(), NULL, NULL, 0),
(1207, '坐席助手详情', 1206, 1, '#', NULL, 1, 'F', 0, 0, 'system:humanAgentConfig:get', '', '坐席助手详情按钮', 1, NOW(), NULL, NULL, 0),
(1208, '保存坐席助手', 1206, 2, '#', NULL, 1, 'F', 0, 0, 'system:humanAgentConfig:edit', '', '保存坐席助手按钮', 1, NOW(), NULL, NULL, 0),
-- 智能客服(原AI坐席配置)：复用 703(原机器人管理空壳) 改名+改 component + F 按钮
(1209, '智能客服详情', 703, 1, '#', NULL, 1, 'F', 0, 0, 'system:aiCallbotConfig:get', '', '智能客服详情按钮', 1, NOW(), NULL, NULL, 0),
(1210, '保存智能客服', 703, 2, '#', NULL, 1, 'F', 0, 0, 'system:aiCallbotConfig:edit', '', '保存智能客服按钮', 1, NOW(), NULL, NULL, 0);

-- 复用 703：改名"智能客服" + component 指向 aiCallbot（原 robot/index 空壳废弃）
UPDATE `sys_menu` SET `menu_name`='智能客服', `path`='ai-callbot', `component`='robot/aiCallbot/index', `icon`='mdi:robot' WHERE `menu_id`=703;

-- ===== 智能客服菜单改名（智能客服→智能客服配置，与"坐席助手配置"对称） =====
-- 注：上方 changeset 20260803-ai-config-menu 已运行（703=智能客服），liquibase 已运行 changeset 不可改（checksum 校验会报错），
-- 故本次改名用新 changeset 追加（遵循"liquibase 变更只追加"范式）。

-- changeset hsc:20260803-ai-callbot-menu-rename
UPDATE `sys_menu` SET `menu_name`='AI智能客服配置' WHERE `menu_id`=703;

-- ===== 智能客服 config_json 全量默认预置（用户保存前有完整默认值） =====
-- changeset hsc:20260804-ai-callbot-config-defaults
UPDATE `ai_callbot_config` SET `config_json` = '{"agent_api_key":"","agent_api_url":"http://host.docker.internal:13456/api/v1/chat/completions","agent_selected_app":"","aliyun_asr_appkey":"","aliyun_asr_token":"","aliyun_asr_url":"wss://nls-gateway.cn-shanghai.aliyuncs.com/ws/v1","aliyun_tts_appkey":"","aliyun_tts_format":"wav","aliyun_tts_sample_rate":16000,"aliyun_tts_token":"","aliyun_tts_url":"https://nls-gateway.cn-shanghai.aliyuncs.com/stream/v1/tts","aliyun_tts_voice":"siyue","asr_engine":"local","asr_pause_threshold":1.0,"barge_in_backchannel_words":"嗯,嗯嗯,哦,哦哦,啊,是,是的,是啊,对,对的,没错,好的,行,知道,明白,了解","barge_in_enabled":false,"barge_in_grace_s":1.0,"barge_in_max_tail_s":1.2,"barge_in_require_text":true,"endpoint_silence_frames":25,"fallback_reply_default":"抱歉，这个问题暂时没有相关资料，您可以换个方式问问看，如需转接人工，请说转人工或者按0。","goodbye_reply":"好的，如果您有其他问题，欢迎随时联系我们，祝您生活愉快，再见！","greeting_reply":"您好，能听到！请问有什么可以帮您？","holargpt_base_url":"","holargpt_list_api_key":"","holartts_url":"http://host.docker.internal:8000/tts/","holartts_voice_id":"28","interrupt_keywords":"停,停！,停下,停止,闭嘴,安静,够了,打住,好了,等等,好了停,等一下,等下,停一下,先别说,别说了,不要说了,别讲了,别继续,可以了,好了好了,先别说了,打住打住,停停停,知道了,明白","interrupt_min_question_len":4,"knowledge_base_embedding_weight":0.7,"knowledge_base_id":"","knowledge_base_limit":100,"knowledge_base_rerank_weight":0.9,"knowledge_base_similarity":0.5,"knowledge_base_url":"http://host.docker.internal:13456/api/core/dataset/searchTest","knowledge_base_using_rerank":true,"local_asr_url":"wss://host.docker.internal:8763","local_tts_audio_path":"Audios/female/甜美桃子.wav","local_tts_pro_url":"http://tts.holardata.com:11196/tts_url","reply_mode":"knowledge_base","silence_hangup_prompt":"感谢您的来电，再见","silence_max_reminds":3,"silence_remind_prompt":"您还在吗，请问有什么可以帮您","silence_remind_sec":20,"thinking_prefix":"好的，稍等，马上为您查询","transfer_blacklist":"","transfer_blacklist_prompt":"抱歉，当前人工坐席全忙，暂时无法为您转接。您的问题我可以直接帮您解答，请问还有什么想了解的？","transfer_busy_prompt":"当前坐席忙，请稍后再试。","transfer_dtmf_key":"0","transfer_enabled":false,"transfer_fallback_prompt":"抱歉，转接失败，请稍后再拨。","transfer_keywords":"转人工,人工客服,找人工,人工服务,转接人工,我要找人工","transfer_noanswer_prompt":"坐席暂时无法接听，请稍后再试。","transfer_prompt":"好的，正在为您转接人工客服，请稍候...","transfer_refer_timeout":15,"transfer_targets":"sip:分机号@交换机IP:5060","tts_engine":"local","tts_local_instructions":"","tts_local_url":"http://host.docker.internal:17151/v1/audio/speech","tts_local_voice":"Serena","vad_mode":2,"vad_rms_floor":300,"vad_self_suppress_ms":400,"vad_speech_threshold_frames":15,"welcome_enabled":true,"welcome_message":"您好呀！我是招生热线助手～ 招生简介、招生要点都可以问我。随时说''好了，停''就能打断我～ 您想问什么呢？"}' WHERE `id` = 1;

-- ===== call-record-detail-fullpage: 角色总结 prompt 改输出 JSON 数组（供前端角色卡片渲染） =====
-- changeset hsc:20260804-role-summary-prompt-array
UPDATE `chat_promptword` SET `content` = '##角色\n你是一个文本摘要专家，擅长按角色提炼对话内容。\n\n要求：\n1、按对话中出现的角色（如客户、坐席、AI助手）分别总结；\n2、严格输出 JSON 数组，每个元素形如 {"roleName":"角色名","roleSummary":"该角色的总结(不超过80字)"}；\n3、只输出 JSON 数组本身，不要任何解释、前后缀或 markdown 代码块标记。\n', `update_time` = NOW() WHERE `title` = '角色总结' AND `del_flag` = 0;

-- ===== 访问控制页改造:list 锁/node 开/中文 remark + 防盗打 domains 放宽(2026-08-06) =====

-- changeset hsc:20260806-fs-acl-add-remark
-- 访问控制 list 加中文显示名(remark):界面展示用,不影响 FS 下发(FsAclXmlCurlHandler 只读 name/defaultType/node 字段)。
ALTER TABLE `fs_acl` ADD COLUMN `remark` varchar(255) DEFAULT NULL COMMENT '中文显示名' AFTER `domain`;

-- changeset hsc:20260806-fs-acl-preset-remark
-- 三条预置 list 灌中文 remark(只 list 头需要,node 行 remark 留空)
UPDATE `fs_acl` SET `remark`='WebRTC 媒体白名单', `update_time`=NOW() WHERE `name`='candidate-allow' AND `list_id`=0 AND `del_flag`=0;
UPDATE `fs_acl` SET `remark`='SIP 入站白名单(防盗打)', `update_time`=NOW() WHERE `name`='domains' AND `list_id`=0 AND `del_flag`=0;
UPDATE `fs_acl` SET `remark`='ESL 连接白名单', `update_time`=NOW() WHERE `name`='event_socket.auto' AND `list_id`=0 AND `del_flag`=0;

-- changeset hsc:20260806-fs-acl-domains-broaden
-- 防盗打(apply-inbound-acl=domains,见 sip_profiles/internal.xml+external.xml)放宽 domains 放行网段:
-- 覆盖坐籍话机(18.18.18.x)/软电话浏览器(RFC1918)/HX4G(18.18.19.x)/ai-callbot/nginx/FS 所有合法来源,拒绝公网陌生 IP。
-- 原 domains 只 18.18.18.69/32 + 18.18.19.0/24,接 apply-inbound-acl 后会锁死坐籍(如 18.18.18.125 话机)。
INSERT INTO `fs_acl` (`list_id`, `node_type`, `cidr`, `del_flag`, `create_time`)
SELECT t.id, n.node_type, n.cidr, 0, NOW()
FROM (SELECT id FROM fs_acl WHERE name='domains' AND list_id=0 AND del_flag=0) t
         CROSS JOIN (
    SELECT 'allow' AS node_type, '18.18.0.0/16' AS cidr UNION ALL
    SELECT 'allow', '192.168.0.0/16' UNION ALL
    SELECT 'allow', '10.0.0.0/8' UNION ALL
    SELECT 'allow', '172.16.0.0/12' UNION ALL
    SELECT 'allow', '127.0.0.1/32'
) n
WHERE NOT EXISTS (
    SELECT 1 FROM fs_acl sub WHERE sub.list_id=t.id AND sub.cidr=n.cidr AND sub.del_flag=0
);

-- ===== sip-gateway-menu-rename: SIP网关菜单更名为「SIP中继网关」 =====

-- changeset hsc:20260810-sip-gateway-menu-rename
-- 线路配置→SIP网关(menu_id=202) 更名为「SIP中继网关」：
-- 该菜单配的是 fs_sip_gateway(sofia gateway 出局中继)，加「（中继）」点明是出局中继配置，
-- 并与硬件网关设备(HX4G 等)区分。仅改展示名 menu_name，path/component/perms 不变。
UPDATE `sys_menu` SET `menu_name`='SIP中继网关' WHERE `menu_id`=202;

-- ===== migrate-transfer-skillid: 转人工目标改技能组（删 transfer_targets 等 unirtc 死字段，加 transfer_skill_id） =====
-- 注：20260804-ai-callbot-config-defaults 已运行不可改（checksum 锁定），本次用新 changeset 追加。
-- 转人工从"ai-callbot SIP REFER 到 transfer_targets"改为"回调 hsc 按 transfer_skill_id 技能组选坐席"；
-- transfer_targets / transfer_refer_timeout / transfer_busy_prompt / transfer_noanswer_prompt 不再使用，一并清除。
-- changeset hsc:20260811-ai-callbot-config-transfer-skillid
UPDATE `ai_callbot_config` SET `config_json` = JSON_SET(JSON_REMOVE(`config_json`, '$.transfer_targets', '$.transfer_refer_timeout', '$.transfer_busy_prompt', '$.transfer_noanswer_prompt'), '$.transfer_skill_id', '') WHERE `id` = 1;

-- ===== menu-rename-20260812: 菜单去「管理/配置」泛化词、按业务语义重命名 =====
-- 一级目录与子菜单统一去「管理」冗余（保留「系统管理」作 RBAC 后台唯一例外），按业务语义见名知意。
-- 关键消歧义：呼叫中心「语音引擎」(menu_id=309, ASR/TTS 供 IVR) vs 智能机器人「大模型引擎」(menu_id=701, LLM) 成对区分，避免「AI引擎」混淆。
-- 仅改展示名 menu_name，path/component/perms/icon 均不变，不影响路由与鉴权。

-- changeset hsc:20260812-menu-rename
-- 一级目录
UPDATE `sys_menu` SET `menu_name`='呼叫中心' WHERE `menu_id`=3;
UPDATE `sys_menu` SET `menu_name`='客户资源' WHERE `menu_id`=5;
UPDATE `sys_menu` SET `menu_name`='IVR编排' WHERE `menu_id`=6;
UPDATE `sys_menu` SET `menu_name`='智能机器人' WHERE `menu_id`=7;
-- 系统管理子菜单(100-104，103 操作日志不变)
UPDATE `sys_menu` SET `menu_name`='用户账号' WHERE `menu_id`=100;
UPDATE `sys_menu` SET `menu_name`='角色权限' WHERE `menu_id`=101;
UPDATE `sys_menu` SET `menu_name`='菜单配置' WHERE `menu_id`=102;
UPDATE `sys_menu` SET `menu_name`='部门组织' WHERE `menu_id`=104;
-- 线路配置子菜单(201/203/204 不变，202 SIP中继网关不变)
UPDATE `sys_menu` SET `menu_name`='主机实例' WHERE `menu_id`=200;
-- 呼叫中心子菜单(原呼叫管理 300-309)
UPDATE `sys_menu` SET `menu_name`='坐席账号' WHERE `menu_id`=300;
UPDATE `sys_menu` SET `menu_name`='号码路由' WHERE `menu_id`=301;
UPDATE `sys_menu` SET `menu_name`='SIP号码' WHERE `menu_id`=302;
UPDATE `sys_menu` SET `menu_name`='显号池' WHERE `menu_id`=303;
UPDATE `sys_menu` SET `menu_name`='显号管理' WHERE `menu_id`=304;
UPDATE `sys_menu` SET `menu_name`='语音文件' WHERE `menu_id`=305;
UPDATE `sys_menu` SET `menu_name`='日程配置' WHERE `menu_id`=306;
UPDATE `sys_menu` SET `menu_name`='技能组' WHERE `menu_id`=307;
UPDATE `sys_menu` SET `menu_name`='通话记录' WHERE `menu_id`=308;
UPDATE `sys_menu` SET `menu_name`='语音引擎' WHERE `menu_id`=309;
-- 外呼任务子菜单
UPDATE `sys_menu` SET `menu_name`='任务列表' WHERE `menu_id`=400;
-- 客户资源子菜单(原客户管理 500-503)
UPDATE `sys_menu` SET `menu_name`='客户模板' WHERE `menu_id`=500;
UPDATE `sys_menu` SET `menu_name`='客户字段' WHERE `menu_id`=501;
UPDATE `sys_menu` SET `menu_name`='公海池' WHERE `menu_id`=502;
UPDATE `sys_menu` SET `menu_name`='客户人群' WHERE `menu_id`=503;
-- IVR编排子菜单(原IVR管理 600)
UPDATE `sys_menu` SET `menu_name`='IVR流程' WHERE `menu_id`=600;
-- 智能机器人子菜单(原机器人配置 700-703/1200/1206)
UPDATE `sys_menu` SET `menu_name`='意图识别' WHERE `menu_id`=700;
UPDATE `sys_menu` SET `menu_name`='大模型引擎' WHERE `menu_id`=701;
UPDATE `sys_menu` SET `menu_name`='知识库' WHERE `menu_id`=702;
UPDATE `sys_menu` SET `menu_name`='AI智能客服' WHERE `menu_id`=703;
UPDATE `sys_menu` SET `menu_name`='提示词库' WHERE `menu_id`=1200;
UPDATE `sys_menu` SET `menu_name`='坐席助手' WHERE `menu_id`=1206;

-- ===== calling-config-preset: 预制呼叫链路固定配置(网关/拨号计划/日程/路由/技能组),新环境部署自带、只改 IP =====
-- changeset hsc:20260812-calling-config-preset

-- 1) SIP中继网关:呼出(HX4G 出局,加拨号前缀0)/转坐席(FS loopback,坐席互打与转接用)
INSERT IGNORE INTO `fs_sip_gateway` (`id`,`name`,`user_name`,`password`,`realm`,`proxy`,`register`,`transport`,`caller_id_in_from`,`dial_prefix`,`from_domain`,`retry_time`,`ping_time`,`expire_time`,`type`,`gateway_type`,`create_by`,`create_time`,`update_by`,`update_time`,`del_flag`) VALUES
(1,'呼出','amdin','admin','对端ip','对端ip',0,1,0,'0','对端ip',30,25,300,2,1,1,NOW(),1,NOW(),0),
-- 转坐席网关:loopback 回 FS 内部,坐席互打/转接用;gateway_type=0 非外线(dial_prefix 不生效)。
-- type=1(internal)→makeCall 拼 user/分机@realm 直投注册表,不走 sofia gateway、不消费 user_name/password,故留空;
-- register=0:不再让 FS 向自身(18.18.18.69 即 FS)发起无意义 REGISTER,消除注册噪声。realm/proxy 保留占位 IP(拼 endpoint 用)。
(2,'转坐席',NULL,NULL,'转坐席用本机ip','转坐席用本机ip',0,1,0,'','转坐席用本机ip',25,30,300,1,0,1,NOW(),1,NOW(),0);

-- 2) 拨号计划:呼入(8000/1411 进 park 走 hsc 路由分发)/呼出(手机号进 park 走 hsc 外呼)
INSERT IGNORE INTO `fs_dialplan` (`id`,`group_id`,`name`,`type`,`expression`,`context_name`,`content`,`describe`,`create_by`,`create_time`,`update_by`,`update_time`,`del_flag`) VALUES
(1,0,'呼入','json',NULL,'public','{\n  \"condition\": [\n    {\n      \"field\": \"destination_number\",\n      \"expression\": \"^(8000|1411)$\",\n      \"action\": [\n        {\n          \"application\": \"park\",\n          \"data\": \"\"\n        }\n      ]\n    }\n  ]\n}','呼入坐席',1,NOW(),1,NOW(),0),
(2,0,'呼出','json',NULL,'public','{\n  \"condition\": [\n    {\n      \"field\": \"destination_number\",\n      \"expression\": \"^1[0-9]{10}$\",\n      \"action\": [\n        {\n          \"application\": \"park\",\n          \"data\": \"\"\n        }\n      ]\n    }\n  ]\n}','呼出手机',1,NOW(),1,NOW(),0);

-- 3) 日程:7x24 全天(呼入/呼出各一,绑定到对应号码路由 call_route.schedule_id)
-- type=1(相对时间):start_day/end_day 是"每月几号"(1~31,按月循环),永不过期(避 type=0 绝对日期会过期的坑);
-- work_cycle=1-7 每天命中、start_time/end_time 沿用线上值(呼入留到 23:39、呼出 23:00 的下班前微调保留)。
INSERT IGNORE INTO `call_schedule` (`id`,`name`,`level`,`type`,`start_day`,`end_day`,`start_time`,`end_time`,`work_cycle`,`create_by`,`create_time`,`update_by`,`update_time`,`del_flag`) VALUES
(1,'7x24-all-day呼入',0,1,'1','31','00:00','23:39','1,2,3,4,5,6,7',1,NOW(),1,NOW(),0),
(2,'7x24-all-day呼出',0,1,'1','31','00:00','23:00','1,2,3,5,4,6,7',1,NOW(),1,NOW(),0);

-- 4) 号码路由:呼入1411→AI智能坐席(route_type=8,schedule=日程1);呼出手机号→网关1(route_type=2,route_value=网关id,schedule=日程2)
INSERT IGNORE INTO `call_route` (`id`,`name`,`route_num`,`type`,`level`,`status`,`schedule_id`,`route_type`,`route_value`,`create_by`,`create_time`,`update_by`,`update_time`,`del_flag`) VALUES
(1,'呼入路由坐席(座机等)','^(8000|1411)$',1,1,1,1,8,'20',1,NOW(),1,NOW(),0),
(2,'坐席呼出手机号','^1[0-9]{10}$',2,0,1,2,2,'1',1,NOW(),1,NOW(),0);

-- 5) 技能组:AI 转人工默认技能组(pickFreeAgent 按 ai_callbot_config.transfer_skill_id 选 READY 坐席)
-- 固定策略(轮询/全忙挂机/排队超时30s/最大队列10)保留;灵活可配字段(号码池 caller/callee_phone_pool、排队音/转坐席音)留空,部署后按需在「技能组」页面配
INSERT IGNORE INTO `call_skill` (`id`,`group_id`,`name`,`priority`,`describe`,`strategy_type`,`full_busy_type`,`overflow_type`,`overflow_value`,`time_out`,`queue_length`,`queue_voice`,`agent_voice`,`caller_phone_pool`,`callee_phone_pool`,`create_by`,`create_time`,`update_by`,`update_time`,`del_flag`) VALUES
(1,0,'转人工技能组',0,'',1,2,0,NULL,30,10,NULL,NULL,NULL,NULL,1,NOW(),1,NOW(),0);

-- changeset hsc:20260821-voice-engine-menu-perms
-- 语音引擎菜单(309) F 按钮权限对齐新 VoiceEngineController（call_engine 旧接口已下线）
UPDATE `sys_menu` SET `perms`='call:voiceEngine:add' WHERE `menu_id`=1114 AND `perms`='call:engine:add';
UPDATE `sys_menu` SET `perms`='call:voiceEngine:edit' WHERE `menu_id`=1115 AND `perms`='call:engine:edit';
UPDATE `sys_menu` SET `perms`='call:voiceEngine:delete' WHERE `menu_id`=1116 AND `perms`='call:engine:delete';

-- changeset hsc:20260824-menu-disable-and-remove
-- 1) 模块配置(201)停用：页面为死功能留观（仅 xml_cdr/odbc_cdr 两口且 FS 未加载对应插件），
--    前后端代码保留，仅菜单 status=1 停用（getRouters 剔除停用节点），后续要用再启用
UPDATE `sys_menu` SET `status`=1 WHERE `menu_id`=201;
-- 2) 意图识别(700)/知识库(702)菜单删除，前后端页面代码同步移除：
--    意图库是前大模型时代架构，呼入转人工三触发(关键词/DTMF/AI标记)够用、外呼走 prompt+规则+LLM 分类，
--    后端从未实现且无消费方（论证见《方案/预测式自动外呼设计方案》附录 B/C/G）；
--    知识库走外部自有知识库系统（AI智能客服配置 knowledge_base_* 直连外部），本管理页为占位空壳无消费方
DELETE FROM `sys_menu` WHERE `menu_id` IN (700, 702);
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (700, 702);

-- changeset hsc:20260824-menu-309-icon-reapply
-- 重申语音引擎(309)图标 mdi:pulse。历史坑：1eff796 曾把已执行的 20260722-menu-icon-dedup
-- 原地改值(mdi:waveform→mdi:pulse)，liquibase 不重跑已执行 changeset，历史环境库中仍是
-- 旧值 mdi:waveform(iconify 集中不存在 → 菜单无图标)。此 changeset 幂等对齐，已对的库无副作用。
UPDATE `sys_menu` SET `icon`='mdi:pulse' WHERE `menu_id`=309 AND `icon`<>'mdi:pulse';

-- changeset hsc:20260824-sip-number-menu-icon
-- SIP号码(302)图标 mdi:ip-network → mdi:sim(SIM卡,号码载体语义更贴；ip-network 偏网络拓扑易误解)
UPDATE `sys_menu` SET `icon`='mdi:sim' WHERE `menu_id`=302 AND `icon`='mdi:ip-network';

-- ===== ivr-flow-version: 存量已发布流程补发布快照 =====
-- 升级后新通话从 published_flow_data 读流程，存量 status=2 流程无快照会导致一通都进不去，按草稿补齐
-- changeset hsc:20260825-flow-info-published-init
UPDATE `flow_info` SET `published_flow_data` = `flow_data` WHERE `published_flow_data` IS NULL AND `status` = 2 AND `del_flag` = 0;

-- ===== rename-ai-agent-term: AI叫法统一为"AI智能坐席" =====
-- 与话单坐席名(agent_name)、IVR转接锚点、转写channel_type统一叫法（坐席是系统领域词，客服是客户视角词）。
-- 历史轨迹：703 菜单曾名"智能客服"(20260803)→"AI智能客服配置"→"AI智能客服"(20260814)，本次定稿"AI智能坐席"。
-- changeset hsc:20260826-menu-703-rename-ai-agent
UPDATE `sys_menu` SET `menu_name`='AI智能坐席' WHERE `menu_id`=703;

-- ===== rename-ai-agent-term-2: 角色总结提示词举例"AI助手"改"AI智能坐席"(与channel_type叫法统一) =====
-- 摘要 LLM 按提示词举例命名角色，举例改后摘要展示与转写气泡一致。
-- WHERE 限定默认值：若提示词已在管理界面被自定义过则不动，不强覆盖用户配置。
-- changeset hsc:20260826-promptword-role-ai-agent
UPDATE `chat_promptword` SET `content` = REPLACE(`content`, 'AI助手', 'AI智能坐席'), `update_time` = NOW()
WHERE `title` = '角色总结' AND `del_flag` = 0 AND `content` LIKE '%AI助手%';

-- ===== summary-feedback-stats: 统计分析菜单目录 + AI摘要满意率报表 =====
-- 新一级目录「统计分析」承载后续所有报表；F 按钮 perms 与 DialogueController
-- GET /system/v1/dialogue/feedback/stats 的 @PreAuthorize 逐字对齐(system:dialogue:stats)。
-- changeset hsc:20260826-summary-feedback-stats-menu
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`) VALUES
(1211, '统计分析', 0, 8, '/stats', NULL, 1, 'M', 0, 0, NULL, 'mdi:chart-bar', '统计报表目录', 1, NOW(), NULL, NULL, 0),
(1212, 'AI摘要满意率', 1211, 1, 'summary-feedback', 'stats/summary-feedback/list', 1, 'C', 0, 0, NULL, 'mdi:thumb-up-outline', 'AI摘要满意率报表', 1, NOW(), NULL, NULL, 0),
(1213, '满意率统计', 1212, 1, '#', NULL, 1, 'F', 0, 0, 'system:dialogue:stats', '', '满意率统计按钮', 1, NOW(), NULL, NULL, 0);

-- ===== manual-outbound-closure: 外呼菜单信息架构收敛（人工外呼唯一形态 + AI外呼占位） =====
-- ①任务列表(400)改名"人工外呼"：AI自动外呼已决策暂缓后置专项（方案/预测式...文档 2026-08-26 决策），
--   人工外呼(原预览式)成为当前唯一外呼形态，任务表单同步删"任务类型"选择；
-- ②新增"AI智能外呼"占位 C 菜单(1214)：component 用将来真页面路径 outbound/ai/list，专项实施时只换组件内容不动菜单；
-- ③一级目录排序对调(4外呼任务↔5客户资源)：客户资源是外呼名单供给侧，排到消费方之前符合操作动线；
-- ④F 按钮(1215/1216) perms 与 CallTaskController 新接口 @PreAuthorize 逐字对齐
--   (call:task:contact:my / call:task:contact:dial)，挂 menu 400 下 order 续 11/12。
-- changeset hsc:20260827-manual-outbound-menu
UPDATE `sys_menu` SET `menu_name`='人工外呼' WHERE `menu_id`=400;
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`) VALUES
(1214, 'AI智能外呼', 4, 2, 'ai-outbound', 'outbound/ai/list', 1, 'C', 0, 0, NULL, 'mdi:phone-forward', 'AI自动批量外呼（功能规划中，占位）', 1, NOW(), NULL, NULL, 0),
(1215, '我的待拨', 400, 11, '#', NULL, 1, 'F', 0, 0, 'call:task:contact:my', '', '坐席我的待拨列表权限(F按钮,非页面)', 1, NOW(), NULL, NULL, 0),
(1216, '任务拨打', 400, 12, '#', NULL, 1, 'F', 0, 0, 'call:task:contact:dial', '', '坐席任务联系人拨打权限(F按钮,非页面)', 1, NOW(), NULL, NULL, 0);
-- 排序对调须两条同序执行（外呼任务 4→5、客户资源 5→4），中间态无第三目录占用 4/5 无冲突
UPDATE `sys_menu` SET `order_num`=5 WHERE `menu_id`=4 AND `order_num`=4;
UPDATE `sys_menu` SET `order_num`=4 WHERE `menu_id`=5 AND `order_num`=5;

-- ===== voice-engine-preset: 新环境预制默认 ASR/TTS 引擎实例（开箱可用） =====

-- changeset hsc:20260827-voice-engine-preset
-- 预制一条 ASR(funasr)+一条 TTS(pro)（连接参数固化自现网验证过的配置），
-- 并把 ai_callbot_config 的引擎引用(asr_engine_id/tts_engine_id)指向它们——
-- 新环境开箱 AI坐席/IVR/语音文件合成即有引擎可用，无需先手工建引擎。
-- INSERT IGNORE 幂等：已有环境(存量迁移的库) id 1/2 已存在则跳过；UPDATE 天然幂等。
-- ⚠️ funasr 的 local_asr_url 是内网地址，新环境 FunASR 服务不在该 IP 时需在页面改配置。
INSERT IGNORE INTO `voice_engine` (`id`, `name`, `engine_type`, `config`, `remark`, `create_by`, `create_time`, `del_flag`) VALUES
(1, '默认ASR引擎', 'funasr', '{"local_asr_url": "wss://host.docker.internal:8763"}', '系统预制默认引擎', 1, NOW(), 0),
(2, '默认TTS引擎', 'pro', '{"local_tts_pro_url": "http://tts.holardata.com:11196/tts_url", "local_tts_audio_path": "Audios/female/甜美桃子.wav"}', '系统预制默认引擎', 1, NOW(), 0);
UPDATE `ai_callbot_config` SET `config_json` = JSON_SET(`config_json`, '$.asr_engine_id', 1, '$.tts_engine_id', 2) WHERE `id` = 1;

-- ===== oem-brand-config: 品牌设置菜单 + 保存权限按钮 =====
-- 系统管理(menu_id=1)下新增「品牌设置」C 菜单；F 按钮 perms 与 BrandConfigController
-- POST /system/v1/brandConfig/save 的 @PreAuthorize 逐字对齐(system:brand:save)。
-- 查询接口 /get 免鉴权(登录页用)，无 F 按钮；menu_id 从 1216 续编。
-- changeset hsc:20260828-brand-config-menu
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`) VALUES
(1217, '品牌设置', 1, 9, 'brand', 'system/brand/index', 1, 'C', 0, 0, NULL, 'mdi:palette', 'OEM品牌设置(应用名/logo/文案)', 1, NOW(), NULL, NULL, 0),
(1218, '保存品牌配置', 1217, 1, '#', NULL, 1, 'F', 0, 0, 'system:brand:save', '', '保存品牌配置按钮', 1, NOW(), NULL, NULL, 0);

-- ===== dashboard-analytics-revamp: 话务报表菜单 =====
-- 「统计分析」目录(1211,注释即"承载后续所有报表")下新增「话务报表」C 菜单,
-- order_num=0 排在「AI摘要满意率」(1212,order=1)之前;只追加不动既有菜单。
-- 无 F 按钮:报表接口(/system/v1/dashboard/*)不加 @PreAuthorize,菜单可见性即访问控制。
-- menu_id 从 1218 续编。
-- changeset hsc:20260828-call-report-menu
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`) VALUES
(1219, '话务报表', 1211, 0, 'call-report', 'stats/call-report/list', 1, 'C', 0, 0, NULL, 'mdi:chart-box-outline', '话务量统计报表(趋势/坐席/接听方式/满意度等)', 1, NOW(), NULL, NULL, 0);

-- ===== customer-menu-order: 客户字段/客户模板菜单顺序对调 =====
-- 业务依赖顺序:先建客户字段、再由字段组装客户模板,「客户字段」(501)应排在
-- 「客户模板」(500)之前;基线 system.sql 里模板=1/字段=2,此处对调 order_num。
-- 与外呼任务/客户资源目录对调同款幂等写法(带原值条件,重跑不动)。
-- changeset hsc:20260829-customer-menu-order
UPDATE `sys_menu` SET `order_num`=2 WHERE `menu_id`=500 AND `order_num`=1;
UPDATE `sys_menu` SET `order_num`=1 WHERE `menu_id`=501 AND `order_num`=2;

-- ===== calling-menu-order: 通话记录/语音引擎菜单顺序对调 =====
-- 语音引擎(309)是配置类资源(被AI坐席/IVR/语音文件合成引用,属上游),通话记录(308)
-- 是运行数据;配置在前、记录在后,调换后呼叫管理前9项全为配置、末项为记录查询。
-- 与 customer-menu-order 同款幂等写法(带原值条件,重跑不动)。
-- changeset hsc:20260829-calling-menu-order
UPDATE `sys_menu` SET `order_num`=10 WHERE `menu_id`=308 AND `order_num`=9;
UPDATE `sys_menu` SET `order_num`=9 WHERE `menu_id`=309 AND `order_num`=10;

-- ===== menu-empty-path-fix: 修复目录菜单 path 被清空的存量数据 =====
-- 菜单管理表单对目录(M)隐藏 path 字段且提交时清空,目录一经编辑保存 path 即被抹成空串,
-- getRouters 生成 name="Root" 与前端 vben 根路由同名冲突,路由初始化失败页面打不开。
-- 前端已改为 M 不清 path、后端 buildVbenRouters 已加空 path 兜底(toRouteName 按 menuId 兜底),
-- 此处修正存量:顶级(parent_id=0)补绝对路径、子级补相对路径,均按 menu_id 生成稳定值。
-- changeset hsc:20260829-menu-empty-path-fix
UPDATE `sys_menu` SET `path`=CONCAT('/menu-', `menu_id`) WHERE `menu_type`='M' AND `parent_id`=0 AND (`path` IS NULL OR `path`='');
UPDATE `sys_menu` SET `path`=CONCAT('menu-', `menu_id`) WHERE `menu_type`='M' AND `parent_id`<>0 AND (`path` IS NULL OR `path`='');

-- ===== human-agent-config-url-fix: holargpt 默认地址改 host.docker.internal =====
-- 初始值 127.0.0.1:13456 在后端容器内指向容器自身,holargpt 实际部署在宿主机,
-- 推荐话术(streamChat)连接拒绝全线失败;对齐 ai_callbot_config 默认值(20260804)
-- 改为 host.docker.internal。带原值条件幂等,不覆盖运营已在页面配置的实际地址。
-- changeset hsc:20260901-human-agent-config-url-fix
UPDATE `human_agent_config`
SET `holargpt_base_url` = 'http://host.docker.internal:13456',
    `agent_api_url` = 'http://host.docker.internal:13456/api/v1/chat/completions',
    `update_time` = NOW()
WHERE `id` = 1 AND `holargpt_base_url` = 'http://127.0.0.1:13456';

-- ===== fs-acl-node-remark-backfill: 预置 ACL 节点补"说明" =====
-- 复用 remark 字段(list 行=中文显示名,node 行=节点说明),界面展示用,不影响 FS 下发。
-- 同一网段在两个列表含义不同:domains=SIP 信令门(谁的信令能进 FS),candidate-allow=WebRTC 媒体门
-- (浏览器 ICE 候选地址),故按"列表×网段"组合给文案;只补空 remark,不覆盖运营已填。
-- changeset hsc:20260901-fs-acl-node-remark-backfill
UPDATE `fs_acl` n
JOIN `fs_acl` l ON n.list_id = l.id AND l.del_flag = 0
SET n.remark = CASE n.cidr
        WHEN '18.18.18.19/32' THEN '语音网关(运营商线路接入),来电信令来源,删除则客户来电全部被拒'
        WHEN '18.18.18.0/24'  THEN '现场 IP 话机网段,座机注册与呼叫的信令来源,删除则座机全部离线'
        WHEN '18.18.18.18/32' THEN 'FS 服务器自身 IP(保险)'
        WHEN '18.18.18.69/32' THEN '旧环境 FS 本机 IP(历史预置,可按需删除)'
        WHEN '18.18.19.0/24'  THEN '语音网关网段(历史预置)'
        WHEN '18.18.0.0/16'   THEN '现场大网段,现场设备(话机/办公网)SIP 信令来源兜底'
        WHEN '192.168.0.0/16' THEN '内网标准段(办公网常见),软电话等设备的 SIP 信令来源,删除则该网段无法签入'
        WHEN '10.0.0.0/8'     THEN '内网标准段,软电话等设备的 SIP 信令来源,删除则该网段无法签入'
        WHEN '172.16.0.0/12'  THEN '内网标准段,软电话等设备的 SIP 信令来源,删除则该网段无法签入'
        WHEN '127.0.0.1/32'   THEN '本机回环,FS 同宿主机进程的 SIP 来源(保险)'
    END, n.update_time = NOW()
WHERE l.name = 'domains' AND n.del_flag = 0
  AND (n.remark IS NULL OR n.remark = '')
  AND n.cidr IN ('18.18.18.19/32','18.18.18.0/24','18.18.18.18/32','18.18.18.69/32','18.18.19.0/24',
                 '18.18.0.0/16','192.168.0.0/16','10.0.0.0/8','172.16.0.0/12','127.0.0.1/32');

UPDATE `fs_acl` n
JOIN `fs_acl` l ON n.list_id = l.id AND l.del_flag = 0
SET n.remark = CASE n.cidr
        WHEN '18.18.0.0/16'   THEN '现场大网段,现场浏览器软电话的媒体候选网段'
        WHEN '192.168.0.0/16' THEN '内网标准段(办公网常见),浏览器软电话媒体候选网段,删除则接通后无声音'
        WHEN '10.0.0.0/8'     THEN '内网标准段,浏览器软电话媒体候选网段,删除则该网段接通后无声音'
        WHEN '172.16.0.0/12'  THEN '内网标准段,浏览器软电话媒体候选网段,删除则该网段接通后无声音'
        WHEN '127.0.0.1/32'   THEN '本机回环,同机浏览器测试软电话用'
    END, n.update_time = NOW()
WHERE l.name = 'candidate-allow' AND n.del_flag = 0
  AND (n.remark IS NULL OR n.remark = '')
  AND n.cidr IN ('18.18.0.0/16','192.168.0.0/16','10.0.0.0/8','172.16.0.0/12','127.0.0.1/32');

-- ===== sys-config-management: 预置 9 个运营参数 =====
-- 键/默认值与 openspec sys-config-management design D1 清单一致；默认值=改造前代码常量
-- (拆分项除外:外呼30/呼入坐席30/呼入座机60)。页面仅可改 config_value 与 remark。
-- changeset hsc:20260901-sys-config-preset
INSERT INTO `sys_config` (`config_key`, `config_name`, `config_value`, `remark`, `create_by`, `create_time`, `del_flag`) VALUES
('call.ring-timeout.outbound',          '外呼被叫振铃超时(秒)',   '30',    '坐席软电话外呼/预览式外呼任务的被叫振铃时长，超时即挂断释放。建议 20~30，客户响铃 15~25 秒接听常见', 1, NOW(), 0),
('call.ring-timeout.inbound-agent',     '呼入转坐席振铃超时(秒)', '30',    '呼入转坐席/技能组轮呼/AI转人工/SIP 时 B 腿(坐席侧)振铃时长。建议 25~35；轮呼时单个坐席超时即跳下一个', 1, NOW(), 0),
('call.ring-timeout.inbound-extension', '呼入转座机振铃超时(秒)', '60',    '呼入路由为座机时话机振铃时长，按话机与人的距离调整。建议 30~60，过长则客户干听回铃', 1, NOW(), 0),
('call.desk-dial-agent-ring-timeout',   '座机代拨坐席腿超时(秒)', '10',    '座机代拨第一段(后端先拨坐席话机)的振铃时长，人在电脑旁话机在手边，无需过长。建议 10~20', 1, NOW(), 0),
('call.queue-timeout-default',          '排队超时兜底(秒)',       '60',    '技能组自身未配置排队超时时使用的兜底值，排队超过该时长按溢出策略处理', 1, NOW(), 0),
('call.queue-capacity-default',         '排队容量兜底(人)',       '100',   '技能组自身未配置排队容量时使用的兜底值，排队人数达到上限按溢出策略处理', 1, NOW(), 0),
('call.missed-reassign-limit',          '漏接重分配次数上限',     '1',     '技能组坐席腿未接通挂断(漏接)后，本通电话最多换人重拨的次数，0=漏接不换人直接按挂断处理', 1, NOW(), 0),
('call.queue-announce-interval-ms',     '排队播报间隔(毫秒)',     '30000', '客户在技能组排队等待期间周期性插播提示音的间隔，30000=30秒', 1, NOW(), 0),
('session.token-expire-minutes',        '登录有效期(分钟)',       '720',   '登录会话 token 有效期，720=12小时；仅对修改后新登录的会话生效', 1, NOW(), 0);

-- ===== sys-config-management: 参数管理菜单 + 查询/修改 F 按钮 =====
-- 系统管理(menu_id=1)下新增「参数管理」C 菜单；F 按钮 perms 与 SysConfigController 的
-- @PreAuthorize 逐字对齐(system:sysConfig:list / system:sysConfig:edit)。
-- 无新增/删除接口，故无对应 F 按钮；menu_id 从 1219 续编。
-- changeset hsc:20260901-sys-config-menu
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`) VALUES
(1220, '参数管理', 1, 10, 'sys-config', 'system/sysConfig/index', 1, 'C', 0, 0, NULL, 'mdi:tune-variant', '系统运营参数管理(仅参数值与备注可改,不可增删)', 1, NOW(), NULL, NULL, 0),
(1221, '参数查询', 1220, 1, '#', NULL, 1, 'F', 0, 0, 'system:sysConfig:list', '', '参数管理查询按钮', 1, NOW(), NULL, NULL, 0),
(1222, '参数修改', 1220, 2, '#', NULL, 1, 'F', 0, 0, 'system:sysConfig:edit', '', '参数管理修改按钮', 1, NOW(), NULL, NULL, 0);

-- ===== customer-anchor-fields: 预制客户系统字段(客户名称/联系方式) =====
-- sys_type=0 系统字段：字段列表只读(前端已按 sysType 禁用编辑/删除按钮，Service 校验兜底)，
-- 模板必须包含全部系统字段(CustomerTemplateService 校验)。fieldName 与 customer_seas
-- 生成列提取 key($.name/$.phone)对齐。前置 DELETE 清理与系统字段标识撞名的自定义字段，
-- 避免唯一键 idx_unique_name 冲突(模板关联如悬空需自行清理)。
-- changeset hsc:20260902-customer-field-preset
DELETE FROM `customer_field` WHERE `field_name` IN ('name', 'phone') AND `sys_type` = 1;
INSERT INTO `customer_field` (`field_label`, `field_name`, `status`, `field_type`, `required`, `options`, `sys_type`, `create_by`, `create_time`, `del_flag`) VALUES
('客户名称', 'name', 1, 1, 1, NULL, 0, 1, NOW(), 0),
('联系方式', 'phone', 1, 0, 1, NULL, 0, 1, NOW(), 0);

-- ===== sip-profile-normalization: fs_acl 全量重置(分面信任名单) =====
-- 为什么全量重置而非增量迁移(2026-09-02 决策):
-- 1. fs_acl 由"遇坑即改"的混用名单(domains)重构为职责分明的四个名单,终态与旧数据差异大,
--    增量迁移需大量 NOT EXISTS/子查询防御式写法,可读性差且依赖库中历史数据状态;
-- 2. 测试环境手工调试数据无需保留,重置后按页面节点说明重新配置现场网段,一次到位;
-- 3. 表已清空,显式指定 id 插入,list 头与节点的关联(list_id)直接可见,无需按 name 反查。
-- 四个名单与 sip_profiles 引用一一对应(重置后页面四个名单应与下表完全一致,多/少都属异常):
--   candidate-allow   → internal apply-candidate-acl(WebRTC ICE 候选)
--   internal-trust    → internal apply-inbound-acl(坐席接入,主防线是 401 鉴权)
--   external-trust    → external apply-inbound-acl(中继面,唯一防线,仅中继网段)
--   event_socket.auto → event_socket ESL(节点由后端代码注入,此处留空)
-- 旧 domains 名单随重置消失(P0 后无任何 profile 引用);其原节点去向:
--   18.18.0.0/16(含旧 FS 本机 18.18.18.69)/RFC1918/127.0.0.1 → internal-trust 继承;
--   18.18.19.0/24(语音网关（HX4G） 中继) → external-trust 接管(P0 后中继呼入走 external 口)。
-- 回滚:git revert 本 changeset 后重建库(重置为物理删除,无逻辑删除可恢复)。
-- changeset hsc260902:fs-acl-full-reset
DELETE FROM `fs_acl`;
INSERT INTO `fs_acl` (`id`,`name`,`default_type`,`list_id`,`remark`,`description`,`del_flag`,`create_by`,`create_time`) VALUES
(1,'candidate-allow','deny',0,'WebRTC 媒体白名单','WebRTC 通话 ICE 候选地址白名单(internal 专用)。收紧可能导致浏览器软电话媒体协商失败(488)',0,1,NOW()),
(2,'internal-trust','deny',0,'坐席接入白名单','internal(5060/WS5066) 坐席话机接入的入站白名单。放行来源 IP 后仍需分机密码鉴权(401)。收紧会导致软电话/IP话机无法签入或通话',0,1,NOW()),
(3,'external-trust','deny',0,'中继面白名单','external(9280) 中继面入站白名单:仅放行对接的中继/网关 IP。收紧会拒掉合法中继呼入,放宽有盗打风险',0,1,NOW()),
(4,'event_socket.auto','deny',0,'ESL 连接白名单','后端 ESL(8021) 连接白名单。已内置后端 IP 强制放行,一般无需改动',0,1,NOW());
INSERT INTO `fs_acl` (`id`,`list_id`,`node_type`,`cidr`,`remark`,`del_flag`,`create_by`,`create_time`) VALUES
-- candidate-allow(id=1): WebRTC ICE 候选网段(浏览器媒体)
(11,1,'allow','18.18.0.0/16','当前现场网段,浏览器软电话的媒体候选网段',0,1,NOW()),
(12,1,'allow','192.168.0.0/16','内网标准段(办公网常见),媒体候选网段,删除则该网段接通后无声音',0,1,NOW()),
(13,1,'allow','10.0.0.0/8','内网标准段,媒体候选网段,删除则该网段接通后无声音',0,1,NOW()),
(14,1,'allow','172.16.0.0/12','内网标准段(含 docker 容器网段),媒体候选网段,删除则该网段接通后无声音',0,1,NOW()),
(15,1,'allow','127.0.0.1/32','本机回环,同机浏览器测试软电话用',0,1,NOW()),
-- internal-trust(id=2): 坐席话机接入网段(名单是第一道防线,主防线是 401 鉴权)
(21,2,'allow','127.0.0.1/32','本机回环,FS 同宿主机进程来源',0,1,NOW()),
(22,2,'allow','10.0.0.0/8','内网标准段,坐席话机/软电话来源,删除则该网段无法签入',0,1,NOW()),
(23,2,'allow','172.16.0.0/12','内网标准段(含 docker 容器网段),坐席话机/nginx 转发来源',0,1,NOW()),
(24,2,'allow','192.168.0.0/16','内网标准段(办公网常见),坐席话机来源',0,1,NOW()),
(25,2,'allow','18.18.0.0/16','当前现场网段(坐席话机如：18.18.18.x),新环境交付按实际网段调整',0,1,NOW()),
-- external-trust(id=3): 中继面对端网段(external 无鉴权,此名单是唯一防线)
(31,3,'allow','18.18.19.0/24','语音网关网段,新环境交付按实际中继对端 IP 调整',0,1,NOW());

-- ===== sip-profile-normalization: external-trust 说明文案端口修正 5080→9280 =====
-- 5080 被宿主上其他应用的端口映射占用(5443 同样被占),FS host 网络绑不上导致 external
-- profile 创建失败(Invalid Profile 外呼全断),端口回归 9280。本条幂等 UPDATE 兼容
-- 已执行过 full-reset(文案写 5080)与未执行(INSERT 已改 9280,重写无害)两种库状态。
-- changeset hsc:20260902-fs-acl-external-port-desc
UPDATE `fs_acl` SET `description`='external(9280) 中继面入站白名单:仅放行对接的中继/网关 IP。收紧会拒掉合法中继呼入,放宽有盗打风险', `update_time`=NOW()
WHERE `name`='external-trust' AND `list_id`=0 AND `del_flag`=0
  AND `description` LIKE 'external(5080)%';

-- ===== customer-field: 删除改物理删除，清理存量逻辑删除行 =====
-- customer_field 是配置表：已删字段在任何页面都查不到(getList 硬编码 del_flag=0)，
-- 无展示/恢复入口，逻辑删除的行只会占 field_name 唯一索引(idx_unique_name)挡住
-- 同名字段重建。delete 已改 removeByIds 物理删除，此处清掉历史逻辑删除的存量行。
-- changeset hsc:20260902-customer-field-physical-delete
DELETE FROM `customer_field` WHERE `del_flag` = 1;

-- ===== admin-default-password: 预制超管默认密码 12345678 → admin =====
-- init/system.sql 基线不动(只追加范式)，此处 UPDATE 对新库(建库后追平)与存量库统一生效。
-- 注意：admin 仅 5 位，低于改密链路"至少 6 位"下限(前端 password-setting min6 / 后端
-- UserProfilePasswordQuery @Size(min=6))；登录链路无长度校验不受影响，但之后无法在页面
-- 把密码改回 6 位以下(只能再跑 SQL)。幂等：重复执行重置为同一密文。
-- changeset hsc:20260903-admin-default-password
UPDATE `sys_user` SET `password`='$2a$10$F9f9HrmT5pDm/Gw8AVGkPul1kJfvljEPc1/dF4WUwMs91HNuX96ja', `update_time`=NOW()
WHERE `user_id`=1 AND `user_name`='admin';

-- ===== customer-pool-outbound-feedback: 任务联系人存量重复清理 =====
-- 历史导入无查重，同任务同号码存在多行(列表重复展示/分配多坐席)。保留 id 最小行、
-- 其余软删；导入预检(应用层)保证今后不再产生任务内重复。幂等：重复执行无匹配行。
-- 派生表包一层避免 MySQL "不能在 UPDATE 中子查询同表"限制。
-- changeset hsc:20260903-assignment-dedup-cleanup
UPDATE `call_task_assignment` a
JOIN (
    SELECT MIN(t.id) AS keep_id, t.task_id, t.phone
    FROM `call_task_assignment` t
    WHERE t.del_flag = 0
    GROUP BY t.task_id, t.phone
    HAVING COUNT(*) > 1
) k ON a.task_id = k.task_id AND a.phone = k.phone AND a.id <> k.keep_id
SET a.del_flag = 1, a.update_time = NOW()
WHERE a.del_flag = 0;

-- ===== customer-pool-outbound-feedback: 话后小结参数预制 + 拨打历史F按钮 =====
-- 话后结果选项：JSON 数组 [{value,label}]，电销客户可改为意向类(有意向A/待跟进B/无意向C…)、
-- 通知客户改为送达类(已通知/无人接听再约/空号…)——同一功能两用，只改参数不改代码。
-- 必填开关默认 0(通用系统保守起步，通知型高频外呼不被强制拖慢；电销客户可改 1 保证数据质量)。
-- F 按钮 1223 perms 与 CallTaskController 新接口 @PreAuthorize 逐字对齐(call:task:contact:diallog)。
-- changeset hsc:20260903-disposition-config-seed
INSERT INTO `sys_config` (`config_key`, `config_name`, `config_value`, `remark`, `create_by`, `create_time`, `del_flag`) VALUES
('call.disposition.options', '话后结果选项', '[{"value":"connected-interested","label":"接通-有意向"},{"value":"connected-follow","label":"接通-待跟进"},{"value":"connected-reject","label":"接通-无意向"},{"value":"no-answer-retry","label":"未接通-稍后重拨"},{"value":"bad-number","label":"号码无效(空号/停机)"},{"value":"appointment","label":"预约回访"}]', '坐席挂断后话后小结的可选结果(JSON数组value+label)。电销场景可改为意向等级，通知场景可改为送达结果；修改后新通话弹层即生效', 1, NOW(), 0),
('call.disposition.required', '话后小结必填', 'false', '坐席挂断后是否必须选择话后结果才能继续拨打下一通。false=可跳过(默认，通知型友好)，true=必填(电销型保证数据质量)', 1, NOW(), 0);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`) VALUES
(1223, '拨打历史', 400, 13, '#', NULL, 1, 'F', 0, 0, 'call:task:contact:diallog', '', '任务联系人拨打历史明细权限(F按钮,非页面)', 1, NOW(), NULL, NULL, 0);

-- ===== customer-pool-outbound-feedback: 客户归属(私海)菜单与F按钮 =====
-- 「我的客户」C 菜单挂客户资源目录(5)下公海(502)之后；分配/收回挂公海菜单(502)——
-- 管理员分配制：分配(乐观锁)/收回(含原因)为管理员操作，坐席侧只读我的客户。
-- perms 与 CustomerSeasController 新接口 @PreAuthorize 逐字对齐。
-- changeset hsc:20260903-customer-pool-menu
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`) VALUES
(1224, '我的客户', 5, 4, 'private-seas', 'customer/private/list', 1, 'C', 0, 0, NULL, 'mdi:account-star', '私海：归属当前坐席的客户（管理员分配制）', 1, NOW(), NULL, NULL, 0),
(1225, '分配客户', 502, 1, '#', NULL, 1, 'F', 0, 0, 'customer:seas:assign', '', '公海客户分配给坐席(进私海)', 1, NOW(), NULL, NULL, 0),
(1226, '收回客户', 502, 2, '#', NULL, 1, 'F', 0, 0, 'customer:seas:release', '', '收回私海客户回公海', 1, NOW(), NULL, NULL, 0),
(1227, '我的客户列表', 1224, 1, '#', NULL, 1, 'F', 0, 0, 'customer:seas:mine', '', '我的客户分页查询(坐席本人)', 1, NOW(), NULL, NULL, 0),
(1228, '客户流转时间线', 502, 3, '#', NULL, 1, 'F', 0, 0, 'customer:seas:timeline', '', '客户流转时间线(归属动作+外呼明细混排)', 1, NOW(), NULL, NULL, 0);

-- ===== dept-company-name-neutralize: 清除预制部门的公司名 =====

-- changeset hsc:20260904-dept-name-neutralize
-- 开发期预制的根部门"海纳数聚"是公司内部名，交付环境不应出现，改为中性的"科技有限公司"；
-- 带 dept_name 条件幂等（新库从头跑：先 INSERT 原名再 UPDATE，结果一致）
UPDATE `sys_dept` SET `dept_name` = '科技有限公司' WHERE `dept_id` = 1 ;