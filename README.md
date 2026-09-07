# holar-hsc

「Holar Smart Call」智能呼叫中心的业务后端：来电转人工坐席（软电话 / 座机 / 技能组）、AI 智能坐席编排、IVR 流程、外呼任务、话单与统计。

## 开源与来源声明（必读）

- 本项目基于开源项目 **[openCallHub](https://github.com/sxwdmjy/openCallHub)**（GPL-3.0）二次开发修改而来，**整体按 [GPL-3.0](./LICENSE) 协议开源发布**。
- 上游项目版权归其作者所有，在此致谢。
- 本项目对上游代码的修改，均在对应**文件头部以注释标注**（含修改者与修改日期）；未带标注的文件为新增内容，同样以 GPL-3.0 发布。
- `doc/kamailio.cfg` 等示例配置源自 [Kamailio](https://www.kamailio.org/)（GPL-2.0+）默认配置的修改版，仅作部署参考。

## 技术栈

- Java 17 + Spring Boot 3.5.3 + Spring Security/JWT + MyBatis-Plus + PageHelper
- MySQL（Liquibase 启动自动建库变更）+ Redis
- Quartz（集群模式任务调度）
- FreeSWITCH（ESL 长连接控制 + xml_curl 动态配置）、Kamailio（SIP 代理）

## 模块结构

| 模块 | 职责 |
|------|------|
| hsc-api | 唯一业务 Web 服务（REST + WebSocket，端口 4320），装配全部模块 |
| hsc-security | 认证 / 鉴权 / 全局异常处理 |
| hsc-system | 系统管理与业务 Service / Entity / Mapper |
| hsc-common | 统一返回、基类、Redis、JWT、自定义注解、枚举、事件 |
| hsc-esl | FreeSWITCH ESL 客户端与统一呼叫路由（8 类路由，含 AI 坐席路由） |
| hsc-ivr | IVR 流程引擎（Spring StateMachine） |
| hsc-call-task | 外呼任务调度（Quartz）+ 客户公海 / 人群 |
| hsc-ai | LLM 网关（通话摘要 / 坐席推荐话术） |
| hsc-websocket | 坐席 WebSocket |
| hsc-file | 文件上传 / 对象存储 / TTS 语音合成 |
| hsc-file-client | 语音文件同步 Netty 客户端（当前部署改用共享挂载，保留备用） |

## 快速开始

环境要求：JDK 17+、Maven 3.6+、MySQL 8、Redis

```bash
# 全量构建（模块间走 Maven 依赖，内部模块变更后需全量 install）
mvn clean install -DskipTests

# 运行主服务（默认端口 4320；数据源 / Redis 地址在 hsc-api/src/main/resources/application.yml 配置）
java -jar hsc-api/target/hsc-api-0.0.1.jar
```

首次启动 Liquibase 自动建表（库名 `holarSmartCall`）。完整运行还需 FreeSWITCH 与（可选）AI 坐席服务，参见 `doc/` 下文档。

## 许可证

本项目以 [GPL-3.0](./LICENSE) 协议发布。
