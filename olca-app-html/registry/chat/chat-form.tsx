"use client"

import * as React from "react"
import { cn } from "@/lib/utils"

/**
 * A form component that wraps the message input and submit button.
 * 
 * Handles the state management for file uploads internally and uses a render 
 * function to pass them down to your input component. Used to build your own 
 * chat if not using the default Chat component.
 * 
 * @example
 * ```tsx
 * <ChatForm
 *   className="mt-auto"
 *   isPending={isLoading}
 *   handleSubmit={handleSubmit}
 * >
 *   {({ files, setFiles }) => (
 *     <MessageInput
 *       value={input}
 *       onChange={handleInputChange}
 *       allowAttachments
 *       files={files}
 *       setFiles={setFiles}
 *     />
 *   )}
 * </ChatForm>
 * ```
 * 
 * @param isPending - Whether form submission is pending
 * @param handleSubmit - Form submission handler
 * @param className - Additional CSS classes
 * @param children - Render function for form content that receives files and setFiles
 */
export interface ChatFormProps {
  isPending: boolean
  handleSubmit: (event?: React.FormEvent, options?: any) => void
  className?: string
  children: (props: { 
    files: File[] | null
    setFiles: (files: File[] | null) => void 
  }) => React.ReactElement
}

export function ChatForm({ 
  isPending, 
  handleSubmit, 
  className, 
  children 
}: ChatFormProps) {
  const [files, setFiles] = React.useState<File[] | null>(null)

  return (
    <form 
      onSubmit={handleSubmit}
      className={cn("flex w-full items-end gap-2 p-4", className)}
    >
      {children({ files, setFiles })}
    </form>
  )
}
