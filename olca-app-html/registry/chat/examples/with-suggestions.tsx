"use client"

import { useChat } from "ai/react"
import { Chat } from "@/components/ui/chat"

/**
 * Chat component with prompt suggestions example.
 * 
 * This example shows how to use the Chat component with prompt suggestions
 * that appear when the chat is empty, helping users get started.
 */
export function ChatWithSuggestions() {
  const {
    messages,
    input,
    handleInputChange,
    handleSubmit,
    append,
    status,
    stop,
  } = useChat()

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
        append={append}
        suggestions={[
          "Generate a tasty vegan lasagna recipe for 3 people.",
          "Generate a list of 5 questions for a frontend job interview.",
          "Who won the 2022 FIFA World Cup?",
          "Explain step-by-step how to solve this math problem: If x² + 6x + 9 = 25, what is x?",
          "Design a simple algorithm to find the longest palindrome in a string.",
        ]}
      />
    </div>
  )
}
