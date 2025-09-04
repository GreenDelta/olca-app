"use client"

import { PromptSuggestions } from "@/components/ui/prompt-suggestions"

/**
 * PromptSuggestions with custom label example.
 * 
 * This example shows how to use the PromptSuggestions component with
 * custom label text to provide context-specific guidance.
 */
export function CustomLabelPromptSuggestions() {
  const append = (message: { role: "user"; content: string }) => {
    console.log("Appending message:", message)
    // In a real app, this would add the message to your chat state
  }

  return (
    <div className="w-full max-w-2xl mx-auto p-4">
      <h3 className="text-lg font-semibold mb-4">Custom Label Prompt Suggestions</h3>
      <p className="text-sm text-muted-foreground mb-4">
        Custom label text can provide specific context or instructions for users.
      </p>
      
      <PromptSuggestions
        label="Try one of these prompts!"
        append={append}
        suggestions={[
          "What is the capital of France?",
          "Who won the 2022 FIFA World Cup?",
          "Give me a vegan lasagna recipe for 3 people.",
          "Explain step-by-step how to solve this math problem: If x² + 6x + 9 = 25, what is x?",
          "Design a simple algorithm to find the longest palindrome in a string.",
          "What are the main environmental impacts of plastic production?",
        ]}
      />
    </div>
  )
}
