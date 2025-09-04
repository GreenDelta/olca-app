"use client"

import * as React from "react"
import { cn } from "@/lib/utils"

export type TypingIndicatorSize = "sm" | "md" | "lg"
export type TypingIndicatorColor = "default" | "primary" | "muted"

/**
 * A simple animated typing indicator component.
 * 
 * A minimal and elegant typing indicator component that shows three animated dots 
 * to indicate ongoing activity, commonly used in chat interfaces to show when 
 * someone is typing or when AI is generating a response.
 * 
 * @example
 * ```tsx
 * // Basic usage
 * import { TypingIndicator } from "@/components/ui/typing-indicator"
 * 
 * export function Message({ isPending, content }: { isPending: boolean; content: string }) {
 *   return isPending ? <TypingIndicator /> : <p>{content}</p>
 * }
 * ```
 * 
 * @example
 * ```tsx
 * // With custom styling
 * export function CustomTypingIndicator() {
 *   return (
 *     <TypingIndicator 
 *       size="lg" 
 *       color="primary" 
 *       className="my-4" 
 *     />
 *   )
 * }
 * ```
 * 
 * @example
 * ```tsx
 * // In a chat message
 * export function ChatMessage({ isTyping, content }: { isTyping: boolean; content: string }) {
 *   return (
 *     <div className="flex items-start gap-2">
 *       <div className="bg-muted rounded-lg px-4 py-2">
 *         {isTyping ? (
 *           <TypingIndicator size="sm" />
 *         ) : (
 *           <p>{content}</p>
 *         )}
 *       </div>
 *     </div>
 *   )
 * }
 * ```
 * 
 * @param size - Size of the typing indicator (default: "md")
 * @param color - Color variant (default: "default")
 * @param className - Additional CSS classes
 */
export interface TypingIndicatorProps {
  size?: TypingIndicatorSize
  color?: TypingIndicatorColor
  className?: string
}

export function TypingIndicator({
  size = "md",
  color = "default",
  className,
}: TypingIndicatorProps) {
  const getSizeClasses = () => {
    switch (size) {
      case "sm":
        return "h-1 w-1"
      case "lg":
        return "h-3 w-3"
      default: // md
        return "h-2 w-2"
    }
  }

  const getColorClasses = () => {
    switch (color) {
      case "primary":
        return "bg-primary"
      case "muted":
        return "bg-muted-foreground"
      default:
        return "bg-muted-foreground"
    }
  }

  return (
    <div 
      className={cn("flex items-center gap-1", className)}
      role="status"
      aria-label="Typing indicator"
    >
      <div
        className={cn(
          "rounded-full animate-bounce",
          getSizeClasses(),
          getColorClasses()
        )}
        style={{ animationDelay: "0ms" }}
      />
      <div
        className={cn(
          "rounded-full animate-bounce",
          getSizeClasses(),
          getColorClasses()
        )}
        style={{ animationDelay: "150ms" }}
      />
      <div
        className={cn(
          "rounded-full animate-bounce",
          getSizeClasses(),
          getColorClasses()
        )}
        style={{ animationDelay: "300ms" }}
      />
    </div>
  )
}
