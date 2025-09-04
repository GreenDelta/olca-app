"use client"

import { useState } from "react"
import { MessageInput } from "@/components/ui/message-input"

/**
 * Basic usage example of the MessageInput component.
 * 
 * This example shows the simplest way to use the MessageInput component
 * with auto-resizing textarea and basic functionality.
 */
export function BasicMessageInput() {
  const [input, setInput] = useState("")
  const [isGenerating, setIsGenerating] = useState(false)

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (input.trim()) {
      console.log("Submitting:", input)
      setIsGenerating(true)
      // Simulate AI generation
      setTimeout(() => {
        setIsGenerating(false)
        setInput("")
      }, 2000)
    }
  }

  return (
    <div className="w-full max-w-2xl mx-auto p-4">
      <h3 className="text-lg font-semibold mb-4">Basic Message Input</h3>
      <MessageInput
        value={input}
        onChange={(e) => setInput(e.target.value)}
        isGenerating={isGenerating}
        onSubmit={handleSubmit}
        placeholder="Type your message here..."
      />
    </div>
  )
}
