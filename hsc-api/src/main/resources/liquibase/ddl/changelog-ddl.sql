-- liquibase formatted sql

-- 建表/改表 DDL 增量记录（后续每次表结构变更在此追加 -- changeset）

-- ===== integrate-hotline-assistant: 坐席助手业务融合（dialog_record/abstract_record/call_ai_summary/model_config/chat_promptword） =====

-- changeset hsc:20260720-dialog-record
CREATE TABLE `dialog_record` (
                                `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                `call_id` varchar(64) NOT NULL COMMENT '通话ID(关联 call_record.call_id)',
                                `channel_type` varchar(32) DEFAULT NULL COMMENT '发言人类型 坐席/客户/AI助手',
                                `msg_type` varchar(32) DEFAULT NULL COMMENT '消息类型 ASR/ai_answer/transfer',
                                `dialog_txt` text COMMENT '对话文本',
                                `asr_json` text COMMENT 'ASR原始JSON',
                                `trans_txt` text COMMENT '翻译文本',
                                `gaixie_txt` text COMMENT 'AI改写文本',
                                `seq` bigint(20) DEFAULT NULL COMMENT '序号',
                                `speak_time` datetime DEFAULT NULL COMMENT '说话时间',
                                `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '摘要处理状态 0-未处理 1-已处理',
                                `create_by` bigint(20) DEFAULT NULL COMMENT '创建人',
                                `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                `update_by` bigint(20) DEFAULT NULL COMMENT '更新人',
                                `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                `del_flag` tinyint(4) NOT NULL DEFAULT '0' COMMENT '删除标识 0 正常 1 删除',
                                PRIMARY KEY (`id`),
                                KEY `idx_call_id` (`call_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通话逐句对话记录表';

-- changeset hsc:20260720-abstract-record
CREATE TABLE `abstract_record` (
                                  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                  `call_id` varchar(64) NOT NULL COMMENT '通话ID',
                                  `dialog_txt` text COMMENT '原文快照',
                                  `title` varchar(500) DEFAULT NULL COMMENT '标题摘要',
                                  `content` text COMMENT '内容摘要',
                                  `feedback` tinyint(4) NOT NULL DEFAULT '0' COMMENT '反馈标记 0-无 1-满意 2-不满意',
                                  `feedback_txt` text COMMENT '反馈文本',
                                  `create_by` bigint(20) DEFAULT NULL COMMENT '创建人',
                                  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                  `update_by` bigint(20) DEFAULT NULL COMMENT '更新人',
                                  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                  `del_flag` tinyint(4) NOT NULL DEFAULT '0' COMMENT '删除标识 0 正常 1 删除',
                                  PRIMARY KEY (`id`),
                                  KEY `idx_call_id` (`call_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通话过程摘要表';

-- changeset hsc:20260720-call-ai-summary
CREATE TABLE `call_ai_summary` (
                                   `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                   `call_id` varchar(64) NOT NULL COMMENT '通话ID(关联 call_record.call_id)',
                                   `summary` text COMMENT '全文总结',
                                   `keyword_abstract` text COMMENT '关键词',
                                   `role_summary` text COMMENT '角色总结',
                                   `keypoint_extract` text COMMENT '要点提炼',
                                   `todo_things` text COMMENT '代办事项',
                                   `markdown_json` text COMMENT '思维导图JSON',
                                   `summary_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '请求状态 0-未请求 1-处理中 2-已完成',
                                   `keyword_status` tinyint(4) NOT NULL DEFAULT '0',
                                   `role_status` tinyint(4) NOT NULL DEFAULT '0',
                                   `keypoint_status` tinyint(4) NOT NULL DEFAULT '0',
                                   `todo_status` tinyint(4) NOT NULL DEFAULT '0',
                                   `xmind_status` tinyint(4) NOT NULL DEFAULT '0',
                                   `keyword_feedback` tinyint(4) NOT NULL DEFAULT '0' COMMENT '反馈标记 0-无 1-满意 2-不满意',
                                   `role_feedback` tinyint(4) NOT NULL DEFAULT '0',
                                   `keypoint_feedback` tinyint(4) NOT NULL DEFAULT '0',
                                   `todo_feedback` tinyint(4) NOT NULL DEFAULT '0',
                                   `xmind_feedback` tinyint(4) NOT NULL DEFAULT '0',
                                   `create_by` bigint(20) DEFAULT NULL COMMENT '创建人',
                                   `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                   `update_by` bigint(20) DEFAULT NULL COMMENT '更新人',
                                   `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                   `del_flag` tinyint(4) NOT NULL DEFAULT '0' COMMENT '删除标识 0 正常 1 删除',
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uk_call_id` (`call_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通话AI摘要扩展表';

-- changeset hsc:20260720-model-config
CREATE TABLE `model_config` (
                               `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                               `name` varchar(64) NOT NULL COMMENT '模型配置名称',
                               `base_model` varchar(128) NOT NULL COMMENT '基础模型名(如 qwen-plus/deepseek-chat)',
                               `api_domain` varchar(255) NOT NULL COMMENT 'OpenAI兼容baseUrl',
                               `config` text COMMENT '附加配置JSON(api_key/temperature/max_tokens等)',
                               `is_default` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否默认 0-否 1-是',
                               `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态 0-停用 1-启用',
                               `remark` varchar(255) DEFAULT NULL COMMENT '备注',
                               `create_by` bigint(20) DEFAULT NULL COMMENT '创建人',
                               `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                               `update_by` bigint(20) DEFAULT NULL COMMENT '更新人',
                               `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                               `del_flag` tinyint(4) NOT NULL DEFAULT '0' COMMENT '删除标识 0 正常 1 删除',
                               PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LLM模型配置表';

-- changeset hsc:20260720-chat-promptword
CREATE TABLE `chat_promptword` (
                                   `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                   `title` varchar(64) NOT NULL COMMENT '提示词标题(过程摘要/全文总结等)',
                                   `content` text COMMENT '提示词内容',
                                   `remark` varchar(255) DEFAULT NULL COMMENT '备注',
                                   `create_by` bigint(20) DEFAULT NULL COMMENT '创建人',
                                   `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                   `update_by` bigint(20) DEFAULT NULL COMMENT '更新人',
                                   `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                   `del_flag` tinyint(4) NOT NULL DEFAULT '0' COMMENT '删除标识 0 正常 1 删除',
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uk_title` (`title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体提示词表';

-- ===== integrate-hotline-assistant: 人工坐席助手配置（human_agent_config，单例 id=1） =====

-- changeset hsc:20260721-human-agent-config
CREATE TABLE `human_agent_config` (
                                    `id` bigint(20) NOT NULL COMMENT '主键ID(单例固定1)',
                                    `holargpt_base_url` varchar(255) DEFAULT NULL COMMENT 'holargpt基础地址',
                                    `list_api_key` varchar(255) DEFAULT NULL COMMENT '应用列表接口apiKey',
                                    `agent_selected_app` varchar(128) DEFAULT NULL COMMENT '选用的智能体应用',
                                    `agent_api_url` varchar(255) DEFAULT NULL COMMENT '智能体流式接口地址',
                                    `agent_api_key` varchar(255) DEFAULT NULL COMMENT '智能体接口apiKey',
                                    `create_by` bigint(20) DEFAULT NULL COMMENT '创建人',
                                    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                    `update_by` bigint(20) DEFAULT NULL COMMENT '更新人',
                                    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                    `del_flag` tinyint(4) NOT NULL DEFAULT '0' COMMENT '删除标识 0 正常 1 删除',
                                    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人工坐席助手配置表(单例,id=1)';

-- ===== drop-fs-config-username: 主机配置 user_name 死字段清理(ESL 仅 password 鉴权,从不读 user_name) =====

-- changeset hsc:20260727-fs-config-drop-username
ALTER TABLE `fs_config` DROP COLUMN `user_name`;

-- ===== sip-terminal-type: SIP号码加终端类型(0=软电话/1=座机),软电话密码后端自动生成、座机用户填纯数字≥8 =====

-- changeset hsc:20260728-ko-subscriber-add-terminal-type
ALTER TABLE `ko_subscriber` ADD COLUMN `terminal_type` tinyint(4) NOT NULL DEFAULT '0' COMMENT '终端类型 0-软电话 1-座机' AFTER `status`;

-- ===== fs-config-status-drop: fs_config.status 改为运行时内存状态(后端 FsClient 实时查 ESL),不再落库 =====

-- changeset hsc:20260728-fs-config-drop-status
ALTER TABLE `fs_config` DROP COLUMN `status`;

-- ===== migrate-ai-config: AI 坐席配置（ai_callbot_config，单例 id=1） =====

-- changeset hsc:20260803-ai-callbot-config
CREATE TABLE `ai_callbot_config` (
                                    `id` bigint(20) NOT NULL COMMENT '主键ID(单例固定1)',
                                    `config_json` text COMMENT '完整业务配置JSON(对齐 ai-callbot RuntimeConfig)',
                                    `create_by` bigint(20) DEFAULT NULL COMMENT '创建人',
                                    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                    `update_by` bigint(20) DEFAULT NULL COMMENT '更新人',
                                    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                    `del_flag` tinyint(4) NOT NULL DEFAULT '0' COMMENT '删除标识 0 正常 1 删除',
                                    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI坐席配置表(单例,id=1)';
-- ===== call-record-detail-fullpage: 话单详情整页化契约补齐（dialog_record 音频时间戳/改写开关 + call_ai_summary 全文反馈） =====

-- changeset hsc:20260804-dialog-record-audio-timestamp
ALTER TABLE `dialog_record` ADD COLUMN `audio_start` bigint(20) DEFAULT NULL COMMENT '音频起始时间戳(毫秒) 供前端音频-文字联动' AFTER `status`;
ALTER TABLE `dialog_record` ADD COLUMN `audio_end` bigint(20) DEFAULT NULL COMMENT '音频结束时间戳(毫秒) 供前端音频-文字联动' AFTER `audio_start`;
ALTER TABLE `dialog_record` ADD COLUMN `gaixie_whether` tinyint(4) DEFAULT '0' COMMENT '是否显示改写 0-否 1-是' AFTER `audio_end`;

-- changeset hsc:20260804-call-ai-summary-summary-feedback
ALTER TABLE `call_ai_summary` ADD COLUMN `summary_feedback` tinyint(4) DEFAULT '0' COMMENT '全文总结反馈 0-无 1-满意 2-不满意' AFTER `xmind_feedback`;

-- ===== ai-route-unified: AI 智能坐席纳入 hsc 统一路由（dialog_record 加来源 + call_record 加 AI/转人工标记） =====

-- changeset hsc:20260805-dialog-record-source
ALTER TABLE `dialog_record` ADD COLUMN `source` varchar(20) DEFAULT NULL COMMENT '来源 ai-ai-callbot对话段 / bypass-人工通话旁路ASR' AFTER `msg_type`;

-- changeset hsc:20260805-call-record-ai-mark
ALTER TABLE `call_record` ADD COLUMN `is_ai_first` tinyint(1) DEFAULT '0' COMMENT '是否AI先接听 0-否 1-是';
ALTER TABLE `call_record` ADD COLUMN `transfer_time` datetime DEFAULT NULL COMMENT 'AI转人工时间';

-- changeset hsc:20260805-call-record-call-id-unique
-- call_id 加唯一索引：话单合并方案下"按 callId 查 update"并发风险变大，需唯一约束。
-- ⚠️ 若存量有重复 call_id，此 changeset 会失败——执行前先排查/清理：
--    SELECT call_id, COUNT(*) c FROM call_record WHERE del_flag=0 GROUP BY call_id HAVING c>1;
ALTER TABLE `call_record` ADD UNIQUE INDEX `uk_call_id` (`call_id`);

-- ===== call-record-route: 话单展示呼叫路由名(call_record 关联 call_route) =====

-- changeset hsc:20260806-call-record-route-id
ALTER TABLE `call_record` ADD COLUMN `route_id` bigint(20) DEFAULT NULL COMMENT '命中呼叫路由ID(关联 call_route.id)' AFTER `transfer_time`;

-- ===== sip-gateway-dial-prefix: SIP网关出局号码前缀(外呼originate时拼在called前,如"0"走二次拨号;仅外线网关gatewayType=1生效,空=不加) =====

-- changeset hsc:20260810-fs-sip-gateway-add-dial-prefix
ALTER TABLE `fs_sip_gateway` ADD COLUMN `dial_prefix` varchar(16) DEFAULT NULL COMMENT '出局号码前缀(外呼时拼在called前,如0;空=不加)' AFTER `caller_id_in_from`;

-- ===== call-record-transfer-human: 话单加"是否转人工"字段(仅AI智能坐席接听且成功转人工=1,其余=0) =====
-- changeset hsc:20260811-call-record-transfer-human
ALTER TABLE `call_record` ADD COLUMN `transfer_human` tinyint DEFAULT 0 COMMENT '是否转人工 0-否 1-是';

-- ===== skill-member-type: 技能组成员类型(0=坐席 1=座机)，让技能组能同时配坐席和座机 =====
-- changeset hsc:20260811-skill-agent-rel-member-type
ALTER TABLE `call_skill_agent_rel` ADD COLUMN `member_type` tinyint NOT NULL DEFAULT 0 COMMENT '成员类型 0-坐席 1-座机';

-- ===== voice-file-tts-to-engine: 语音文件TTS字段从云厂商编码(1腾讯/2阿里/3讯飞)改为引擎key(aliyun/pro/holartts/local) =====
-- 老云厂商TTS合成链路(hsc-file FileTtsService)已废弃删除，语音合成改复用 aiCallbotConfig 四引擎。
-- 存量 tts(1/2/3) 不做迁移(老功能从未跑通、无有效数据)，改为 varchar 后老值变字符串不匹配新引擎、需重新合成。
-- changeset hsc:20260812-voice-file-tts-to-engine-key
ALTER TABLE `voice_file` MODIFY COLUMN `tts` varchar(32) DEFAULT NULL COMMENT 'TTS引擎 aliyun/pro/holartts/local(type=2生效)';

-- ===== ivr-engine-completion: IVR满意度节点评分落库 =====

-- changeset hsc:20260817-ivr-satisfaction-record
CREATE TABLE `ivr_satisfaction_record` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `instance_id` bigint(20) DEFAULT NULL COMMENT '流程实例ID(关联 flow_instances.id)',
    `call_id` bigint(20) DEFAULT NULL COMMENT '通话ID(关联 call_record.call_id)',
    `flow_id` bigint(20) DEFAULT NULL COMMENT 'IVR流程ID(关联 flow_info.id)',
    `node_id` varchar(64) DEFAULT NULL COMMENT '满意度节点ID',
    `score` tinyint(4) DEFAULT '0' COMMENT '评分 1-5(用户按键),0-未评价',
    `dtmf` varchar(16) DEFAULT NULL COMMENT '用户原始按键',
    `create_by` bigint(20) DEFAULT NULL,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` bigint(20) DEFAULT NULL,
    `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    `del_flag` tinyint(4) NOT NULL DEFAULT '0' COMMENT '0有效 1删除',
    PRIMARY KEY (`id`),
    KEY `idx_call_id` (`call_id`),
    KEY `idx_flow_id` (`flow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IVR满意度评分记录表';

-- ===== call-route-caller-num: 号码路由加主叫号码匹配(灰度测试/VIP直达/黑名单) =====
-- caller_num 为主叫号正则(与 route_num 同为 regexp 语法)；NULL/空 = 不限主叫(存量行为不变)。
-- 匹配规则：被叫号 regexp route_num 且( caller_num 为空 或 主叫号 regexp caller_num )，命中多条按 level 降序取首条。
-- changeset hsc:20260819-call-route-caller-num
ALTER TABLE `call_route` ADD COLUMN `caller_num` varchar(32) DEFAULT NULL COMMENT '主叫号码正则(空=不限主叫)';

-- ===== unify-voice-engine-config: 语音引擎实例表(一处配置,AI坐席/IVR/语音文件合成引用) =====
-- call_engine(MRCP遗产,FS链路不可用)代码下线、表保留不删；引擎类型为代码内置清单
-- (TTS: aliyun-tts/pro/openai-tts/holartts/volc-tts; ASR: funasr/aliyun-nls/tencent-asr/xfyun-asr)。
-- config 的 key 与 ai-callbot RuntimeConfig 平铺字段名一致(如 aliyun_tts_appkey)，
-- aiCallbotConfig/get 接口按 id 查本表读时展开，ai-callbot Python 侧零改动。
-- changeset hsc:20260821-voice-engine-create
CREATE TABLE `voice_engine` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` varchar(64) NOT NULL COMMENT '引擎实例名称(如 阿里TTS-甜美女声)',
    `engine_type` varchar(32) NOT NULL COMMENT '引擎类型 aliyun-tts/pro/openai-tts/holartts/volc-tts/funasr/aliyun-nls/tencent-asr/xfyun-asr',
    `config` json DEFAULT NULL COMMENT '该类型连接参数JSON(key与RuntimeConfig平铺字段名一致)',
    `remark` varchar(255) DEFAULT NULL COMMENT '备注',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建人',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` bigint(20) DEFAULT NULL COMMENT '更新人',
    `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `del_flag` tinyint(4) NOT NULL DEFAULT '0' COMMENT '删除标识 0 有效 1删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='语音引擎实例表';

-- ===== ivr-flow-version: 流程发布版本化（保存/发布不打断进行中通话） =====
-- 旧机制 edit() 即删 Redis node/edge 流程缓存，通话中下一步读不到节点直接静默卡死（不流转不挂断）。
-- 新机制：flow_data 只作草稿；发布把草稿快照进 published_flow_data 并 version+1；
-- 通话进入时把「快照+版本」钉死进 FlowDataContext，全程按版本读 Redis 流程缓存
-- （key=flowId+version，跨版本天然隔离，旧版本靠 TTL 过期，无主动清理点）。
-- changeset hsc:20260825-flow-info-published
ALTER TABLE `flow_info` ADD COLUMN `published_flow_data` json DEFAULT NULL COMMENT '已发布版本流程数据快照(发布时从flow_data复制,IVR运行时只读本列)';
ALTER TABLE `flow_info` ADD COLUMN `version` int NOT NULL DEFAULT 0 COMMENT '发布版本号(每次发布+1,流程Redis缓存key组成部分)';

-- ===== call-record-bridge-time: 话单加"首次接通时刻"列 =====
-- 详情"接通耗时"(bridge_time-call_start_time)的锚点。原候选口径均不稳：
-- ringing_time=被叫开始振铃(坐席摘机时长没算进)；answer_time 随链路漂移
-- (IVR场景=IVR应答,早应答直转=接通时刻)。首次CHANNEL_BRIDGE(真人/AI接上)是稳定语义。
-- changeset hsc:20260826-call-record-bridge-time
ALTER TABLE `call_record` ADD COLUMN `bridge_time` datetime DEFAULT NULL COMMENT '首次接通时刻(最早一条腿bridge,真人/AI接上)';

-- ===== manual-outbound-closure: 任务联系人补客户溯源列 =====
-- 人群导入联系人时落客户原始ID（此前只拷 phone/name/sex/ext/crowdId，溯源丢失）：
-- ①人工外呼联系人可追溯客户档案；②客户公海私海流转方案 V1"通话即跟进"依赖该列
-- （见 方案/客户公海私海流转方案.md D8 决策）。文件导入路径无客户档案，留空为预期行为。
-- changeset hsc:20260827-assignment-customer-id
ALTER TABLE `call_task_assignment` ADD COLUMN `customer_id` bigint(20) DEFAULT NULL COMMENT '来源客户ID(customer_seas.id)，人群导入时有值';

-- ===== drop-call-task-predictive-columns: 清理预测式外呼遗留列 =====
-- 外呼任务收敛为人工外呼（预览式）唯一形态；后续 AI 外呼专项将用独立新表全新建模，
-- 不复用 call_task（2026-08-27 拍板，推翻 08-17 方案 D1"不拆表"决策）。预测遗留列全部清理：
-- type(0预测/1预览,恒1)、is_priority+receive_limit(优先接听/接待上限,预测坐席接听侧概念)、
-- transfer_type+transfer_value(预测打完转技能组/IVR/机器人)、
-- recall+recall_num+recall_time(自动重拨,零消费死配置)、complete_type(完成即停,零消费死配置)。
-- changeset hsc:20260827-drop-call-task-predictive-columns
ALTER TABLE `call_task`
    DROP COLUMN `type`,
    DROP COLUMN `is_priority`,
    DROP COLUMN `receive_limit`,
    DROP COLUMN `transfer_type`,
    DROP COLUMN `transfer_value`,
    DROP COLUMN `recall`,
    DROP COLUMN `recall_num`,
    DROP COLUMN `recall_time`,
    DROP COLUMN `complete_type`;

-- ===== oem-brand-config: 站点配置 KV 表（OEM 品牌要素） =====
-- 品牌设置页(系统管理→品牌设置)存储：appName/logoFileId/faviconFileId/sloganImageFileId/
-- pageTitle/pageDescription/loginSubtitle。图片键存 sys_file.id（前端拼 /system/v1/file/play/{id}
-- 访问，免鉴权），不存完整 URL——/api 前缀与域名随环境变，存 id 不受影响。
-- 键值对设计：后续新增品牌文案/图片键只插数据行、零 DDL。
-- config_value 空串=回退前端构建默认值（上线空表行为与现状一致）。
-- changeset hsc:20260828-site-config-table
CREATE TABLE `site_config` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `config_key` varchar(64) NOT NULL COMMENT '配置键(品牌要素键名)',
    `config_value` varchar(1024) NOT NULL DEFAULT '' COMMENT '配置值(空串=回退构建默认)',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建人',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint(20) DEFAULT NULL COMMENT '更新人',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站点配置KV表(OEM品牌要素)';

-- ===== oem-brand-config: site_config 补 del_flag 列 =====
-- BaseEntity 含 @TableField("del_flag")，MyBatis-Plus 生成的 SELECT 自动带该列，
-- 首建表漏了导致 "Unknown column 'del_flag'"（对齐工程全表约定：0有效/1删除，
-- 无 @TableLogic，仅作列存在）。原建表 changeset 已在环境执行、按只追加铁律不回改，
-- 本 changeset 对新旧库统一补列（新库=建表后 ALTER，存量库=直接 ALTER）。
-- changeset hsc:20260828-site-config-del-flag
ALTER TABLE `site_config`
    ADD COLUMN `del_flag` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否删除:0有效,1删除';

-- ===== dashboard-analytics-revamp: call_record 统计时间索引 =====
-- 统计看板全部按 call_start_time 范围过滤聚合（overview/trend/region/heatmap 等），
-- 该表此前仅主键、零二级索引，量级增长后全表扫描。为只读统计加索引无写放大顾虑。
-- changeset hsc:20260828-call-record-start-time-index
ALTER TABLE `call_record`
    ADD INDEX `idx_call_start_time` (`call_start_time`);

-- ===== call-record-fix: hangup_cause_code 列类型扩容 =====
-- FsHangupCauseEnum.code 落库值域 19~609（ORIGINATOR_CANCEL=487、BLIND_TRANSFER=600、
-- MEDIA_TIMEOUT=604 等），原建表 tinyint(4) 上限 127，凡挂断原因码 ≥128（487 最常见），
-- FsChannelHangUpCompleteEslEventHandler 收尾 updateById 必报 Data truncation 失败，
-- 且异常中断后续收尾：CallHangupEvent（全文摘要）、TaskDialFinishEvent（任务回写）、
-- removeCallInfo（Redis 清理）全部不执行。smallint(6) 覆盖 19~609 即可，实体侧本就是 Integer。
-- changeset hsc:20260829-call-record-hangup-cause-code-widen
ALTER TABLE `call_record`
    MODIFY COLUMN `hangup_cause_code` smallint(6) DEFAULT NULL COMMENT '挂机原因 ';

-- ===== 显号类型清理：删除 call_display.type 列 =====
-- "号码类型 1-主叫显号 2-被叫显号"分类无消费方：全库无任何查询按 type=1 过滤（主叫显号
-- 是上游为"呼入改主叫显示"（隐私号残影，技能组号码池摆设字段）预留的资源标签，该链路
-- 本身不查 type）；外呼取号两条链路删本列后统一为全表随机（软电话直拨原 setType(2) 与
-- 座机代拨原不过滤的分裂行为随之消失）。存量 type=1 记录删列后自动成为普通外显号。
-- changeset hsc:20260831-drop-call-display-type
ALTER TABLE `call_display`
    DROP COLUMN `type`;

-- ===== skill-group-queue-upgrade: 删除技能组号码池两列 =====
-- 主/被叫号码池为隐私号(CLI masking)方案残影：主叫池仅改坐席腿 originate 的
-- origination_caller_id_number(坐席弹屏来显，真实主叫/话单/回拨不变)；被叫池无 SIP 层
-- 消费，且呼入链路 callee 为空、话单被叫号码用 calleeDisplay 兜底落库而污染话单。
-- 本产品定位(自用呼叫中心，坐席需真实客户号回拨/识别)下永远无消费方，删除。
-- changeset hsc:20260831-drop-call-skill-phone-pool
ALTER TABLE `call_skill`
    DROP COLUMN `caller_phone_pool`,
    DROP COLUMN `callee_phone_pool`;

-- ===== skill-group-queue-upgrade: 技能组新增排队播报音列 =====
-- 排队期间每 30s(固定间隔，不做配置项)插播一次的播报音，存 voice_file.id；NULL=不播报。
-- changeset hsc:20260831-add-call-skill-queue-announce-voice
ALTER TABLE `call_skill`
    ADD COLUMN `queue_announce_voice` bigint(20) DEFAULT NULL COMMENT '排队播报音(voice_file.id,每30s插播,NULL不播)' AFTER `queue_voice`;

-- ===== sys-config-management: 系统参数表 =====
-- 运营参数（振铃超时/排队兜底/漏接重分配上限/token 有效期/旁路 ASR 开关等）此前散落在
-- Java 常量与 application.yml，调整需改代码重新构建部署，私有化交付客户无法自助。
-- 本表只存系统预置参数（DML 预置 10 条），页面仅允许修改 config_value 与 remark，
-- 不提供新增/删除（键名唯一约束兜底防脏数据重复插入）。
-- changeset hsc:20260901-sys-config-table
CREATE TABLE `sys_config` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `config_key` varchar(64) NOT NULL COMMENT '参数键(代码引用,只读)',
    `config_name` varchar(64) NOT NULL COMMENT '参数名(中文展示,只读)',
    `config_value` varchar(256) NOT NULL COMMENT '参数值(页面唯一可编辑列)',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注(参数含义与建议值,可编辑)',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建人',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint(20) DEFAULT NULL COMMENT '更新人',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否删除:0有效,1删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统参数表(运营参数预置,仅值与备注可改)';

-- ===== customer-anchor-fields: 客户锚点字段预制 + 性别退出锚点 =====
-- 客户数据两层：customer_info JSON 存全部动态字段(正本)；phone/name 两列为系统锚点列，
-- 供公海列表/搜索、外呼拨号取号、任务联系人使用，值由 MySQL 生成列自动从 JSON 提取。
-- 本变更：① name 生成列提取 key 由 $.customerName 改为 $.name，与预制的系统字段标识
--   (name/phone，见 dml 20260902-customer-field-preset)对齐，锚点列从此必有值；
-- ② 性别全链路退出锚点(客户可能是企业，性别非外呼刚需)，两表 DROP sex 列。
-- changeset hsc:20260902-customer-seas-anchor-refactor
ALTER TABLE `customer_seas`
    MODIFY COLUMN `name` varchar(50) GENERATED ALWAYS AS (json_unquote(json_extract(`customer_info`,'$.name'))) STORED COMMENT '客户名称(生成列,自动取customer_info系统字段name)',
    DROP COLUMN `sex`;
ALTER TABLE `call_task_assignment` DROP COLUMN `sex`;

-- ===== sip-profile-normalization: fs_acl 列表级用途说明字段 =====
-- 仅 list 级行(list_id=0)写值:管哪个口/作用/改动后果;节点级说明继续用 remark("节点说明")。
-- remark 在 list 级被用作中文显示名("ACL名称"),不混用。
-- ⚠️ 锚点用 domain 不用 remark:remark 列是历史 changeset(changelog-dml.sql:433,DDL 误写进 dml,
-- 已执行不可搬)添加的,而 liquibase 执行顺序 init→ddl→dml——全新建库时 ddl 先于 dml 执行,
-- AFTER remark 会因列不存在而启动失败。
-- changeset hsc:20260902-fs-acl-description
ALTER TABLE `fs_acl` ADD COLUMN `description` varchar(500) DEFAULT NULL COMMENT '用途说明(仅list级:管哪个口/作用/改动后果)' AFTER `domain`;

-- ===== customer-seas: 删除无消费的第三方ID预留字段 =====
-- third_id 原为 API 导入对接第三方系统预留(存外部系统主键做同步去重/关联)，
-- 但 API 导入链路未实现、无任何业务逻辑消费(不参与去重/查询/回写)，前后端整体退出。
-- changeset hsc:20260902-customer-seas-drop-third-id
ALTER TABLE `customer_seas` DROP COLUMN `third_id`;

-- ===== customer-pool-outbound-feedback: 任务联系人拨打结果反馈列 =====
-- call_status(0/1)保留"是否拨打过"快语义(进度统计兼容)；call_result 为七类结果语义列
-- (1接通/2未接听/3占线/4拒接/5空号或停机/6关机或无法接通/7呼叫失败，NULL=未拨打或历史未知)，
-- 挂断回写时与 last_dial_time/last_talk_duration 一并更新为"最近一次"语义。
-- idx_task_phone 用普通联合索引非唯一：软删模式下唯一索引会拦"删后重导"(软删行占坑)，
-- 且 liquibase 执行顺序 ddl 全文件先于 dml，存量清理无法先于建索引跑；任务内防重由导入预检应用层保证。
-- changeset hsc:20260903-assignment-result-columns
ALTER TABLE `call_task_assignment`
    ADD COLUMN `call_result` tinyint(4) DEFAULT NULL COMMENT '最近拨打结果 1接通 2未接听 3占线 4拒接 5空号或停机 6关机或无法接通 7呼叫失败(NULL=未拨打或历史未知)' AFTER `call_status`,
    ADD COLUMN `last_dial_time` datetime DEFAULT NULL COMMENT '最后拨打完成时间(挂断回写)' AFTER `call_result`,
    ADD COLUMN `last_talk_duration` int(11) DEFAULT NULL COMMENT '最后通话时长(秒,未接通为0)' AFTER `last_dial_time`,
    ADD INDEX `idx_assignment_customer` (`customer_id`),
    ADD INDEX `idx_task_phone` (`task_id`, `phone`);

-- ===== customer-pool-outbound-feedback: 外呼拨打历史表 =====
-- 一行=一次真实发生并挂断的外呼呼叫(任务拨打/私海拨打)；点拨未呼出无挂断事件不落行。
-- task_id/assignment_id 可空：私海维度拨打(我的客户页签发起)仅带 customer_id。
-- call_record_id/call_id 关联话单(话单表不存业务标识，关联方向由本表持有，见设计 D1)。
-- disposition/disposition_remark：坐席话后小结(选项字典走 sys_config 参数)。
-- changeset hsc:20260903-dial-log-table
CREATE TABLE `call_task_dial_log` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `task_id` bigint(20) DEFAULT NULL COMMENT '任务ID(call_task.id,私海拨打为空)',
    `assignment_id` bigint(20) DEFAULT NULL COMMENT '任务联系人ID(call_task_assignment.id,私海拨打为空)',
    `agent_id` bigint(20) DEFAULT NULL COMMENT '拨打坐席(sip_agent.id)',
    `customer_id` bigint(20) DEFAULT NULL COMMENT '客户ID(customer_seas.id),无档案为空',
    `phone` varchar(20) NOT NULL DEFAULT '' COMMENT '被叫号码快照',
    `call_record_id` bigint(20) DEFAULT NULL COMMENT '话单ID(call_record.id)',
    `call_id` varchar(64) DEFAULT NULL COMMENT '雪花callId(冗余,直查话单用)',
    `call_result` tinyint(4) NOT NULL COMMENT '拨打结果 1接通 2未接听 3占线 4拒接 5空号或停机 6关机或无法接通 7呼叫失败',
    `hangup_cause_code` smallint(6) DEFAULT NULL COMMENT 'FS挂机原因码(冗余,FsHangupCauseEnum)',
    `hangup_cause` varchar(64) DEFAULT NULL COMMENT 'FS挂机原因原始串(排障用)',
    `disposition` varchar(64) DEFAULT NULL COMMENT '坐席话后结果(sys_config参数字典值,挂断后坐席补记)',
    `disposition_remark` varchar(512) DEFAULT NULL COMMENT '话后备注',
    `dial_time` datetime NOT NULL COMMENT '呼叫发起时间(话单callStartTime)',
    `talk_duration` int(11) NOT NULL DEFAULT 0 COMMENT '通话时长(秒,未接通为0)',
    `create_by` bigint(20) DEFAULT NULL COMMENT '创建人',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint(20) DEFAULT NULL COMMENT '更新人',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `del_flag` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否删除:0有效,1删除',
    PRIMARY KEY (`id`),
    KEY `idx_dial_log_assignment` (`assignment_id`),
    KEY `idx_dial_log_customer` (`customer_id`),
    KEY `idx_dial_log_call_id` (`call_id`),
    KEY `idx_dial_log_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外呼拨打历史表(一行=一次真实呼叫的挂断快照)';

-- ===== customer-pool-outbound-feedback: 客户归属(私海)与外呼痕迹列 =====
-- 私海=同一张表的两个视图(owner_id 空=公海/非空=归属坐席)，不拆表不加池类型字段(模板/字段/人群机制全复用)。
-- 归属为管理员分配制(无坐席自助认领)：分配=条件UPDATE乐观锁，收回清 owner 当晚人群重算后重新可圈选。
-- last_follow_time/release_time/no_recycle 不加：自动回收等电销管理件本期不做，无消费不加列。
-- changeset hsc:20260903-customer-seas-owner-columns
ALTER TABLE `customer_seas`
    ADD COLUMN `owner_id` bigint(20) DEFAULT NULL COMMENT '归属坐席(sip_agent.id)，NULL=公海' AFTER `phone`,
    ADD COLUMN `owner_time` datetime DEFAULT NULL COMMENT '最近分配时间' AFTER `owner_id`,
    ADD COLUMN `last_dial_time` datetime DEFAULT NULL COMMENT '最近被外呼时间(挂断回写)' AFTER `owner_time`,
    ADD COLUMN `last_dial_result` tinyint(4) DEFAULT NULL COMMENT '最近外呼结果 1~7(挂断回写,NULL=未外呼)' AFTER `last_dial_time`,
    ADD COLUMN `dial_count` int(11) NOT NULL DEFAULT 0 COMMENT '累计被外呼次数' AFTER `last_dial_result`,
    ADD INDEX `idx_seas_owner` (`owner_id`);

-- ===== customer-pool-outbound-feedback: 客户流转日志表 =====
-- 金蝶订单日志式：记录归属动作低频事件(导入/分配/收回)；拨打事实复用 call_task_dial_log，
-- 客户详情时间线按 customer_id UNION 两表时间倒序混排展示。只增不改(审计)。
-- changeset hsc:20260903-customer-pool-log-table
CREATE TABLE `customer_pool_log` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `customer_id` bigint(20) NOT NULL COMMENT '客户ID(customer_seas.id)',
    `action` tinyint(4) NOT NULL COMMENT '动作 1-导入入库 2-分配 3-收回',
    `from_agent` bigint(20) DEFAULT NULL COMMENT '原归属坐席(分配时空)',
    `to_agent` bigint(20) DEFAULT NULL COMMENT '新归属坐席(收回时空)',
    `operator` bigint(20) DEFAULT NULL COMMENT '操作管理员(user_id,导入事件为空)',
    `reason` varchar(255) DEFAULT NULL COMMENT '原因(收回时填写)',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_pool_log_customer` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户流转日志表(归属动作,只增不改)';

-- ===== customer-pool-outbound-feedback: sys_config 参数值列扩容 =====
-- config_value 原 varchar(256) 装不下话后结果选项 JSON（约 300 字符，运营自定义更长），
-- 参数值本就是运营可编辑内容，扩到 2000 治本（先于 dml 种子执行，失败种子重启重跑即成功）。
-- changeset hsc:20260903-sys-config-value-length
ALTER TABLE `sys_config` MODIFY COLUMN `config_value` varchar(2000) NOT NULL COMMENT '参数值(页面唯一可编辑列)';