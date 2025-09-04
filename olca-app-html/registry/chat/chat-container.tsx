"use client"

import * as React from "react"
import { cn } from "@/lib/utils"

/**
 * A container component that wraps the whole chat interface.
 * 
 * Used to build your own chat if not using the default Chat component.
 * Provides the main layout structure for the chat interface.
 * 
 * @example
 * ```tsx
 * <ChatContainer className="h-[600px]">
 *   <ChatMessages>
 *     <MessageList messages={messages} />
 *   </ChatMessages>
 *   <ChatForm handleSubmit={handleSubmit}>
 *     <MessageInput value={input} onChange={handleInputChange} />
 *   </ChatForm>
 * </ChatContainer>
 * ```
 * 
 * @param children - Child components to render
 * @param className - Additional CSS classes for Chat
 */
export interface ChatContainerProps {
  children: React.ReactNode
  className?: string
}

export function ChatContainer({ children, className }: ChatContainerProps) {
  return (
    <div className={cn("flex h-full w-full flex-col", className)}>
      {children}
    </div>
  )
}
