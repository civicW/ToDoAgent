from database_of_messages import main as db_main, DateTimeEncoder
from typing import List, Dict, Any, Optional
import json
import os
from dotenv import load_dotenv
import random
import copy

# Load environment variables
load_dotenv()

class MessageProcessor:
    """消息处理器类，用于从数据库获取和过滤消息"""
    
    def __init__(self, host: Optional[str] = None, database: Optional[str] = None, password: Optional[str] = None):
        """
        初始化消息处理器
        
        参数:
            host: 数据库主机地址，默认从环境变量获取
            database: 数据库名称，默认从环境变量获取
            password: 数据库密码，默认从环境变量获取
        """
        self.host = host or os.getenv('DB_HOST', '103.116.245.150')
        self.database = database or os.getenv('DB_NAME', 'ToDoAgent')
        self.password = password or os.getenv('DB_PASSWORD', '4bc6bc963e6d8443453676')
        self.all_data = None
    
    def fetch_all_messages(self) -> List[Dict[str, Any]]:
        """
        从数据库获取所有消息
        
        返回:
            所有消息的列表
        """
        self.all_data = db_main(
            host=self.host,
            database=self.database,
            password=self.password
        )
        return self.all_data
    
    def filter_messages_by_ids(self, target_ids: List[int], data: Optional[List[Dict[str, Any]]] = None) -> List[Dict[str, Any]]:
        """
        根据目标ID过滤消息数据
        
        参数:
            target_ids: 目标消息ID列表
            data: 要过滤的数据，默认使用已获取的数据
        返回:
            过滤后的消息列表，每个消息包含完整信息
        """
        # 如果没有提供数据且没有已获取的数据，则获取所有消息
        if data is None:
            if self.all_data is None:
                self.fetch_all_messages()
            data = self.all_data
        
        result = []
        for msg in data:
            msg_id = msg.get('message_id')
            if msg_id is not None and msg_id in target_ids:
                # 返回完整的消息字典
                result.append(msg)
        return result
    
    def process_messages(self, target_ids: List[int]) -> List[Dict[str, Any]]:
        """
        处理指定ID的消息
        
        参数:
            target_ids: 目标消息ID列表
        返回:
            过滤后的消息列表
        """
        # 确保已获取数据
        if self.all_data is None:
            self.fetch_all_messages()
        
        # 过滤消息
        filtered_result = self.filter_messages_by_ids(target_ids)
        
        return filtered_result
    
    def to_json(self, data: List[Dict[str, Any]]) -> str:
        """
        将数据转换为JSON字符串
        
        参数:
            data: 要转换的数据
        返回:
            格式化的JSON字符串
        """
        return json.dumps(data, ensure_ascii=False, indent=4, cls=DateTimeEncoder)


class DataTransformer:
    """数据转换器类，用于转换和处理数据格式"""
    
    @staticmethod
    def rename_date_to_start_time(data_list: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        将数据列表中的date字段重命名为start_time
        
        参数:
            data_list: 包含消息数据的字典列表
        返回:
            处理后的数据列表
        """
        result = []
        for item in data_list:
            # 创建一个新的字典，避免修改原始数据
            new_item = item.copy()
            if 'date' in new_item:
                # 将date字段的值赋给新的start_time字段
                new_item['start_time'] = new_item['date']
                # 删除原来的date字段
                del new_item['date']
            result.append(new_item)
        return result
    
    @staticmethod
    def combine_content_and_start_time(data_list: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        将content和start_time字段整合为一个新的content字段
        
        参数:
            data_list: 包含消息数据的字典列表
        返回:
            处理后的数据列表
        """
        result = []
        for item in data_list:
            # 创建一个新的字典，避免修改原始数据
            new_item = item.copy()
            if 'content' in new_item and 'start_time' in new_item:
                # 保存原始content
                original_content = new_item['content']
                # 整合content和start_time
                new_item['content'] = f"起始于{new_item['start_time']}，{original_content}"
                # 删除start_time字段
                del new_item['start_time']
            result.append(new_item)
        return result
    
    @staticmethod
    def transform_data(data_list: List[Dict[str, Any]], rename_date: bool = True, combine_fields: bool = False) -> List[Dict[str, Any]]:
        """
        转换数据，可以选择是否重命名date字段和是否合并字段
        
        参数:
            data_list: 要转换的数据列表
            rename_date: 是否将date重命名为start_time
            combine_fields: 是否合并content和start_time字段
        返回:
            转换后的数据列表
        """
        result = data_list
        
        if rename_date:
            result = DataTransformer.rename_date_to_start_time(result)
        
        if combine_fields:
            result = DataTransformer.combine_content_and_start_time(result)
        
        return result


class ExtendedDataTransformer(DataTransformer):
    """扩展数据转换器，增加更多指标"""
    
    @staticmethod
    def add_new_indicators(data_list: List[Dict[str, Any]], original_content_map: Dict[int, str] = None) -> List[Dict[str, Any]]:
        """
        添加新的指标：
        - list_id: 随机生成的唯一标识符
        - todo_content: 默认为null
        - last_modified: 最后修改时间，默认为null
        - done: 完成状态，默认为0
        - time: 包含start_time和end_time的时间信息
        - location: 位置信息，默认为null
        
        参数:
            data_list: 包含消息数据的字典列表
            original_content_map: 消息ID到原始内容的映射（已废弃，保留参数是为了兼容性）
        返回:
            添加了新指标的数据列表
        """
        for item in data_list:
            # 生成随机唯一的list_id
            item['list_id'] = random.randint(100000000, 999999999)
            
            # 设置todo_content为null
            item['todo_content'] = None
            
            # 添加时间相关字段
            item['time'] = {
                'start_time': None,
                'end_time': None
            }
            
            # 添加位置字段
            item['location'] = None
            
            # 添加last_modified和done字段
            item['last_modified'] = None
            item['done'] = 0
        
        return data_list


if __name__ == '__main__':
    """主函数示例"""
    # 创建消息处理器实例
    processor = MessageProcessor()
    
    # 定义目标消息ID
    target_ids = [
        318002613,
        318003223,
        318002211,
        317222110,
        318211265

    ]
    
    # 处理消息
    result = processor.process_messages(target_ids)
    
    # 保存原始数据的副本
    original_data = copy.deepcopy(result)
    
    # 创建原始内容映射
    original_content_map = {}
    for item in original_data:
        if 'message_id' in item and 'content' in item:
            original_content_map[item['message_id']] = item['content']
    
    # 使用数据转换器转换数据
    transformer = ExtendedDataTransformer()
    transformed_data = transformer.transform_data(result, rename_date=True, combine_fields=True)
    
    # 添加新指标
    transformed_data = transformer.add_new_indicators(transformed_data, original_content_map)
    
    # 打印结果
    print(processor.to_json(transformed_data))
    




