"use client"

import * as React from "react"
import { cn } from "@/lib/utils"
import { ChatMessage } from "./chat-message"

export interface Message {
  id: string
  role: "user" | "assistant"
  content: string
  createdAt?: Date
  attachments?: File[]
}

export interface AdditionalMessageOptions {
  className?: string
  showAvatar?: boolean
  showTimestamp?: boolean
  onRateResponse?: (messageId: string, rating: "thumbs-up" | "thumbs-down") => void
  onCopy?: (content: string) => void
  onEdit?: (messageId: string, newContent: string) => void
  onDelete?: (messageId: string) => void
}

/**
 * A component for rendering a list of chat messages with a typing indicator.
 * 
 * The MessageList component renders a vertical list of chat messages with optional 
 * typing indicators, timestamps, and customizable message options. It supports 
 * both static and dynamic message styling based on message properties.
 * 
 * @example
 * ```tsx
 * // Basic usage
 * import { MessageList } from "@/components/ui/message-list"
 * 
 * export function ChatDemo() {
 *   const messages = [
 *     {
 *       id: "1",
 *       role: "user",
 *       content: "Hello, how are you?",
 *     },
 *     {
 *       id: "2",
 *       role: "assistant",
 *       content: "I'm doing well, thank you for asking!",
 *     },
 *   ]
 * 
 *   return <MessageList messages={messages} isTyping={false} />
 * }
 * ```
 * 
 * @example
 * ```tsx
 * // With custom message options
 * export function ChatWithCustomOptions() {
 *   return (
 *     <MessageList
 *       messages={messages}
 *       messageOptions={{
 *         className: "custom-message-class",
 *         showAvatar: true,
 *       }}
 *     />
 *   )
 * }
 * ```
 * 
 * @example
 * ```tsx
 * // With dynamic message options
 * export function ChatWithDynamicOptions() {
 *   return (
 *     <MessageList
 *       messages={messages}
 *       messageOptions={(message) => ({
 *         className: message.role === "user" ? "user-message" : "assistant-message",
 *         showAvatar: message.role === "assistant",
 *       })}
 *     />
 *   )
 * }
 * ```
 * 
 * @param messages - Array of messages to display
 * @param showTimeStamps - Whether to show timestamps on messages (default: true)
 * @param isTyping - Whether to show the typing indicator (default: false)
 * @param messageOptions - Additional options to pass to each ChatMessage component
 * @param className - Additional CSS classes for the message list container
 */
export interface MessageListProps {
  messages: Message[]
  showTimeStamps?: boolean
  isTyping?: boolean
  messageOptions?: AdditionalMessageOptions | ((message: Message) => AdditionalMessageOptions)
  className?: string
}

export function MessageList({
  messages,
  showTimeStamps = true,
  isTyping = false,
  messageOptions,
  className,
}: MessageListProps) {
  const messagesEndRef = React.useRef<HTMLDivElement>(null)

  // Auto-scroll to bottom when new messages are added or typing state changes
  React.useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [messages, isTyping])

  const getMessageOptions = (message: Message): AdditionalMessageOptions => {
    if (typeof messageOptions === "function") {
      return messageOptions(message)
    }
    return messageOptions || {}
  }

  return (
    <div className={cn("flex flex-col gap-4 p-4", className)}>
      {messages.map((message) => {
        const options = getMessageOptions(message)
        
        return (
          <ChatMessage
            key={message.id}
            message={message}
            showTimestamp={showTimeStamps && options.showTimestamp !== false}
            showAvatar={options.showAvatar}
            onRateResponse={options.onRateResponse}
            onCopy={options.onCopy}
            onEdit={options.onEdit}
            onDelete={options.onDelete}
            className={options.className}
          />
        )
      })}
      
      {isTyping && (
        <div className="flex items-start gap-2">
          <div className="bg-muted rounded-lg px-4 py-2">
            <div className="flex gap-1">
              <div className="h-2 w-2 bg-muted-foreground rounded-full animate-bounce" />
              <div className="h-2 w-2 bg-muted-foreground rounded-full animate-bounce [animation-delay:0.1s]" />
              <div className="h-2 w-2 bg-muted-foreground rounded-full animate-bounce [animation-delay:0.2s]" />
            </div>
          </div>
        </div>
      )}
      
      <div ref={messagesEndRef} />
    </div>
  )
}
