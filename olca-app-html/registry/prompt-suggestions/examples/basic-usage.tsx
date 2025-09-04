"use client"

import { PromptSuggestions } from "@/components/ui/prompt-suggestions"

/**
 * Basic usage example of the PromptSuggestions component.
 * 
 * This example shows the simplest way to use the PromptSuggestions component
 * with a basic set of general conversation starters.
 */
export function BasicPromptSuggestions() {
  const append = (message: { role: "user"; content: string }) => {
    console.log("Appending message:", message)
    // In a real app, this would add the message to your chat state
  }

  return (
    <div className="w-full max-w-2xl mx-auto p-4">
      <h3 className="text-lg font-semibold mb-4">Basic Prompt Suggestions</h3>
      <p className="text-sm text-muted-foreground mb-4">
        Click on any suggestion to see it added to the chat (check console for output).
      </p>
      
      <PromptSuggestions
        label="Get started with some examples"
        append={append}
        suggestions={[
          "Tell me a joke",
          "What's the weather like today?",
          "Write a poem about cats",
          "Explain quantum computing in simple terms",
          "Help me plan a healthy meal",
          "What are the benefits of renewable energy?",
        ]}
      />
    </div>
  )
}
