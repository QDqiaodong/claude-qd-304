# 实验室 · 试剂柜与领用

实验室的日常台账：**试剂柜**、**试剂**、**仪器**、**领用登记**。

业务重心在**试剂的进出**：
- 领用与退回都记一条流水，库存由试剂自己扣/加；
- **过期的试剂领不动、也退不回**（有效期比着当天算）；
- 库存不够时会把「只剩几份」直接说出来；
- 试剂柜停用前，要求先把柜里的试剂清空。

## 技术栈

- 后端：Spring Boot 3.3 / Java 17、Spring Data JPA、MySQL 8、Redis 7
- 业务规则**写在实体自己身上**（`Reagent.expired()` / `deduct()` / `refund()` / `assertLendable()`），
  Service 只做取数与落库，没有把判断散在各处
- 前端：Vue 3（Composition API + **两个自定义指令** `v-autofocus` / `v-digits`，
  顶层用 `provide/inject` 提供统一提示）+ Element Plus + Vite
- 一键起：`./start.sh`

## 业务模块

1. **试剂柜**（`cabinet`）—— 编号名称、柜型、可用与停用（停用前要求清空柜内试剂）
2. **试剂**（`reagent`）—— 编号名称规格、存放柜、库存、有效期、可用/已用完/停用
3. **仪器**（`instrument`）—— 编号名称型号、保管人、可用/维修中/停用
4. **领用登记**（`usage_log`）—— 领用与退回两个方向，一次扣加一瓶试剂

## 本地跑起来

| | 地址 |
| --- | --- |
| 前端页面 | http://127.0.0.1:8234/ |
| 后端接口 | http://127.0.0.1:8334/api/reagents |
| MySQL | 127.0.0.1:3534（库 `lab_center`） |
| Redis | 127.0.0.1:6534 |

容器名统一是 `claude-qd-304-{mysql,redis,backend,frontend}`。

```bash
./start.sh              # 起容器
docker compose ps       # 看状态
docker compose down -v  # 停掉并清数据
```
