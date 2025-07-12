export type VideoEventType = 'PLAY' | 'PAUSE' | 'SEEK' | 'ENDED'

export interface VideoEventRequest {
  studentId:number
  resourceId: number
  eventType: VideoEventType
  videoTimestamp: number
  sessionId: string
  duration: number
}