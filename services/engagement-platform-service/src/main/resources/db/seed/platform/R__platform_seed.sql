INSERT INTO sys_config(config_key,config_value,remark) VALUES
('delivery.auto_advance','true','模拟配送自动推进'),
('delivery.tick_minutes','3','模拟配送推进间隔（分钟）')
ON DUPLICATE KEY UPDATE config_value=VALUES(config_value),remark=VALUES(remark),is_deleted=0;

INSERT INTO sys_dict(dict_type,dict_key,dict_value,sort_order) VALUES
('complaint_category','service','服务态度',1),
('complaint_category','quality','商品质量',2),
('complaint_category','delivery','配送问题',3),
('complaint_category','other','其他',9),
('support_template','t1','您好，请问有什么可以帮您？',1),
('support_template','t2','感谢您的反馈，我们会立即核实',2),
('support_template','t3','已为您加急处理，请稍候',3),
('support_template','t4','问题已经处理完成，欢迎再次反馈',4)
ON DUPLICATE KEY UPDATE dict_value=VALUES(dict_value),sort_order=VALUES(sort_order),is_deleted=0;
