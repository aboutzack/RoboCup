
以下文件若无特别说明根目录皆为 rcrs-server/scripts/remote-control
# 自动跑全图时
## 本地配置
```
echo "118.31.126.8	c11" >> /etc/hosts
echo "47.114.161.17	c12" >> /etc/hosts
echo "47.114.172.127 c13" >> /etc/hosts
echo "47.114.113.99	c14" >> /etc/hosts
```

## config.sh
LOCAL_USER为远程用户名，REMOTE_USER 为本地用户名
将文件中的`yyx`替换为本地用户名即可

## autoRun.sh
