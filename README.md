# LD-Attribute

> 综合属性 / 卡片 / 符文 / 宠物系统插件，适用于 Minecraft 1.12.2 (Paper/Spigot)

![Minecraft](https://img.shields.io/badge/Minecraft-1.12.2-green)
![Paper](https://img.shields.io/badge/Paper-1562-blue)
![Version](https://img.shields.io/badge/version-1.0.0-orange)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

---

## ✨ 特性

- 🎴 **卡片系统** — 33 种卡片，5 个稀有度（T1~T5），支持升级 / 升星 / 分解 / 合成 / 绑定
- 💠 **符文系统** — 38 个符文，5 种孔位，打孔 / 镶嵌 / 取出 / 升级 / 合成 / 回收 / 锁定
- ⚔️ **属性系统** — 62 种属性（攻击 / 防御 / 法术 / 元素 / 状态）
- 🐾 **宠物系统** — 11 只宠物，进化链 + 多页 + 出战 + 喂食
- 🔮 **法术系统** — 60 个法术，可升级，绑定法术书
- 🎯 **共鸣 / 羁绊 / 套装** — 31 + 36 + 套装加成
- 🌟 **幸运掉落** — 基础 100% 起，可提升 MM 掉落次数
- 📊 **属性面板** — 分页 + 来源追踪 + 实时统计
- 🔄 **热重载** — /ldc reload 无需重启，自动重算在线玩家数据
- 💾 **自动备份** — 每小时备份玩家数据，保留 24 份
- 🌐 **多语言** — 繁中 / 简中 / 英文
- 🔌 **兼容** — Vault / PlaceholderAPI / MythicMobs 4.4.0

---

## 📦 安装

1. 下载 `LD-Attribute-1.0.0.jar`
2. 放入服务器 `plugins/` 目录
3. 重启服务器（首次会自动解压默认配置）
4. 编辑配置后执行 `/ldc reload`

### 依赖

| 插件 | 必需 | 说明 |
|---|---|---|
| **Vault** | 可选 | 经济功能 |
| **PlaceholderAPI** | 可选 | 占位符 |
| **MythicMobs 4.4.0** | 可选 | 掉落系统 |

---

## 🚀 快速开始

### 玩家

```
/ldc                    打开卡片背包
/ldc collection         卡片图鉴
/ldc stats              属性面板
/ldc rune collection     符文图鉴
/ldc rune recycle        符文回收
/ldc pet                宠物背包
```

### 管理员

```
/ldc give <卡片ID> [玩家]         给卡片
/ldc rune give <符文ID> [玩家]    给符文
/ldc pet give <宠物ID> [玩家]     给宠物
/ldc reload                       热重载
/ldc backup                       备份数据
/ldc debug                        诊断
```

完整指令见 **[DOCS.md](DOCS.md)**

---

## 📸 界面预览

| 卡片详情 | 符文界面 | 属性面板 |
|---|---|---|
| 待截图 | 待截图 | 待截图 |

> 截图放到 `docs/screenshots/` 目录，替换上面的占位

---

## 🔧 配置文件

配置目录：`plugins/LD-Attribute/`

| 文件 | 说明 |
|---|---|
| `config.yml` | 属性优先级 / 备份 |
| `item.yml` | 卡片定义 |
| `cardlevel.yml` | 卡片升级 |
| `rune.yml` | 符文 / 孔位 / 合成 / 升级 |
| `pet.yml` | 宠物定义 |
| `spells.yml` | 法术 |
| `mm.yml` | MythicMobs 掉落 |
| `ui.yml` | 界面槽位 |
| `recipe.yml` | 合成配方 |
| `lang/*.yml` | 多语言别名 |

全部配置说明见 **[DOCS.md](DOCS.md)**

---

## 🔌 PlaceholderAPI

```
%ldattr_攻击力%                    玩家当前属性
%ldattr_top_攻击力_1%             排行榜第 1 名
%ldattr_top_攻击力_1_value%       第 1 名的值
%ldattr_mana%                     当前法力
%ldattr_mana_bar%                 法力条
```

---

## 🏗️ 从源码构建

```bash
git clone <repo>
cd LD-Attribute
mvn clean package -DskipTests
# 产物在 target/LD-Attribute-1.0.0.jar
```

**环境要求**：JDK 8+，Maven 3.6+

---

## 📁 项目结构

```
LD-Attribute/
├── pom.xml
├── README.md
├── DOCS.md
├── src/main/
│   ├── java/com/longdrange/ldattribute/
│   │   ├── LDAttribute.java            主类
│   │   ├── api/                        API 接口
│   │   ├── card/                       卡片系统
│   │   ├── combat/                     战斗系统
│   │   ├── command/                    指令
│   │   ├── compat/                     MythicMobs 兼容
│   │   ├── data/attribute/             属性系统
│   │   ├── listener/                   事件监听
│   │   ├── pet/                        宠物系统
│   │   ├── rune/                       符文系统
│   │   ├── spell/                      法术系统
│   │   └── util/                       工具类
│   └── resources/
│       ├── *.yml                       配置
│       ├── lang/                       多语言
│       └── plugin.yml
└── target/
```

---

## 📝 更新日志

### v1.0.0 (2026-09-24)

- 首次发布
- 33 种卡片 + 62 种属性 + 38 个符文 + 11 只宠物 + 60 个法术
- 符文系统（打孔 / 镶嵌 / 升级 / 合成 / 回收 / 锁定 / 图鉴）
- 幸运掉落机制
- 属性面板（分页 + 来源追踪）
- 热重载 + 自动备份 + PAPI 排行榜

---

## 📄 许可

MIT License

---

## 🙏 鸣谢

- Paper / Spigot 服务端
- Vault / PlaceholderAPI / MythicMobs
- 所有测试者
