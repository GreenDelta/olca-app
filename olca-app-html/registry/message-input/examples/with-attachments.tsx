"use client"

import { useState } from "react"
import { MessageInput } from "@/components/ui/message-input"
import { Button } from "@/components/ui/button"
import { X } from "lucide-react"

/**
 * MessageInput with file attachments example.
 * 
 * This example shows how to use the MessageInput component with file
 * attachment support, including drag-and-drop functionality.
 */
export function MessageInputWithAttachments() {
  const [input, setInput] = useState("")
  const [files, setFiles] = useState<File[] | null>(null)
  const [isGenerating, setIsGenerating] = useState(false)

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (input.trim() || files) {
      console.log("Submitting:", { input, files })
      setIsGenerating(true)
      // Simulate AI generation
      setTimeout(() => {
        setIsGenerating(false)
        setInput("")
        setFiles(null)
      }, 2000)
    }
  }

  const removeFile = (index: number) => {
    if (files) {
      const newFiles = files.filter((_, i) => i !== index)
      setFiles(newFiles.length > 0 ? newFiles : null)
    }
  }

  return (
    <div className="w-full max-w-2xl mx-auto p-4">
      <h3 className="text-lg font-semibold mb-4">Message Input with Attachments</h3>
      
      {files && files.length > 0 && (
        <div className="mb-4 p-3 bg-muted rounded-lg">
          <h4 className="text-sm font-medium mb-2">Attached Files:</h4>
          <div className="space-y-2">
            {files.map((file, index) => (
              <div key={index} className="flex items-center justify-between text-sm">
                <span className="truncate">{file.name}</span>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => removeFile(index)}
                  className="h-6 w-6 p-0"
                >
                  <X className="h-3 w-3" />
                </Button>
              </div>
            ))}
          </div>
        </div>
      )}

      <MessageInput
        value={input}
        onChange={(e) => setInput(e.target.value)}
        isGenerating={isGenerating}
        onSubmit={handleSubmit}
        allowAttachments
        files={files}
        setFiles={setFiles}
        placeholder="Type your message or drag files here..."
      />
    </div>
  )
}
