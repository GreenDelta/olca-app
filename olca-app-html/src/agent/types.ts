// Enhanced TypeScript interfaces for LangGraph integration

export interface Message {
  id: string;
  role: 'user' | 'assistant' | 'tool' | 'system';
  content: string;
  createdAt?: Date;
  attachments?: File[];
  // LangGraph specific fields
  threadId?: string;
  runId?: string;
  // Tool transparency
  toolCalls?: ToolCall[];
  toolResults?: ToolResult[];
  reasoning?: string;
  // Human input handling
  humanInputRequest?: HumanInputRequest;
  // Status tracking
  status: 'pending' | 'streaming' | 'completed' | 'error' | 'waiting_for_input';
}

export interface ToolCall {
  id: string;
  name: string;
  parameters: Record<string, any>;
  status: 'pending' | 'executing' | 'completed' | 'failed';
  timestamp: Date;
  executionTime?: number;
  category: 'analysis' | 'data' | 'search' | 'calculation' | 'other';
  error?: string;
}

export interface ToolResult {
  toolCallId: string;
  result: any;
  success: boolean;
  error?: string;
  executionTime?: number;
  timestamp: Date;
}

export interface HumanInputRequest {
  id: string;
  type: 'choice' | 'text' | 'confirmation' | 'file_upload';
  message: string;
  options?: string[];
  required: boolean;
  context?: string;
  placeholder?: string;
  threadId?: string;
  runId?: string;
}

export interface LangGraphConfig {
  url: string;
  apiKey: string;
  timeout?: number;
  retryAttempts?: number;
}

export interface StreamChunk {
  type: 'tool_call' | 'tool_result' | 'human_input_required' | 'message' | 'error' | 'status';
  data: any;
  timestamp: Date;
}

export interface ConnectionStatus {
  status: 'connected' | 'disconnected' | 'reconnecting';
  lastConnected?: Date;
  retryCount: number;
}

export interface ThreadInfo {
  id: string;
  createdAt: Date;
  lastMessageAt?: Date;
  messageCount: number;
  status: 'active' | 'archived' | 'deleted';
}

// Tool categories for color coding
export const TOOL_CATEGORIES = {
  analysis: { color: '#3B82F6', name: 'Analysis' },
  data: { color: '#10B981', name: 'Data' },
  search: { color: '#8B5CF6', name: 'Search' },
  calculation: { color: '#F59E0B', name: 'Calculation' },
  other: { color: '#6B7280', name: 'Other' }
} as const;

// Status colors
export const STATUS_COLORS = {
  pending: '#F59E0B',
  executing: '#3B82F6',
  completed: '#10B981',
  failed: '#EF4444',
  error: '#EF4444',
  streaming: '#3B82F6',
  waiting_for_input: '#F59E0B'
} as const;
