export interface Comment {
  id: number;
  content: string;
  authorId: number;
  authorUsername: string;
  threadId: number;
  parentId?: number; // Optional parent comment ID (null for top-level comments)
  referencedNodeIds?: Set<number>;
  upvoteCount: number;
  downvoteCount: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  userVoteType: string | null | undefined; // User's current vote (UPVOTE, DOWNVOTE, or null if not voted)
  hasUserVoted?: boolean; // Whether the current user has voted on this comment
} 