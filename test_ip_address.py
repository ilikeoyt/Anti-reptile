#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试IP地址获取
"""

import requests
import json

# 测试获取IP地址
def test_ip_address():
    print("测试获取IP地址...")
    
    try:
        # 访问demo系统
        response = requests.get('http://localhost:8080')
        print(f"状态码: {response.status_code}")
        print(f"响应内容: {response.text[:200]}...")
        
        # 查看本地IP
        import socket
        local_ip = socket.gethostbyname(socket.gethostname())
        print(f"本地主机名: {socket.gethostname()}")
        print(f"本地IP: {local_ip}")
        
        # 尝试访问一个返回IP的服务
        try:
            ip_response = requests.get('http://httpbin.org/ip')
            ip_data = ip_response.json()
            print(f"公网IP: {ip_data.get('origin')}")
        except Exception as e:
            print(f"获取公网IP失败: {e}")
            
    except Exception as e:
        print(f"测试失败: {e}")

if __name__ == "__main__":
    test_ip_address()
