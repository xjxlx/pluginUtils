# pluginUtils

git submodule add <url> <path>  其中，url为子模块的路径，path为该子模块存储的目录路径
例子：git submodule add  https://github.com/xjxlx/apphelper.git  apphelper

git diff --cached 查看修改内容可以看到增加了子模块，并且新文件下为子模块的提交hash摘要

git commit 提交即完成子模块的添加

更换子模块路径
1：将新的 URL 复制到本地配置中
2：执行命令
$ git submodule sync --recursive
3：从新 URL 更新子模块
$ git submodule update --init --recursive

修改子模块的地址

1.修改命令
　　git remote set-url origin [NEW_URL]

2.先删后加
　　git remote rm origin
　　git remote add origin [url]

3.直接修改.git/config文件
