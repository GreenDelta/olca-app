"use client"

import { MessageList } from "@/components/ui/message-list"

/**
 * Basic usage example of the MessageList component.
 * 
 * This example shows the simplest way to use the MessageList component
 * with a basic array of messages.
 */
export function BasicMessageList() {
  const messages = [
    {
      id: "1",
      role: "user" as const,
      content: "Hello, how are you?",
      createdAt: new Date("2024-01-15T10:30:00"),
    },
    {
      id: "2",
      role: "assistant" as const,
      content: "I'm doing well, thank you for asking! How can I help you today?",
      createdAt: new Date("2024-01-15T10:30:05"),
    },
    {
      id: "3",
      role: "user" as const,
      content: "Can you explain what a lifecycle assessment is?",
      createdAt: new Date("2024-01-15T10:31:00"),
    },
    {
      id: "4",
      role: "assistant" as const,
      content: "A Life Cycle Assessment (LCA) is a systematic analysis of the environmental impacts of a product, process, or service throughout its entire life cycle - from raw material extraction through production, use, and disposal. It helps identify environmental hotspots and opportunities for improvement.",
      createdAt: new Date("2024-01-15T10:31:15"),
    },
  ]

  return (
    <div className="w-full max-w-2xl mx-auto p-4">
      <h3 className="text-lg font-semibold mb-4">Basic Message List</h3>
      <div className="h-[400px] overflow-auto border rounded-lg">
        <MessageList messages={messages} />
      </div>
    </div>
  )
}
