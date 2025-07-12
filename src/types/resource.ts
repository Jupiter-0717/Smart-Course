export enum ResourceType {
  PPT = 'ppt',
  PDF = 'pdf',
  VIDEO = 'video',
  DOC = 'doc'
}

// 资源对象（从服务器获取）
export interface Resource {
  resourceId: number; // 统一使用 id 作为主键
  courseId: number;
  name: string;
  type: ResourceType;
  url: string;
  createdAt: string;
  description: string;
  courseName?: string; // 添加课程名称属性
}

// 资源上传请求（用于上传表单）
export interface ResourceUploadRequest {
  courseId: number;
  name: string;
  type: ResourceType;
  file: File;
  description?: string;
  // 注意：没有 id 属性，因为是新资源
}

// 资源更新请求（用于编辑表单）
export interface ResourceUpdateRequest {
  name: string;
  description: string;
  // 注意：没有 id 属性
}

// 添加文件更新请求类型
export interface ResourceFileUpdateRequest {
  file: File;
}