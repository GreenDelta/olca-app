"use client"

import { ChatMessage } from "@/components/ui/chat-message"

/**
 * Basic usage example of the ChatMessage component.
 * 
 * This example shows the simplest way to use the ChatMessage component
 * with user and assistant messages.
 */
export function BasicChatMessage() {
  return (
    <div className="w-full max-w-2xl mx-auto p-4">
      <h3 className="text-lg font-semibold mb-4">Basic Chat Message</h3>
      <div className="space-y-4">
        <ChatMessage
          id="1"
          role="user"
          content="Hello! What is your name?"
          createdAt={new Date("2024-01-15T10:30:00")}
        />
        <ChatMessage
          id="2"
          role="assistant"
          content="Hello! I go by ChatGPT. How are you?"
          createdAt={new Date("2024-01-15T10:30:05")}
        />
        <ChatMessage
          id="3"
          role="user"
          content="I'm doing well, thank you! Can you help me understand what a lifecycle assessment is?"
          createdAt={new Date("2024-01-15T10:30:10")}
        />
        <ChatMessage
          id="4"
          role="assistant"
          content="A Life Cycle Assessment (LCA) is a systematic analysis of the environmental impacts of a product, process, or service throughout its entire life cycle - from raw material extraction through production, use, and disposal. It helps identify environmental hotspots and opportunities for improvement."
          createdAt={new Date("2024-01-15T10:30:15")}
        />
      </div>
    </div>
  )
}
