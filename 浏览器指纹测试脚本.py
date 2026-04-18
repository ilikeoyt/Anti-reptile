#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
自动化浏览器测试脚本 - 用于自动验证码测试

使用Selenium启动浏览器，自动识别和填写验证码
"""

from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.chrome.service import Service
import time
import base64
import requests
import cv2
import numpy as np
from PIL import Image
import io

class ManualVerificationTester:
    def __init__(self, base_url="http://localhost:8080"):
        self.base_url = base_url
        self.driver = None
    
    def setup_browser(self):
        """设置浏览器"""
        print("正在启动浏览器...")
        
        try:
            # 配置Chrome选项 - 保留自动化特征
            chrome_options = Options()
            # 不使用反检测措施，保留自动化特征
            # 移除所有反检测配置，让自动化特征被检测到
            
            # 添加启动参数，提高启动速度
            chrome_options.add_argument('--no-sandbox')
            chrome_options.add_argument('--disable-dev-shm-usage')
            chrome_options.add_argument('--disable-gpu')
            chrome_options.add_argument('--disable-extensions')
            chrome_options.add_argument('--disable-software-rasterizer')
            chrome_options.add_argument('--disable-web-security')
            chrome_options.add_argument('--disable-features=VizDisplayCompositor')
            
            # 设置页面加载超时
            chrome_options.page_load_timeout = 30  # 30秒
            chrome_options.implicit_wait = 10  # 10秒
            
            print("正在初始化ChromeDriver...")
            
            # 尝试启动浏览器
            # 使用指定的ChromeDriver路径
            from selenium.webdriver.chrome.service import Service
            chromedriver_path = "C:\\Program Files\\Google\\Chrome\\Application\\chromedriver.exe"
            service = Service(chromedriver_path)
            self.driver = webdriver.Chrome(service=service, options=chrome_options)
            
            # 最大化窗口
            self.driver.maximize_window()
            
            print("浏览器启动成功！")
        except Exception as e:
            print(f"启动浏览器失败: {e}")
            print("可能的原因:")
            print("1. ChromeDriver未正确安装或版本不匹配")
            print("2. Chrome浏览器未安装或版本不匹配")
            print("3. 网络问题导致下载ChromeDriver失败")
            print("4. ChromeDriver路径未正确配置")
            print("\n建议解决方案:")
            print("1. 检查Chrome浏览器版本: chrome://version")
            print("2. 下载对应版本的ChromeDriver: https://chromedriver.chromium.org/downloads")
            print("3. 将ChromeDriver放到系统PATH中")
            print("4. 或在代码中指定ChromeDriver路径")
            self.driver = None
    
    def navigate_to_products(self):
        """导航到产品页面"""
        if not self.driver:
            print("浏览器启动失败，无法继续测试")
            return
        
        print(f"导航到: {self.base_url}/products")
        self.driver.get(f"{self.base_url}/products")
        
        # 等待页面加载
        time.sleep(2)
        
        print("页面加载完成，您可以开始测试:")
        print("1. 观察是否触发验证码")
        print("2. 手动输入验证码")
        print("3. 观察验证结果")
        print("\n完成测试后，请按Enter键关闭浏览器")
    
    def test_product_routes(self):
        """测试产品路由：先登录，再访问/products，再快速访问/product/1,/product/2,/product/3各三次"""
        if not self.driver:
            print("浏览器启动失败，无法继续测试")
            return
        
        print("=" * 60)
        print("测试产品路由访问")
        print("=" * 60)
        
        # 1. 先登录
        print("步骤0: 登录系统")
        self.navigate_to_login()
        time.sleep(3)  # 等待登录完成
        
        # 2. 访问 /products
        print(f"步骤1: 访问 {self.base_url}/products")
        self.driver.get(f"{self.base_url}/products")
        time.sleep(2)
        
        # 检查是否需要验证码
        has_captcha = self.check_and_fill_captcha()
        if has_captcha:
            print("检测到验证码页面，停止访问路由")
            return
        
        # 3. 快速访问 /product/1, /product/2, /product/3 各三次
        product_ids = [1, 2, 3]
        
        for product_id in product_ids:
            for i in range(3):
                print(f"步骤2: 访问 {self.base_url}/product/{product_id} (第{i+1}次)")
                self.driver.get(f"{self.base_url}/product/{product_id}")
                time.sleep(1)  # 快速访问，只等待1秒
                
                # 检查是否需要验证码
                has_captcha = self.check_and_fill_captcha()
                if has_captcha:
                    print("检测到验证码页面，停止访问路由")
                    return
        
        print("\n产品路由测试完成！")
        print("请观察是否有拦截情况")
        print("\n按Enter键关闭浏览器...")
    
    def navigate_to_login(self):
        """导航到登录页面并自动登录"""
        if not self.driver:
            print("浏览器启动失败，无法继续测试")
            return
        
        print(f"导航到: {self.base_url}/login")
        self.driver.get(f"{self.base_url}/login")
        
        # 等待页面加载
        time.sleep(2)
        
        try:
            # 自动填写登录表单
            print("正在自动填写登录表单...")
            
            # 定位用户名输入框
            username_input = self.driver.find_element(by="name", value="username")
            # 定位密码输入框
            password_input = self.driver.find_element(by="name", value="password")
            # 定位登录按钮
            login_button = self.driver.find_element(by="xpath", value="//button[@type='submit']")
            
            # 填写用户名和密码
            username_input.send_keys("admin")
            password_input.send_keys("admin123")
            
            print("已填写用户名: admin")
            print("已填写密码: admin123")
            
            # 点击登录按钮
            print("点击登录按钮...")
            login_button.click()
            
            # 等待登录结果
            time.sleep(3)
            
            # 检查是否出现验证码页面
            self.check_and_fill_captcha()
            
            print("登录操作完成")
            
        except Exception as e:
            print(f"自动填写表单失败: {e}")
            print("可能是页面结构不同，您可以手动填写登录表单")
    
    def capture_captcha(self):
        """截图验证码"""
        try:
            # 查找验证码图片元素 - 验证码图片的id是verifyImg
            captcha_image = self.driver.find_element(by="id", value="verifyImg")
            if captcha_image:
                # 获取设备像素比，用于高分辨率屏幕的坐标修正
                device_pixel_ratio = self.driver.execute_script("return window.devicePixelRatio")
                print(f"设备像素比: {device_pixel_ratio}")
                
                # 获取验证码图片的位置和大小
                location = captcha_image.location
                size = captcha_image.size
                print(f"验证码位置: {location}, 大小: {size}")
                
                # 截图整个页面
                screenshot = self.driver.get_screenshot_as_png()
                image = Image.open(io.BytesIO(screenshot))
                print(f"截图尺寸: {image.size}")
                
                # 计算验证码区域（考虑设备像素比）
                left = int(location['x'] * device_pixel_ratio)
                top = int(location['y'] * device_pixel_ratio)
                right = int((location['x'] + size['width']) * device_pixel_ratio)
                bottom = int((location['y'] + size['height']) * device_pixel_ratio)
                
                print(f"裁剪区域: left={left}, top={top}, right={right}, bottom={bottom}")
                
                # 裁剪验证码
                captcha = image.crop((left, top, right, bottom))
                
                # 保存验证码图片
                captcha.save("captcha.png")
                print("验证码截图保存成功")
                return captcha
        except Exception as e:
            print(f"截图验证码失败: {e}")
        return None
    
    def preprocess_captcha(self, captcha):
        """预处理验证码图片"""
        try:
            # 转换为灰度图像
            gray = cv2.cvtColor(np.array(captcha), cv2.COLOR_RGB2GRAY)
            
            # 二值化
            _, binary = cv2.threshold(gray, 127, 255, cv2.THRESH_BINARY)
            
            # 去除噪声
            kernel = np.ones((2, 2), np.uint8)
            binary = cv2.morphologyEx(binary, cv2.MORPH_OPEN, kernel)
            
            # 保存处理后的图片
            cv2.imwrite("captcha_processed.png", binary)
            print("验证码预处理完成")
            return binary
        except Exception as e:
            print(f"预处理验证码失败: {e}")
        return None
    
    def recognize_captcha(self, image):
        """使用OCR识别验证码"""
        try:
            print("使用OCR识别验证码...")
            
            # 尝试使用pytesseract进行OCR识别
            try:
                import pytesseract
                
                # 如果是OpenCV格式（numpy数组），转换为PIL格式
                if isinstance(image, np.ndarray):
                    img_pil = Image.fromarray(image)
                else:
                    img_pil = image
                
                # 使用pytesseract进行OCR识别
                # 配置参数：--psm 7 表示将图像视为单行文本
                captcha_text = pytesseract.image_to_string(
                    img_pil, 
                    config='--psm 7 --oem 3 -c tessedit_char_whitelist=ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
                ).strip()
                
                # 清理识别结果，只保留字母和数字
                captcha_text = ''.join(c for c in captcha_text if c.isalnum())
                
                if captcha_text:
                    print(f"OCR识别到验证码: {captcha_text}")
                    return captcha_text
                else:
                    print("OCR识别结果为空")
                    
            except ImportError:
                print("pytesseract未安装，尝试使用ddddocr...")
                
                # 尝试使用ddddocr库
                try:
                    import ddddocr
                    
                    # 将图像转换为字节
                    buffer = io.BytesIO()
                    if isinstance(image, np.ndarray):
                        img_pil = Image.fromarray(image)
                        img_pil.save(buffer, format='PNG')
                    else:
                        image.save(buffer, format='PNG')
                    
                    # 使用ddddocr识别
                    ocr = ddddocr.DdddOcr()
                    captcha_text = ocr.classification(buffer.getvalue())
                    
                    # 清理识别结果
                    captcha_text = ''.join(c for c in captcha_text if c.isalnum())
                    
                    if captcha_text:
                        print(f"ddddocr识别到验证码: {captcha_text}")
                        return captcha_text
                    else:
                        print("ddddocr识别结果为空")
                        
                except ImportError:
                    print("ddddocr也未安装")
                
            except Exception as e:
                print(f"OCR识别失败: {e}")
                
        except Exception as e:
            print(f"验证码识别失败: {e}")
        
        print("使用默认验证码: ABCD")
        return "ABCD"  # 默认值
    
    def check_and_fill_captcha(self):
        """检查是否出现验证码页面并自动填写，返回是否出现验证码"""
        try:
            # 检查是否在验证码页面
            current_url = self.driver.current_url
            print(f"当前URL: {current_url}")
            
            # 尝试查找验证码输入框
            captcha_input = self.driver.find_element(by="id", value="result")
            if captcha_input:
                print("检测到验证码页面，自动识别并填写验证码...")
                
                # 截图验证码
                captcha = self.capture_captcha()
                if captcha:
                    # 预处理验证码
                    processed_captcha = self.preprocess_captcha(captcha)
                    if processed_captcha is not None:
                        # 识别验证码
                        captcha_text = self.recognize_captcha(processed_captcha)
                        print(f"识别到验证码: {captcha_text}")
                        
                        # 填写验证码
                        captcha_input.send_keys(captcha_text)
                        print(f"已填写验证码: {captcha_text}")
                    else:
                        # 预处理失败，使用默认值
                        captcha_input.send_keys("ABCD")
                        print("预处理失败，使用默认验证码: ABCD")
                else:
                    # 截图失败，使用默认值
                    captcha_input.send_keys("ABCD")
                    print("截图失败，使用默认验证码: ABCD")
                
                # 尝试多种方式查找提交按钮
                submit_button = None
                try:
                    # 尝试通过onclick属性查找
                    submit_button = self.driver.find_element(by="xpath", value="//button[@onclick='validate()']")
                except:
                    try:
                        # 尝试通过文本查找
                        submit_button = self.driver.find_element(by="xpath", value="//button[contains(text(), '验证')]")
                    except:
                        try:
                            # 尝试通过type属性查找
                            submit_button = self.driver.find_element(by="xpath", value="//input[@type='button' or @type='submit']")
                        except:
                            pass
                
                if submit_button:
                    print("点击提交按钮...")
                    submit_button.click()
                    
                    # 等待验证结果
                    time.sleep(3)
                    
                    # 检查是否验证成功
                    try:
                        # 检查是否出现错误提示
                        alert = self.driver.switch_to.alert
                        print(f"验证码验证结果: {alert.text}")
                        alert.accept()
                    except:
                        # 没有弹出提示，可能验证成功
                        print("验证码提交完成")
                return True  # 出现了验证码页面
        except Exception as e:
            print(f"检查或填写验证码失败: {e}")
            print("可能没有出现验证码，或页面结构不同")
        return False  # 没有出现验证码页面
    
    def close_browser(self):
        """关闭浏览器"""
        if self.driver:
            print("正在关闭浏览器...")
            try:
                self.driver.quit()
                print("浏览器已关闭")
            except Exception as e:
                print(f"关闭浏览器时出错: {e}")
        else:
            print("浏览器未启动")
    
    def run_test(self):
        """运行测试"""
        try:
            # 设置浏览器
            self.setup_browser()
            
            if not self.driver:
                print("浏览器启动失败，测试无法继续")
                return
            
            # 测试产品路由
            self.test_product_routes()
            
            # 等待用户完成测试
            input("")
            
        except Exception as e:
            print(f"测试过程中出错: {e}")
        finally:
            # 关闭浏览器
            self.close_browser()

def check_dependencies():
    """检查依赖"""
    print("检查依赖...")
    
    try:
        import selenium
        print("✓ Selenium 已安装")
        return True
    except ImportError:
        print("✗ Selenium 未安装")
        print("请运行: pip install selenium")
        return False

def main():
    """主函数"""
    print("=" * 60)
    print("自动化浏览器手动验证码测试工具")
    print("=" * 60)
    
    # 检查依赖
    if not check_dependencies():
        return
    
    # 创建测试器实例
    tester = ManualVerificationTester(base_url="http://localhost:8080")
    
    # 运行测试
    tester.run_test()
    
    print("\n使用说明:")
    print("1. 确保demo项目正在运行在 http://localhost:8080")
    print("2. 安装Selenium: pip install selenium")
    print("3. 确保Chrome浏览器已安装")
    print("4. 下载对应版本的ChromeDriver并添加到PATH")
    print("5. 运行脚本: python manual_verification_test.py")
    print("6. 在浏览器中手动进行验证码测试")

if __name__ == "__main__":
    main()