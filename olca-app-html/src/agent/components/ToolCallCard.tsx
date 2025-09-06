import React, { useState } from 'react';
import { ToolCall, ToolResult, TOOL_CATEGORIES, STATUS_COLORS } from '../types';

interface ToolCallCardProps {
  toolCall: ToolCall;
  toolResult?: ToolResult;
  onRetry?: (toolCallId: string) => void;
  onExpand?: (toolCallId: string) => void;
}

export const ToolCallCard: React.FC<ToolCallCardProps> = ({
  toolCall,
  toolResult,
  onRetry,
  onExpand
}) => {
  const [isExpanded, setIsExpanded] = useState(false);

  const categoryInfo = TOOL_CATEGORIES[toolCall.category];
  const statusColor = STATUS_COLORS[toolCall.status];

  const handleExpand = () => {
    setIsExpanded(!isExpanded);
    if (onExpand) {
      onExpand(toolCall.id);
    }
  };

  const handleRetry = () => {
    if (onRetry) {
      onRetry(toolCall.id);
    }
  };

  const getStatusIcon = () => {
    switch (toolCall.status) {
      case 'pending':
        return <div className="status-icon pending">⏳</div>;
      case 'executing':
        return <div className="status-icon executing spinning">⚙️</div>;
      case 'completed':
        return <div className="status-icon completed">✅</div>;
      case 'failed':
        return <div className="status-icon failed">❌</div>;
      default:
        return <div className="status-icon">❓</div>;
    }
  };

  const formatExecutionTime = (time?: number) => {
    if (!time) return null;
    return time < 1000 ? `${time}ms` : `${(time / 1000).toFixed(1)}s`;
  };

  return (
    <div className={`tool-call-card ${toolCall.status} ${toolCall.category}`}>
      <div className="tool-header" onClick={handleExpand}>
        <div className="tool-info">
          {getStatusIcon()}
          <div className="tool-details">
            <span 
              className="tool-name" 
              style={{ color: categoryInfo.color }}
            >
              {toolCall.name}
            </span>
            <span className="tool-category">{categoryInfo.name}</span>
          </div>
        </div>
        
        <div className="tool-meta">
          {toolCall.executionTime && (
            <span className="execution-time">
              {formatExecutionTime(toolCall.executionTime)}
            </span>
          )}
          {toolResult?.executionTime && (
            <span className="execution-time">
              {formatExecutionTime(toolResult.executionTime)}
            </span>
          )}
          <span 
            className="status-badge"
            style={{ backgroundColor: statusColor }}
          >
            {toolCall.status}
          </span>
          <button className="expand-button">
            {isExpanded ? '▼' : '▶'}
          </button>
        </div>
      </div>

      {isExpanded && (
        <div className="tool-content">
          <div className="tool-section">
            <h4>Parameters</h4>
            <pre className="parameters">
              {JSON.stringify(toolCall.parameters, null, 2)}
            </pre>
          </div>

          {toolResult && (
            <div className="tool-section">
              <h4>Result</h4>
              {toolResult.success ? (
                <pre className="result success">
                  {typeof toolResult.result === 'string' 
                    ? toolResult.result 
                    : JSON.stringify(toolResult.result, null, 2)
                  }
                </pre>
              ) : (
                <div className="result error">
                  <strong>Error:</strong> {toolResult.error}
                </div>
              )}
            </div>
          )}

          {toolCall.error && (
            <div className="tool-section">
              <h4>Error</h4>
              <div className="error-message">{toolCall.error}</div>
            </div>
          )}

          {toolCall.status === 'failed' && onRetry && (
            <div className="tool-actions">
              <button 
                className="retry-button"
                onClick={handleRetry}
              >
                🔄 Retry
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

// Tool call list component
interface ToolCallListProps {
  toolCalls: ToolCall[];
  toolResults: ToolResult[];
  onRetry?: (toolCallId: string) => void;
  onExpand?: (toolCallId: string) => void;
}

export const ToolCallList: React.FC<ToolCallListProps> = ({
  toolCalls,
  toolResults,
  onRetry,
  onExpand
}) => {
  if (toolCalls.length === 0) return null;

  return (
    <div className="tool-call-list">
      <h4>Tool Execution</h4>
      {toolCalls.map((toolCall) => {
        const toolResult = toolResults.find(
          result => result.toolCallId === toolCall.id
        );
        
        return (
          <ToolCallCard
            key={toolCall.id}
            toolCall={toolCall}
            toolResult={toolResult}
            onRetry={onRetry}
            onExpand={onExpand}
          />
        );
      })}
    </div>
  );
};
