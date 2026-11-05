# 部署机 SSH 免密配置（10.80.62.200）

> 目标：本地执行 `06-deploy-frontend.sh` / `07-build-local-backend.sh` 时**全程不再输入密码**。
> 方式：SSH 密钥认证（而非把密码写进脚本——密码明文落盘风险高，且 scp/ssh/sudo 三处都要单独处理）。

当前状态：**已配置完成并验证**（2026-11-05），下面为原理与重装/换机时的复现步骤。

---

## 一、涉及的三类认证

| 场景 | 由谁控制 | 现状 |
| --- | --- | --- |
| ssh / scp 登录 | `~/.ssh/id_ed25519` ↔ 服务器 `~/.ssh/authorized_keys` | 密钥免密，已验证 |
| 服务启停（systemctl） | `/etc/sudoers.d/cacch`（限定命令 NOPASSWD） | 免密，已验证 |
| 前端 SELinux 上下文（restorecon） | 同上 | 免密，已验证 |

> 注意：sudoers 只授权了**具体命令**（`/usr/bin/systemctl start|stop|restart|status|is-active cacch-tomcat`、`/usr/sbin/restorecon`）。
> 因此调用时必须写 `sudo -n <命令>`，**不能用 `sudo sh -c '...'` 包一层**——包一层后匹配不到授权规则，会退回要密码。

---

## 二、首次配置（新电脑 / 重装系统后）

### 1. 本地生成密钥（已有可跳过）

```bash
ls ~/.ssh/id_ed25519 || ssh-keygen -t ed25519 -C "your@email" -N ""
```

`-N ""` 表示不设密钥口令，否则每次使用仍会提示输入 passphrase。

### 2. 把公钥装到服务器（只需一次密码）

```bash
ssh-copy-id -i ~/.ssh/id_ed25519.pub felix@10.80.62.200
# 或手动：
cat ~/.ssh/id_ed25519.pub | ssh felix@10.80.62.200 \
  'mkdir -p ~/.ssh && chmod 700 ~/.ssh && cat >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys'
```

### 3. 本地 `~/.ssh/config`

```
Host 10.80.62.200 cacch cacch-server
    HostName 10.80.62.200
    User felix
    IdentityFile ~/.ssh/id_ed25519
    IdentitiesOnly yes
    StrictHostKeyChecking accept-new
    ServerAliveInterval 30
    ServerAliveCountMax 3
```

> `StrictHostKeyChecking accept-new`：首次连接自动接受指纹，避免脚本卡在 `yes/no` 提问。
> 不要启用 `ControlMaster`：Windows(Git Bash) 下连接复用会报
> `mux_client_request_session: read from master failed`。

### 4. 验证（三条都通过才算配好）

```bash
ssh -o BatchMode=yes 10.80.62.200 'echo SSH-KEY-OK'      # 不询问密码，输出 SSH-KEY-OK
scp /tmp/t.txt 10.80.62.200:/tmp/t.txt && echo SCP-OK    # 不询问密码
ssh 10.80.62.200 'sudo -n systemctl is-active cacch-tomcat'   # 输出 active，且不问密码
```

`-o BatchMode=yes` 的作用是**强制不弹密码提示**，这样一旦密钥有问题会立刻报错而不是卡住等待输入。

---

## 三、常见问题

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| 仍提示输入密码 | 公钥没进 `authorized_keys`；或文件权限不对 | 服务器上 `ls -ld ~/.ssh` 应为 `700`、`authorized_keys` 为 `600`，属主 felix |
| 提示 `Permission denied (publickey...)` | 本地私钥权限过宽 / 用了别的密钥 | `IdentitiesOnly yes` + 显式 `IdentityFile`；或 `ssh -v` 看实际尝试的密钥 |
| 提示 `Enter passphrase for key` | 生成密钥时设了口令 | `ssh-keygen -p -N "" -f ~/.ssh/id_ed25519` 清空口令 |
| 脚本卡住不返回 | 缺 `StrictHostKeyChecking accept-new`，等 `yes/no` | 按上文补 `~/.ssh/config` |
| `sudo: a password is required` | 命令不在 sudoers 白名单，或用了 `sh -c` 包装 | 用 `sudo -n <具体命令>`；新增命令需改 `deploy/sudoers-cacch` 并 `visudo -c` 校验 |

---

## 四、安全说明

- 私钥 `~/.ssh/id_ed25519` **不要**提交到 Git，也不要拷给别人；丢失后从服务器 `authorized_keys` 删除对应行即可吊销。
- 服务器密码 `felix` 保持不变，作为密钥失效时的兜底手段（例如本地换机器、密钥丢失）。
- 如需多台机器部署，各自生成密钥、各自追加公钥到 `authorized_keys`（每行一把），互不影响。
