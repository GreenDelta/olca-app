"use client"

import { useChat } from "ai/react"
import { Chat } from "@/components/ui/chat"

/**
 * Basic usage example of the Chat component.
 * 
 * This example shows the simplest way to use the Chat component
 * with the useChat hook from ai/react.
 */
export function ChatDemo() {
  const { messages, input, handleInputChange, handleSubmit, status, stop } =
    useChat()

  const isLoading = status === "submitted" || status === "streaming"

  return (
    <div className="h-[600px] w-full max-w-2xl mx-auto">
      <Chat
        messages={messages}
        input={input}
        handleInputChange={handleInputChange}
        handleSubmit={handleSubmit}
        isGenerating={isLoading}
        stop={stop}
      />
    </div>
  )
}
