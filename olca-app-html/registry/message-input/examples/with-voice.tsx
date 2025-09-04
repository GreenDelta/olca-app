"use client"

import { useState } from "react"
import { MessageInput } from "@/components/ui/message-input"

/**
 * MessageInput with voice input example.
 * 
 * This example shows how to use the MessageInput component with
 * voice input and transcription functionality.
 */
export function MessageInputWithVoice() {
  const [input, setInput] = useState("")
  const [isGenerating, setIsGenerating] = useState(false)

  // Mock transcription function
  const transcribeAudio = async (blob: Blob): Promise<string> => {
    console.log("Transcribing audio blob:", blob)
    
    // Simulate transcription delay
    await new Promise(resolve => setTimeout(resolve, 2000))
    
    // Mock transcription result
    const mockTranscriptions = [
      "Hello, how are you today?",
      "Can you help me with this problem?",
      "What is the weather like?",
      "Tell me a joke please",
      "How do I implement this feature?"
    ]
    
    const randomTranscription = mockTranscriptions[Math.floor(Math.random() * mockTranscriptions.length)]
    console.log("Transcription result:", randomTranscription)
    
    return randomTranscription
  }

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
      <h3 className="text-lg font-semibold mb-4">Message Input with Voice</h3>
      <p className="text-sm text-muted-foreground mb-4">
        Click the microphone button to start voice input. The component will simulate 
        transcription and add the result to the input field.
      </p>
      
      <MessageInput
        value={input}
        onChange={(e) => setInput(e.target.value)}
        isGenerating={isGenerating}
        onSubmit={handleSubmit}
        transcribeAudio={transcribeAudio}
        placeholder="Type your message or use voice input..."
      />
    </div>
  )
}
