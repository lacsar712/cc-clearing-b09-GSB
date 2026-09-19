# 多币种轧差清算工作台（Clearing Netting Workbench）

单币种多边轧差清算全栈演示：录入义务 → 执行轧差 → 查看净头寸 → 确认 settle。

## How to Run

```bash
cd projects/01-clearing-netting
docker compose up --build
```

镜像默认走 `docker.m.daocloud.io` 与 Maven/npm 国内源，便于在受限网络下构建。若本机已有同名官方镜像亦可直接使用。

后台运行：

```bash
docker compose up --build -d
```

停止：

```bash
docker compose down
```

## Services

| 服务 | 宿主机地址 |
|------|------------|
| Frontend | http://localhost:3171 |
| Backend API | http://localhost:8171 |
| PostgreSQL | localhost:54371 |

容器内：backend 监听 `8080`，frontend nginx 将 `/api` 反代到 `backend:8080`。

## 测试账号

| 用户名 | 密码 | 权限 |
|--------|------|------|
| operator | op123456 | 可写（轧差、settle、新建会员/义务） |
| viewer | view123456 | 只读 |

## Verification

1. 打开 http://localhost:3171 ，使用 `operator` / `op123456` 登录
2. 首页查看 seed 灌入的待轧差义务摘要与最近批次
3. 「会员」页确认演示会员为 ACTIVE；可新建或启停
4. 「义务」页筛选 OPEN 义务，或新建一笔同币种义务
5. 「轧差执行」选择 settleDate + currency（如 USD），执行轧差
6. 确认净头寸表 ΣnetAmount = 0，批次状态 COMPLETED
7. 进入批次详情，点击 Settle，义务变为 SETTLED
8. 使用 `viewer` 登录，确认只能浏览、无法执行写操作

### 疑似重复检测

1. 在「义务」页对同一付款方/收款方/币种/交割日录入两笔金额完全相同的 OPEN 义务
2. 「重复检测」页出现一组「待处理」疑似重复（可展开查看各笔义务 ID、金额、创建时间）
3. 「轧差执行」选择相同交割日 + 币种：出现醒目警告且无法执行（后端同样拦截，报 `DUPLICATE_SUSPECTS_PENDING`）
4. 在组内任选一笔「标记重复并取消」（原因必填），或对整组「标记已复核」
5. 回到「轧差执行」，同条件轧差可正常跑通
6. 已复核的组若再录入一笔相同义务，会重新变为「待处理」并再次阻止轧差
7. `viewer` 可查看重复组，但无取消/复核按钮（后端同样拒绝，403）

健康检查：

```bash
curl http://localhost:8171/api/health
```

登录：

```bash
curl -X POST http://localhost:8171/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"operator\",\"password\":\"op123456\"}"
```

## 技术栈

- Backend: Java 17、Spring Boot 3、Hexagonal、JPA、PostgreSQL、JWT
- Frontend: Vue 3、Vite、Element Plus、Pinia、Vue Router、nginx
- Infra: Docker Compose（db / backend / seed / frontend）

## 项目结构

```
01-clearing-netting/
├── PRD.md
├── README.md
├── docker-compose.yml
├── backend/
├── frontend/
└── seed/
```
