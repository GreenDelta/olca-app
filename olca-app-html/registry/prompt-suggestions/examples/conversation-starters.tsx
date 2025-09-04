"use client"

import { PromptSuggestions } from "@/components/ui/prompt-suggestions"

/**
 * Conversation starters prompt suggestions example.
 * 
 * This example shows how to use the PromptSuggestions component with
 * various conversation starter prompts for different use cases.
 */
export function ConversationStartersPromptSuggestions() {
  const append = (message: { role: "user"; content: string }) => {
    console.log("Appending conversation starter:", message)
    // In a real app, this would add the message to your chat state
  }

  return (
    <div className="w-full max-w-2xl mx-auto p-4">
      <h3 className="text-lg font-semibold mb-4">Conversation Starters</h3>
      <p className="text-sm text-muted-foreground mb-4">
        A variety of conversation starters for different topics and use cases.
      </p>
      
      <PromptSuggestions
        label="Choose a topic to explore!"
        append={append}
        suggestions={[
          "Generate a tasty vegan lasagna recipe for 3 people.",
          "Generate a list of 5 questions for a frontend job interview.",
          "Who won the 2022 FIFA World Cup?",
          "What is the weather in San Francisco?",
          "Explain step-by-step how to solve this math problem: If x² + 6x + 9 = 25, what is x?",
          "Design a simple algorithm to find the longest palindrome in a string.",
          "Help me write a professional email to my manager about a project delay.",
          "What are the key principles of sustainable design?",
          "Create a workout plan for someone who wants to get back into fitness.",
          "Explain the basics of machine learning in simple terms.",
        ]}
      />
    </div>
  )
}
