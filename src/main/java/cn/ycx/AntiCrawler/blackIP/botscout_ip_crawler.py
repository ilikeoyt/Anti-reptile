#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
BotScout IP 黑名单爬虫
定时爬取 https://botscout.com/last_caught_cache.htm 上的IP信息
"""

import requests
import re
import json
import time
from datetime import datetime
from pathlib import Path
import schedule


class BotScoutCrawler:
    """BotScout IP 黑名单爬虫"""
    
    def __init__(self, output_file=None):
        self.url = "https://botscout.com/last_caught_cache.htm"
        # 默认为脚本所在目录的botscout_blacklist.json
        if output_file:
            self.output_file = output_file
        else:
            # 获取脚本所在目录的绝对路径
            script_dir = Path(__file__).parent.absolute()
            self.output_file = str(script_dir / "botscout_blacklist.json")
        self.headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
        }
        
    def fetch_data(self):
        """获取网页数据"""
        try:
            print(f"[{datetime.now()}] 正在获取数据...")
            response = requests.get(self.url, headers=self.headers, timeout=30)
            response.raise_for_status()
            print(f"[{datetime.now()}] 数据获取成功")
            return response.text
        except Exception as e:
            print(f"[{datetime.now()}] 获取数据失败: {e}")
            return None
    
    def parse_ips(self, html_content):
        """解析IP地址"""
        if not html_content:
            return []
        
        # 使用正则表达式匹配IP地址
        # 匹配 IPv4 地址
        ipv4_pattern = r'\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\b'
        
        # 匹配 IPv6 地址（简化版）
        ipv6_pattern = r'\b(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}\b|\b(?:[0-9a-fA-F]{1,4}:){1,7}:\b|\b(?:[0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}\b'
        
        ipv4_addresses = set(re.findall(ipv4_pattern, html_content))
        ipv6_addresses = set(re.findall(ipv6_pattern, html_content))
        
        all_ips = list(ipv4_addresses.union(ipv6_addresses))
        
        # 过滤掉私有IP和保留IP
        filtered_ips = self._filter_private_ips(all_ips)
        
        print(f"[{datetime.now()}] 解析到 {len(filtered_ips)} 个有效IP地址")
        return filtered_ips
    
    def _filter_private_ips(self, ips):
        """过滤私有IP和保留IP"""
        filtered = []
        for ip in ips:
            if self._is_valid_public_ip(ip):
                filtered.append(ip)
        return filtered
    
    def _is_valid_public_ip(self, ip):
        """检查是否为有效的公网IP"""
        # 检查是否是IPv4
        if '.' in ip:
            parts = ip.split('.')
            if len(parts) != 4:
                return False
            
            try:
                first_octet = int(parts[0])
                second_octet = int(parts[1])
                
                # 过滤私有IP段
                # 10.0.0.0/8
                if first_octet == 10:
                    return False
                # 172.16.0.0/12
                if first_octet == 172 and 16 <= second_octet <= 31:
                    return False
                # 192.168.0.0/16
                if first_octet == 192 and second_octet == 168:
                    return False
                # 127.0.0.0/8 (回环地址)
                if first_octet == 127:
                    return False
                # 169.254.0.0/16 (链路本地地址)
                if first_octet == 169 and second_octet == 254:
                    return False
                # 224.0.0.0/4 (多播地址)
                if 224 <= first_octet <= 239:
                    return False
                # 240.0.0.0/4 (保留地址)
                if 240 <= first_octet <= 255:
                    return False
                
                return True
            except ValueError:
                return False
        
        # IPv6 暂时全部保留
        return True
    
    def save_to_json(self, ips):
        """保存IP列表到JSON文件"""
        if not ips:
            print(f"[{datetime.now()}] 没有IP需要保存")
            return
        
        data = {
            'update_time': datetime.now().isoformat(),
            'source': self.url,
            'total_count': len(ips),
            'ips': ips
        }
        
        try:
            # 如果文件存在，先读取旧数据
            existing_ips = set()
            if Path(self.output_file).exists():
                with open(self.output_file, 'r', encoding='utf-8') as f:
                    old_data = json.load(f)
                    existing_ips = set(old_data.get('ips', []))
                    print(f"[{datetime.now()}] 已存在 {len(existing_ips)} 个IP")
            
            # 合并新旧数据
            all_ips = list(existing_ips.union(set(ips)))
            data['total_count'] = len(all_ips)
            data['ips'] = all_ips
            data['new_count'] = len(set(ips) - existing_ips)
            
            # 保存到文件
            with open(self.output_file, 'w', encoding='utf-8') as f:
                json.dump(data, f, indent=2, ensure_ascii=False)
            
            print(f"[{datetime.now()}] 保存成功: {len(all_ips)} 个IP (新增 {data['new_count']} 个)")
            
        except Exception as e:
            print(f"[{datetime.now()}] 保存失败: {e}")
    
    def save_to_txt(self, ips):
        """保存IP列表到纯文本文件"""
        txt_file = self.output_file.replace('.json', '.txt')
        
        try:
            # 读取已存在的IP
            existing_ips = set()
            if Path(txt_file).exists():
                with open(txt_file, 'r', encoding='utf-8') as f:
                    existing_ips = set(line.strip() for line in f if line.strip())
            
            # 合并新旧数据
            all_ips = sorted(list(existing_ips.union(set(ips))))
            
            # 保存到文件
            with open(txt_file, 'w', encoding='utf-8') as f:
                for ip in all_ips:
                    f.write(f"{ip}\n")
            
            print(f"[{datetime.now()}] TXT文件保存成功: {len(all_ips)} 个IP")
            
        except Exception as e:
            print(f"[{datetime.now()}] TXT文件保存失败: {e}")
    
    def run(self):
        """运行爬虫"""
        print(f"[{datetime.now()}] 开始爬取 BotScout IP 黑名单...")
        
        # 获取数据
        html_content = self.fetch_data()
        if not html_content:
            return
        
        # 解析IP
        ips = self.parse_ips(html_content)
        if not ips:
            print(f"[{datetime.now()}] 未解析到IP地址")
            return
        
        # 保存数据
        self.save_to_json(ips)
        self.save_to_txt(ips)
        
        print(f"[{datetime.now()}] 爬取完成")
    
    def get_statistics(self):
        """获取统计信息"""
        try:
            if Path(self.output_file).exists():
                with open(self.output_file, 'r', encoding='utf-8') as f:
                    data = json.load(f)
                    return {
                        'total_ips': data.get('total_count', 0),
                        'last_update': data.get('update_time', '未知'),
                        'source': data.get('source', self.url)
                    }
        except Exception as e:
            print(f"获取统计信息失败: {e}")
        return None


def scheduled_job():
    """定时任务"""
    crawler = BotScoutCrawler()
    crawler.run()


def main():
    """主函数"""
    import argparse
    
    parser = argparse.ArgumentParser(description='BotScout IP 黑名单爬虫')
    parser.add_argument('--run-once', action='store_true', help='只运行一次')
    parser.add_argument('--interval', type=int, default=60, help='定时运行间隔（分钟），默认60分钟')
    parser.add_argument('--output', type=str, default='botscout_blacklist.json', help='输出文件路径')
    parser.add_argument('--stats', action='store_true', help='显示统计信息')
    
    args = parser.parse_args()
    
    crawler = BotScoutCrawler(output_file=args.output)
    
    # 显示统计信息
    if args.stats:
        stats = crawler.get_statistics()
        if stats:
            print("=" * 60)
            print("BotScout IP 黑名单统计信息")
            print("=" * 60)
            print(f"总IP数: {stats['total_ips']}")
            print(f"最后更新: {stats['last_update']}")
            print(f"数据源: {stats['source']}")
            print("=" * 60)
        else:
            print("暂无统计信息，请先运行爬虫")
        return
    
    # 只运行一次
    if args.run_once:
        crawler.run()
        return
    
    # 定时运行
    print(f"[{datetime.now()}] BotScout IP 黑名单爬虫已启动")
    print(f"[{datetime.now()}] 定时运行间隔: {args.interval} 分钟")
    print(f"[{datetime.now()}] 输出文件: {args.output}")
    print(f"[{datetime.now()}] 按 Ctrl+C 停止")
    print("=" * 60)
    
    # 立即运行一次
    crawler.run()
    
    # 设置定时任务
    schedule.every(args.interval).minutes.do(scheduled_job)
    
    try:
        while True:
            schedule.run_pending()
            time.sleep(1)
    except KeyboardInterrupt:
        print(f"\n[{datetime.now()}] 爬虫已停止")


if __name__ == "__main__":
    main()
