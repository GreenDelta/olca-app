"use client"

import * as React from "react"
import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { Paperclip, Mic, Square } from "lucide-react"

/**
 * Input component for typing messages in the chat interface.
 * 
 * Supports text input, file attachments, voice input, and stop generation functionality.
 * Integrates with the chat form to handle message submission.
 * 
 * @example
 * ```tsx
 * <MessageInput
 *   value={input}
 *   onChange={handleInputChange}
 *   allowAttachments
 *   files={files}
 *   setFiles={setFiles}
 *   stop={stop}
 *   isGenerating={isGenerating}
 * />
 * ```
 * 
 * @param value - Current input value
 * @param onChange - Input change handler
 * @param allowAttachments - Whether to allow file attachments
 * @param files - Current files attached
 * @param setFiles - Function to update attached files
 * @param stop - Function to stop AI generation
 * @param isGenerating - Whether AI is currently generating
 * @param transcribeAudio - Function to transcribe audio (required for voice input)
 * @param className - Additional CSS classes
 */
export interface MessageInputProps {
  value: string
  onChange: (e: React.ChangeEvent<HTMLTextAreaElement>) => void
  allowAttachments?: boolean
  files?: File[] | null
  setFiles?: (files: File[] | null) => void
  stop?: () => void
  isGenerating?: boolean
  transcribeAudio?: (blob: Blob) => Promise<string>
  className?: string
}

export function MessageInput({
  value,
  onChange,
  allowAttachments = false,
  files,
  setFiles,
  stop,
  isGenerating = false,
  transcribeAudio,
  className,
}: MessageInputProps) {
  const [isRecording, setIsRecording] = React.useState(false)
  const textareaRef = React.useRef<HTMLTextAreaElement>(null)

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && setFiles) {
      setFiles(Array.from(e.target.files))
    }
  }

  const handleVoiceToggle = () => {
    if (isRecording) {
      // Stop recording logic here
      setIsRecording(false)
    } else {
      // Start recording logic here
      setIsRecording(true)
    }
  }

  return (
    <div className={cn("flex w-full items-end gap-2", className)}>
      {allowAttachments && (
        <div className="flex items-center gap-2">
          <input
            type="file"
            multiple
            onChange={handleFileChange}
            className="hidden"
            id="file-upload"
          />
          <Button
            type="button"
            variant="ghost"
            size="icon"
            onClick={() => document.getElementById('file-upload')?.click()}
          >
            <Paperclip className="h-4 w-4" />
          </Button>
        </div>
      )}

      <div className="flex-1">
        <Textarea
          ref={textareaRef}
          value={value}
          onChange={onChange}
          placeholder="Type your message..."
          className="min-h-[60px] resize-none"
          disabled={isGenerating}
        />
      </div>

      <div className="flex items-center gap-2">
        {transcribeAudio && (
          <Button
            type="button"
            variant="ghost"
            size="icon"
            onClick={handleVoiceToggle}
            className={cn(isRecording && "bg-red-100 text-red-600")}
          >
            <Mic className="h-4 w-4" />
          </Button>
        )}

        {isGenerating && stop ? (
          <Button type="button" variant="destructive" size="icon" onClick={stop}>
            <Square className="h-4 w-4" />
          </Button>
        ) : (
          <Button type="submit" disabled={!value.trim() || isGenerating}>
            Send
          </Button>
        )}
      </div>
    </div>
  )
}
