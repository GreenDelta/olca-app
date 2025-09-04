"use client"

import { useChat } from "ai/react"
import { 
  ChatContainer, 
  ChatForm, 
  ChatMessages, 
  MessageInput, 
  MessageList, 
  PromptSuggestions 
} from "@/components/ui/chat"

/**
 * Custom implementation example using individual chat components.
 * 
 * This example shows how to build your own chat interface using the
 * individual sub-components instead of the pre-built Chat component.
 * This gives you more control over the layout and behavior.
 */
export function CustomChat() {
  const {
    messages,
    input,
    handleInputChange,
    handleSubmit,
    append,
    status,
    stop,
  } = useChat()

  const isLoading = status === 'submitted' || status === 'streaming'
  const lastMessage = messages.at(-1)
  const isEmpty = messages.length === 0
  const isTyping = lastMessage?.role === "user"

  const handleRateResponse = (messageId: string, rating: "thumbs-up" | "thumbs-down") => {
    console.log(`Rated message ${messageId} with ${rating}`)
    // Implement your rating logic here
  }

  return (
    <div className="h-[600px] w-full max-w-2xl mx-auto">
      <ChatContainer>
        {isEmpty ? (
          <PromptSuggestions
            append={append}
            suggestions={[
              "What is the capital of France?", 
              "Tell me a joke",
              "Explain quantum computing in simple terms"
            ]}
          />
        ) : null}

        {!isEmpty ? (
          <ChatMessages>
            <MessageList 
              messages={messages} 
              isTyping={isTyping}
              onRateResponse={handleRateResponse}
            />
          </ChatMessages>
        ) : null}

        <ChatForm
          className="mt-auto"
          isPending={isLoading || isTyping}
          handleSubmit={handleSubmit}
        >
          {({ files, setFiles }) => (
            <MessageInput
              value={input}
              onChange={handleInputChange}
              allowAttachments
              files={files}
              setFiles={setFiles}
              stop={stop}
              isGenerating={isLoading}
            />
          )}
        </ChatForm>
      </ChatContainer>
    </div>
  )
}
