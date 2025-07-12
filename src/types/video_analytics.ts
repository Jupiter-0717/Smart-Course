export interface VideoAnalyticsDTO{
    totalViewers:number
    averageWatchTime:number
    completionRate:number
    hotSpots:HotSpot[]
    studentWatchCount:number
    studentLastWatchTime:number
    studentProgressRate:number
}
export interface HotSpot{
    startTime:number
    endTime:number
    replayCount:number
}
export interface VideoResourceAnalyticsDTO {
  totalViewers: number
  averageWatchTime: number
  completionRate: number
  hotSpots: HotSpot[]
}
