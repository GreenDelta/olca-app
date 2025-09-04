"use client"

import { useState } from "react"
import { MessageInput } from "@/components/ui/message-input"

/**
 * MessageInput with stop generation example.
 * 
 * This example shows how to use the MessageInput component with
 * stop generation functionality during AI processing.
 */
export function MessageInputWithStop() {
  const [input, setInput] = useState("")
  const [isGenerating, setIsGenerating] = useState(false)

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (input.trim()) {
      console.log("Starting generation for:", input)
      setIsGenerating(true)
      
      // Simulate AI generation that can be stopped
      const timeoutId = setTimeout(() => {
        setIsGenerating(false)
        setInput("")
        console.log("Generation completed")
      }, 5000) // 5 second generation

      // Store timeout ID to clear it if stopped
      ;(window as any).currentTimeoutId = timeoutId
    }
  }

  const handleStop = () => {
    console.log("Stopping generation...")
    setIsGenerating(false)
    // Clear the timeout if it exists
    if ((window as any).currentTimeoutId) {
      clearTimeout((window as any).currentTimeoutId)
      ;(window as any).currentTimeoutId = null
    }
  }

  return (
    <div className="w-full max-w-2xl mx-auto p-4">
      <h3 className="text-lg font-semibold mb-4">Message Input with Stop</h3>
      <p className="text-sm text-muted-foreground mb-4">
        Submit a message to start generation, then use the stop button to interrupt it.
      </p>
      
      <MessageInput
        value={input}
        onChange={(e) => setInput(e.target.value)}
        isGenerating={isGenerating}
        onSubmit={handleSubmit}
        stop={handleStop}
        placeholder="Type your message here..."
      />
    </div>
  )
}
