"use client"

import * as React from "react"
import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"

export interface Message {
  role: "user" | "assistant"
  content: string
}

/**
 * A component that displays clickable prompt suggestions for empty chat states.
 * 
 * The PromptSuggestions component displays a grid of predefined prompts that users 
 * can click to quickly start a conversation. It's designed for empty chat states 
 * to help users get started with meaningful interactions.
 * 
 * @example
 * ```tsx
 * // Basic usage
 * import { PromptSuggestions } from "@/components/ui/prompt-suggestions"
 * 
 * export function PromptSuggestionsDemo() {
 *   const append = (message: { role: "user"; content: string }) => {
 *     // Handle appending message to chat
 *     console.log("Appending message:", message)
 *   }
 * 
 *   return (
 *     <PromptSuggestions
 *       label="Get started with some examples"
 *       append={append}
 *       suggestions={[
 *         "Tell me a joke",
 *         "What's the weather like today?",
 *         "Write a poem about cats",
 *       ]}
 *     />
 *   )
 * }
 * ```
 * 
 * @example
 * ```tsx
 * // LCA-focused suggestions
 * export function LCAPromptSuggestions() {
 *   const append = (message: Message) => {
 *     // Handle LCA-specific message
 *     handleLCAMessage(message)
 *   }
 * 
 *   return (
 *     <PromptSuggestions
 *       label="Try one of these LCA prompts!"
 *       append={append}
 *       suggestions={[
 *         "What is a lifecycle assessment?",
 *         "How do I calculate carbon footprint?",
 *         "What are Scope 1, 2, and 3 emissions?",
 *         "Help me analyze my product's environmental impact",
 *       ]}
 *     />
 *   )
 * }
 * ```
 * 
 * @param label - The heading text displayed above the suggestions
 * @param append - Function called when a suggestion is clicked
 * @param suggestions - Array of suggestion strings to display as buttons
 * @param className - Additional CSS classes
 */
export interface PromptSuggestionsProps {
  label: string
  append: (message: Message) => void
  suggestions: string[]
  className?: string
}

export function PromptSuggestions({
  label,
  append,
  suggestions,
  className,
}: PromptSuggestionsProps) {
  const handleSuggestionClick = (suggestion: string) => {
    append({
      role: "user",
      content: suggestion,
    })
  }

  return (
    <div className={cn("flex flex-col gap-4 p-8", className)}>
      <div className="text-center">
        <h3 className="text-lg font-semibold mb-2">{label}</h3>
        <p className="text-muted-foreground text-sm">
          Click on any suggestion to get started
        </p>
      </div>
      
      <div className="grid gap-2 max-w-2xl mx-auto w-full">
        {suggestions.map((suggestion, index) => (
          <Button
            key={index}
            variant="outline"
            className="h-auto p-4 text-left justify-start whitespace-normal hover:bg-accent hover:text-accent-foreground transition-colors"
            onClick={() => handleSuggestionClick(suggestion)}
          >
            <span className="text-sm">{suggestion}</span>
          </Button>
        ))}
      </div>
    </div>
  )
}
