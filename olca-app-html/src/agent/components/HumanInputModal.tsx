import React, { useState, useEffect, useRef } from 'react';
import { HumanInputRequest } from '../types';

interface HumanInputModalProps {
  request: HumanInputRequest;
  onSubmit: (response: any) => void;
  onCancel: () => void;
  isVisible: boolean;
}

export const HumanInputModal: React.FC<HumanInputModalProps> = ({
  request,
  onSubmit,
  onCancel,
  isVisible
}) => {
  const [response, setResponse] = useState<any>('');
  const [selectedOption, setSelectedOption] = useState<string>('');
  const [uploadedFile, setUploadedFile] = useState<File | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  
  const inputRef = useRef<HTMLInputElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    if (isVisible) {
      // Focus the appropriate input when modal opens
      setTimeout(() => {
        if (request.type === 'text' && textareaRef.current) {
          textareaRef.current.focus();
        } else if (request.type === 'choice' && inputRef.current) {
          inputRef.current.focus();
        }
      }, 100);
    }
  }, [isVisible, request.type]);

  const handleSubmit = async () => {
    if (isSubmitting) return;
    
    setIsSubmitting(true);
    
    try {
      let finalResponse: any;
      
      switch (request.type) {
        case 'choice':
          finalResponse = selectedOption;
          break;
        case 'text':
          finalResponse = response;
          break;
        case 'confirmation':
          finalResponse = response === 'yes';
          break;
        case 'file_upload':
          finalResponse = uploadedFile;
          break;
        default:
          finalResponse = response;
      }
      
      if (request.required && !finalResponse) {
        alert('This field is required');
        return;
      }
      
      await onSubmit(finalResponse);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit();
    } else if (e.key === 'Escape') {
      onCancel();
    }
  };

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    setUploadedFile(file || null);
  };

  if (!isVisible) return null;

  return (
    <div className="human-input-overlay">
      <div className="human-input-modal">
        <div className="modal-header">
          <h3>Agent Needs Your Input</h3>
          <button className="close-button" onClick={onCancel}>
            ✕
          </button>
        </div>
        
        <div className="modal-content">
          <div className="request-message">
            <p>{request.message}</p>
            {request.context && (
              <div className="context-info">
                <strong>Context:</strong> {request.context}
              </div>
            )}
          </div>

          <div className="input-section">
            {request.type === 'choice' && request.options && (
              <div className="choice-input">
                <label>Select an option:</label>
                <div className="options-list">
                  {request.options.map((option, index) => (
                    <label key={index} className="option-item">
                      <input
                        ref={index === 0 ? inputRef : undefined}
                        type="radio"
                        name="choice"
                        value={option}
                        checked={selectedOption === option}
                        onChange={(e) => setSelectedOption(e.target.value)}
                        onKeyDown={handleKeyDown}
                      />
                      <span className="option-text">{option}</span>
                    </label>
                  ))}
                </div>
              </div>
            )}

            {request.type === 'text' && (
              <div className="text-input">
                <label htmlFor="text-response">
                  {request.placeholder || 'Enter your response:'}
                </label>
                <textarea
                  ref={textareaRef}
                  id="text-response"
                  value={response}
                  onChange={(e) => setResponse(e.target.value)}
                  onKeyDown={handleKeyDown}
                  placeholder={request.placeholder}
                  rows={4}
                  disabled={isSubmitting}
                />
              </div>
            )}

            {request.type === 'confirmation' && (
              <div className="confirmation-input">
                <label>Please confirm:</label>
                <div className="confirmation-buttons">
                  <button
                    className={`confirm-button ${response === 'yes' ? 'selected' : ''}`}
                    onClick={() => setResponse('yes')}
                    disabled={isSubmitting}
                  >
                    ✅ Yes
                  </button>
                  <button
                    className={`confirm-button ${response === 'no' ? 'selected' : ''}`}
                    onClick={() => setResponse('no')}
                    disabled={isSubmitting}
                  >
                    ❌ No
                  </button>
                </div>
              </div>
            )}

            {request.type === 'file_upload' && (
              <div className="file-upload-input">
                <label htmlFor="file-upload">
                  Upload a file:
                </label>
                <input
                  id="file-upload"
                  type="file"
                  onChange={handleFileUpload}
                  disabled={isSubmitting}
                />
                {uploadedFile && (
                  <div className="file-info">
                    <strong>Selected:</strong> {uploadedFile.name} 
                    ({(uploadedFile.size / 1024).toFixed(1)} KB)
                  </div>
                )}
              </div>
            )}
          </div>

          {request.required && (
            <div className="required-notice">
              <span className="required-indicator">*</span> This field is required
            </div>
          )}
        </div>

        <div className="modal-footer">
          <button
            className="cancel-button"
            onClick={onCancel}
            disabled={isSubmitting}
          >
            Cancel
          </button>
          <button
            className="submit-button"
            onClick={handleSubmit}
            disabled={isSubmitting || (request.required && !response && !selectedOption && !uploadedFile)}
          >
            {isSubmitting ? 'Submitting...' : 'Submit'}
          </button>
        </div>
      </div>
    </div>
  );
};

// Connection status indicator
interface ConnectionStatusProps {
  status: 'connected' | 'disconnected' | 'reconnecting';
  lastConnected?: Date;
  retryCount: number;
}

export const ConnectionStatus: React.FC<ConnectionStatusProps> = ({
  status,
  lastConnected,
  retryCount
}) => {
  const getStatusColor = () => {
    switch (status) {
      case 'connected': return '#10B981';
      case 'disconnected': return '#EF4444';
      case 'reconnecting': return '#F59E0B';
      default: return '#6B7280';
    }
  };

  const getStatusText = () => {
    switch (status) {
      case 'connected': return 'Connected';
      case 'disconnected': return 'Disconnected';
      case 'reconnecting': return `Reconnecting... (${retryCount})`;
      default: return 'Unknown';
    }
  };

  return (
    <div className="connection-status">
      <div 
        className="status-indicator"
        style={{ backgroundColor: getStatusColor() }}
      />
      <span className="status-text">{getStatusText()}</span>
      {lastConnected && status === 'connected' && (
        <span className="last-connected">
          Last connected: {lastConnected.toLocaleTimeString()}
        </span>
      )}
    </div>
  );
};
