"use client"

import * as React from "react"
import { cn } from "@/lib/utils"
import { Message } from "./chat"

/**
 * Provides a message list with auto-scrolling behavior and typing indicators.
 * 
 * Used to build your own chat if not using the default Chat component.
 * Handles the display of messages with automatic scrolling to the latest message.
 * 
 * @example
 * ```tsx
 * <ChatMessages>
 *   <MessageList messages={messages} isTyping={isTyping} />
 * </ChatMessages>
 * ```
 * 
 * @param messages - Array of messages to display
 * @param isTyping - Whether to show typing indicator
 * @param children - Child components (typically MessageList)
 */
export interface ChatMessagesProps {
  messages?: Message[]
  isTyping?: boolean
  children: React.ReactNode
}

export function ChatMessages({ children }: ChatMessagesProps) {
  return (
    <div className="flex-1 overflow-auto">
      {children}
    </div>
  )
}
