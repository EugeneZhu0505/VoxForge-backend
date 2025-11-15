# 朱玉辰

><span alt="icon">&#xe8b4; </span> 2000.05.05 &emsp;&emsp; <span alt="icon">&#xe60f;</span> 18595336468 &emsp;&emsp; <span alt="icon">&#xe7ca;</span> eugenezhu05&#64;163.com &emsp;&emsp;

<img alt="avatar" src="https://eugenezhu-java.oss-cn-beijing.aliyuncs.com/expansion_20250903154848926.jpg">

## &#xe80c; 教育经历

<div alt="entry-title">
    <h3>浙江理工大学 - 本科 - 智能科学与技术  (2018.09 - 2022.06)</h3> 
</div>

<div alt="entry-title">
    <h3>华中农业大学 - 硕士 - 计算机应用技术  (2023.09 - 至今)</h3> 
</div>

## &#xe618; 实习经历

<div alt="entry-title">
    <h3>宁波工业互联网研究院 - JAVA 后端开发实习生</h3> 
    <p>2025.06 - 2025.09</p>
</div>

<h4>项目描述</h4> 

基于开源视频监控平台二次开发，打通算法端—后端—前端/第三方平台链路；新增设备数据推送、报警数据管理等业务；优化部分核心功能。

<h4>工作内容</h4>

- **事件驱动的设备目录一致性**：基于 <mark>Canal</mark> 订阅 MySQL binlog，仅捕获设备表 INSERT、DELETE 事件并通过 <mark>kafka</mark> 分发；主控消费后使用 <mark>CompletableFuture</mark> 并行采集各服务器设备数据，设置整体超时与失败兜底；汇总结果通过 HTTP 推送第三方，支持可配置重试与请求超时。
  
- **高并发 SIP 消息处理与背压设计**：采用 <mark>ConcurrentLinkedQueue</mark> 无锁队列 + <mark>ScheduledExecutorService</mark> 差异化批处理（目录 400ms、告警 200ms 等）；手动设置队列上限，满载丢弃并打点，INVITE 等资源不足场景明确返回 486 BUSY_HERE；使用 <mark>AtomicLong</mark> 采样到达/处理/丢弃量，线程池使用 <mark>CallerRunsPolicy</mark> 实现自然回压。
- **AI 告警分析与算法批量配置**：提供按设备通道/告警类型/区域的统计与小时/天/周趋势聚合接口；设计基于 <mark>EasyExcel</mark> 的批量导入，表头驱动算法类型解析，行级校验与异常跳过；结合设备映射自动生成接入地址以降低人工处理复杂度。

## &#xe635; 项目经历

<div alt="entry-title">
    <h3>VoxForge 桌面端智能语音助手 - 后端开发</h3> 
    <p>2025.09 - 至今</p>
</div>

<h4>项目描述</h4> 

基于 <mark>Spring Boot</mark> 和 <mark>Spring WebFlux</mark> 构建的非阻塞响应式后端架构，集成七牛云大模型 API，支持 RAG 知识增强、流式响应、语音交互、任务链管理等功能，实现了高并发、低延迟的语音助手服务。

<h4>工作内容</h4>

- **非阻塞异步通信与流量控制**：采用 <mark>Spring WebFlux</mark> 构建非阻塞架构，使用 <mark>WebClient</mark> 与 七牛云 API 服务进行异步通信；基于 <mark>Reactor</mark> 背压机制与 <mark>Semaphore</mark>/令牌桶实现限流与并发控制，支持内存/线程/拒绝率驱动的动态调节；集成 <mark>Resilience4j</mark> 实现熔断、隔离与重试机制。

- **RAG + 命令缓存（Redis）**：构建基于 <mark>Embedding</mark> 的命令模板检索，将模板与用户环境信息（OS/CLI 历史/偏好）拼接，生成上下文化的可执行指令；将常用命令、欢迎语与上下文提示缓存到 <mark>Redis</mark>，实现应用启动即推送欢迎词（含常用命令）。设计 TTL/分层缓存策略（常用命令永久、上下文30分钟、欢迎语2小时），实现缓存预热与定时刷新；命中率达85%，平均响应时间降至20ms内。

- **流式对话与状态管理**：基于 <mark>Reactor Flux</mark> 实现流式对话，使用 <mark>Server-Sent Events</mark> 推送；维护最近10轮上下文并在多用户场景下通过 <mark>ConcurrentMap</mark> + <mark>Redis</mark> 管理会话状态。设计任务链状态机（PENDING→RUNNING→DONE/FAILED），支持状态切换、超时与重试；采用 <mark>Saga</mark> 补偿事务实现跨步骤回滚，保证幂等与一致性；对话与任务事件持久化，支持历史查询与导出。


## &#xecfa; 专业技能

- 熟练掌握数据结构、操作系统核心知识，了解计算机网络协议与计算机组成原理
- 熟练掌握 Java 语法、集合、反射等基础知识，具备扎实的面向对象编程思想；深入理解 Java 并发编程，掌握 JUC 常用工具类、线程池及 Java 内存模型
- 熟练使用 MySQL、Redis，熟悉 MySQL 索引、事务、存储引擎及锁机制，了解 MySQL 底层数据结构
- 熟练掌握 Spring、SpringBoot、MyBatis-Plus 等主流框架，对框架核心原理有一定研究与实践
