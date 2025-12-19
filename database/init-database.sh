#!/bin/bash

echo "===================================="
echo "数据库初始化脚本"
echo "===================================="
echo

read -p "请输入MySQL用户名 (默认: root): " DB_USER
DB_USER=${DB_USER:-root}

read -sp "请输入MySQL密码: " DB_PASSWORD
echo

if [ -z "$DB_PASSWORD" ]; then
    echo "错误：密码不能为空"
    exit 1
fi

read -p "请输入数据库名 (默认: beverage_platform): " DB_NAME
DB_NAME=${DB_NAME:-beverage_platform}

echo
echo "正在执行数据库初始化..."
echo

# 创建数据库
echo "[1/3] 创建数据库 $DB_NAME..."
mysql -u"$DB_USER" -p"$DB_PASSWORD" -e "CREATE DATABASE IF NOT EXISTS $DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
if [ $? -ne 0 ]; then
    echo "错误：创建数据库失败"
    exit 1
fi

# 执行表结构脚本
echo "[2/3] 执行表结构脚本..."
mysql -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < database/schema.sql
if [ $? -ne 0 ]; then
    echo "错误：执行表结构脚本失败"
    exit 1
fi

# 询问是否导入测试数据
read -p "是否导入测试数据? (y/n, 默认: n): " IMPORT_TEST_DATA
if [ "$IMPORT_TEST_DATA" = "y" ] || [ "$IMPORT_TEST_DATA" = "Y" ]; then
    echo "[3/3] 导入测试数据..."
    mysql -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < database/test-data.sql
    if [ $? -ne 0 ]; then
        echo "警告：导入测试数据失败，但数据库结构已创建成功"
    else
        echo "测试数据导入成功！"
    fi
else
    echo "[3/3] 跳过测试数据导入"
fi

echo
echo "===================================="
echo "数据库初始化完成！"
echo "===================================="
echo
echo "数据库名称: $DB_NAME"
echo "用户名: $DB_USER"
echo
echo "请在 backend/src/main/resources/application.properties 中配置数据库连接："
echo "spring.datasource.url=jdbc:mysql://localhost:3306/$DB_NAME"
echo "spring.datasource.username=$DB_USER"
echo "spring.datasource.password=your_password"
echo

