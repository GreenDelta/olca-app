"use client"

import { TypingIndicator } from "@/components/ui/typing-indicator"

/**
 * Basic usage example of the TypingIndicator component.
 * 
 * This example shows the simplest way to use the TypingIndicator component
 * with default styling.
 */
export function BasicTypingIndicator() {
  return (
    <div className="w-full max-w-2xl mx-auto p-4">
      <h3 className="text-lg font-semibold mb-4">Basic Typing Indicator</h3>
      <p className="text-sm text-muted-foreground mb-4">
        A simple animated typing indicator with three bouncing dots.
      </p>
      
      <div className="flex items-center justify-center p-8 bg-muted rounded-lg">
        <TypingIndicator />
      </div>
    </div>
  )
}
