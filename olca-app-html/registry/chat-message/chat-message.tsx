"use client"

import * as React from "react"
import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { ThumbsUp, ThumbsDown, Copy, Paperclip } from "lucide-react"

export type MessageRole = "user" | "assistant"
export type AnimationType = "none" | "slide" | "scale" | "fade"

/**
 * A customizable message bubble component for chat interfaces with support for user and AI messages.
 * 
 * The ChatMessage component provides a styled message bubble with support for markdown content,
 * timestamps, animations, and contextual actions. It's designed to be accessible and keyboard navigable.
 * 
 * @example
 * ```tsx
 * // Basic usage
 * import { ChatMessage } from "@/components/ui/chat-message"
 * 
 * export function ChatDemo() {
 *   return (
 *     <div className="space-y-4">
 *       <ChatMessage
 *         id="1"
 *         role="user"
 *         content="Hello, how are you?"
 *         createdAt={new Date()}
 *         showTimeStamp
 *       />
 *       <ChatMessage
 *         id="2"
 *         role="assistant"
 *         content="I'm doing well, thank you for asking!"
 *         createdAt={new Date()}
 *         showTimeStamp
 *       />
 *     </div>
 *   )
 * }
 * ```
 * 
 * @example
 * ```tsx
 * // With actions
 * export function ChatWithActions() {
 *   const actions = (
 *     <div className="flex gap-1">
 *       <Button variant="ghost" size="sm">
 *         <ThumbsUp className="h-3 w-3" />
 *       </Button>
 *       <Button variant="ghost" size="sm">
 *         <Copy className="h-3 w-3" />
 *       </Button>
 *     </div>
 *   )
 * 
 *   return (
 *     <ChatMessage
 *       id="1"
 *       role="assistant"
 *       content="Here's a message with actions"
 *       actions={actions}
 *     />
 *   )
 * }
 * ```
 * 
 * @example
 * ```tsx
 * // With different animations
 * export function ChatWithAnimations() {
 *   return (
 *     <div className="space-y-4">
 *       <ChatMessage
 *         id="1"
 *         role="user"
 *         content="Slide animation"
 *         animation="slide"
 *       />
 *       <ChatMessage
 *         id="2"
 *         role="assistant"
 *         content="Scale animation"
 *         animation="scale"
 *       />
 *       <ChatMessage
 *         id="3"
 *         role="user"
 *         content="Fade animation"
 *         animation="fade"
 *       />
 *     </div>
 *   )
 * }
 * ```
 * 
 * @param role - The role of the message sender
 * @param content - The message content (supports markdown)
 * @param id - Unique identifier for the message
 * @param createdAt - Timestamp for the message
 * @param showTimeStamp - Whether to show the timestamp (default: false)
 * @param animation - Animation style for the message bubble (default: "scale")
 * @param actions - Actions to show on hover (assistant only)
 * @param attachments - Array of attached files
 * @param className - Additional CSS classes
 */
export interface ChatMessageProps {
  role: MessageRole
  content: string
  id: string
  createdAt?: Date
  showTimeStamp?: boolean
  animation?: AnimationType
  actions?: React.ReactNode
  attachments?: File[]
  className?: string
}

export function ChatMessage({
  role,
  content,
  id,
  createdAt,
  showTimeStamp = false,
  animation = "scale",
  actions,
  attachments,
  className,
}: ChatMessageProps) {
  const [isVisible, setIsVisible] = React.useState(false)
  const [isCopied, setIsCopied] = React.useState(false)

  // Trigger animation on mount
  React.useEffect(() => {
    setIsVisible(true)
  }, [])

  const handleCopy = () => {
    navigator.clipboard.writeText(content)
    setIsCopied(true)
    setTimeout(() => setIsCopied(false), 2000)
  }

  const formatTimestamp = (date: Date) => {
    return new Intl.DateTimeFormat("en-US", {
      hour: "2-digit",
      minute: "2-digit",
      hour12: true,
    }).format(date)
  }

  const getAvatarInitials = (role: MessageRole) => {
    return role === "user" ? "U" : "AI"
  }

  const getAvatarColor = (role: MessageRole) => {
    return role === "user" 
      ? "bg-primary text-primary-foreground" 
      : "bg-secondary text-secondary-foreground"
  }

  const getAnimationClasses = () => {
    if (!isVisible) {
      switch (animation) {
        case "slide":
          return role === "user" 
            ? "translate-x-full opacity-0" 
            : "-translate-x-full opacity-0"
        case "scale":
          return "scale-0 opacity-0"
        case "fade":
          return "opacity-0"
        default:
          return ""
      }
    }

    switch (animation) {
      case "slide":
        return "translate-x-0 opacity-100 transition-all duration-300 ease-out"
      case "scale":
        return "scale-100 opacity-100 transition-all duration-200 ease-out"
      case "fade":
        return "opacity-100 transition-opacity duration-300 ease-out"
      default:
        return ""
    }
  }

  return (
    <div
      className={cn(
        "flex flex-col gap-2",
        role === "user" ? "items-end" : "items-start",
        getAnimationClasses(),
        className
      )}
    >
      <div className="flex items-start gap-2 max-w-[80%] group">
        {role === "assistant" && (
          <Avatar className="h-8 w-8 mt-1">
            <AvatarImage src="" />
            <AvatarFallback className={getAvatarColor(role)}>
              {getAvatarInitials(role)}
            </AvatarFallback>
          </Avatar>
        )}
        
        <div className="flex flex-col gap-1">
          <div
            className={cn(
              "rounded-lg px-4 py-2 text-sm relative",
              role === "user"
                ? "bg-primary text-primary-foreground"
                : "bg-muted"
            )}
          >
            <p className="whitespace-pre-wrap">{content}</p>
            
            {attachments && attachments.length > 0 && (
              <div className="mt-2 flex flex-wrap gap-1">
                {attachments.map((file, index) => (
                  <div
                    key={index}
                    className="flex items-center gap-1 text-xs bg-background/50 rounded px-2 py-1"
                  >
                    <Paperclip className="h-3 w-3" />
                    <span className="truncate max-w-[100px]">{file.name}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
          
          {showTimeStamp && createdAt && (
            <time 
              className="text-xs text-muted-foreground px-1"
              dateTime={createdAt.toISOString()}
            >
              {formatTimestamp(createdAt)}
            </time>
          )}
        </div>
        
        {role === "user" && (
          <Avatar className="h-8 w-8 mt-1">
            <AvatarImage src="" />
            <AvatarFallback className={getAvatarColor(role)}>
              {getAvatarInitials(role)}
            </AvatarFallback>
          </Avatar>
        )}
      </div>
      
      {/* Actions - only show for assistant messages */}
      {role === "assistant" && actions && (
        <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity ml-10">
          {actions}
        </div>
      )}
    </div>
  )
}
