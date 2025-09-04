"use client"

import * as React from "react"
import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { ThumbsUp, ThumbsDown, Copy, Edit, Trash2, Paperclip } from "lucide-react"
import { Message, AdditionalMessageOptions } from "./message-list"

/**
 * Individual chat message component with customizable options.
 * 
 * Renders a single chat message with support for avatars, timestamps,
 * rating buttons, copy functionality, and file attachments.
 * 
 * @param message - The message object to display
 * @param showTimestamp - Whether to show the message timestamp
 * @param showAvatar - Whether to show the user avatar
 * @param onRateResponse - Callback for rating responses
 * @param onCopy - Callback for copying message content
 * @param onEdit - Callback for editing messages
 * @param onDelete - Callback for deleting messages
 * @param className - Additional CSS classes
 */
export interface ChatMessageProps {
  message: Message
  showTimestamp?: boolean
  showAvatar?: boolean
  onRateResponse?: (messageId: string, rating: "thumbs-up" | "thumbs-down") => void
  onCopy?: (content: string) => void
  onEdit?: (messageId: string, newContent: string) => void
  onDelete?: (messageId: string) => void
  className?: string
}

export function ChatMessage({
  message,
  showTimestamp = true,
  showAvatar = false,
  onRateResponse,
  onCopy,
  onEdit,
  onDelete,
  className,
}: ChatMessageProps) {
  const [isCopied, setIsCopied] = React.useState(false)

  const handleCopy = () => {
    if (onCopy) {
      onCopy(message.content)
    } else {
      navigator.clipboard.writeText(message.content)
    }
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

  const getAvatarInitials = (role: string) => {
    return role === "user" ? "U" : "AI"
  }

  const getAvatarColor = (role: string) => {
    return role === "user" 
      ? "bg-primary text-primary-foreground" 
      : "bg-secondary text-secondary-foreground"
  }

  return (
    <div
      className={cn(
        "flex flex-col gap-2",
        message.role === "user" ? "items-end" : "items-start",
        className
      )}
    >
      <div className="flex items-start gap-2 max-w-[80%]">
        {showAvatar && message.role === "assistant" && (
          <Avatar className="h-8 w-8 mt-1">
            <AvatarImage src="" />
            <AvatarFallback className={getAvatarColor(message.role)}>
              {getAvatarInitials(message.role)}
            </AvatarFallback>
          </Avatar>
        )}
        
        <div className="flex flex-col gap-1">
          <div
            className={cn(
              "rounded-lg px-4 py-2 text-sm",
              message.role === "user"
                ? "bg-primary text-primary-foreground"
                : "bg-muted"
            )}
          >
            <p className="whitespace-pre-wrap">{message.content}</p>
            
            {message.attachments && message.attachments.length > 0 && (
              <div className="mt-2 flex flex-wrap gap-1">
                {message.attachments.map((file, index) => (
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
          
          {showTimestamp && message.createdAt && (
            <div className="text-xs text-muted-foreground px-1">
              {formatTimestamp(message.createdAt)}
            </div>
          )}
        </div>
        
        {showAvatar && message.role === "user" && (
          <Avatar className="h-8 w-8 mt-1">
            <AvatarImage src="" />
            <AvatarFallback className={getAvatarColor(message.role)}>
              {getAvatarInitials(message.role)}
            </AvatarFallback>
          </Avatar>
        )}
      </div>
      
      {/* Message actions */}
      <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
        {onCopy && (
          <Button
            variant="ghost"
            size="sm"
            onClick={handleCopy}
            className="h-6 w-6 p-0"
          >
            <Copy className="h-3 w-3" />
          </Button>
        )}
        
        {message.role === "assistant" && onRateResponse && (
          <>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => onRateResponse(message.id, "thumbs-up")}
              className="h-6 w-6 p-0"
            >
              <ThumbsUp className="h-3 w-3" />
            </Button>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => onRateResponse(message.id, "thumbs-down")}
              className="h-6 w-6 p-0"
            >
              <ThumbsDown className="h-3 w-3" />
            </Button>
          </>
        )}
        
        {onEdit && (
          <Button
            variant="ghost"
            size="sm"
            onClick={() => onEdit(message.id, message.content)}
            className="h-6 w-6 p-0"
          >
            <Edit className="h-3 w-3" />
          </Button>
        )}
        
        {onDelete && (
          <Button
            variant="ghost"
            size="sm"
            onClick={() => onDelete(message.id)}
            className="h-6 w-6 p-0 text-destructive hover:text-destructive"
          >
            <Trash2 className="h-3 w-3" />
          </Button>
        )}
      </div>
    </div>
  )
}
