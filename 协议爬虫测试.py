#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试/products路由的Python脚本
"""

import requests
import json

def test_products_route():
    """测试/products路由"""
    # 目标URL
    url = "http://localhost:8080/product/1"
    
    print("测试 /products 路由...")
    print(f"请求URL: {url}")
    print("=" * 50)
    
    try:
        # 设置Cookie
        cookies = {
            'JSESSIONID': 'A69A9EE58E6A98F09DB04C7CFE304571'
        }
        
        # 设置请求头
        headers = {
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7',
            'Accept-Encoding': 'gzip, deflate, br, zstd',
            'Accept-Language': 'zh-CN,zh;q=0.9',
            'Cache-Control': 'no-cache',
            'Connection': 'keep-alive',
            'Host': 'localhost:8080',
            'Pragma': 'no-cache',
            'Referer': 'http://localhost:8080/login',
            'Sec-Fetch-Dest': 'document',
            'Sec-Fetch-Mode': 'navigate',
            'Sec-Fetch-Site': 'same-origin',
            'Sec-Fetch-User': '?1',
            'Upgrade-Insecure-Requests': '1',
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36',
            'sec-ch-ua': '"Not:A-Brand";v="99", "Google Chrome";v="145", "Chromium";v="145"',
            'sec-ch-ua-mobile': '?0',
            'sec-ch-ua-platform': '"Windows"'
        }
        
        # 发送GET请求
        response = requests.get(url, cookies=cookies, headers=headers, timeout=10)
        
        # 打印响应状态码
        print(f"响应状态码: {response.status_code}")
        print("-" * 50)
        
        # 打印响应头
        print("响应头:")
        for key, value in response.headers.items():
            print(f"  {key}: {value}")
        print("-" * 50)
        
        # 打印响应内容
        print("响应内容:")
        try:
            # 尝试解析JSON响应
            json_data = response.json()
            print(json.dumps(json_data, ensure_ascii=False, indent=2))
        except json.JSONDecodeError:
            # 如果不是JSON，直接打印文本
            print(response.text)
        
        print("=" * 50)
        print("测试完成!")
        
    except requests.exceptions.RequestException as e:
        print(f"请求失败: {e}")
        print("可能的原因:")
        print("1. 服务器未运行")
        print("2. 网络连接问题")
        print("3. 路由不存在")

if __name__ == "__main__":
    # 检查是否安装了requests库
    try:
        import requests
    except ImportError:
        print("错误: 未安装requests库")
        print("请运行: pip install requests")
        exit(1)
    
    test_products_route()
