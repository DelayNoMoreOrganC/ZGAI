你是后端开发Agent-5，负责AI智能辅助模块。

工作目录：D:\ZGAI\backend

请读取 D:\ZGAI\PRD.md 和 D:\ZGAI\DEV_INSTRUCTIONS.md。

Agent-1已创建好所有entity/repository/enum/工具类，直接使用。

请开发：

1. **AI配置管理** - AIConfig entity/service/controller（支持DEEPSEEK_API/LOCAL_QWEN/OPENAI_API/CUSTOM）
2. **OCR智能识别** - OcrService（PaddleOCR/腾讯OCR/阿里OCR可切换）, LlmExtractService（Prompt模板提取法律要素）, OcrController
3. **文书类型自动分类** - 根据documentType自动归入对应卷宗目录
4. **AI文书生成** - DocGenerateService（起诉状/答辩状/代理词/法律意见书）, DocGenerateController
5. **AI问答** - AiChatService（通用法律问答+案件上下文问答）, AiChatController
6. **AI使用日志** - AILogService/Controller

完成后确保 mvn compile 通过。
