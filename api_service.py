from flask import Flask, request,jsonify
from knowledge_point import KnowledgeExtractor

app=Flask(__name__)
extractor=KnowledgeExtractor("D:/Hugging-Face/DeepSeek-1.5B")

@app.route('/extract',methods=['POST'])
def extract_concepts():
    file_paths=request.json.get('file_path',[])
    processed_paths = []
    for path in file_paths:
        # 统一转换为正斜杠或双反斜杠
        fixed_path = path.replace('\\', '/')  # 或 path.replace('\\', '\\\\')
        processed_paths.append(fixed_path)
    
    print("修正后的路径:", processed_paths)  # 调试打印
    
    # 直接传递处理后的路径列表
    result = extractor.process_multiple_documents(processed_paths)
    print(result)
    return jsonify(result)


if __name__=='__main__':
    app.run(host='0.0.0.0',port=5000)
