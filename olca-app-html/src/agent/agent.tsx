import React, { useState, useCallback } from "react";
import { render } from "react-dom";

// Types for chat messages
interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  createdAt?: Date;
  attachments?: File[];
}

// LCA-focused prompt suggestions
const lcaSuggestions = [
  "Help me understand the carbon footprint of this process",
  "What are the main environmental impacts in this LCA?",
  "How can I optimize this product system?",
  "Explain the uncertainty in these results",
  "What data quality issues should I address?",
  "Help me interpret these impact assessment results"
];

const AgentChat: React.FC = () => {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [isGenerating, setIsGenerating] = useState(false);

  // Handle message submission
  const handleSubmit = useCallback(async (event?: React.FormEvent) => {
    if (event) event.preventDefault();
    if (!input.trim() || isGenerating) return;

    const userMessage: Message = {
      id: Date.now().toString(),
      role: 'user',
      content: input.trim(),
      createdAt: new Date()
    };

    setMessages(prev => [...prev, userMessage]);
    setInput("");
    setIsGenerating(true);

    // Simulate AI response (replace with actual AI integration)
    setTimeout(() => {
      const aiMessage: Message = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: `I understand you're asking about: "${userMessage.content}". This is a simulated response. In the full implementation, this would connect to an AI service that can help with LCA analysis, data interpretation, and process optimization.`,
        createdAt: new Date()
      };
      
      setMessages(prev => [...prev, aiMessage]);
      setIsGenerating(false);
    }, 1500);
  }, [input, isGenerating]);

  // Handle input change
  const handleInputChange = useCallback((e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setInput(e.target.value);
  }, []);

  // Handle prompt suggestion click
  const handleSuggestionClick = useCallback((suggestion: string) => {
    setInput(suggestion);
  }, []);

  // Stop generation
  const stopGeneration = useCallback(() => {
    setIsGenerating(false);
  }, []);

  return (
    <div className="agent-chat-container">
      <div className="chat-container">
        <div className="messages-container">
          {messages.length === 0 ? (
            <div className="welcome-section">
              <h3>Welcome to the openLCA Agent</h3>
              <div className="suggestions-grid">
                {lcaSuggestions.map((suggestion, index) => (
                  <button
                    key={index}
                    className="suggestion-button"
                    onClick={() => handleSuggestionClick(suggestion)}
                  >
                    {suggestion}
                  </button>
                ))}
              </div>
            </div>
          ) : (
            <div className="message-list">
              {messages.map((message) => (
                <div key={message.id} className={`message ${message.role}`}>
                  <div className="message-content">
                    {message.content}
                  </div>
                  <div className="message-time">
                    {message.createdAt?.toLocaleTimeString()}
                  </div>
                </div>
              ))}
              {isGenerating && (
                <div className="message assistant">
                  <div className="message-content">
                    <div className="typing-indicator">
                      <span></span>
                      <span></span>
                      <span></span>
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
        
        <form className="chat-form" onSubmit={handleSubmit}>
          <div className="input-container">
            <textarea
              value={input}
              onChange={handleInputChange}
              placeholder="Ask the openLCA Agent..."
              className="message-input"
              rows={1}
              disabled={isGenerating}
            />
            <button
              type="submit"
              disabled={!input.trim() || isGenerating}
              className="send-button"
            >
              {isGenerating ? "..." : "Send"}
            </button>
            {isGenerating && (
              <button
                type="button"
                onClick={stopGeneration}
                className="stop-button"
              >
                Stop
              </button>
            )}
          </div>
        </form>
      </div>
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
      <div className="agent-header">
        <h1>openLCA Agent</h1>
        <button 
          onClick={toggleTheme}
          className="theme-toggle"
          title="Toggle theme"
        >
          {isDark ? '☀️' : '🌙'}
        </button>
      </div>
      <AgentChat />
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
    getMessages: () => Message[];
    setMessages: (messages: Message[]) => void;
    clearChat: () => void;
  }
}

// Expose functions for Java integration
window.setTheme = (isDark: boolean) => {
  document.body.className = isDark ? 'dark' : 'light';
};

window.getMessages = () => {
  // This would need to be connected to the React state
  // For now, return empty array
  return [];
};

window.setMessages = (messages: Message[]) => {
  // This would need to be connected to the React state
  // For now, do nothing
};

window.clearChat = () => {
  // This would need to be connected to the React state
  // For now, do nothing
};

// Initialize when DOM is ready
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initializeApp);
} else {
  initializeApp();
}

export default App;
