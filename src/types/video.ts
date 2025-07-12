// src/types/video.ts
export interface VideoRecommendationDTO {
  videoId: number;
  title: string;
  durationSeconds: number;
  thumbnailUrl?: string;
  viewCount?: number;
  recommendationReason?: string;
}

export interface VideoDetailDTO {
  videoId: number;
  title: string;
  url: string;
  durationSeconds: number;
  description: string;
}