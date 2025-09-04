"use client"

import * as React from "react"
import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import { Message } from "./chat"

/**
 * Component for displaying prompt suggestions when the chat is empty.
 * 
 * Shows a list of suggested prompts that users can click to quickly
 * start a conversation. Integrates with the append function to add
 * messages to the chat.
 * 
 * @example
 * ```tsx
 * <PromptSuggestions
 *   append={append}
 *   suggestions={[
 *     "What is the capital of France?",
 *     "Tell me a joke",
 *     "Explain quantum computing"
 *   ]}
 * />
 * ```
 * 
 * @param append - Function to append a new message to the chat
 * @param suggestions - Array of prompt suggestions to display
 * @param className - Additional CSS classes
 */
export interface PromptSuggestionsProps {
  append: (message: Message) => void
  suggestions: string[]
  className?: string
}

export function PromptSuggestions({ 
  append, 
  suggestions, 
  className 
}: PromptSuggestionsProps) {
  const handleSuggestionClick = (suggestion: string) => {
    append({
      id: Date.now().toString(),
      role: "user",
      content: suggestion,
      createdAt: new Date(),
    })
  }

  return (
    <div className={cn("flex flex-col gap-4 p-8", className)}>
      <div className="text-center">
        <h3 className="text-lg font-semibold mb-2">Try these prompts ✨</h3>
        <p className="text-muted-foreground text-sm">
          Click on any suggestion to get started
        </p>
      </div>
      
      <div className="grid gap-2 max-w-2xl mx-auto w-full">
        {suggestions.map((suggestion, index) => (
          <Button
            key={index}
            variant="outline"
            className="h-auto p-4 text-left justify-start whitespace-normal"
            onClick={() => handleSuggestionClick(suggestion)}
          >
            <span className="text-sm">{suggestion}</span>
          </Button>
        ))}
      </div>
    </div>
  )
}
