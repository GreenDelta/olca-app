import { Client } from '@langchain/langgraph-sdk';
import { 
  LangGraphConfig, 
  StreamChunk, 
  ConnectionStatus, 
  ThreadInfo, 
  ToolCall, 
  ToolResult, 
  HumanInputRequest 
} from './types';

export class LangGraphService {
  private client: Client;
  private currentThreadId: string | null = null;
  private connectionStatus: ConnectionStatus = {
    status: 'disconnected',
    retryCount: 0
  };
  private retryTimeout: NodeJS.Timeout | null = null;

  constructor(config: LangGraphConfig) {
    this.client = new Client({
      apiUrl: config.url,
      apiKey: config.apiKey,
      timeoutMs: config.timeout || 30000
    });
  }

  // Connection management
  async connect(): Promise<boolean> {
    try {
      this.connectionStatus.status = 'reconnecting';
      this.connectionStatus.retryCount++;
      
      // Test connection by making a simple request
      await this.client.assistants.search({ limit: 1 });
      
      this.connectionStatus.status = 'connected';
      this.connectionStatus.lastConnected = new Date();
      this.connectionStatus.retryCount = 0;
      
      return true;
    } catch (error) {
      console.error('Failed to connect to LangGraph:', error);
      this.connectionStatus.status = 'disconnected';
      
      // Schedule retry
      if (this.connectionStatus.retryCount < 3) {
        this.scheduleRetry();
      }
      
      return false;
    }
  }

  private scheduleRetry(): void {
    if (this.retryTimeout) {
      clearTimeout(this.retryTimeout);
    }
    
    const delay = Math.min(1000 * Math.pow(2, this.connectionStatus.retryCount), 10000);
    this.retryTimeout = setTimeout(() => {
      this.connect();
    }, delay);
  }

  getConnectionStatus(): ConnectionStatus {
    return { ...this.connectionStatus };
  }

  // Thread management
  async createThread(): Promise<ThreadInfo> {
    try {
      const thread = await this.client.threads.create();
      this.currentThreadId = thread.thread_id;
      
      return {
        id: thread.thread_id,
        createdAt: new Date(thread.created_at),
        messageCount: 0,
        status: 'active'
      };
    } catch (error) {
      console.error('Failed to create thread:', error);
      throw new Error('Failed to create conversation thread');
    }
  }

  async getThread(threadId: string): Promise<ThreadInfo> {
    try {
      const thread = await this.client.threads.get(threadId);
      
      return {
        id: thread.thread_id,
        createdAt: new Date(thread.created_at),
        lastMessageAt: thread.updated_at ? new Date(thread.updated_at) : undefined,
        messageCount: 0, // This would need to be calculated from messages
        status: 'active'
      };
    } catch (error) {
      console.error('Failed to get thread:', error);
      throw new Error('Failed to retrieve conversation thread');
    }
  }

  async deleteThread(threadId: string): Promise<void> {
    try {
      await this.client.threads.delete(threadId);
      if (this.currentThreadId === threadId) {
        this.currentThreadId = null;
      }
    } catch (error) {
      console.error('Failed to delete thread:', error);
      throw new Error('Failed to delete conversation thread');
    }
  }

  getCurrentThreadId(): string | null {
    return this.currentThreadId;
  }

  setCurrentThreadId(threadId: string): void {
    this.currentThreadId = threadId;
  }

  // Streaming and message handling
  async *stream(input: {
    threadId: string;
    input: Record<string, any>;
    streamMode?: 'updates' | 'values';
  }): AsyncGenerator<StreamChunk, void, unknown> {
    try {
      // For now, we'll use a mock assistant ID since we don't have a real one
      const assistantId = 'mock-assistant-id';
      
      const stream = this.client.runs.stream(input.threadId, assistantId, {
        input: input.input,
        streamMode: input.streamMode || 'updates'
      });

      for await (const chunk of stream) {
        yield this.processStreamChunk(chunk);
      }
    } catch (error) {
      console.error('Streaming error:', error);
      yield {
        type: 'error',
        data: { error: error.message },
        timestamp: new Date()
      };
    }
  }

  private processStreamChunk(chunk: any): StreamChunk {
    // Process different types of stream chunks
    if (chunk.event === 'on_tool_call') {
      return {
        type: 'tool_call',
        data: {
          id: chunk.data.id,
          name: chunk.data.name,
          parameters: chunk.data.args,
          status: 'executing',
          timestamp: new Date(),
          category: this.categorizeTool(chunk.data.name)
        },
        timestamp: new Date()
      };
    }

    if (chunk.event === 'on_tool_result') {
      return {
        type: 'tool_result',
        data: {
          toolCallId: chunk.data.tool_call_id,
          result: chunk.data.result,
          success: !chunk.data.error,
          error: chunk.data.error,
          executionTime: chunk.data.execution_time,
          timestamp: new Date()
        },
        timestamp: new Date()
      };
    }

    if (chunk.event === 'on_interrupt') {
      return {
        type: 'human_input_required',
        data: {
          id: chunk.data.id,
          type: chunk.data.type || 'text',
          message: chunk.data.message,
          options: chunk.data.options,
          required: chunk.data.required !== false,
          context: chunk.data.context,
          placeholder: chunk.data.placeholder,
          threadId: chunk.data.thread_id,
          runId: chunk.data.run_id
        },
        timestamp: new Date()
      };
    }

    if (chunk.event === 'on_llm_stream') {
      return {
        type: 'message',
        data: {
          content: chunk.data.content,
          isStreaming: true
        },
        timestamp: new Date()
      };
    }

    // Default case
    return {
      type: 'status',
      data: chunk,
      timestamp: new Date()
    };
  }

  private categorizeTool(toolName: string): ToolCall['category'] {
    const name = toolName.toLowerCase();
    
    if (name.includes('analy') || name.includes('impact') || name.includes('assessment')) {
      return 'analysis';
    }
    if (name.includes('data') || name.includes('query') || name.includes('fetch')) {
      return 'data';
    }
    if (name.includes('search') || name.includes('find') || name.includes('lookup')) {
      return 'search';
    }
    if (name.includes('calc') || name.includes('compute') || name.includes('math')) {
      return 'calculation';
    }
    
    return 'other';
  }

  // Human input handling
  async resumeWithHumanInput(input: {
    threadId: string;
    runId: string;
    humanInput: any;
  }): Promise<void> {
    try {
      // For now, we'll simulate resuming with human input
      // In a real implementation, this would use the proper LangGraph API
      console.log('Resuming with human input:', input);
    } catch (error) {
      console.error('Failed to resume with human input:', error);
      throw new Error('Failed to process human input');
    }
  }

  // Tool retry functionality
  async retryTool(toolCallId: string): Promise<void> {
    try {
      // This would depend on the specific LangGraph API for retrying tools
      // For now, we'll implement a placeholder
      console.log('Retrying tool:', toolCallId);
    } catch (error) {
      console.error('Failed to retry tool:', error);
      throw new Error('Failed to retry tool execution');
    }
  }

  // Health check
  async health(): Promise<boolean> {
    try {
      await this.client.assistants.search({ limit: 1 });
      return true;
    } catch (error) {
      console.error('Health check failed:', error);
      return false;
    }
  }

  // Cleanup
  destroy(): void {
    if (this.retryTimeout) {
      clearTimeout(this.retryTimeout);
    }
  }
}

