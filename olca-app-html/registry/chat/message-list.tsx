"use client"

import * as React from "react"
import { cn } from "@/lib/utils"
import { Message } from "./chat"
import { Button } from "@/components/ui/button"
import { ThumbsUp, ThumbsDown } from "lucide-react"

/**
 * List component for displaying messages in the chat interface.
 * 
 * Renders a list of messages with support for different message types,
 * typing indicators, and optional rating functionality.
 * 
 * @example
 * ```tsx
 * <MessageList 
 *   messages={messages} 
 *   isTyping={isTyping}
 *   onRateResponse={onRateResponse}
 * />
 * ```
 * 
 * @param messages - Array of messages to display
 * @param isTyping - Whether to show typing indicator
 * @param onRateResponse - Callback to handle user rating of AI responses
 * @param className - Additional CSS classes
 */
export interface MessageListProps {
  messages: Message[]
  isTyping?: boolean
  onRateResponse?: (messageId: string, rating: "thumbs-up" | "thumbs-down") => void
  className?: string
}

export function MessageList({ 
  messages, 
  isTyping = false, 
  onRateResponse,
  className 
}: MessageListProps) {
  const messagesEndRef = React.useRef<HTMLDivElement>(null)

  React.useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [messages, isTyping])

  return (
    <div className={cn("flex flex-col gap-4 p-4", className)}>
      {messages.map((message) => (
        <div
          key={message.id}
          className={cn(
            "flex flex-col gap-2",
            message.role === "user" ? "items-end" : "items-start"
          )}
        >
          <div
            className={cn(
              "max-w-[80%] rounded-lg px-4 py-2",
              message.role === "user"
                ? "bg-primary text-primary-foreground"
                : "bg-muted"
            )}
          >
            <p className="text-sm">{message.content}</p>
          </div>
          
          {message.role === "assistant" && onRateResponse && (
            <div className="flex gap-1">
              <Button
                variant="ghost"
                size="sm"
                onClick={() => onRateResponse(message.id, "thumbs-up")}
              >
                <ThumbsUp className="h-3 w-3" />
              </Button>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => onRateResponse(message.id, "thumbs-down")}
              >
                <ThumbsDown className="h-3 w-3" />
              </Button>
            </div>
          )}
        </div>
      ))}
      
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
