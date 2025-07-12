import { defineStore } from 'pinia';
import api from '@/api/knowledgeGraphAPI';
import { ElMessage } from 'element-plus';

// 明确定义知识点的接口
export interface KnowledgePoint {
  id: number;
  name: string;
  description: string;
  positionX: number;
  positionY: number;
}

// 明确定义知识图谱节点接口
export interface KnowledgeGraphNode {
  id: string; // 格式为 "knowledge_{id}"
  title: string;
  description: string;
  type: string;
  courseId: number;
  positionX: number | null; // 修改：允许为 null
  positionY: number | null; // 修改：允许为 null
  resources: ResourceDTO[] | null; // 修改：允许为 null
}

// 明确定义知识图谱边接口
export interface KnowledgeGraphEdge {
  source: string; // 格式为 "knowledge_{id}"
  target: string; // 格式为 "knowledge_{id}"
  relationship: string;
}

// 资源DTO接口
export interface ResourceDTO {
  id: number;
  name: string;
  type: string;
  url: string;
  linkedAt: string; // ISO8601格式
  knowledgePointId?: number; // 添加可选属性
}

// 状态接口
interface KnowledgeGraphState {
  nodes: KnowledgeGraphNode[];
  edges: KnowledgeGraphEdge[];
  currentCourseId: number | null;
  selectedNode: KnowledgeGraphNode | null;
  pointResources: ResourceDTO[];
  loading: boolean; 
}

export const useKnowledgeGraphStore = defineStore('knowledgeGraph', {
  state: (): KnowledgeGraphState => ({
    nodes: [],
    edges: [],
    currentCourseId: null,
    selectedNode: null,
    pointResources: [],
    loading: false,
  }),

  actions: {
    // 加载课程知识图谱
   async loadCourseGraph(teacherId: number, courseId: number) {
  this.loading = true;
  try {
    const response = await api.getCourseKnowledgeGraph(teacherId, courseId);
    
    console.log('API响应:', response.data); // 添加日志
    
    // 直接使用后端返回的节点和边
    this.nodes = response.data.nodes || [];
    this.edges = response.data.edges || [];
    
    console.log('Store节点:', this.nodes); // 添加日志
    console.log('Store边:', this.edges); // 添加日志
    
    this.currentCourseId = courseId;
  } catch (error) {
    console.error('加载知识图谱失败', error);
    ElMessage.error('加载知识图谱失败');
  } finally {
    this.loading = false;
  }
},

    // 创建知识点
    async createKnowledgePoint(teacherId: number, data: any) {
      this.loading = true;
      try {
        const response = await api.createKnowledgePoint(teacherId, data);
        const newPoint: KnowledgePoint = response.data;
        
        // 创建新节点
        const newNode: KnowledgeGraphNode = {
          id: `knowledge_${newPoint.id}`,
          title: data.name,
          description: data.description,
          type: 'knowledge',
          courseId: data.courseId,
          positionX: newPoint.positionX,
          positionY: newPoint.positionY,
          resources: [] // 初始化为空数组
        };
        
        // 添加到节点列表
        this.nodes.push(newNode);
        
        // 如果指定了父节点，添加连接
        if (data.parentId) {
          this.edges.push({
            source: `knowledge_${data.parentId}`,
            target: `knowledge_${newPoint.id}`,
            relationship: 'subtopic'
          });
        }
        
        return newPoint.id;
      } catch (error) {
        console.error('创建知识点失败', error);
        ElMessage.error('创建知识点失败');
        throw error; // 抛出错误以便在组件中处理
      } finally {
        this.loading = false;
      }
    },

    // 删除知识点
    async deleteKnowledgePoint(teacherId: number, pointId: number) {
      this.loading = true;
      try {
        await api.deleteKnowledgePoint(teacherId, pointId);
        
        // 移除节点
        this.nodes = this.nodes.filter(node => node.id !== `knowledge_${pointId}`);
        
        // 移除相关边
        this.edges = this.edges.filter(edge => 
          edge.source !== `knowledge_${pointId}` && 
          edge.target !== `knowledge_${pointId}`
        );
        
        // 清除相关资源缓存
        this.pointResources = this.pointResources.filter(res => 
          !res.knowledgePointId || res.knowledgePointId !== pointId
        );
        
        // 清除选中的节点
        if (this.selectedNode?.id === `knowledge_${pointId}`) {
          this.selectedNode = null;
        }
        
        ElMessage.success('删除成功');
      } catch (error) {
        console.error('删除知识点失败', error);
        ElMessage.error('删除失败');
      } finally {
        this.loading = false;
      }
    },

    // 更新节点位置
    async updateNodePosition(teacherId: number, pointId: number, position: { positionX: number, positionY: number }) {
      try {
        await api.updateKnowledgePointPosition(teacherId, pointId, position);
        
        // 更新本地节点位置
        const node = this.nodes.find(n => n.id === `knowledge_${pointId}`);
        if (node) {
          node.positionX = position.positionX;
          node.positionY = position.positionY;
        }
      } catch (error) {
        console.error('更新位置失败', error);
        ElMessage.error('更新位置失败');
      }
    },

     // AI生成知识图谱
    async generateAIGraph(teacherId: number, courseId: number) {
      this.loading = true;
      try {
        const response = await api.generateByAI(teacherId, courseId);

        
    console.log('API响应:', response.data); // 添加日志
    
    // 直接使用后端返回的节点和边
    this.nodes = response.data.nodes || [];
    this.edges = response.data.edges || [];
    
    console.log('Store节点:', this.nodes); // 添加日志
    console.log('Store边:', this.edges); // 添加日志
    
    this.currentCourseId = courseId;
  } catch (error) {
    console.error('加载知识图谱失败', error);
    ElMessage.error('加载知识图谱失败');
  } finally {
    this.loading = false;
  }
    },


    // 加载知识点关联资源
    async loadPointResources(pointId: number) {
      this.loading = true;
      try {
        const response = await api.getPointResources(pointId);
        // 添加knowledgePointId到每个资源对象
        this.pointResources = response.data.map((resource: any) => ({
          ...resource,
          knowledgePointId: pointId // 添加知识点ID
        }));
        return this.pointResources;
      } catch (error) {
        console.error('加载资源失败', error);
        ElMessage.error('加载资源失败');
        return [];
      } finally {
        this.loading = false;
      }
    },

    // 添加资源关联
    async addResourceAssociation(pointId: number, resourceId: number) {
      this.loading = true;
      try {
        await api.addResourceAssociation(pointId, resourceId);
        
        // 添加到资源列表（这里需要实际API调用获取资源详情）
        // 实际项目中应调用API获取资源详情后添加
        // 这里简化为添加一个占位对象
        this.pointResources.push({
          id: resourceId,
          knowledgePointId: pointId, // 添加知识点ID
          name: `资源 ${resourceId}`,
          type: '未知',
          url: '',
          linkedAt: new Date().toISOString()
        });
        
        ElMessage.success('资源关联成功');
      } catch (error) {
        console.error('关联失败', error);
        ElMessage.error('关联失败');
      } finally {
        this.loading = false;
      }
    },

    // 移除资源关联
    async removeResourceAssociation(pointId: number, resourceId: number) {
      this.loading = true;
      try {
        await api.removeResourceAssociation(pointId, resourceId);
        
        // 从资源列表中移除
        this.pointResources = this.pointResources.filter(
          resource => !(resource.id === resourceId && resource.knowledgePointId === pointId)
        );
        
        ElMessage.success('移除关联成功');
      } catch (error) {
        console.error('移除关联失败', error);
        ElMessage.error('移除关联失败');
      } finally {
        this.loading = false;
      }
    }
  }
});