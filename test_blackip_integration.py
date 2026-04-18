#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试BotScout IP黑名单集成
"""

import requests
import time
from datetime import datetime

def test_blackip_integration():
    """测试黑名单集成"""
    print("=" * 60)
    print("测试 BotScout IP 黑名单集成")
    print("=" * 60)
    
    # 测试1: 检查黑名单文件是否存在
    print("\n1. 检查黑名单文件:")
    import os
    
    json_file = "src/main/java/cn/ycx/AntiCrawler/blackIP/botscout_blacklist.json"
    txt_file = "src/main/java/cn/ycx/AntiCrawler/blackIP/botscout_blacklist.txt"
    
    if os.path.exists(json_file):
        print(f"✓ JSON 黑名单文件存在: {json_file}")
        with open(json_file, 'r', encoding='utf-8') as f:
            import json
            data = json.load(f)
            print(f"  - IP 数量: {data.get('total_count', 0)}")
            print(f"  - 最后更新: {data.get('update_time', '未知')}")
    else:
        print(f"✗ JSON 黑名单文件不存在: {json_file}")
    
    if os.path.exists(txt_file):
        print(f"✓ TXT 黑名单文件存在: {txt_file}")
        with open(txt_file, 'r', encoding='utf-8') as f:
            lines = [line.strip() for line in f if line.strip()]
            print(f"  - IP 数量: {len(lines)}")
    else:
        print(f"✗ TXT 黑名单文件不存在: {txt_file}")
    
    # 测试2: 运行爬虫
    print("\n2. 运行 BotScout 爬虫:")
    try:
        import subprocess
        
        script_path = "src/main/java/cn/ycx/AntiCrawler/blackIP/botscout_ip_crawler.py"
        if os.path.exists(script_path):
            print(f"运行爬虫脚本: {script_path}")
            result = subprocess.run(
                ["python", script_path, "--run-once"],
                capture_output=True,
                text=True,
                timeout=60
            )
            
            print(f"退出码: {result.returncode}")
            if result.returncode == 0:
                print("✓ 爬虫运行成功")
                print("输出:")
                print(result.stdout)
            else:
                print("✗ 爬虫运行失败")
                print("错误:")
                print(result.stderr)
        else:
            print(f"✗ 爬虫脚本不存在: {script_path}")
    except Exception as e:
        print(f"✗ 运行爬虫失败: {e}")
    
    # 测试3: 检查API访问
    print("\n3. 测试 API 访问:")
    try:
        # 测试正常访问
        response = requests.get("http://localhost:8080/products", timeout=10)
        print(f"访问 /products: {response.status_code}")
        
        # 测试登录
        login_data = {
            "username": "admin",
            "password": "admin123"
        }
        login_response = requests.post("http://localhost:8080/login", data=login_data, timeout=10)
        print(f"登录: {login_response.status_code}")
        
        # 获取Cookie
        cookies = login_response.cookies.get_dict()
        print(f"获取到Cookie: {cookies}")
        
    except Exception as e:
        print(f"✗ API 访问失败: {e}")
    
    print("\n" + "=" * 60)
    print("集成测试完成")
    print("=" * 60)

if __name__ == "__main__":
    test_blackip_integration()
