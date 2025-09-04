"use client"

import * as React from "react"
import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { Paperclip, Mic, Square, Send } from "lucide-react"

/**
 * A textarea component with file attachment support, auto-resizing, and drag-and-drop capabilities.
 * 
 * The MessageInput component provides a rich textarea experience with support for file attachments,
 * auto-resizing, drag-and-drop file uploads, voice input, and generation control.
 * 
 * @example
 * ```tsx
 * // Basic usage
 * import { MessageInput } from "@/components/ui/message-input"
 * 
 * export function BasicMessageInput() {
 *   const [input, setInput] = useState("")
 *   
 *   return (
 *     <MessageInput
 *       value={input}
 *       onChange={(e) => setInput(e.target.value)}
 *       isGenerating={false}
 *     />
 *   )
 * }
 * ```
 * 
 * @example
 * ```tsx
 * // With file attachments
 * export function MessageInputWithAttachments() {
 *   const [input, setInput] = useState("")
 *   const [files, setFiles] = useState<File[] | null>(null)
 * 
 *   return (
 *     <MessageInput
 *       value={input}
 *       onChange={(e) => setInput(e.target.value)}
 *       isGenerating={false}
 *       allowAttachments
 *       files={files}
 *       setFiles={setFiles}
 *     />
 *   )
 * }
 * ```
 * 
 * @example
 * ```tsx
 * // With stop generation
 * export function MessageInputWithStop() {
 *   const [input, setInput] = useState("")
 *   const [isGenerating, setIsGenerating] = useState(false)
 * 
 *   return (
 *     <MessageInput
 *       value={input}
 *       onChange={(e) => setInput(e.target.value)}
 *       isGenerating={isGenerating}
 *       stop={() => setIsGenerating(false)}
 *     />
 *   )
 * }
 * ```
 * 
 * @example
 * ```tsx
 * // With interrupt behavior
 * export function MessageInputWithInterrupt() {
 *   const [input, setInput] = useState("")
 *   const [isGenerating, setIsGenerating] = useState(false)
 * 
 *   return (
 *     <MessageInput
 *       value={input}
 *       onChange={(e) => setInput(e.target.value)}
 *       isGenerating={isGenerating}
 *       stop={() => setIsGenerating(false)}
 *       enableInterrupt={true}
 *     />
 *   )
 * }
 * ```
 * 
 * @param value - Current input value
 * @param onChange - Input change handler
 * @param isGenerating - Whether AI is generating
 * @param submitOnEnter - Whether to submit on Enter key (default: true)
 * @param stop - Function to stop generation
 * @param placeholder - Input placeholder text (default: "Ask AI...")
 * @param allowAttachments - Enable file attachments
 * @param enableInterrupt - Enable double-enter interrupt (default: true)
 * @param transcribeAudio - Function to transcribe audio
 * @param files - Currently attached files (required when allowAttachments is true)
 * @param setFiles - Files state setter (required when allowAttachments is true)
 * @param className - Additional CSS classes
 * @param onSubmit - Form submission handler
 */
export interface MessageInputProps {
  value: string
  onChange: (e: React.ChangeEvent<HTMLTextAreaElement>) => void
  isGenerating: boolean
  submitOnEnter?: boolean
  stop?: () => void
  placeholder?: string
  allowAttachments?: boolean
  enableInterrupt?: boolean
  transcribeAudio?: (blob: Blob) => Promise<string>
  files?: File[] | null
  setFiles?: React.Dispatch<React.SetStateAction<File[] | null>>
  className?: string
  onSubmit?: (e: React.FormEvent) => void
}

export function MessageInput({
  value,
  onChange,
  isGenerating,
  submitOnEnter = true,
  stop,
  placeholder = "Ask AI...",
  allowAttachments = false,
  enableInterrupt = true,
  transcribeAudio,
  files,
  setFiles,
  className,
  onSubmit,
}: MessageInputProps) {
  const [isRecording, setIsRecording] = React.useState(false)
  const [showInterruptPrompt, setShowInterruptPrompt] = React.useState(false)
  const [isDragOver, setIsDragOver] = React.useState(false)
  const textareaRef = React.useRef<HTMLTextAreaElement>(null)
  const fileInputRef = React.useRef<HTMLInputElement>(null)

  // Auto-resize textarea
  React.useEffect(() => {
    const textarea = textareaRef.current
    if (textarea) {
      textarea.style.height = "auto"
      const scrollHeight = textarea.scrollHeight
      const maxHeight = 240 // 6rem
      textarea.style.height = `${Math.min(scrollHeight, maxHeight)}px`
    }
  }, [value])

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault()
      
      if (isGenerating && enableInterrupt) {
        if (showInterruptPrompt) {
          // Second Enter press - actually stop
          stop?.()
          setShowInterruptPrompt(false)
        } else {
          // First Enter press - show interrupt prompt
          setShowInterruptPrompt(true)
          // Hide prompt after 3 seconds if not acted upon
          setTimeout(() => setShowInterruptPrompt(false), 3000)
        }
      } else if (submitOnEnter && !isGenerating) {
        onSubmit?.(e as any)
      }
    }
  }

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && setFiles) {
      setFiles(Array.from(e.target.files))
    }
  }

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault()
    setIsDragOver(false)
    
    if (allowAttachments && setFiles) {
      const droppedFiles = Array.from(e.dataTransfer.files)
      setFiles(droppedFiles)
    }
  }

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault()
    if (allowAttachments) {
      setIsDragOver(true)
    }
  }

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault()
    setIsDragOver(false)
  }

  const handleVoiceToggle = async () => {
    if (isRecording) {
      // Stop recording logic here
      setIsRecording(false)
    } else {
      // Start recording logic here
      setIsRecording(true)
    }
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!isGenerating && value.trim()) {
      onSubmit?.(e)
    }
  }

  return (
    <form onSubmit={handleSubmit} className={cn("w-full", className)}>
      <div
        className={cn(
          "flex w-full items-end gap-2 rounded-lg border p-2 transition-colors",
          isDragOver && "border-primary bg-primary/5",
          isGenerating && "opacity-75"
        )}
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
      >
        {allowAttachments && (
          <div className="flex items-center gap-2">
            <input
              ref={fileInputRef}
              type="file"
              multiple
              onChange={handleFileChange}
              className="hidden"
            />
            <Button
              type="button"
              variant="ghost"
              size="icon"
              onClick={() => fileInputRef.current?.click()}
              className="h-8 w-8"
            >
              <Paperclip className="h-4 w-4" />
            </Button>
            {files && files.length > 0 && (
              <span className="text-xs text-muted-foreground">
                {files.length} file{files.length > 1 ? 's' : ''}
              </span>
            )}
          </div>
        )}

        <div className="flex-1">
          <Textarea
            ref={textareaRef}
            value={value}
            onChange={onChange}
            onKeyDown={handleKeyDown}
            placeholder={placeholder}
            className="min-h-[40px] max-h-[240px] resize-none border-0 p-0 shadow-none focus-visible:ring-0"
            disabled={isGenerating}
          />
          
          {showInterruptPrompt && (
            <div className="mt-2 text-xs text-muted-foreground">
              Press Enter again to interrupt generation
            </div>
          )}
        </div>

        <div className="flex items-center gap-1">
          {transcribeAudio && (
            <Button
              type="button"
              variant="ghost"
              size="icon"
              onClick={handleVoiceToggle}
              className={cn(
                "h-8 w-8",
                isRecording && "bg-red-100 text-red-600 hover:bg-red-200"
              )}
            >
              <Mic className="h-4 w-4" />
            </Button>
          )}

          {isGenerating && stop ? (
            <Button 
              type="button" 
              variant="destructive" 
              size="icon" 
              onClick={stop}
              className="h-8 w-8"
            >
              <Square className="h-4 w-4" />
            </Button>
          ) : (
            <Button 
              type="submit" 
              disabled={!value.trim() || isGenerating}
              size="icon"
              className="h-8 w-8"
            >
              <Send className="h-4 w-4" />
            </Button>
          )}
        </div>
      </div>
    </form>
  )
}
