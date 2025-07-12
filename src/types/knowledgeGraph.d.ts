export interface KnowledgePoint {
  id: number
  name: string
  description: string
  positionX: number
  positionY: number
  courseId: number
}

export interface KnowledgeGraph {
  nodes: {
    id: string
    title: string
    description: string
    type: string
    courseId: number
    positionX: number
    positionY: number
    resources: ResourceDTO[]
  }[]
  edges: {
    source: string
    target: string
    relationship: string
  }[]
}

export interface ResourceDTO {
  id: number
  name: string
  type: string
  url: string
  linkedAt: string
}

export interface PositionUpdateRequest {
  positionX: number
  positionY: number
}
