import React, { useState, useCallback, useEffect, useRef } from "react";
import { render } from "react-dom";
import { 
  Message, 
  ToolCall, 
  ToolResult, 
  HumanInputRequest, 
  LangGraphConfig,
  StreamChunk 
} from "./types";
import { LangGraphService } from "./langgraphService";
import { HumanInputModal, ConnectionStatus } from "./components/HumanInputModal";
import { TypingIndicator } from "@/components/ui/typing-indicator";
import { ChatMessage } from "@/components/ui/chat-message";
import { PromptSuggestions } from "@/components/ui/prompt-suggestions";
import { MessageInput } from "@/components/ui/message-input";
import "../styles/global.css";
import "./agent.css";

// LCA-focused prompt suggestions
const lcaSuggestions = [
  "Help me understand the carbon footprint of this process",
  "What are the main environmental impacts in this LCA?",
  "How can I optimize this product system?",
  "Explain the uncertainty in these results",
  "What data quality issues should I address?",
  "Help me interpret these impact assessment results",
  "🎯 Consolidated CSS structure!"
];

interface AgentChatProps {
  isDark: boolean;
  onToggleTheme: () => void;
}

const AgentChat: React.FC<AgentChatProps> = ({ isDark, onToggleTheme }) => {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [isGenerating, setIsGenerating] = useState(false);
  const [currentThreadId, setCurrentThreadId] = useState<string | null>(null);
  const [toolCalls, setToolCalls] = useState<ToolCall[]>([]);
  const [toolResults, setToolResults] = useState<ToolResult[]>([]);
  const [humanInputRequest, setHumanInputRequest] = useState<HumanInputRequest | null>(null);
  const [connectionStatus, setConnectionStatus] = useState<'connected' | 'disconnected' | 'reconnecting'>('disconnected');
  const [lastConnected, setLastConnected] = useState<Date | undefined>();
  const [retryCount, setRetryCount] = useState(0);
  
  const langGraphService = useRef<LangGraphService | null>(null);
  const currentStream = useRef<AsyncGenerator<StreamChunk, void, unknown> | null>(null);

  // Initialize LangGraph service
  useEffect(() => {
    const config: LangGraphConfig = {
      url: 'http://localhost:8000',
      apiKey: 'xy',
      timeout: 30000,
      retryAttempts: 3
    };

    // Initialize real LangGraph service
    langGraphService.current = new LangGraphService(config);
    
    // Connect to service
    const connect = async () => {
      const connected = await langGraphService.current!.connect();
      if (connected) {
        setConnectionStatus('connected');
        setLastConnected(new Date());
        setRetryCount(0);
      } else {
        setConnectionStatus('disconnected');
      }
    };

    connect();

    return () => {
      if (langGraphService.current) {
        langGraphService.current.destroy();
      }
    };
  }, []);

  // Handle message submission
  const handleSubmit = useCallback(async (event?: React.FormEvent) => {
    if (event) event.preventDefault();
    if (!input.trim() || isGenerating || !langGraphService.current) return;

    const userMessage: Message = {
      id: Date.now().toString(),
      role: 'user',
      content: input.trim(),
      createdAt: new Date(),
      status: 'completed'
    };

    setMessages(prev => [...prev, userMessage]);
    setInput("");
    setIsGenerating(true);

    try {
      // Create or get current thread
      if (!currentThreadId) {
        const thread = await langGraphService.current.createThread();
        setCurrentThreadId(thread.id);
      }

      // Create streaming assistant message
      const assistantMessageId = (Date.now() + 1).toString();
      const assistantMessage: Message = {
        id: assistantMessageId,
        role: 'assistant',
        content: '',
        createdAt: new Date(),
        status: 'streaming',
        threadId: currentThreadId || undefined
      };

      setMessages(prev => [...prev, assistantMessage]);

      // Stream from LangGraph
      const stream = langGraphService.current.stream({
        threadId: currentThreadId!,
        input: { message: userMessage.content },
        streamMode: 'updates'
      });

      currentStream.current = stream;

      // Process streaming updates
      for await (const chunk of stream) {
        await processStreamChunk(chunk, assistantMessageId);
      }

      // Mark message as completed
      setMessages(prev => prev.map(msg => 
        msg.id === assistantMessageId 
          ? { ...msg, status: 'completed' }
          : msg
      ));

    } catch (error) {
      console.error('Streaming error:', error);
      
      // Add error message
      const errorMessage: Message = {
        id: (Date.now() + 2).toString(),
        role: 'system',
        content: `Error: ${error instanceof Error ? error.message : 'Unknown error occurred'}`,
        createdAt: new Date(),
        status: 'error'
      };
      
      setMessages(prev => [...prev, errorMessage]);
    } finally {
      setIsGenerating(false);
      currentStream.current = null;
    }
  }, [input, isGenerating, currentThreadId]);

  // Process streaming chunks
  const processStreamChunk = useCallback(async (chunk: StreamChunk, messageId: string) => {
    switch (chunk.type) {
      case 'tool_call':
        const toolCall: ToolCall = {
          ...chunk.data,
          timestamp: new Date()
        };
        setToolCalls(prev => [...prev, toolCall]);
        break;

      case 'tool_result':
        const toolResult: ToolResult = {
          ...chunk.data,
          timestamp: new Date()
        };
        setToolResults(prev => [...prev, toolResult]);
        
        // Update tool call status
        setToolCalls(prev => prev.map(tc => 
          tc.id === toolResult.toolCallId 
            ? { ...tc, status: toolResult.success ? 'completed' : 'failed', error: toolResult.error }
            : tc
        ));
        break;

      case 'human_input_required':
        setHumanInputRequest(chunk.data);
        setIsGenerating(false);
        break;

      case 'message':
        setMessages(prev => prev.map(msg => 
          msg.id === messageId 
            ? { ...msg, content: chunk.data.content, status: chunk.data.isStreaming ? 'streaming' : 'completed' }
            : msg
        ));
        break;

      case 'error':
        console.error('Stream error:', chunk.data.error);
        break;
    }
  }, []);

  // Handle input change
  const handleInputChange = useCallback((e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setInput(e.target.value);
  }, []);

  // Handle prompt suggestion click
  const handleSuggestionClick = useCallback((suggestion: string) => {
    setInput(suggestion);
  }, []);

  // Handle human input submission
  const handleHumanInputSubmit = useCallback(async (response: any) => {
    if (!humanInputRequest || !langGraphService.current) return;

    try {
      await langGraphService.current.resumeWithHumanInput({
        threadId: humanInputRequest.threadId || currentThreadId!,
        runId: humanInputRequest.runId || '',
        humanInput: response
      });

      setHumanInputRequest(null);
      setIsGenerating(true);

      // Continue processing the stream
      // The stream will continue automatically after human input

    } catch (error) {
      console.error('Failed to submit human input:', error);
      setHumanInputRequest(null);
    }
  }, [humanInputRequest, currentThreadId]);

  // Handle human input cancellation
  const handleHumanInputCancel = useCallback(() => {
    setHumanInputRequest(null);
    setIsGenerating(false);
  }, []);

  // Retry failed tool
  const handleToolRetry = useCallback(async (toolCallId: string) => {
    if (!langGraphService.current) return;

    try {
      await langGraphService.current.retryTool(toolCallId);
    } catch (error) {
      console.error('Failed to retry tool:', error);
    }
  }, []);

  // Stop generation
  const stopGeneration = useCallback(() => {
    setIsGenerating(false);
    currentStream.current = null;
  }, []);

  return (
    <div className="agent-chat-container">
      <div className="chat-container">
        <div className="connection-header">
          <ConnectionStatus 
            status={connectionStatus}
            lastConnected={lastConnected}
            retryCount={retryCount}
          />
          <button 
            onClick={onToggleTheme}
            className="theme-toggle"
            title="Toggle theme"
          >
            {isDark ? '☀️' : '🌙'}
          </button>
        </div>
        
        <div className="messages-container">
          {messages.length === 0 ? (
            <PromptSuggestions
              label="Welcome to the openLCA Agent"
              append={(message) => {
                setInput(message.content);
                handleSubmit();
              }}
              suggestions={lcaSuggestions}
            />
          ) : (
            <div className="message-list space-y-4 p-4">
              {messages.map((message) => (
                <div key={message.id}>
                  {(message.role === 'user' || message.role === 'assistant') && (
                    <ChatMessage
                      id={message.id}
                      role={message.role}
                      content={message.content}
                      createdAt={message.createdAt}
                      showTimeStamp={true}
                      animation="scale"
                      toolInvocations={message.toolCalls?.map(toolCall => ({
                        state: toolCall.status === 'completed' ? 'result' : 
                               toolCall.status === 'failed' ? 'result' : 'call',
                        toolName: toolCall.name,
                        result: toolCall.status === 'completed' ? 
                          { result: toolResults.find(r => r.toolCallId === toolCall.id)?.result } :
                          toolCall.status === 'failed' ? 
                          { __cancelled: true, error: toolCall.error } : undefined
                      }))}
                    />
                  )}
                  
                  {(message.role === 'tool' || message.role === 'system') && (
                    <div className="flex items-start gap-2">
                      <div className="h-8 w-8 rounded-full bg-muted flex items-center justify-center">
                        <span className="text-xs font-medium">
                          {message.role === 'tool' ? '🔧' : '⚙️'}
                        </span>
                      </div>
                      <div className="bg-muted rounded-lg px-4 py-2 max-w-[80%]">
                        <p className="text-sm">{message.content}</p>
                        {message.createdAt && (
                          <time className="text-xs text-muted-foreground">
                            {message.createdAt.toLocaleTimeString()}
                          </time>
                        )}
                      </div>
                    </div>
                  )}
                  
                  {message.status === 'error' && (
                    <div className="ml-10 mt-2 text-sm text-red-600">
                      ⚠️ Error occurred
                    </div>
                  )}
                </div>
              ))}
              
              {isGenerating && !humanInputRequest && (
                <div className="flex items-start gap-2">
                  <div className="h-8 w-8 rounded-full bg-secondary flex items-center justify-center">
                    <span className="text-xs font-medium">AI</span>
                  </div>
                  <TypingIndicator />
                </div>
              )}
            </div>
          )}
        </div>
        
        <div className="p-4 border-t">
          <form onSubmit={handleSubmit}>
            <MessageInput
              value={input}
              onChange={handleInputChange}
              isGenerating={isGenerating}
              stop={stopGeneration}
              placeholder="Ask the openLCA Agent..."
              enableInterrupt={true}
              allowAttachments={false}
            />
          </form>
        </div>
      </div>
      
      {/* Human Input Modal */}
      {humanInputRequest && (
        <HumanInputModal
          request={humanInputRequest}
          onSubmit={handleHumanInputSubmit}
          onCancel={handleHumanInputCancel}
          isVisible={!!humanInputRequest}
        />
      )}
    </div>
  );
};

// Main App component
const App: React.FC = () => {
  const [isDark, setIsDark] = useState(true); // Default to dark mode

  // Toggle theme
  const toggleTheme = useCallback(() => {
    setIsDark(prev => !prev);
    document.body.className = isDark ? 'light' : 'dark';
  }, [isDark]);

  return (
    <div className={`agent-app ${isDark ? 'dark' : 'light'}`}>
      <div className="agent-chat-container">
        <AgentChat isDark={isDark} onToggleTheme={toggleTheme} />
      </div>
    </div>
  );
};

// Initialize the app
const initializeApp = () => {
  const appElement = document.getElementById('app');
  if (appElement) {
    render(<App />, appElement);
  }
};

// Global functions for Java integration
declare global {
  interface Window {
    setTheme: (isDark: boolean) => void;
  }
}

// Expose theme function for Java integration
// This allows the Java app to control light/dark mode and opens the door for HTML <-> Java communication
window.setTheme = (isDark: boolean) => {
  document.body.className = isDark ? 'dark' : 'light';
};

// Initialize when DOM is ready
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initializeApp);
} else {
  initializeApp();
}

// Hot Module Replacement (HMR) support
if (module.hot) {
  module.hot.accept();
}

export default App;
