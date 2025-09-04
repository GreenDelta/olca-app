"use client"

import { PromptSuggestions } from "@/components/ui/prompt-suggestions"

/**
 * LCA-focused prompt suggestions example.
 * 
 * This example shows how to use the PromptSuggestions component with
 * LCA (Life Cycle Assessment) and environmental analysis focused prompts.
 */
export function LCAFocusedPromptSuggestions() {
  const append = (message: { role: "user"; content: string }) => {
    console.log("Appending LCA message:", message)
    // In a real app, this would add the message to your LCA chat state
  }

  return (
    <div className="w-full max-w-2xl mx-auto p-4">
      <h3 className="text-lg font-semibold mb-4">LCA-Focused Prompt Suggestions</h3>
      <p className="text-sm text-muted-foreground mb-4">
        Specialized prompts for Life Cycle Assessment and environmental analysis.
      </p>
      
      <PromptSuggestions
        label="Start your LCA analysis with these prompts!"
        append={append}
        suggestions={[
          "What is a lifecycle assessment and how does it work?",
          "How do I calculate the carbon footprint of my product?",
          "What are Scope 1, 2, and 3 emissions?",
          "Help me analyze my product's environmental impact",
          "What are the main environmental impacts of plastic production?",
          "How can I reduce the environmental impact of my supply chain?",
          "What is the difference between cradle-to-gate and cradle-to-grave LCA?",
          "How do I choose the right functional unit for my LCA study?",
          "What are the key steps in conducting a lifecycle assessment?",
          "How can I interpret LCA results and make decisions?",
        ]}
      />
    </div>
  )
}
